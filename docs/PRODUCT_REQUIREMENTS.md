# Product Requirements Specification — GroupDeal

**Product:** GroupDeal — *Buy Together, Save More* — locality-based group buying with dynamic pricing and supplier bidding.
**Platforms:** Android customer app; web supplier portal, admin console and public deal pages (D-11).
**Release labels:** **MVP** (first production release), **V1** (next), **FUTURE**.

## 1. Roles

| Role | Who | Authenticated by | Can |
|---|---|---|---|
| Customer | Resident buying for a household | Phone OTP / Google (Android) | Browse their locality, join/leave groups, request/upvote products, pay, track, rate, raise disputes |
| Supplier | Farm co-op, wholesaler, brand | Phone OTP / Google (web) + **admin verification** | Propose deals (anchor supplier), bid in auctions for their service areas, fulfil orders, view analytics |
| Admin | Platform operator | Google (web) + `role=admin` claim | Verify suppliers, approve deals, open groups, manage localities, moderate, refund, resolve disputes, configure |

A user has exactly one role. Supplier accounts are separate from customer accounts (a person who is both uses two sign-ins). This keeps rules simple and audit trails clean.

## 2. Customer journey (Android)

| # | Step | Requirement | Release |
|---|---|---|---|
| C1 | Onboarding | 3 intro cards (how group pricing works, pay after lock, collection-point delivery). Skippable. | MVP |
| C2 | Authentication | Phone OTP (primary), Google (secondary). New user → profile (name; phone from auth). | MVP |
| C3 | Locality | Ask for coarse location permission with rationale; on grant, `resolveLocality(lat,lng)`; on deny, pincode/area search. User confirms locality and collection point. Change anytime (max 3 changes/30 days). | MVP |
| C4 | Home | Locality header; "Closing soon", "Almost at next tier", "New in your area" rails; demand banner. | MVP |
| C5 | Discovery | List of OPEN/FILLING/TARGET_REACHED groups in the locality; category chips; search by product/brand; sort by closing time, savings, momentum. | MVP |
| C6 | Deal detail | Images, MRP, current price, savings %, tier roadmap with active tier, members x/target, members to next tier, countdown, supplier (anchor) name and rating, delivery terms, quantity picker (1..max). | MVP |
| C7 | Join | Confirm sheet: quantity, current price **estimate**, "final price is set when the group closes and can only go down", pay-after-lock explanation. Join returns the authoritative new state. | MVP |
| C8 | Progress | Live member count and price via listener; tier-crossing animation; push when tier changes (opt-in). | MVP |
| C9 | Leave | Allowed before lock; warns if leaving drops the tier. | MVP |
| C10 | Lock & auction wait | Status "Locked at ₹949 — suppliers are bidding, price can only go down". | MVP |
| C11 | Payment | On SUPPLIER_SELECTED: order created; pay within the window via Razorpay (UPI/card/netbanking). Status screen polls server, never trusts SDK callback alone. | MVP |
| C12 | Tracking | Order timeline: Paid → Supplier confirmed → Dispatched → Ready at collection point → Delivered. | MVP |
| C13 | Notifications | Inbox + push: tier reached, closing soon (watchlist), group locked, supplier selected + pay now, payment confirmed, dispatched, ready for pickup, refund issued. | MVP |
| C14 | Rating | After DELIVERED: 1–5 stars + optional text for supplier; once per order. | MVP |
| C15 | Requests | Request a product (name, category, target price, reference price); upvote others (one vote each). | MVP |
| C16 | Watchlist | Save groups; get "closing soon" reminder. | V1 |
| C17 | Savings dashboard | Lifetime and monthly savings vs MRP. | V1 |
| C18 | Disputes | Raise issue on a delivered/paid order (missing, damaged, wrong item). | MVP |

## 3. Supplier journey (web portal)

| # | Step | Requirement | Release |
|---|---|---|---|
| S1 | Registration | Business name, GSTIN (optional MVP), FSSAI licence for food (upload), contact, bank details placeholder for settlement. | MVP |
| S2 | Verification | Admin reviews documents → VERIFIED / REJECTED (reason). Only VERIFIED suppliers bid or propose deals. | MVP |
| S3 | Service area | Select localities served + max delivery days per locality. | MVP |
| S4 | Propose deal | Product, images, MRP, base price, 1–5 tiers, `maxQtyPerMember`, capacity, duration, delivery terms. Becomes **anchor supplier** (committed to deliver at the locked price). Admin approval required. | MVP |
| S5 | Opportunities | Auctions in BIDDING for served localities: product, units committed, ceiling price, deadline. | MVP |
| S6 | Bid | Unit price (≤ ceiling, ≥ floor), delivery days, `minUnits`. Revise or withdraw before deadline (max 10 revisions). Sealed. | MVP |
| S7 | Result | Won/lost notification; won → fulfilment list. | MVP |
| S8 | Fulfilment | Confirm order batch, mark dispatched, mark ready at collection point, mark delivered per order. | MVP |
| S9 | Analytics | Bids won/lost, revenue, ratings, on-time rate. | V1 |

## 4. Admin journey (web console)

| Area | Capabilities | Release |
|---|---|---|
| Users | Search, view, suspend/unsuspend, view trust history | MVP |
| Suppliers | Verify/reject/suspend, view documents and performance | MVP |
| Localities | Create/edit registry (pincode, name, collection points, active flag) | MVP |
| Deals | Approve/reject supplier proposals, create admin deals, pause/archive | MVP |
| Groups | Open groups per locality, extend deadline (once), cancel with reason | MVP |
| Requests | Review THRESHOLD_REACHED requests, convert to deal, reject | MVP |
| Auctions/bids | View all bids, invalidate a bid with reason (audited) | MVP |
| Orders/payments | View, initiate refunds, view reconciliation exceptions | MVP |
| Disputes | Assign, resolve (refund/partial refund/reject) | MVP |
| Moderation | Hide requests/ratings that violate policy | MVP |
| Fraud signals | Review queue (vote bursts, device reuse, amount mismatch) | V1 |
| Analytics | GMV, groups closed/failed, avg savings, payment conversion | V1 |
| Configuration | Fees, thresholds, windows, feature flags (`config/app`) | MVP |

Every admin action writes an `adminActions` audit record.

## 5. Functional requirements (summary)

FR-01 Auth by phone OTP/Google · FR-02 Locality resolution and change limits · FR-03 Locality-scoped group feed · FR-04 Search/filter/sort · FR-05 Deal detail with tier roadmap · FR-06 Transactional join/leave with quantity · FR-07 Server-computed price, tier, savings, next tier · FR-08 Lock at deadline/capacity, fail below minimum · FR-09 Sealed reverse auction with ceiling · FR-10 Orders created at supplier selection · FR-11 Razorpay payment with server verification and webhooks · FR-12 Payment window, defaults, refunds · FR-13 Order tracking · FR-14 Push + inbox notifications · FR-15 Ratings and trust score · FR-16 Requests and one-vote-per-user upvotes, conversion to deals · FR-17 Supplier onboarding/verification · FR-18 Admin console with audit · FR-19 Disputes · FR-20 Public shareable deal page.

## 6. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-01 Correctness | Price is a pure function of (schedule, memberCount); golden vectors pass on server, web and Android. |
| NFR-02 Consistency | All clients show the server's price; displayed estimates are labelled. |
| NFR-03 Security | No client-trusted money values; App Check enforced; least-privilege rules. |
| NFR-04 Idempotency | Every mutation and webhook is safe to retry. |
| NFR-05 Availability | Payment and join paths degrade gracefully (clear errors, retries); no silent data loss. |
| NFR-06 Privacy | Store locality, not precise location; members lists not public; DPDP Act 2023 obligations reviewed before launch (**NOT VERIFIED** legal review). |
| NFR-07 Accessibility | TalkBack labels, 48dp targets, contrast ≥ WCAG AA, dynamic type. |
| NFR-08 Localisation | English MVP; Tamil, Hindi V1. |
| NFR-09 Observability | Structured logs with request IDs; Crashlytics; alerting on payment/webhook failures. |
| NFR-10 Compatibility | Android 7.0 (API 24)+; web: last 2 versions of Chrome, Edge, Firefox, Safari. |
| NFR-11 Performance | Targets set after baseline measurement in staging (no numbers asserted here). |

## 7. Out of scope (V1)
iOS app; courier integration; credit/BNPL; multi-product carts; supplier price negotiations outside auctions.
