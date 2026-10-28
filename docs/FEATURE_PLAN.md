# N. New Feature Plan

Evaluation rule: a feature ships only if it (a) increases the probability that a group reaches a better tier, (b) increases trust/safety, or (c) reduces operational cost. Complexity: S (≤ 3 days), M (≤ 2 weeks), L (> 2 weeks) for the two-person team.

| Feature | User value | Technical design | Data | Backend | Security | Cx | Release |
|---|---|---|---|---|---|---|---|
| **Tier-reach notifications** | Members learn the price dropped; watchers learn a deal is near a tier | `onPriceHistoryCreated` → topic `group_<gid>`; "1 more for ₹X" when `membersToNextTier ≤ 2` (debounced 30 min) | priceHistory, watchers | trigger, FCM | no PII in payload | S | **MVP** |
| **Deal countdown** | Urgency, clarity on lock time | `CountdownChip(closesAt)`; "closes around" wording (D-16) | closesAt | none | none | S | **MVP** |
| **Group momentum indicator** | Shows if the group is likely to reach the next tier | joins in last 24 h from `activity` count, shown as "+6 today" (no prediction) | activity | aggregate field `joins24h` updated hourly | anonymised | S | **MVP** |
| **Shareable deal links** | Members recruit neighbours → more tiers reached (core growth loop) | `/g/{gid}` SSR page + Android App Links; share sheet with "12 more for ₹799" | group | web SSR read | public data only; no member info | S | **MVP** |
| **Deal activity timeline** | Transparency ("tier unlocked at 15") | list from `activity` + `priceHistory` | existing | none | anonymised | S | **MVP** |
| **Savings dashboard** | Shows value delivered → retention | sum of `savedPaise` over PAID/DELIVERED orders, monthly rollup in user doc | orders | trigger on order DELIVERED | own data only | S | V1 |
| **Favorites / Watchlist** | Get reminded before close | `users/{uid}/watchlist`; "closing in 3 h" sweeper notification | watchlist | sweeper | rules: self only | S | V1 |
| **Supplier reputation** | Trust when choosing deals; auction tiebreak | ratings → `suppliers.score` (avg ×100, count, on-time %) | ratings, orders | trigger | ratings only after delivery | M | **MVP** (needed for tiebreak) |
| **Dispute support** | Recourse for damaged/missing goods | disputes collection + admin workflow + refunds | disputes, photos | callables, admin | evidence, one per order | M | **MVP** |
| **Price history** | Proof that group buying beats MRP over time | per-deal series of `finalUnitPricePaise` per completed group | groups | rollup | public | S | V1 |
| **Estimated time-to-next-tier** | Helps decide to wait vs share | simple rate = joins in last 24 h; "at the current pace, about X h" only if ≥ 3 joins/24 h, otherwise hidden; labelled estimate | activity | hourly aggregate | none | S | V1 |
| **Personalised locality feed** | Relevant deals first | rank by category affinity (past orders/votes) then closing time; rule-based | orders, votes | callable `getFeedRanking` or client-side sort | own data | M | V1 |
| **Smart reorder** | Repeat staples quickly | "notify me when this deal reopens" → subscription doc; triggered when a group opens for the same deal in the locality | subscriptions | trigger on group create | self only | S | V1 |
| **Supplier analytics** | Suppliers price better | win rate, avg award vs ceiling, ratings | auctions, orders | rollups | supplier sees own only | M | V1 |
| **Admin analytics** | Operate the marketplace | daily rollups (GMV, fail reasons, conversion) | all | `rollupDailyStats` | admin only | M | V1 |
| **Suspicious activity detection** | Protects fairness | rule-based signals (SECURITY §8) | votes, joins, payments | triggers | admin review | M | MVP (rules) / V1 (dashboard) |
| **Tamil/Hindi localisation** | Reach beyond English users | string resources, locale switch | — | notification templates per language | — | M | V1 |
| **Referral rewards** | Growth | credits on referee's first delivered order | referrals | callable + trigger | abuse risk (self-referral) → device/phone checks | M | FUTURE |
| **Smart deal recommendations (ML)** | Discovery | needs data volume first; start rule-based | orders | — | — | L | FUTURE |
| **Demand forecasting (ML)** | Suppliers plan supply | needs ≥ several months of history | requests, orders | offline model | aggregated only | L | FUTURE |
| **Locality demand heatmap** | Admin/supplier planning | aggregate open requests + joins per locality on a map (admin web) | aggregates | rollup | aggregated, no individuals | M | FUTURE |
| **Delivery tracking integration** | Live courier tracking | courier API adapter | shipments | webhooks | — | L | FUTURE |

**Rejected for now:** in-app chat between members (moderation burden, privacy), dynamic ML pricing (contradicts transparent tier rule), cash on delivery (breaks pay-after-lock guarantees for suppliers).
