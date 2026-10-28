# Database Schema — Cloud Firestore

Conventions: money = integer paise (D-03); timestamps = Firestore `Timestamp` set by the server; `*` = required; **S** = server-only field (clients can never write it; for most collections clients can't write at all). Every server-written document carries audit fields `createdAt*`, `updatedAt*` and, where state changes, `stateHistory` is kept in a sub-collection or array (bounded).

## 0. Design reasoning
- **Groups, not deals, carry live state.** A deal is reusable; each locality round is a group with its own count, price and auction (D-04). This bounds write contention to one locality round.
- **Members as a sub-collection** (`groups/{gid}/members/{uid}`): doc ID = uid makes "one membership per user" a structural guarantee and lets the join transaction read exactly one doc.
- **Bids keyed by supplier** (`auctions/{gid}/bids/{supplierId}`): one live bid per supplier; revisions in a child collection for audit.
- **Votes keyed by user** (`requests/{rid}/votes/{uid}`): one vote per user, structurally.
- **Denormalised snapshots** on groups and orders (ADR-12).
- **No `orderItems`:** an order is one product from one group; quantity lives on the order. Multi-product carts are out of scope.
- **Watchlist = favorites** (one concept, `users/{uid}/watchlist`).
- **Activity logs** are split: public anonymised `groups/{gid}/activity`; private `adminActions`; operational logs in Cloud Logging (not Firestore).

## 1. `config/app`  (server/admin-managed)
| Field | Type | Default | Notes |
|---|---|---|---|
| platformFeeBps* | int | 0 | basis points charged to supplier at settlement |
| deliveryFeePaise* | int | 0 | added per order |
| maxQtyPerMemberDefault* | int | 3 | |
| paymentWindowHours* | int | 24 | |
| auctionDurationHoursDefault* | int | 6 | |
| bidFloorPercentOfMrp* | int | 30 | bids below floor are rejected and flagged |
| maxBidRevisions* | int | 10 | |
| maxJoinCyclesPerGroup* | int | 3 | D-20 |
| requestVoteThresholdDefault* | int | 15 | per-category override map `requestThresholds` |
| fillingRatio* | number | 0.6 | FILLING state threshold |
| localityChangeLimit30d* | int | 3 | |
| features* | map<string,bool> | | feature flags |

## 2. `users/{uid}`
| Field | Type | Req | Writer | Notes |
|---|---|---|---|---|
| displayName | string ≤ 60 | * | user | |
| phone | string E.164 | | S | from Auth |
| email | string | | S | Google sign-in |
| photoUrl | string | | user | Storage path |
| role | enum customer/supplier/admin | * | S | mirror of claim, display only |
| localityId | string | | S | set by `setUserLocality` |
| collectionPointId | string | | S | |
| geohash5 | string(5) | | S | coarse only |
| localityChanges | array<Timestamp> (≤ 5) | | S | rate limit |
| trust | map {score:int 0–1000, tier:enum, completed:int, defaults:int, onTimePayRate:int%} | * | S | |
| preferences | map {notifyTierChange:bool, notifyClosingSoon:bool, language:enum en/ta/hi} | | user | |
| status | enum ACTIVE/SUSPENDED | * | S | |
| createdAt, updatedAt | Timestamp | * | S | |

Sub-collections: `devices/{deviceId}` {fcmToken, platform, appVersion, updatedAt} (user-writable, own only) · `notifications/{nid}` {type, title, body, deepLink, groupId?, orderId?, createdAt, readAt?} (server writes, user may set `readAt`) · `watchlist/{groupId}` {addedAt}.

**Lifecycle:** created by auth trigger → ACTIVE → SUSPENDED (admin). Deletion request → anonymise PII, keep orders for accounting (retention per legal review, NOT VERIFIED).

## 3. `suppliers/{supplierUid}`
| Field | Type | Req | Notes |
|---|---|---|---|
| businessName* | string | * | |
| contactName, phone, email | string | * | |
| gstin | string | | validated format |
| fssaiLicenceNo | string | food * | |
| documents | array<{type, storagePath, uploadedAt}> | * | Storage `supplier-docs/{uid}/...`, admin-read only |
| status* | enum PENDING/VERIFIED/REJECTED/SUSPENDED | * | S; mirrored to claim `supplierStatus` |
| rejectionReason | string | | S |
| serviceAreas | array<{localityId, maxDeliveryDays}> (≤ 50) | * | via `supplierUpdateServiceAreas` |
| serviceLocalityIds | array<string> | * | S, derived for `array-contains` queries |
| score | map {rating:int x100, ratingCount, onTimeRate:int%, wins:int, fulfilled:int} | * | S |
| settlement | map {mode: MANUAL/ROUTE, routeAccountId?} | | S, admin |

## 4. `localities/{localityId}`  e.g. `IN-560034`
| Field | Type | Notes |
|---|---|---|
| pincode* | string(6) | |
| name* | string | "Koramangala" |
| city*, state* | string | |
| geohash5* | string | centroid |
| collectionPoints | array<{id, name, address, hours}> | |
| active* | bool | only active localities are selectable |
| stats | map {openGroups:int, openRequests:int} | S, maintained by triggers |

## 5. `deals/{dealId}`
| Field | Type | Req | Notes |
|---|---|---|---|
| title, description, brand, category* | string | * | category enum list in config |
| images | array<storagePath> (≤ 6) | * | |
| unit* | string | * | "1 L jar" |
| mrpPaise*, basePricePaise* | int | * | |
| tiers* | array<{minMembers:int, unitPricePaise:int}> (1–5) | * | must pass `validateSchedule` |
| scheduleVersion* | int | * | increments on edit |
| maxQtyPerMember* | int 1–10 | * | |
| capacity* | int | * | max members per group (supply cap) |
| minMembersToProceed* | int | * | default tiers[0].minMembers |
| targetMembers* | int | * | default top tier threshold (display goal) |
| groupDurationHours* | int | * | |
| auctionEnabled* | bool | * | |
| auctionDurationHours | int | | |
| anchorSupplierId | string | | proposing supplier; null for admin deals from requests |
| eligibleLocalityIds* | array<string> | * | |
| deliveryTerms | map {maxDeliveryDays:int, collectionPointOnly:bool} | * | |
| referencePricePaise | int | | admin-verified market reference (D-19) |
| sourceRequestId | string | | if converted from a request |
| status* | enum DRAFT/PENDING_REVIEW/ACTIVE/PAUSED/ARCHIVED/REJECTED | * | S |

## 6. `groups/{groupId}`  (public read)
| Field | Type | Writer | Notes |
|---|---|---|---|
| dealId*, localityId*, category* | string | S | |
| deal* | map snapshot {title, brand, unit, imagePath, mrpPaise, basePricePaise, tiers, scheduleVersion, maxQtyPerMember} | S | frozen at creation |
| anchorSupplier | map {id, name, rating} | S | |
| memberCount* | int ≥ 0 | S | active members only |
| unitsCommitted* | int ≥ 0 | S | |
| capacity*, minMembersToProceed*, targetMembers* | int | S | copied from deal |
| currentTierIndex* | int (-1..n) | S | |
| currentUnitPricePaise* | int | S | |
| savingsPercent* | int | S | |
| membersToNextTier | int \| null | S | never negative |
| state* | enum (STATE_MACHINES §2) | S | |
| failureReason | enum MIN_NOT_REACHED/NO_SUPPLIER/INSUFFICIENT_PAYMENT/ADMIN_CANCELLED | S | |
| opensAt*, closesAt* | Timestamp | S | |
| lockedAt, lockedTierIndex, lockedUnitPricePaise | | S | ceiling |
| auctionClosesAt | Timestamp | S | |
| selectedSupplier | map {id, name, bidUnitPricePaise, deliveryDays, minUnits} | S | |
| finalUnitPricePaise | int | S | ≤ lockedUnitPricePaise |
| paymentDueAt | Timestamp | S | |
| paidUnits, paidMembers | int | S | |
| version* | int | S | +1 on every write; used for stale detection |

Sub-collections:
- `members/{uid}` {uid*, quantity* 1..max, status* ACTIVE/LEFT/COMMITTED/PAID/DEFAULTED/RELEASED/REFUNDED (transitions in STATE_MACHINES §3), joinCycles*, joinedAt*, leftAt?, orderId?} — read: self/admin.
- `priceHistory/{autoId}` {fromTier, toTier, unitPricePaise, memberCount, at} — public; written only when the tier changes.
- `activity/{autoId}` {type JOINED/LEFT/TIER_UNLOCKED/LOCKED/SUPPLIER_SELECTED, memberCount, at} — public, **no user identifiers**.

**Lifecycle:** created by `adminOpenGroup` (or scheduled from deal) → state machine → COMPLETED/FAILED/CANCELLED; archived after 180 days (still readable).

## 7. `auctions/{groupId}`
| Field | Type | Notes |
|---|---|---|
| groupId*, dealId*, eligibleLocalityId* | string | |
| status* | enum OPEN/CLOSING/AWARDED/NO_VALID_BIDS/CANCELLED | |
| ceilingPaise*, floorPaise* | int | |
| unitsCommitted* | int | |
| opensAt*, closesAt* | Timestamp | |
| bidCount* | int | visible to suppliers |
| winningSupplierId | string | |
| closedAt | Timestamp | |

`bids/{supplierId}` {supplierId*, unitPricePaise*, deliveryDays*, minUnits*, status* ACTIVE/WITHDRAWN/INVALIDATED/WON/LOST, revision*, priceSetAt*, firstSubmittedAt*, updatedAt*, invalidationReason?} · `bids/{sid}/revisions/{n}` {unitPricePaise, deliveryDays, minUnits, action SUBMIT/REVISE/WITHDRAW, at, requestId}.

## 8. `orders/{orderId}`
| Field | Type | Notes |
|---|---|---|
| uid*, groupId*, dealId*, supplierId*, localityId*, collectionPointId* | string | |
| product* | map {title, unit, imagePath} | snapshot |
| quantity* | int | |
| unitPricePaise*, deliveryFeePaise*, amountPaise* | int | amount = unit × qty + fee |
| mrpPaise*, savedPaise* | int | saved = (mrp − unit) × qty |
| status* | enum (STATE_MACHINES §7) | |
| paymentDueAt* | Timestamp | |
| paymentId | string | |
| tracking | array<{status, at, note?}> (≤ 20) | |
| rated | bool | |
| refund | map {status, amountPaise, razorpayRefundId?, reason} | |

## 9. `payments/{paymentId}`
{orderId*, uid*, amountPaise*, currency* "INR", razorpayOrderId*, razorpayPaymentId?, status* (STATE_MACHINES §8), method?, attempts*, lastError?, verifiedBy (CLIENT_SIGNATURE+FETCH / WEBHOOK / RECONCILIATION)}.
`paymentEvents/{razorpayEventId}` {event, paymentId, receivedAt, processed:bool, payloadHash} — webhook dedupe (server-only).

## 10. `requests/{requestId}`
{localityId*, createdBy* uid, productName* (3–80), category*, targetPricePaise*, referencePricePaise*, voteCount* (S), threshold* (S), status* OPEN/THRESHOLD_REACHED/UNDER_REVIEW/CONVERTED/REJECTED/EXPIRED (S), convertedDealId?, expiresAt* (default 30 days), moderation {hidden:bool, reason?}}.
`votes/{uid}` {at}.

## 11. Other collections
- `ratings/{orderId}` {uid*, supplierId*, groupId*, stars* 1–5, text ≤ 500, createdAt*, hidden} — one per order (doc ID).
- `disputes/{disputeId}` {orderId*, uid*, type* MISSING/DAMAGED/WRONG_ITEM/OTHER, description*, photos[], status* OPEN/IN_REVIEW/RESOLVED_REFUND/RESOLVED_PARTIAL/REJECTED, resolution?, adminId?}.
- `fraudSignals/{id}` {type VOTE_BURST/DEVICE_REUSE/AMOUNT_MISMATCH/BID_ANOMALY/JOIN_CHURN, subjectType, subjectId, details map, severity, status OPEN/DISMISSED/ACTIONED, createdAt}.
- `adminActions/{id}` {adminUid*, action*, targetType*, targetId*, before?, after?, reason*, at*}.
- `idempotencyKeys/{uid}_{requestId}` {fn*, response map, createdAt*} — TTL policy 7 days.
- `rateLimits/{uid}_{key}` {tokens*, refilledAt*} — token-bucket state for `rateLimit()` guards (server-only).
- `stats/daily/{yyyy-mm-dd}` — admin analytics roll-ups written by `rollupDailyStats` (admin read).
- `config/admins` {emails: array} — admin allow-list for `requireAdmin` (server-only; never client-readable).

## 12. Indexes
Defined in `/firestore/firestore.indexes.json`: groups by (localityId, state, closesAt), (localityId, category, closesAt), (state, closesAt); auctions by (status, closesAt), (eligibleLocalityId, status, closesAt); requests by (localityId, status, voteCount desc); orders by (uid, createdAt desc), (supplierId, status, createdAt desc), (status, paymentDueAt); payments by (status, updatedAt); members collection group by (uid, status, joinedAt desc) for "My Groups".

TTL policies: `idempotencyKeys.createdAt` (7 days), `users/*/notifications.createdAt` (90 days).

## 13. Storage layout
`deal-images/{dealId}/{n}.webp` (public read via signed/download URLs) · `supplier-docs/{uid}/{file}` (owner write, admin read) · `dispute-photos/{disputeId}/{n}.jpg` (owner write, admin read) · `avatars/{uid}.webp`. Storage rules restrict content type and size (images ≤ 5 MB, docs ≤ 10 MB).
