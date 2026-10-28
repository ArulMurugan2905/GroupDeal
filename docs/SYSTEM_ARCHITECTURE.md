# System Architecture

## 1. High-level architecture

```mermaid
flowchart LR
  subgraph Clients
    A[Android customer app<br/>Kotlin + Compose]
    W[Web: supplier portal,<br/>admin console, share pages<br/>Next.js]
  end
  subgraph Firebase["Firebase project (asia-south1)"]
    AUTH[Authentication]
    AC[App Check]
    FS[(Cloud Firestore)]
    CF[Cloud Functions 2nd gen<br/>callables, triggers, schedulers, webhook]
    ST[(Cloud Storage)]
    FCM[Cloud Messaging]
    SM[Secret Manager]
  end
  subgraph External
    RZ[Razorpay]
    GM[Google Maps Platform<br/>Places / Geocoding]
    PS[Price sources<br/>FUTURE adapters]
  end
  A -- sign-in --> AUTH
  W -- sign-in --> AUTH
  A -- reads/listeners --> FS
  W -- reads/listeners --> FS
  A -- callables + App Check --> CF
  W -- callables + App Check --> CF
  CF -- Admin SDK transactions --> FS
  CF --> FCM --> A
  CF -- orders, refunds --> RZ
  RZ -- webhooks --> CF
  CF -- reverse geocode --> GM
  A -- Places autocomplete --> GM
  CF --> SM
  A -- images --> ST
  W -- uploads (docs, images) --> ST
  CF -. FUTURE .-> PS
```

## 2. Logical architecture (bounded contexts)

| Context | Owns (collections) | Key operations |
|---|---|---|
| Identity | `users`, claims, `suppliers` | sign-in hooks, roles, supplier verification |
| Locality | `localities` | resolve, search, set user locality |
| Catalogue | `deals` | propose, approve, schedule groups |
| Group buying | `groups`, `groups/*/members`, `priceHistory`, `activity` | join, leave, lock, state |
| Pricing | `@groupdeal/pricing` (pure) | quote, validate, final price |
| Auction | `auctions`, `bids`, `revisions` | open, bid, close, award |
| Orders | `orders` | create at award, track, cancel |
| Payments | `payments`, `paymentEvents` | create order, verify, webhook, refund, reconcile |
| Engagement | `requests`, `votes`, `users/*/watchlist`, `users/*/notifications` | request, vote, notify |
| Trust & safety | `ratings`, `disputes`, `fraudSignals`, `adminActions` | rate, dispute, signal, audit |

## 3. Trust boundaries

| Value / operation | Trusted source | Never accepted from a client |
|---|---|---|
| Member count, units committed | Firestore transaction in `joinGroup`/`leaveGroup` | Any count, tier or price |
| Unit price, tier, savings | `@groupdeal/pricing` inside the function | Client-computed price |
| Locked/final price, order amount | Lock and award functions | Amount in `createPaymentOrder` |
| Payment success | Server signature check + Razorpay fetch, or verified webhook | SDK success callback alone |
| Auction winner | `closeAuctions` scheduled function | Supplier-declared result |
| Role, supplier status | Custom claims set by admin functions | Role field in profile |
| Trust score | Server recompute | Any client value |
| Locality of a user | Server-set `localityId` | Arbitrary locality in join requests (server reads profile) |
| Timestamps | `FieldValue.serverTimestamp()` | Client clocks |

**Authorisation happens in two places:** Firestore rules (reads, self-owned writes) and function guards (`requireAuth`, `requireRole`, `requireVerifiedSupplier`, ownership checks). App Check is enforced on both.

## 4. Responsibilities

**Android client:** render state, collect input, call functions with `requestId`, show estimates labelled as such, cache, handle Razorpay checkout UI, register FCM token.
**Web client:** supplier/admin UIs, SSR public share pages (read-only Admin SDK), same callables.
**Functions:** all invariants, money, deadlines, notifications, external APIs, audit, idempotency.

## 5. Core flows

### 5.1 Authentication
```mermaid
sequenceDiagram
  participant U as User
  participant App
  participant Auth as Firebase Auth
  participant F as Functions
  participant DB as Firestore
  U->>App: phone number
  App->>Auth: verifyPhoneNumber
  Auth-->>U: SMS OTP
  U->>App: OTP
  App->>Auth: signInWithCredential
  Auth-->>F: beforeUserCreated (blocking) / onUserCreated trigger
  F->>DB: users/{uid} {role: customer, trust: 500}
  F->>Auth: setCustomUserClaims {role: customer}
  App->>Auth: getIdToken(forceRefresh)
  App->>DB: read users/{uid}
```

### 5.2 Join group (authoritative)
```mermaid
sequenceDiagram
  participant App
  participant F as joinGroup
  participant DB as Firestore
  App->>F: {groupId, quantity, requestId} + ID token + App Check
  F->>DB: tx.get(idempotencyKeys/uid_requestId)
  alt already processed
    F-->>App: stored response
  else new
    F->>DB: tx.get(groups/gid), tx.get(members/uid), tx.get(users/uid)
    F->>F: check state ∈ {OPEN,FILLING,TARGET_REACHED}, locality match, capacity, join limit
    F->>F: quote(schedule, n+1)
    F->>DB: tx.set(member), tx.update(group: count, units, tier, price, state, version+1), tx.set(idempotency)
    F-->>App: {memberCount, unitPricePaise, tierIndex, membersToNextTier, state}
  end
  DB-->>App: listener update (all members)
```

### 5.3 Pricing / tier change side effects
```mermaid
flowchart LR
  J[join/leave transaction] -->|tier changed| PH[priceHistory doc]
  PH --> T[onPriceHistoryCreated trigger]
  T --> N[FCM topic group_gid + inbox docs]
  J --> ACT[activity doc: anonymised 'someone joined']
```

### 5.4 Purchase lifecycle
```mermaid
flowchart TD
  O[OPEN/FILLING/TARGET_REACHED] -->|closesAt or capacity| L{memberCount ≥ minMembersToProceed?}
  L -- no --> FAIL[FAILED: MIN_NOT_REACHED, no charges]
  L -- yes --> LK[LOCKED: lockedUnitPrice = ceiling]
  LK -->|auctionEnabled| B[BIDDING]
  LK -->|no auction| SS[SUPPLIER_SELECTED = anchor @ ceiling]
  B -->|deadline| C{valid bids?}
  C -- yes --> SS2[SUPPLIER_SELECTED = lowest bid]
  C -- no, anchor exists --> SS
  C -- no anchor --> FAIL2[FAILED: NO_SUPPLIER]
  SS --> PW[PAYMENT_WINDOW: orders AWAITING_PAYMENT]
  SS2 --> PW
  PW -->|window ends| P{paidUnits ≥ minUnits?}
  P -- no --> FAIL3[FAILED: INSUFFICIENT_PAYMENT → refunds]
  P -- yes --> FF[FULFILLING]
  FF -->|all orders closed| DONE[COMPLETED]
```

### 5.5 Supplier auction
```mermaid
sequenceDiagram
  participant S as Supplier (web)
  participant F as Functions
  participant DB as Firestore
  F->>DB: lock: auctions/gid {ceiling, closesAt, OPEN}
  F->>S: FCM/email "new opportunity" (served localities)
  S->>F: submitBid {gid, unitPricePaise, deliveryDays, minUnits, requestId}
  F->>F: verify claims, service area, ceiling ≥ price ≥ floor, deadline, revisions < 10
  F->>DB: tx set bids/sid (+ revision doc), auction.bidCount
  Note over F: scheduled closeAuctions (every 5 min)
  F->>DB: tx: rank valid bids, mark WON/LOST, group SUPPLIER_SELECTED, finalUnitPrice
  F->>DB: create orders (AWAITING_PAYMENT)
  F->>S: notify won/lost
```

### 5.6 Payment
```mermaid
sequenceDiagram
  participant App
  participant F as Functions
  participant RZ as Razorpay
  participant DB as Firestore
  App->>F: createPaymentOrder {orderId, requestId}
  F->>DB: read order (owner, AWAITING_PAYMENT, before paymentDueAt)
  F->>RZ: POST /orders {amount from DB, currency INR, receipt=orderId}
  F->>DB: payments/pid CREATED {razorpayOrderId}
  F-->>App: {razorpayOrderId, amount, keyId}
  App->>RZ: Checkout (UPI/card)
  RZ-->>App: payment_id, order_id, signature
  App->>F: verifyPayment {...}
  F->>F: HMAC_SHA256(order_id|payment_id, key_secret) == signature
  F->>RZ: GET /payments/{id} (status, amount)
  F->>DB: tx: payment CAPTURED, order PAID (idempotent)
  RZ->>F: webhook payment.captured / order.paid (X-Razorpay-Signature)
  F->>DB: tx: same transition if not yet applied (paymentEvents dedupe)
```

### 5.7 Notifications
```mermaid
flowchart LR
  E[Domain event: tier change, lock, award, payment, dispatch] --> F[Trigger function]
  F --> I[users/uid/notifications doc]
  F --> T{audience}
  T -- broadcast --> TOP[FCM topic group_gid]
  T -- personal --> TOK[FCM tokens from users/uid/devices]
  TOK -->|invalid token error| CL[delete device doc]
```

## 6. Component map (deployable units)

| Unit | Tech | Deploy |
|---|---|---|
| `android` | Kotlin, Compose, Hilt, Firebase SDKs, Razorpay SDK, Places SDK | Play Console (internal → closed → production) |
| `web` | Next.js (App Router), TypeScript, Tailwind, Firebase JS SDK, Admin SDK (server) | Firebase App Hosting |
| `functions` | TypeScript, Node 20, firebase-functions v2, firebase-admin, razorpay | `firebase deploy --only functions` |
| `packages/pricing` | TypeScript, zero dependencies | Built into functions and web |
| `firestore` | rules, indexes, rules tests | `firebase deploy --only firestore` |
