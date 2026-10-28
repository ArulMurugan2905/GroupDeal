# API / Service Contracts (Callable Cloud Functions)

All callables: region `asia-south1`, Firebase Auth required unless stated, App Check enforced, input validated with zod, errors as Firebase `HttpsError` codes. Every mutating call includes `requestId: string (UUID v4)`; the server stores the response in `idempotencyKeys/{uid}_{requestId}` and returns it for repeats (D-21). Money = integer paise.

**Client retry policy:** retry with the *same* `requestId` on `unavailable`, `deadline-exceeded`, `aborted`, `internal` (exponential backoff 1 s, 2 s, 4 s, max 3). Never retry `invalid-argument`, `permission-denied`, `failed-precondition`, `already-exists`, `resource-exhausted`.
**Logging:** every call logs `{fn, requestId, uid, outcome, errorCode?, latencyMs}`.

## Auth & profile
| Function | Request | Guards / validation | Processing | Response | Errors |
|---|---|---|---|---|---|
| `onUserCreated` (trigger) | — | — | create `users/{uid}` (trust 500 SILVER), claim customer | — | — |
| `updateProfile` | `{displayName?, photoPath?, preferences?, requestId}` | auth; name 1–60 chars | update allowed fields | `{ok}` | invalid-argument |

## Locality
| `resolveLocality` | `{lat, lng}` (not stored) | auth; rate 10/h | server Geocoding → pincode → nearest active locality | `{candidates: [{localityId, name, pincode, distanceKm}]}` | not-found (no active locality) |
| `setUserLocality` | `{localityId, collectionPointId, requestId}` | auth; locality active; ≤ 3 changes/30 days; no ACTIVE memberships in old locality pre-lock (must leave first) | update user | `{localityId}` | resource-exhausted, failed-precondition |

## Groups & pricing
| `joinGroup` | `{groupId, quantity, requestId}` | customer; PRICING_ENGINE §5 | transaction | `{memberCount, unitPricePaise, tierIndex, savingsPercent, membersToNextTier, state, version}` | failed-precondition (CLOSED, CAPACITY, LOCKED), permission-denied (LOCALITY), already-exists, resource-exhausted (JOIN_LIMIT) |
| `leaveGroup` | `{groupId, requestId}` | customer; ACTIVE member; pre-lock | transaction; recompute tier and state | same shape | failed-precondition, not-found |
| `setQuantity` | `{groupId, quantity, requestId}` | 1..max; pre-lock | transaction | `{quantity, unitsCommitted}` | invalid-argument |
| `quotePreview` | none — clients use the shared engine locally on group snapshot data; no endpoint needed | | | | |

## Requests & votes
| `createRequest` | `{productName, category, targetPricePaise, referencePricePaise, requestId}` | customer with locality; 3/day; name 3–80; target < reference | create OPEN request in user's locality, auto-vote by creator | `{requestId, voteCount: 1, threshold}` | resource-exhausted, invalid-argument |
| `voteRequest` | `{requestId_, requestId}` | same locality; account ≥ 24 h; OPEN | tx: create `votes/{uid}`, `voteCount+1`; ≥ threshold → THRESHOLD_REACHED | `{voteCount, status}` | already-exists |
| `unvoteRequest` | `{requestId_, requestId}` | voted; OPEN | tx: delete vote, −1 | `{voteCount}` | not-found |

## Watchlist & notifications
Direct Firestore writes allowed by rules (`users/{uid}/watchlist/{gid}`, `notifications/{nid}.readAt`). `registerDevice` is a direct write to `users/{uid}/devices/{installationId}`.

## Supplier
| `supplierRegister` | `{businessName, contactName, gstin?, fssaiLicenceNo?, documents[], requestId}` | auth; not already supplier | create supplier PENDING; claim role=supplier, supplierStatus=PENDING | `{status}` | already-exists |
| `supplierUpdateServiceAreas` | `{areas: [{localityId, maxDeliveryDays}], requestId}` | verified supplier; ≤ 50 | update | `{count}` | invalid-argument |
| `supplierProposeDeal` | deal fields | verified; `validateSchedule` = [] | create deal PENDING_REVIEW, anchor = caller | `{dealId}` | invalid-argument (with validation codes) |
| `submitBid` | `{groupId, unitPricePaise, deliveryDays, minUnits, requestId}` | SUPPLIER_AUCTION §4 | tx | `{revision, status}` | failed-precondition (AUCTION_CLOSED), invalid-argument (ABOVE_CEILING, BELOW_FLOOR), resource-exhausted (REVISIONS) |
| `withdrawBid` | `{groupId, requestId}` | before cut-off | tx | `{status: WITHDRAWN}` | failed-precondition |
| `supplierUpdateFulfilment` | `{groupId, status: CONFIRMED/DISPATCHED/READY_FOR_PICKUP, requestId}` or `{orderId, status: DELIVERED, pickupCode?}` | selected supplier of the group | update orders (batch ≤ 500) | `{updated}` | permission-denied |

## Orders & payments
| `createPaymentOrder` | `{orderId, requestId}` | owner; AWAITING_PAYMENT; before due | PAYMENTS §2 | `{keyId, razorpayOrderId, amountPaise, prefill}` | failed-precondition (WINDOW_CLOSED) |
| `verifyPayment` | `{orderId, razorpayOrderId, razorpayPaymentId, signature, requestId}` | owner | HMAC + fetch + tx | `{status: PAID|PENDING}` | permission-denied (SIGNATURE), failed-precondition (AMOUNT_MISMATCH) |
| `cancelOrder` | `{orderId, requestId}` | owner; AWAITING_PAYMENT | CANCELLED, member DEFAULTED (small penalty) | `{status}` | failed-precondition |
| `razorpayWebhook` (HTTPS) | raw Razorpay event | signature header | PAYMENTS §2.4 | 200 / 400 (bad signature) / 500 (retry) | — |

## Ratings & disputes
| `submitRating` | `{orderId, stars, text?, requestId}` | owner; DELIVERED; not rated | create `ratings/{orderId}`; trigger updates scores | `{ok}` | already-exists |
| `raiseDispute` | `{orderId, type, description, photoPaths[], requestId}` | owner; PAID/DELIVERED within 7 days of delivery | create dispute OPEN | `{disputeId}` | failed-precondition |

## Admin (all audited to `adminActions`, `requireAdmin`)
`adminVerifySupplier {supplierId, decision, reason?}` · `adminSetRole {uid, role}` · `adminApproveDeal {dealId, decision, reason?}` · `adminOpenGroup {dealId, localityId, opensAt?}` · `adminExtendGroup {groupId, hours ≤ 48}` (once per group) · `adminCancelGroup {groupId, reason}` · `adminConvertRequest {requestId, dealDraft}` · `adminInvalidateBid {groupId, supplierId, reason}` · `adminRefundOrder {orderId, amountPaise ≤ captured, reason}` · `adminResolveDispute {disputeId, resolution, refundPaise?}` · `adminUpsertLocality {...}` · `adminUpdateConfig {patch}` · `adminSuspendUser {uid, reason}`.

## Analytics (admin/supplier read models)
MVP: computed by scheduled function `rollupDailyStats` into `stats/daily/{yyyy-mm-dd}` (GMV, groups closed/failed by reason, average savings %, payment conversion) and `suppliers/{sid}.score`. No numbers are pre-populated.

## Shared response types (TypeScript, `functions/src/contracts.ts`, mirrored as Kotlin data classes)
```ts
type GroupStateDTO = { groupId: string; memberCount: number; unitPricePaise: number; tierIndex: number;
  savingsPercent: number; membersToNextTier: number | null; state: GroupState; version: number };
type ErrorDetail = { reason: 'CLOSED'|'CAPACITY'|'LOCKED'|'LOCALITY'|'JOIN_LIMIT'|'ABOVE_CEILING'|'BELOW_FLOOR'|
  'AUCTION_CLOSED'|'REVISIONS'|'WINDOW_CLOSED'|'SIGNATURE'|'AMOUNT_MISMATCH' };
```
