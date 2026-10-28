# R. Top Architectural Risks and Mitigations

| # | Risk | Likelihood / impact | Mitigation | Owner stage |
|---|---|---|---|---|
| 1 | Hot group document contention during viral deals (AS-3) | Medium / High | Transactions + client retries; load test; Cloud Tasks join queue fallback; capacity caps | 5, 15 |
| 2 | Non-payment after lock makes supplier economics fail | High / High | `minUnits` in bids, payment window reminders, trust penalties, join gating for repeat defaulters, anchor obligation priced with expected default rate | 7, 9, 11 |
| 3 | Not enough verified suppliers per locality → auctions empty | High / High | Anchor-supplier deals as the default; auctions optional per deal; start in 1–2 localities | 4, 7 |
| 4 | Locality density too low to reach tiers | High / Medium | Tiers designed per locality size; share links; request-driven deals only; widen locality to pincode clusters (FUTURE) | 3, 13 |
| 5 | Payment edge cases (lost webhooks, duplicate capture, late payment) | Medium / Critical | Server verification + webhook + reconciliation + auto refunds; alerts | 9 |
| 6 | Client manipulation of price/state | Medium / Critical | No client writes; App Check; rules tests | 5, 14 |
| 7 | Fake accounts inflating groups or votes | Medium / High | Phone OTP, account-age gate for votes, join-cycle limits, fraud signals | 2, 14 |
| 8 | Supplier fraud (fake bids, non-delivery) | Medium / High | Document verification, floor price, sealed bids, collection-point pickup confirmation, suspension | 7, 12 |
| 9 | Scheduler delays at deadlines | Low / Medium | 5-minute sweeps, "closes around" wording, transactions re-check deadlines, lag alert | 6, 16 |
| 10 | Cost overrun (reads from listeners, SMS OTP) | Medium / Medium | Paged queries, listener scoping by locality, budget alerts, SMS region policy | 4, 16 |
| 11 | Scope creep vs two-person team | High / High | MVP fixed to Stages 0–10 + 14–16; feature flags; V1 list frozen | all |
| 12 | Legal/compliance gaps (DPDP, food safety, tax) | Medium / High | Legal/accounting review before live payments; restrict MVP categories | 16 |
| 13 | Platform parity drift in pricing | Low / High | Single TS source + golden vectors enforced in Android CI | 0 |
| 14 | Vendor dependency (Firebase, Razorpay) | Low / Medium | Gateway interface in functions; data export scripts | 9, 16 |
