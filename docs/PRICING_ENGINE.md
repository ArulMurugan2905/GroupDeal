# Pricing Engine Specification

Implementation: `/packages/pricing/src/engine.ts` (TypeScript, zero dependencies, **executed and tested** — 6/6 tests pass against `golden-vectors.json`). Android ports the same functions in Kotlin (`core/domain/pricing/PricingEngine.kt`) for display previews and must pass the same JSON vectors in a unit test.

## 1. Model
- **Schedule** (frozen into the group at creation): `mrpPaise`, `basePricePaise`, `tiers[] = {minMembers, unitPricePaise}` ascending.
- **Metric:** active member count `n` (D-05). Quantity per member is 1..`maxQtyPerMember`; it changes `unitsCommitted`, not the tier.
- **Price rule (all-units, group-wide):** every unit of every member is charged the price of the highest tier whose `minMembers ≤ n`; below the first tier, the base price.

## 2. Validation (`validateSchedule`) — enforced when a deal is proposed/approved and when a group is opened
| Rule | Error |
|---|---|
| at least one tier | NO_TIERS |
| all money integers, > 0 | NON_INTEGER_MONEY, NON_POSITIVE_PRICE |
| base ≤ MRP | BASE_ABOVE_MRP |
| first tier price < base | FIRST_TIER_NOT_BELOW_BASE |
| first threshold ≥ 2 | THRESHOLD_BELOW_2 |
| thresholds strictly increasing | THRESHOLD_NOT_INCREASING |
| prices strictly decreasing | PRICE_NOT_DECREASING |
Additional business checks in `supplierProposeDeal`: `capacity ≥ top threshold`, `minMembersToProceed ≤ capacity`, 1 ≤ tiers ≤ 5.

## 3. Quote (`quote(schedule, n)`)
```
n ← max(0, n)                                   // never negative
tierIndex ← −1
for i in 0..tiers.size−1: if n ≥ tiers[i].minMembers then tierIndex ← i
unitPrice ← (tierIndex = −1) ? basePrice : tiers[tierIndex].unitPrice
savingsPaise ← max(0, mrp − unitPrice)
savingsPercent ← floor(savingsPaise × 100 / mrp)     // D-23: never overstate
next ← tierIndex + 1 < tiers.size ? tierIndex + 1 : none
membersToNextTier ← next = none ? null : tiers[next].minMembers − n   // > 0 always
```
Complexity O(k), k ≤ 5. Pure, deterministic, integer-only.

## 4. Worked validation — A2 Gir Cow Ghee 1 L
MRP ₹1,400 (140000 p) · base ₹1,250 · 5+ ₹1,099 · 15+ ₹949 · 30+ ₹799.

| n | Active tier | Unit price | Savings | Savings % | Members to next |
|---|---|---|---|---|---|
| 0 / negative | base | ₹1,250 | ₹150 | 10% | 5 |
| 1 | base | ₹1,250 | ₹150 | 10% | 4 |
| 4 | base | ₹1,250 | ₹150 | 10% | 1 |
| 5 | tier 1 | ₹1,099 | ₹301 | 21% | 10 |
| 14 | tier 1 | ₹1,099 | ₹301 | 21% | 1 |
| 15 | tier 2 | ₹949 | ₹451 | 32% | 15 |
| **18** | **tier 2** | **₹949** | **₹451** | **32%** | **12** |
| 29 | tier 2 | ₹949 | ₹451 | 32% | 1 |
| 30 | tier 3 | ₹799 | ₹601 | 42% | — (top) |
| 35 | tier 3 | ₹799 | ₹601 | 42% | — (top) |

At 18 members: tiers examined 5, 15, 30 → highest satisfied = 15 → ₹949. Savings ⌊(140000 − 94900) × 100 / 140000⌋ = ⌊32.2⌋ = 32%. Next tier 30 → 30 − 18 = 12. Locality saving (all qty 1) = (140000 − 94900) × 18 = 811800 p = ₹8,118. With mixed quantities, locality saving = (mrp − price) × `unitsCommitted`.

## 5. Concurrency: authoritative membership transaction
```
joinGroup(uid, groupId, quantity, requestId):            // Cloud Function, Admin SDK
  requireAuth; requireRole(customer); requireAppCheck
  validate 1 ≤ quantity ≤ group.deal.maxQtyPerMember (re-read inside tx)
  runTransaction(tx):
    idem ← tx.get(idempotencyKeys/{uid}_{requestId}); if exists → return idem.response
    g ← tx.get(groups/{groupId}); m ← tx.get(groups/{groupId}/members/{uid}); u ← tx.get(users/{uid})
    assert g.state ∈ {OPEN, FILLING, TARGET_REACHED} and now < g.closesAt           else FAILED_PRECONDITION
    assert u.localityId = g.localityId and u.status = ACTIVE                          else PERMISSION_DENIED
    assert not (m.exists and m.status = ACTIVE)                                       else ALREADY_EXISTS
    assert (m.joinCycles ?? 0) < config.maxJoinCyclesPerGroup                         else RESOURCE_EXHAUSTED
    assert g.memberCount < g.capacity                                                 else FAILED_PRECONDITION(CAPACITY)
    n ← g.memberCount + 1; units ← g.unitsCommitted + quantity
    q ← quote(g.deal, n)
    state ← n ≥ g.targetMembers ? TARGET_REACHED : (n ≥ ceil(fillingRatio × target) ? FILLING : OPEN)
    tx.set(member{uid, quantity, status: ACTIVE, joinCycles: (m.joinCycles ?? 0) + 1, joinedAt: serverTs})
    tx.update(group{memberCount: n, unitsCommitted: units, currentTierIndex: q.tierIndex,
              currentUnitPricePaise: q.unitPricePaise, savingsPercent: q.savingsPercent,
              membersToNextTier: q.membersToNextTier, state, version: g.version + 1, updatedAt: serverTs})
    if q.tierIndex ≠ g.currentTierIndex: tx.create(priceHistory{from, to, price, n, at})
    tx.create(activity{JOINED, n, at})
    if n = g.capacity: tx.update(group{lockRequested: true})        // sweeper locks within ≤ 5 min
    response ← {memberCount: n, unitPricePaise, tierIndex, membersToNextTier, state, version}
    tx.set(idempotencyKeys/{uid}_{requestId}, {fn: 'joinGroup', response, createdAt})
  return response
```
`leaveGroup` mirrors this: requires an ACTIVE membership and a pre-lock state; `n ← memberCount − 1` (assert ≥ 0), `units ← unitsCommitted − m.quantity`, recompute quote **and state** (fixes D1), member status LEFT.

`setQuantity` changes `unitsCommitted` only (no tier change), pre-lock only.

### What the transaction guarantees
| Hazard | Prevention |
|---|---|
| Double counting (double tap, retry) | Member doc ID = uid + ACTIVE check; idempotency key returns the first response |
| Stale counts | Count is read inside the transaction; Firestore retries the transaction on contention, so the write is based on the latest committed count |
| Race at a tier boundary (two users at 14 → 15, 16) | Transactions serialise on the group doc; each recomputes the tier from its own `n` |
| Invalid tier selection | Tier is a pure function of the committed `n`; clients cannot send tier or price |
| Negative counts | Leave requires an ACTIVE member doc; `assert n ≥ 0`; `quote` clamps |
| Inconsistent displayed prices | Single writer; every client renders `currentUnitPricePaise` from the same doc; `version` lets clients drop out-of-order snapshots |
| Client manipulation | Callable takes only `groupId`, `quantity`, `requestId`; rules deny direct writes to groups/members |
| Join after deadline | `now < closesAt` checked in the transaction; sweeper locks shortly after |

**Hot groups (AS-3):** Firestore guidance suggests keeping sustained writes to one document around one per second. Expected locality joins are far below this, but a popular deal could burst. Mitigation (designed, not MVP): when contention errors exceed a threshold, route joins through a per-group Cloud Tasks queue with concurrency 1. Validate with the load test in TEST_STRATEGY.md before launch.

## 6. Lifecycle pricing
| Event | Effect on price |
|---|---|
| Join | Recompute; may drop to a lower tier |
| Leave (pre-lock) | Recompute; may rise to a higher tier; watchers notified |
| Quantity change | No tier change; `unitsCommitted` updated |
| Lock (deadline/capacity) | `lockedTierIndex`, `lockedUnitPricePaise ← currentUnitPricePaise` = auction ceiling; no further membership changes |
| Below minimum at lock | FAILED (MIN_NOT_REACHED); no charges (D-07) |
| Auction award | `finalUnitPricePaise ← finalUnitPrice(locked, winningBid)` ≤ locked |
| No valid bids + anchor | final = locked; anchor supplier fulfils |
| No valid bids, no anchor | FAILED (NO_SUPPLIER) |
| Payment defaults | Price unchanged (D-09); `paidUnits` vs `minUnits` decides continue/fail |
| Admin cancellation | CANCELLED; refunds for any captured payments |

Order amount (server): `amountPaise = finalUnitPricePaise × quantity + deliveryFeePaise`; `savedPaise = (mrpPaise − finalUnitPricePaise) × quantity`.

## 7. Edge cases checklist
Exactly-on-threshold (≥ applies) · above top tier (no next tier, value null not negative) · tiers out of order (rejected at validation; engine assumes validated input) · duplicate thresholds (rejected) · price increases with members (rejected) · base above MRP (rejected) · join exactly at `closesAt` (rejected: `now < closesAt`) · leave after lock (rejected) · capacity reached (join rejected, lock requested) · schedule edited while group open (no effect: group holds snapshot; new `scheduleVersion` applies to new groups) · rounding (integers only; floor on percentage) · bid above ceiling (rejected; `finalUnitPrice` throws).

## 8. Parity across platforms
`golden-vectors.json` lives in `/packages/pricing/test/` and is copied into Android test resources by a Gradle task. CI fails if any platform disagrees. The Android preview is labelled "Estimated — confirmed by server".
