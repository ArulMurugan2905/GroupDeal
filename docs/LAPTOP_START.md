# S. LAPTOP EXECUTION STARTING POINT

Goal of the first sessions: **prove the core of GroupDeal end to end on emulators** — two users join the same group concurrently, the server computes the tier price in a transaction, and the Android app shows the new price live. Everything else builds on this.

## Step 0 — decisions to make before writing code (one sitting)
1. Android application ID (placeholder `in.groupdeal.app`) and whether a domain will be registered.
2. Firebase project IDs (`groupdeal-dev`, `groupdeal-staging`, `groupdeal-prod` or similar). Emulators need no billing; staging/prod need the Blaze plan (AS-2).
3. GitHub repository URL (also needed for the report's Appendix A.11).
Record the answers in `DECISIONS.md` (new D-entries).

## Step 1 — repository and tooling (Stage 0)
```bash
# tools: Node 20 LTS (+ Node 22 optional for --experimental-strip-types), JDK 17, Android Studio, Git
npm i -g firebase-tools
mkdir groupdeal && cd groupdeal && git init
# copy from this package: docs/, ENGINEERING_GUIDELINES.md, packages/pricing/, firestore/
firebase login
firebase init firestore functions emulators   # TypeScript functions; Firestore, Auth, Functions, Storage emulators
```
Point `firebase.json` at `firestore/firestore.rules` and `firestore/firestore.indexes.json`.

## Step 2 — pricing package first (already written)
Run `node --experimental-strip-types --test packages/pricing/test/engine.test.ts` (Node 22), or add Vitest and run it under Node 20. Expect 6 passing tests. Wire it into `functions` as a workspace dependency.

## Step 3 — backend core (Stage 5 pulled forward)
In this order, each with tests on the emulator:
1. `functions/src/guards.ts` (requireAuth, requireRole, validate, rateLimit stub) and `contracts.ts`.
2. `functions/src/domain/states.ts` with the group and membership transition tables.
3. `functions/src/groups/join.ts` and `leave.ts` exactly as PRICING_ENGINE.md §5 (transaction + idempotency key + priceHistory + activity).
4. `scripts/seed.ts`: one locality (`IN-560034`), one approved ghee deal, one OPEN group with `memberCount = 14`, 40 test users in that locality.
5. Rules tests for `groups` and `members` (SE-01, SE-02).
6. `scripts/concurrency/join-burst.ts`: GR-02 (two users at 14 → 16, exactly one priceHistory doc) and GR-03 (50 parallel joins).

## Step 4 — Android thin slice (Stages 0–1, 5)
1. Create the module structure from ANDROID_ARCHITECTURE.md §2 (start with `:app`, `:core:model`, `:core:domain`, `:core:data`, `:core:designsystem`, `:feature:group`).
2. Port `engine.ts` to `core/domain/pricing/PricingEngine.kt` (Long paise) and add a JUnit test that reads `golden-vectors.json`.
3. Connect to the emulators (dev flavor), sign in with the Auth emulator's phone flow.
4. Build `GroupDetailScreen` (TierRoadmap, PriceTag, ProgressMeter) observing `groups/{gid}` and a Join button calling `joinGroup` through `FunctionsClient` with a `requestId`.

## Definition of done for the first milestone
- [ ] Pricing tests pass in CI (TS) and on Android (golden vectors).
- [ ] GR-02 and GR-03 pass on the emulator; member counts exact; one priceHistory doc per tier crossing.
- [ ] Rules tests deny direct client writes to groups and members.
- [ ] On a device/emulator, two signed-in test users join at 14; both screens show **16 members, ₹949, 14 more for ₹799** within the listener update, with no manual refresh.

Then continue with Stage 2 (real authentication) and follow IMPLEMENTATION_ROADMAP.md in order.
