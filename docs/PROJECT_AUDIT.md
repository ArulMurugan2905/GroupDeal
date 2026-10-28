# A. Project Audit — GroupDeal (as of 30 Sep 2026)

**Scope of evidence.** Inspected directly: the `groupdeal.zip` Android project (all 22 Kotlin source files, Gradle scripts, version catalog, manifest, tests), the zeroth-review PPT, the latest poster and the generated PBL report. Anything not provable from source is marked **NOT VERIFIED**. The poster's full-stack technology list describes the *target* system; it is **not** evidence of implementation.

Status legend: **IMPLEMENTED** (works in source) · **PARTIAL** (simplified/simulated) · **PROPOSED** (designed, not coded) · **FUTURE** (beyond V1) · **NOT VERIFIED** (no evidence either way).

## A.1 What exists

| Item | Evidence | Status |
|---|---|---|
| Native Android app, single `app` module, package `com.example` | `settings.gradle.kts`, `app/build.gradle.kts` | IMPLEMENTED |
| Kotlin 2.2.10, Compose BOM 2024.09.00, Material 3, AGP 9.1.1, minSdk 24 / target 36 | version catalog | IMPLEMENTED |
| 22 Kotlin files, 5,434 lines (core 901, ui 4,494, MainActivity 39) | line counts | IMPLEMENTED |
| 9 screens/sheets: Login, Onboarding, LocalityPicker, Discover, GroupDetail, Demands, MyGroups, Orders, Profile | `ui/**` | IMPLEMENTED |
| `PricingEngine` (tier price, tier index, `validateTiers`, members-to-next-tier) | `core/engine/PricingEngine.kt` | IMPLEMENTED (`validateTiers` never called) |
| `GroupDealRepository` Kotlin `object`, 5 `StateFlow`s + `SharedFlow` events | `core/data` | IMPLEMENTED (in-memory only) |
| `LocalityService`: 15 localities / 8 cities, pincode lookup with 2-digit-prefix fallback | `core/data` | IMPLEMENTED |
| Seed data: 4 deals, 3 requests, 1 order, demo user (trust 780) | repository seeding | IMPLEMENTED |
| Unit tests: pricing, pincode, trust tier, string resource (JUnit4 + Robolectric) | `src/test` | PRESENT — Gradle run **NOT VERIFIED** (logic re-computed by a Python port: all assertions agree) |
| Roborazzi screenshot test, instrumented test | templates | PRESENT, not meaningful |

## A.2 Feature classification

| Feature | Status | Notes |
|---|---|---|
| Locality selection (search + pincode) | IMPLEMENTED | Any 6-digit number accepted, incl. invalid pincodes |
| Deal feed filtered by locality | **PARTIAL** | Feed filtered only by search + category; all seeds are Koramangala |
| Search + category filter | IMPLEMENTED | |
| Join / leave group with tier recompute | IMPLEMENTED | Client-side, single device |
| Group state machine | PARTIAL | Only OPEN/FILLING/TARGET_REACHED computed; `leaveGroup` doesn't recompute state (**defect D1**) |
| Members to next tier | PARTIAL | Derived property can go negative above target (**defect D2**) |
| Demand requests + one-vote-per-user | IMPLEMENTED | Threshold fixed at 15; no conversion to deal |
| Checkout / orders / tracking | PARTIAL | Local order record; checkout allowed before target; qty fixed at 1; tracking steps static |
| Payments (Razorpay) | PROPOSED | No SDK, no server |
| Authentication | PARTIAL (simulated) | 400 ms delay; button labelled "Sign In with Firebase" but no Firebase call |
| Roles (customer/supplier/admin) | PARTIAL (label only) | `switchDemoRole` changes a label; no authorisation |
| Supplier bidding | PARTIAL (data model only) | `SupplierBid`, BIDDING/SUPPLIER_SELECTED states, 2 seeded winning bids |
| Trust score | PARTIAL | Static score → tier mapping |
| Notifications | PARTIAL | In-app `SharedFlow` banner; no FCM |
| Price comparison vs marketplaces | PROPOSED | Only stored MRP |
| Firebase Auth/Firestore/Functions/Storage/FCM/App Check | PROPOSED | Firestore/Auth commented out; `firebase-appcheck`, `firebase-ai` declared, unused |
| MVVM / Hilt / Navigation Compose / Room / Retrofit / OkHttp / Moshi | PROPOSED | Some declared in Gradle, **none used in code** |
| Web platform (Next.js) | PROPOSED | Nothing exists |
| CI/CD, environments, signing | PROPOSED | Nothing exists |

## A.3 Quality and risk findings

| Area | Finding | Severity | Production response |
|---|---|---|---|
| Architecture | Global mutable singleton; UI calls repository directly; no DI, no ViewModels | High | Rebuild with MVVM + Clean + Hilt (ANDROID_ARCHITECTURE.md) |
| Data consistency | State per device, resets on restart; two users can never see the same group | Critical | Firestore as source of truth; server-only writes |
| Pricing correctness | Price computed on the client; `Double` money; validation unused; D1, D2 | Critical | Server-authoritative integer-paise engine with golden vectors (PRICING_ENGINE.md) |
| Concurrency | None (single process) | Critical | Transactional membership updates (PRICING_ENGINE.md §5) |
| Authentication | Simulated; any email logs in | Critical | Firebase Auth (phone OTP + Google), App Check |
| Authorisation | Roles are labels | Critical | Custom claims + rules + function guards |
| Payments | "Confirm & Pay" creates an order with no money movement | Critical | Razorpay with server verification + webhooks |
| Supplier bidding | No submission, validation, deadline or audit | High | Sealed-bid server auction (SUPPLIER_AUCTION.md) |
| Locality/privacy | Invalid pincodes accepted; no GPS; random demo counts for unknown pincodes | Medium | Validated locality registry, coarse location only |
| Notifications | In-process only | Medium | FCM + inbox documents |
| Testing | Few unit tests; no rules/integration/E2E/concurrency tests; run status NOT VERIFIED | High | TEST_STRATEGY.md |
| Deployment | No environments, CI, signing, monitoring | High | DEPLOYMENT.md |
| Tech debt | Unused dependencies (Retrofit, Room, Moshi, Navigation, firebase-ai); leftover `Greeting` template code; `MyApplicationTheme` alias | Low | Remove in Stage 0 |
| UX gaps | No loading/error/offline states; no deadlines/countdowns; checkout not gated | Medium | UI_UX_SPECIFICATION.md |
| Data reconciliation | Prototype seed labels 30+ tier as "43%"; floor rule gives 42% | Low | D-23: always floor savings |

## A.4 What to keep from the prototype

Reuse: the domain vocabulary (deal, tier, group, request, trust tier), the screen inventory and visual language (emerald theme, pricing roadmap, progress cards), and the pricing rule — ported to integer paise with validation enforced. **Do not reuse** the repository singleton, the simulated auth, or the client-side price computation. The prototype is a validated UX/logic demo; the production build starts fresh (D-25).
