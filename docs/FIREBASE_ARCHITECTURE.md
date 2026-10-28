# Firebase Architecture

## 1. Projects and environments
| Project ID (proposed) | Purpose | Data |
|---|---|---|
| `groupdeal-dev` | Local development against emulators + shared dev | Synthetic |
| `groupdeal-staging` | Pre-release testing, Razorpay **test** keys | Synthetic |
| `groupdeal-prod` | Production, Razorpay **live** keys | Real |
Each has its own `google-services.json` (Android flavor), web config, App Check providers and secrets. Production access is limited to two maintainers with MFA.

## 2. Services used
| Service | Use | Notes |
|---|---|---|
| Authentication | Phone OTP, Google; blocking functions for claims | SMS quota/billing on Blaze |
| Firestore (Native, asia-south1) | All application data | PITR enabled in prod (verify plan) |
| Cloud Functions 2nd gen (Node 20, TS) | Callables, triggers, schedulers, webhook | Secrets via `defineSecret` |
| Cloud Scheduler | Sweepers (5 min), reconciliation (daily) | Created by `onSchedule` |
| Cloud Storage | Images, supplier documents, dispute photos | Storage rules |
| Cloud Messaging | Push | Topics + tokens |
| App Check | Play Integrity (Android), reCAPTCHA Enterprise (web) | Enforced in staging & prod |
| Crashlytics, Performance, Analytics | Android/web telemetry | No PII in events |
| App Hosting | Next.js web | Separate backends per env |
| Secret Manager | Razorpay key secret, webhook secret, Maps server key | Never in code or client |

## 3. Function inventory
| Name | Type | Trigger | Purpose |
|---|---|---|---|
| `onUserCreated` | Auth (blocking `beforeUserCreated` + doc create) | sign-up | create `users/{uid}`, claim `role=customer` |
| `updateProfile` | callable | client | display name, photo, preferences |
| `resolveLocality` | callable | client | lat/lng → nearest active locality (server Geocoding) |
| `setUserLocality` | callable | client | set locality + collection point, rate-limited |
| `joinGroup` / `leaveGroup` / `setQuantity` | callable | client | transactional membership |
| `createRequest` / `voteRequest` / `unvoteRequest` | callable | client | demand |
| `toggleWatch` | callable | client | optional (rules allow direct write too) |
| `createPaymentOrder` / `verifyPayment` | callable | client | payments |
| `cancelOrder` | callable | client | before payment only |
| `submitRating` / `raiseDispute` | callable | client | trust & safety |
| `supplierRegister` / `supplierUpdateServiceAreas` / `supplierProposeDeal` | callable | web | supplier |
| `submitBid` / `withdrawBid` | callable | web | auction |
| `supplierUpdateFulfilment` | callable | web | order tracking |
| `admin*` (VerifySupplier, SetRole, ApproveDeal, OpenGroup, CancelGroup, ExtendGroup, ConvertRequest, InvalidateBid, RefundOrder, ResolveDispute, UpsertLocality, UpdateConfig, SuspendUser) | callable | web | admin, all audited (full names `adminVerifySupplier` …) |
| `sweepGroups` | schedule 5 min | | lock due groups, fail below minimum, open auctions |
| `sweepAuctions` | schedule 5 min | | close due auctions, award, create orders |
| `sweepPaymentWindows` | schedule 5 min | | default unpaid, fail/advance groups, trigger refunds |
| `reconcilePayments` | schedule daily 02:00 IST | | Razorpay ↔ Firestore reconciliation |
| `razorpayWebhook` | HTTPS onRequest | Razorpay | verified events |
| `onPriceHistoryCreated` | Firestore trigger | tier change | notify topic + watchers |
| `onOrderStatusChanged` | Firestore trigger | order update | notify customer/supplier |
| `onRatingCreated` | Firestore trigger | rating | update supplier score, customer trust |
| `onRequestThresholdReached` | Firestore trigger | request update | notify admins |
| `detectFraudSignals` | Firestore triggers + schedule | votes/joins/payments | write `fraudSignals` |
| `rollupDailyStats` | schedule daily 01:00 IST | | analytics roll-ups into `stats/daily` |
| `createOrdersForGroup` | internal (called after award) | | idempotent order creation, IDs `{gid}_{uid}` |

All callables: `region: 'asia-south1'`, `enforceAppCheck: true` (staging/prod), `consumeAppCheckToken: true` for payment and bid functions (replay protection), input validated with a schema library (zod), structured logging with `requestId`.

## 4. Sweeper design (idempotent)
Each sweep queries due documents (`state == X && closesAt <= now`, limit 200), processes each in its **own transaction** that re-reads and re-checks the condition, so overlapping runs and retries are safe. Long backlogs are drained over successive runs. A per-document `processingLease` field is not needed because transactions are the guard.

## 5. Security layers
1. Firebase Auth (identity) → 2. App Check (genuine client) → 3. Firestore/Storage rules (read/self-write scope) → 4. Function guards (role, ownership, state) → 5. Transactions (invariants) → 6. Audit logs. See SECURITY.md.

## 6. Emulator-first development
`firebase emulators:start --only auth,firestore,functions,storage` with seeded data (`/scripts/seed.ts`), App Check debug provider, Razorpay calls stubbed by an injected `PaymentGateway` interface (real test-mode calls only in staging).
