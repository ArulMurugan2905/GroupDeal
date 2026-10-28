# Test Strategy

No performance numbers are asserted anywhere in this document; performance targets are set only after a staging baseline is measured.

## 1. Levels and tools
| Level | Scope | Tools | Runs |
|---|---|---|---|
| Unit — pricing | `@groupdeal/pricing`, Kotlin port | node:test (implemented, 6/6 pass), JUnit | every commit |
| Unit — functions | guards, validators, state tables, ranking, signature verification | Vitest/Jest | every commit |
| Unit — Android | use cases, ViewModels, mappers | JUnit, MockK, Turbine | every commit |
| Firestore rules | every `allow`/deny path | `@firebase/rules-unit-testing` + emulator | every commit |
| Integration (backend) | callables + triggers + schedulers against emulators, Razorpay stubbed | Vitest + emulator suite | every commit |
| UI (Android) | critical screens/flows | Compose UI test, Robolectric | every PR |
| UI (web) | components | Testing Library | every PR |
| E2E | full purchase in staging with Razorpay **test mode** | Playwright (web), Maestro or UI Automator (Android) — tool choice decided at Stage 15 | nightly + pre-release |
| Concurrency | parallel joins/leaves/bids/webhooks | Node script firing N parallel callables at emulator/staging | pre-release |
| Payment | signature, webhook, duplicates, refunds, reconciliation | stubs + Razorpay test mode | every PR (stub), nightly (test mode) |
| Failure recovery | retries, partial failures, sweeper reruns | fault injection in emulator | pre-release |
| Offline/online | cache, banners, retry with same requestId | Android instrumented + network toggling | pre-release |
| Performance | cold start, feed render, function latency, hot-group contention | Macrobenchmark, Firebase Performance, load script | baseline then regression |
| Security | rules tests, App Check enforcement, secret scanning, dependency audit | rules tests, gitleaks, `npm audit`, Gradle dependency check | every PR |
| Regression | golden vectors + E2E suite | CI | every release |

## 2. Test matrix
Risk: **C** critical · **H** high · **M** medium.

| ID | Feature | Scenario | Input | Expected result | Risk |
|---|---|---|---|---|---|
| PR-01 | Pricing | Golden vectors | n = −3,0,1,4,5,14,15,18,29,30,35 | tier/price/savings/next as PRICING_ENGINE §4 | C |
| PR-02 | Pricing | Monotonic price | n = 0..60 | non-increasing | C |
| PR-03 | Pricing | Next tier never negative | n = 0..100 | null or > 0 | H |
| PR-04 | Pricing | Invalid schedules rejected | unsorted, rising price, dup threshold, base > MRP, float | specific error codes | H |
| PR-05 | Pricing | Android/TS parity | golden JSON | identical outputs | C |
| GR-01 | Join | Happy path | n=18, qty 1 | n=19, ₹949, FILLING, version+1 | C |
| GR-02 | Join | **Two users simultaneously at 14** | A,B join in parallel | n=16, both succeed, tier 2 ₹949, exactly one priceHistory doc (14→15) | C |
| GR-03 | Join | **Join while tier changing** | 50 parallel joins from n=0 | n=50 (≤ capacity), price ₹799, priceHistory 3 docs, no duplicate members | C |
| GR-04 | Join | Same user double tap | same requestId ×2 | one membership, identical responses | C |
| GR-05 | Join | Same user, different requestIds in parallel | 2 calls | one succeeds, one already-exists | C |
| GR-06 | Join | After deadline | now ≥ closesAt | failed-precondition CLOSED | H |
| GR-07 | Join | Capacity | n = capacity | failed-precondition CAPACITY; lockRequested | H |
| GR-08 | Join | Other locality | user locality ≠ group | permission-denied LOCALITY | H |
| GR-09 | Join | Join-cycle limit | 4th join | resource-exhausted | M |
| GR-10 | Leave | **Leave at tier boundary** | n=15 → leave | n=14, ₹1,099, state recomputed (FILLING/OPEN per ratio), watchers notified | C |
| GR-11 | Leave | After lock | state LOCKED/BIDDING | failed-precondition | H |
| GR-12 | Leave | Parallel leave + join at 15 | both | final n=15, tier consistent with n | C |
| GR-13 | Lock | Below minimum | n=4 at closesAt | FAILED MIN_NOT_REACHED, members RELEASED, no orders | C |
| GR-14 | Lock | Sweeper runs twice concurrently | two invocations | single transition, single auction doc | C |
| AU-01 | Auction | Valid bid | price ≤ ceiling | ACTIVE, revision 1, bidCount 1 | H |
| AU-02 | Auction | Above ceiling / below floor | | invalid-argument; below-floor creates fraud signal | H |
| AU-03 | Auction | **Competing bids simultaneously** | 10 suppliers in parallel | 10 bids, bidCount 10 | H |
| AU-04 | Auction | Revision limit | 11th revision | resource-exhausted | M |
| AU-05 | Auction | Withdraw in last 30 min | | failed-precondition | M |
| AU-06 | Auction | Tie | equal price | higher rating wins; then fewer days; then earlier priceSetAt; then supplierId | H |
| AU-07 | Auction | No bids, anchor exists | | SUPPLIER_SELECTED at ceiling | H |
| AU-08 | Auction | No bids, no anchor | | FAILED NO_SUPPLIER | H |
| AU-09 | Auction | Supplier suspended after bidding | | bid INVALIDATED at close | H |
| AU-10 | Auction | Award idempotent | sweeper rerun | one set of orders (IDs `{gid}_{uid}`) | C |
| AU-11 | Auction | Unverified supplier bids | claim PENDING | permission-denied | C |
| PA-01 | Payment | Create order amount | client sends amount | ignored; DB amount used | C |
| PA-02 | Payment | Valid verify | correct signature, captured | order PAID | C |
| PA-03 | Payment | Forged signature | wrong HMAC | permission-denied, payment unchanged | C |
| PA-04 | Payment | **Duplicate client callback** | verify ×2 | one transition, same response | C |
| PA-05 | Payment | **Webhook duplicate** | same event ×3 | processed once | C |
| PA-06 | Payment | Webhook before client verify | | PAID; later verify no-op | C |
| PA-07 | Payment | Amount mismatch in webhook | | not PAID; AMOUNT_MISMATCH signal; alert | C |
| PA-08 | Payment | Bad webhook signature | | 400, nothing written, alert metric | C |
| PA-09 | Payment | Late capture after window | | CAPTURED → auto refund | H |
| PA-10 | Payment | Insufficient paid units | paidUnits < minUnits | group FAILED, all CAPTURED refunded | C |
| PA-11 | Payment | Refund failed event | | REFUND_FAILED, alert, admin retry works | H |
| PA-12 | Payment | Reconciliation | CREATED payment captured at Razorpay, webhook lost | reconcile marks PAID | C |
| PA-13 | Payment | **Network drop after Razorpay success** | app killed before verify | webhook/reconcile marks PAID; app shows PAID on restart | C |
| RQ-01 | Requests | One vote per user | vote ×2 | already-exists | H |
| RQ-02 | Requests | Threshold | 15th vote | THRESHOLD_REACHED, admin notified | M |
| RQ-03 | Requests | Spam | 4th request in a day | resource-exhausted | M |
| RQ-04 | Requests | New-account vote | account < 24 h | failed-precondition | M |
| SE-01 | Rules | Client writes group | direct update memberCount | denied | C |
| SE-02 | Rules | Read others' membership | | denied | H |
| SE-03 | Rules | Read sealed bids of others | | denied | C |
| SE-04 | Rules | Escalate role via profile | update role field | denied | C |
| SE-05 | Rules | Read config/admins | | denied | H |
| SE-06 | App Check | Call without token (prod config) | | unauthenticated | C |
| SE-07 | Admin | Non-admin calls admin fn | | permission-denied | C |
| NO-01 | Notifications | Tier change | 14→15 | topic message + inbox for watchers | M |
| NO-02 | Notifications | Invalid token | FCM unregistered | device doc deleted | M |
| OF-01 | Offline | Open app offline | cached data | cached groups shown, banner, join disabled | M |
| OF-02 | Offline | Retry after reconnect | same requestId | no duplicate membership | C |
| OF-03 | Stale data | Out-of-order snapshot | version lower | ignored by UI | H |
| RS-01 | Restart | App killed mid-payment | | order state recovered from server | C |
| MA-01 | Malicious | Callable with extra fields (price) | | ignored/rejected by schema | C |
| MA-02 | Malicious | Script floods joinGroup | 100 calls/min | rate-limited, App Check blocks without token | H |

## 3. Rules tests
One test file per collection; each `allow` has a positive and a negative case; run in CI with the Firestore emulator. Status of the rules in this package: **written, not yet executed** (requires emulator).

## 4. Concurrency harness
`/scripts/concurrency/join-burst.ts`: creates a group at n = k, fires M parallel `joinGroup` calls (distinct users), asserts final `memberCount = k + M`, member docs = k + M, tier = `quote(n).tierIndex`, priceHistory transitions are strictly increasing and unique. Variants for leave/join interleaving and bid bursts. Run against the emulator in CI and against staging before release (emulator contention behaviour may differ from production — record differences).

## 5. Exit criteria per release
All C-risk tests pass; zero open C/H defects; rules coverage of every `allow`; E2E purchase passes in staging with Razorpay test mode; reconciliation run shows zero exceptions on staging data.
