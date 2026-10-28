# Decisions, Assumptions and Architectural Decision Records

This file is the tie-breaker. If any document contradicts this file, this file wins and the other document is a bug.

## 1. Engineering decisions (D-xx)

| ID | Decision |
|---|---|
| D-01 | Monorepo: `/android`, `/web`, `/functions`, `/packages/pricing`, `/firestore` (rules, indexes, tests), `/docs`, `/scripts`, `CLAUDE.md`. |
| D-02 | All Firebase resources in **asia-south1 (Mumbai)**. |
| D-03 | **Money is integer paise** everywhere (₹949 = `94900`). No floating-point money in storage, APIs or logic. Savings % = `floor((mrp − price) × 100 / mrp)`. |
| D-04 | **Deal** = offer template (product, MRP, tier schedule, rules). **Group** = one buying round of a deal in one locality with a deadline. Prices, members and auctions belong to groups. |
| D-05 | Tier thresholds count **active members**, not units. Each member buys 1..`maxQtyPerMember` units (default 3) at the group unit price. `unitsCommitted` is tracked for suppliers. |
| D-06 | **Pay after lock.** No money moves while a group is open. Members pay the final price after the auction. |
| D-07 | A group **locks** at `closesAt` or when `capacity` is reached. At lock, if `memberCount < minMembersToProceed` (default = first tier threshold) the group FAILS and nobody is charged. |
| D-08 | At lock the current tier price becomes the **ceiling**. Sealed-bid, first-price **reverse auction**; lowest valid bid wins; `finalUnitPrice = winningBid ≤ ceiling`. The deal's **anchor supplier** (if any) is obliged to deliver at the ceiling when no valid bid arrives. |
| D-09 | Members who don't pay in the window **default**; the price is **not** re-raised for others. The winning bid carries `minUnits`; if paid units < `minUnits` the group FAILS and paid members are refunded in full. |
| D-10 | **Clients never write business collections.** Every mutation is a callable Cloud Function using the Admin SDK inside a transaction. Rules allow reads and a few self-owned writes only. |
| D-11 | **Android = customer app.** **Web (Next.js) = supplier portal + admin console + public share pages.** No customer checkout on web in V1. |
| D-12 | Android talks to the backend through Firebase SDKs and **callable functions**; **Retrofit is not used in MVP**. OkHttp is present transitively (Coil). Retrofit is reserved for any future non-Firebase REST API. |
| D-13 | Offline: **Firestore offline persistence is the primary cache.** Room stores device-local data only (recent searches, request drafts, last-known orders snapshot, pending-read markers). |
| D-14 | Locality ID = `IN-<pincode>` (e.g. `IN-560034`) from a curated registry. GPS is reverse-geocoded **server-side**; only `localityId` and a 5-character geohash are stored. |
| D-15 | Roles are Firebase **custom claims** `{role, supplierStatus}` set only by admin functions. The first admin is bootstrapped with a one-off script. |
| D-16 | Deadlines are processed by an idempotent **scheduled sweeper every 5 minutes**. Displayed deadlines say "closes around HH:MM". |
| D-17 | Notifications: per-user FCM tokens for transactional messages; topic `group_<gid>` for broadcast price/tier changes; every user-visible notification also gets an inbox document. |
| D-18 | Web hosting: **Firebase App Hosting** (alternative: Vercel). |
| D-19 | Price comparison through server-side adapters. MVP sources: supplier-declared MRP + admin-verified reference price. Marketplace/affiliate adapters are FUTURE and their availability is **NOT VERIFIED**. |
| D-20 | A user may join/leave a group at most 3 times (anti-griefing). Leaving is allowed only before lock. |
| D-21 | Every mutating callable takes a client-generated `requestId` (UUID v4) for **idempotency**. |
| D-22 | Supplier settlement: MVP = manual settlement recorded by admin. V1 = Razorpay Route split settlement (account eligibility **NOT VERIFIED**). |
| D-23 | Savings shown with floor rounding (30+ ghee tier = **42%**, not the prototype's 43%). Never overstate savings. |
| D-24 | Production group states replace the prototype enum (mapping in STATE_MACHINES.md §2). `FILLING` = `memberCount ≥ 0.6 × targetMembers`, recomputed on **join and leave** (fixes D1). |
| D-25 | The prototype is rebuilt, not refactored. Reused: pricing rule, domain vocabulary, screen designs. |
| D-26 | Delivery = batch to a locality **collection point**; supplier updates tracking in the portal. Courier integration is FUTURE. |
| D-27 | Cloud Functions in **TypeScript, Node.js 20, 2nd gen**. Shared pricing package `@groupdeal/pricing` is the single source of the pricing rule; Android ports it and must pass the same `golden-vectors.json`. |
| D-28 | Phone OTP is the primary sign-in (India); Google sign-in secondary. Email/password not offered. |

## 2. Assumptions (verify before launch)

| ID | Assumption | Verify by |
|---|---|---|
| AS-1 | Razorpay test + live accounts are obtainable by the team/institution | Razorpay onboarding |
| AS-2 | Firebase Blaze plan is available (required for Functions, Scheduler, outbound calls) | Billing setup |
| AS-3 | Firestore single-document sustained write guidance (approx. 1 write/s per document) applies to hot groups | Firestore docs + load test |
| AS-4 | Google Places / Geocoding quotas suffice for locality resolution | Maps Platform console |
| AS-5 | FCM delivery is best-effort; no delivery receipts are relied upon | FCM docs |
| AS-6 | Collection points can be organised per locality (RWA/shop partner) | Operations |

## 3. Architectural Decision Records

### ADR-01 Firebase as the backend platform
- **Context:** Two students, real-time shared state, auth, push, payments glue, limited ops capacity.
- **Decision:** Firebase (Auth, Firestore, Functions, Storage, FCM, App Check, Hosting).
- **Alternatives:** Node/Express + MongoDB (zeroth-review plan); Supabase; Spring Boot + Postgres.
- **Reasoning:** Real-time listeners, managed auth with phone OTP, serverless scaling, first-class Android SDKs, local emulator suite for testing.
- **Trade-offs:** Vendor lock-in; query limitations; per-operation billing; hot-document write limits.
- **Consequences:** Data model designed for Firestore access patterns; aggregates kept on group docs; hot groups need load testing (AS-3).

### ADR-02 Cloud Firestore over Realtime Database
- **Context:** Structured documents, compound queries by locality/state/deadline, transactions.
- **Decision:** Firestore (Native mode).
- **Alternatives:** Realtime Database; Cloud SQL.
- **Reasoning:** Richer queries and indexes, transactions, better security-rule model, offline persistence.
- **Trade-offs:** No joins; write limits per document; index management.
- **Consequences:** Denormalised read models (ADR-12); composite indexes in `firestore.indexes.json`.

### ADR-03 Cloud Functions for all business mutations
- **Context:** Pricing, membership, auctions and payments must be tamper-proof.
- **Decision:** Callable functions (2nd gen, TypeScript) own every business write; scheduled + Firestore-triggered functions handle deadlines and side effects.
- **Alternatives:** Client writes guarded by rules only; separate API server.
- **Reasoning:** Rules cannot express multi-document invariants (price from count) or call Razorpay; one backend for Android and web.
- **Trade-offs:** Cold starts; slightly higher latency than direct writes.
- **Consequences:** Clients are thin; `minInstances` set for `joinGroup` and payment functions in production.

### ADR-04 Kotlin + Jetpack Compose for Android
- **Context:** Prototype already in Kotlin/Compose; course is Android-focused.
- **Decision:** Kotlin, Compose, Material 3.
- **Alternatives:** Flutter (zeroth-review plan), React Native, XML views.
- **Reasoning:** Native performance, team experience from the prototype, official Android direction.
- **Trade-offs:** No iOS from the same codebase.
- **Consequences:** iOS is FUTURE; the web portal covers non-Android roles.

### ADR-05 MVVM + Clean Architecture with Hilt
- **Context:** Prototype's singleton made screens untestable.
- **Decision:** UI (Compose) → ViewModel (`StateFlow<UiState>`) → UseCases → Repositories (interfaces) → data sources; Hilt DI; feature modules.
- **Alternatives:** MVI framework; single-module MVVM.
- **Reasoning:** Testable with MockK; matches Android architecture guidance; incremental adoption.
- **Trade-offs:** More boilerplate.
- **Consequences:** Every screen has a ViewModel and a `UiState` sealed model.

### ADR-06 Server-side pricing
- **Context:** Prices determine money owed.
- **Decision:** Only the server computes and stores `currentUnitPrice`, `lockedUnitPrice`, `finalUnitPrice`, order amounts.
- **Alternatives:** Client computes, server validates.
- **Reasoning:** A modified client must never influence price.
- **Trade-offs:** Display depends on the network (mitigated by listeners and cache).
- **Consequences:** Clients may compute *previews* with the shared engine but never send prices.

### ADR-07 Transaction-based membership updates
- **Context:** Concurrent joins at tier boundaries.
- **Decision:** `joinGroup`/`leaveGroup` run a Firestore transaction reading group + member + idempotency docs and writing all three atomically.
- **Alternatives:** `FieldValue.increment` without transaction; distributed counters; queue.
- **Reasoning:** Price depends on the exact count; increments alone can't recompute the tier atomically.
- **Trade-offs:** Contention on very hot groups (AS-3).
- **Consequences:** Load-test hot groups; fallback = Cloud Tasks join queue per group (designed, not MVP).

### ADR-08 Sealed-bid reverse auction after lock
- **Context:** Suppliers should compete for proven demand without a race to undercut in real time.
- **Decision:** Sealed-bid, first-price, lowest valid bid wins, ceiling = locked tier price.
- **Alternatives:** Open descending auction; fixed supplier per deal; admin selection.
- **Reasoning:** Simple, fair, hard to game, protects customers via ceiling.
- **Trade-offs:** No price discovery from rivals' bids; requires a deadline wait.
- **Consequences:** Bids invisible to other suppliers; anchor supplier fallback.

### ADR-09 Razorpay as payment gateway
- **Context:** Indian customers; UPI is essential.
- **Decision:** Razorpay Standard Checkout (Android SDK) with server-created orders.
- **Alternatives:** Stripe India, PayU, Cashfree.
- **Reasoning:** UPI/cards/netbanking, webhooks, refunds API, Route for marketplaces.
- **Trade-offs:** Account onboarding (AS-1); fees.
- **Consequences:** Secrets in Secret Manager; webhook endpoint required.

### ADR-10 Firebase App Check
- **Context:** Callables and Firestore are reachable by any HTTP client.
- **Decision:** App Check with Play Integrity (Android) and reCAPTCHA Enterprise (web); enforce on callables, Firestore and Storage in production.
- **Alternatives:** Rely on auth only.
- **Reasoning:** Raises the cost of scripted abuse (fake joins, vote spam).
- **Trade-offs:** Debug tokens for emulators/CI.
- **Consequences:** Separate debug providers per environment.

### ADR-11 Server-side payment verification
- **Context:** Client success callbacks can be forged.
- **Decision:** Order becomes PAID only after (a) server HMAC check of the client signature **plus** a server fetch showing `captured`, or (b) a verified webhook. Amount and currency always rechecked.
- **Alternatives:** Trust client callback.
- **Reasoning:** Prevents free goods and replay.
- **Consequences:** `paymentEvents` idempotency store and daily reconciliation.

### ADR-12 Denormalisation where required
- **Context:** Feed screens must render from one query without joins.
- **Decision:** Group docs embed a deal snapshot (title, image, MRP, tiers) frozen at group creation; orders embed product + price snapshot.
- **Alternatives:** Join in client.
- **Reasoning:** Fewer reads, stable history (a deal edit never changes an open group's tiers).
- **Trade-offs:** Duplicate data; edits apply to new groups only.
- **Consequences:** Deal edits create a new `scheduleVersion`.

### ADR-13 Caching and offline support
- **Context:** Patchy mobile networks.
- **Decision:** Firestore persistence for live data; Room for local-only data (D-13); all mutations require connectivity and are idempotent (D-21).
- **Alternatives:** Full offline-first with queued writes.
- **Reasoning:** Joining/paying offline would show prices the server may reject.
- **Consequences:** Clear offline banners; actions disabled offline.

### ADR-14 Pay after lock (vs pay at join)
- **Context:** Price can only fall as members join, then the auction may lower it further.
- **Decision:** D-06.
- **Alternatives:** Pay at join + refund the difference; pre-authorise and capture later.
- **Reasoning:** No refunds for price drops, no long-held authorisations, simpler reconciliation.
- **Trade-offs:** Non-payment risk.
- **Consequences:** Payment window, defaults, trust penalties, supplier `minUnits` (D-09).
