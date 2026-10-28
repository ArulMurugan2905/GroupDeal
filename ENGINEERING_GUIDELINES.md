# GroupDeal Engineering Guidelines & Architecture Invariants

GroupDeal: locality-based group buying with tier-based dynamic pricing and supplier reverse auctions.
Author: Arul Murugan M.

## Source of truth (in order)
1. Code in this repository.
2. `/docs/DECISIONS.md` (decisions D-xx and ADRs) — wins over every other document.
3. Other `/docs/*.md` specifications.
4. The PBL poster/report describe the product; they are **not** evidence that something is implemented.
Never copy content from the sample MAD report (another team's project).

## Repository layout
`/android` (customer app) · `/web` (supplier portal, admin console, share pages) · `/functions` (all business logic) · `/packages/pricing` (the only pricing rule) · `/firestore` (rules, indexes, rules tests) · `/scripts` (seed, migrations, admin bootstrap, concurrency harness) · `/docs`.

## Non-negotiable rules
1. **Money is integer paise.** Never use Float/Double/number-with-decimals for money. Savings % uses floor.
2. **Clients never write business data.** Groups, members, auctions, bids, orders, payments, ratings, requests, votes are written only by Cloud Functions (Admin SDK). Rules deny client writes to them.
3. **Never trust from clients:** counts, tiers, prices, amounts, statuses, roles, trust scores, supplier status, auction results, timestamps.
4. **Pricing rule lives in `@groupdeal/pricing`.** The Android Kotlin port must pass `packages/pricing/test/golden-vectors.json`. Change the vectors first, then the code, on every platform.
5. **Every mutation is idempotent:** callables take `requestId` (UUID v4) and store the response in `idempotencyKeys`. Webhooks dedupe on the Razorpay event ID.
6. **Every multi-document invariant runs in a Firestore transaction** that re-reads state and re-checks preconditions (deadline, state, ownership, limits).
7. **State changes go through `assertTransition()`** with the tables in `STATE_MACHINES.md`; no ad-hoc status strings.
8. **Payments:** an order becomes PAID only after server HMAC verification **plus** a Razorpay fetch showing `captured`, or a verified webhook. Amount and currency are always rechecked against Firestore.
9. **Auction winners are chosen only by `sweepAuctions`** with the deterministic ranking in `SUPPLIER_AUCTION.md`.
10. **Secrets** live in Secret Manager (functions) or CI environment secrets. Never commit keys, `.env`, keystores, service-account JSON or prod `google-services.json`.
11. **Privacy:** store `localityId` + 5-char geohash only; never precise coordinates. Activity feeds are anonymised. No PII in logs or analytics.
12. **App Check** is enforced in staging and prod on Functions, Firestore and Storage.
13. Anything not verified must be written as **NOT VERIFIED** in docs and reports. Never invent test results or performance numbers.

## Coding standards
- **Kotlin/Android:** MVVM + Clean; Hilt; one `UiState` sealed interface per screen; `StateFlow` + `stateIn(WhileSubscribed(5000))`; no business logic in composables; `core:domain` has no Android imports; ktlint + detekt clean.
- **TypeScript (functions/web):** `strict: true`; zod validation for every callable input; typed contracts in `functions/src/contracts.ts`; errors as `HttpsError` with a `reason` detail; structured logs `{fn, requestId, uid, outcome, latencyMs}`.
- **Web:** server components verify session cookies; mutations call callables only; Tailwind; accessible dialogs.
- **Naming:** collections lowerCamelCase plural; enums UPPER_SNAKE; money fields end in `Paise`; timestamps end in `At`.
- **Comments** explain *why*; specs live in `/docs`.

## Testing standards
- New callable ⇒ unit tests + emulator integration test + rules test for any new path.
- Concurrency-sensitive code ⇒ add a case to `/scripts/concurrency`.
- Payment code ⇒ duplicate, forged-signature and amount-mismatch tests.
- Pricing change ⇒ golden vectors updated and passing on TS and Kotlin.
- CI must be green before merge; C-risk tests in `TEST_STRATEGY.md` are release blockers.

## Engineering Workflow for Architecture Changes
1. Read the relevant `/docs` spec and `DECISIONS.md` first.
2. If a change contradicts a decision, stop and add a new decision/ADR instead of silently diverging.
3. Keep changes small, staged per `IMPLEMENTATION_ROADMAP.md`.
4. Update docs in the same PR when behaviour changes.
