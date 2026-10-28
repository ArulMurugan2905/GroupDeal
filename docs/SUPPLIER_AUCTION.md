# Supplier Reverse Auction

## 1. Model
**Sealed-bid, first-price reverse auction** per locked group (ADR-08). Buyers' side = the locked group; sellers = verified suppliers serving the locality. Lowest valid bid wins; the price paid by customers is the winning bid (≤ ceiling).

## 2. Creation (inside the lock transaction, `sweepGroups`)
Preconditions: group due (`closesAt ≤ now` or `lockRequested`), state pre-lock, `memberCount ≥ minMembersToProceed`, `deal.auctionEnabled`.
Writes: group → LOCKED → BIDDING (`lockedUnitPricePaise`, `auctionClosesAt = now + auctionDurationHours`); members ACTIVE → COMMITTED; `auctions/{gid}` = {status OPEN, ceilingPaise = lockedUnitPricePaise, floorPaise = ceil(mrp × bidFloorPercentOfMrp / 100), unitsCommitted, closesAt, bidCount 0}. If `auctionEnabled = false` and an anchor exists → directly SUPPLIER_SELECTED at the locked price.
Notification: verified suppliers whose `serviceLocalityIds` contain the locality (query `array-contains`).

## 3. Eligibility (checked on every bid)
- Auth claims: `role = supplier`, `supplierStatus = VERIFIED` (re-read `suppliers/{sid}.status` in the transaction to catch suspensions issued after the token was minted).
- Serves the locality with `maxDeliveryDays` ≤ deal `deliveryTerms.maxDeliveryDays`.
- Not suspended; no open fraud signal of severity HIGH.

## 4. `submitBid` (create or revise)
Request: `{groupId, unitPricePaise, deliveryDays, minUnits, requestId}`.
Validation (in transaction): auction OPEN and `now < closesAt`; `floorPaise ≤ unitPricePaise ≤ ceilingPaise`; integer paise; `1 ≤ deliveryDays ≤ deal max`; `1 ≤ minUnits ≤ unitsCommitted`; `revision < maxBidRevisions`.
Writes: `bids/{sid}` (ACTIVE, revision+1, `priceSetAt` = now only if the price changed, `firstSubmittedAt` on create), `revisions/{n}`, `auction.bidCount` (+1 on create only), idempotency key.
Response: `{status: ACTIVE, revision, rankHidden: true}`. Suppliers never learn other bids or their rank (sealed).

## 5. `withdrawBid`
Allowed until `closesAt − 30 min` (anti-sniping of withdrawals, configurable). Sets WITHDRAWN, writes revision, `bidCount −1`. A withdrawn supplier may re-bid (counts toward revisions). Anchor suppliers may withdraw their *bid*, but they keep the anchor obligation at the ceiling.

## 6. Invalid bids
| Case | Handling |
|---|---|
| Above ceiling / below floor / after deadline / not eligible | Rejected synchronously (`invalid-argument` / `failed-precondition`); below-floor attempts also create a BID_ANOMALY fraud signal |
| Supplier suspended after bidding | Bid INVALIDATED at close (status re-read) |
| Admin invalidation (evidence of collusion, fake supplier) | `adminInvalidateBid` with reason → INVALIDATED, audited |

## 7. Winner selection (`sweepAuctions`, server-only, idempotent)
```
for auction where status = OPEN and closesAt ≤ now:
  runTransaction:
    a ← get(auction); if a.status ≠ OPEN → return            // another run already closed it
    a.status ← CLOSING
    bids ← all bids with status ACTIVE
    valid ← bids where supplier still VERIFIED and serves locality and floor ≤ price ≤ ceiling
    ranked ← sort valid by (unitPricePaise ASC, supplierScore.rating DESC, deliveryDays ASC, priceSetAt ASC, supplierId ASC)
    if ranked not empty:
       w ← ranked[0]; mark w WON, others LOST (INVALIDATED stay INVALIDATED)
       group: SUPPLIER_SELECTED, selectedSupplier = w, finalUnitPricePaise = finalUnitPrice(ceiling, w.price)
       auction: AWARDED, winningSupplierId
    else if group.anchorSupplier exists:
       group: SUPPLIER_SELECTED with anchor at ceiling, minUnits = minMembersToProceed; auction: NO_VALID_BIDS
    else:
       group: FAILED(NO_SUPPLIER); auction: NO_VALID_BIDS; members → released; notify
  after commit (outside tx, idempotent): createOrdersForGroup(gid) → one order per COMMITTED member
```
Firestore transactions have a document-count limit per transaction; with ≤ 50 bids per auction this fits. Above that, ranking is done first (read-only), then the award transaction re-reads only the winner, the auction and the group and checks `auction.status = OPEN`.

**Tie handling:** deterministic chain above; `supplierId` as final tiebreaker guarantees a total order.

**Reserve price:** the ceiling is the reserve (customers never pay more than the locked price). The floor protects against fake or predatory bids that would fail to deliver.

## 8. Order conversion
`createOrdersForGroup` (idempotent: order ID = `{gid}_{uid}`): for each COMMITTED member, create `orders/{gid}_{uid}` with snapshot, `unitPricePaise = finalUnitPricePaise`, `amountPaise`, status AWAITING_PAYMENT, `paymentDueAt = now + paymentWindowHours`; group → PAYMENT_WINDOW. Notifications: "Supplier selected — pay ₹X by HH:MM" to members; win/loss to suppliers.

## 9. Anti-manipulation
| Threat | Control |
|---|---|
| Fake supplier accounts | Document verification by admin; FSSAI/GSTIN checks; one supplier per verified business |
| Shill bidding / collusion | Sealed bids; bids from suppliers sharing bank/GST/device flagged; admin review of winning bids far below the median of past awards |
| Bid sniping / last-second withdrawal | Withdrawals blocked in the last 30 min |
| Low-ball then default | Floor; supplier on-time and fulfilment score in tiebreak; suspension after failed fulfilment |
| Probing ceiling | Ceiling is shown openly (it is the customers' locked price); no information leak beyond that |
| Replay of bid requests | Idempotency keys + `consumeAppCheckToken` |

## 10. Audit
Every bid action → `revisions`; every award → group `activity` + `adminActions` (system actor) record with ranked list hash; admin invalidations require a reason. Auction data retained for 3 years (policy to confirm).
