# Security Architecture

## 1. Layers
Identity (Firebase Auth) → Client attestation (App Check) → Data access (Firestore/Storage rules) → Business authorisation (function guards) → Invariants (transactions) → Detection (fraud signals, alerts) → Accountability (audit logs).

## 2. Identity and roles
- Phone OTP (primary) and Google sign-in (D-28). SMS region policy restricted to India; Auth abuse protection (SMS fraud controls) enabled — exact console options NOT VERIFIED.
- Custom claims `{role: 'customer'|'supplier'|'admin', supplierStatus?: 'PENDING'|'VERIFIED'|'REJECTED'|'SUSPENDED'}` set only by `onUserCreated`, `supplierRegister` (→ supplier, PENDING) and admin functions. Clients force-refresh the ID token after a claim change.
- Admin bootstrap: `/scripts/grant-admin.ts` run locally with a service account against prod, once; afterwards only `adminSetRole` (requires an existing admin, audited, cannot demote the last admin).
- Admin console requires Google accounts with 2-Step Verification (organisational policy) — enforced by allow-list of admin emails in `config/admins` checked in `requireAdmin`.
- Suspended users: `users.status = SUSPENDED` checked in every guard; tokens revoked with `revokeRefreshTokens`.

## 3. Function guard library (`functions/src/guards.ts`)
`requireAuth(ctx)` · `requireAppCheck(ctx)` (enforced via options) · `requireRole(ctx, role)` · `requireVerifiedSupplier(ctx, tx)` (claim + live doc check) · `requireAdmin(ctx)` · `requireOwner(doc, uid)` · `requireActiveUser(tx, uid)` · `rateLimit(uid, key, limit, window)` (Firestore token bucket in `rateLimits/{uid}_{key}`, server-only) · `validate(schema, data)` (zod).

## 4. Security-sensitive operations (complete list)
| Operation | Risk | Controls |
|---|---|---|
| Sign-in / sign-up | fake accounts, SMS pumping | OTP, App Check, Auth fraud protection, device fingerprint signals (V1) |
| Set role / verify supplier | privilege escalation | admin-only, audited, claims not readable/writable by clients |
| Set locality | locality hopping to farm deals | server-resolved locality, 3 changes / 30 days |
| joinGroup / leaveGroup / setQuantity | count/price manipulation, churn griefing | callable-only, transaction, join-cycle limit, rate limit 30/min/user |
| createRequest | spam | 3 requests/day/user, text moderation (length, blocklist), App Check |
| voteRequest | vote stuffing | one vote doc per uid, account age ≥ 24 h to vote, VOTE_BURST signal |
| supplierProposeDeal | misleading MRP | admin approval, reference price check (D-19) |
| submitBid / withdrawBid | fake/shill bids, sniping | verified supplier, floor/ceiling, sealed, withdrawal cut-off, `consumeAppCheckToken` |
| Award auction | rigging | server-only scheduled function, deterministic ranking, audit hash |
| createPaymentOrder | amount tampering | amount from DB only |
| verifyPayment / webhook | forged success, replay | HMAC verification, server fetch, event dedupe, amount/currency check |
| Refunds | unauthorised refunds | admin or system only, reason required, audited, Razorpay idempotency via payment ID |
| Fulfilment updates | fake delivery | supplier must own the order; DELIVERED requires collection-point OTP or customer confirmation (V1) |
| Ratings | fake reviews | only after DELIVERED, one per order |
| Disputes | abuse | one per order, photo evidence, admin resolution |
| Admin config changes | business sabotage | admin-only, audited with before/after |
| File uploads | malware, oversized files | Storage rules: content-type allow-list, size limits, owner path |

## 5. Values never trusted from clients
Member counts, units, tier, any price or amount, order status, payment status, role/claims, trust score, supplier status, auction results, timestamps, locality of another user, supplier IDs on fulfilment (taken from the order).

## 6. Rules
`/firestore/firestore.rules` (in this package). Principle: public read for catalogue/live group state; owner read for private data; **no client writes** except own profile display fields, own devices, own notification `readAt`, own watchlist. Rules tests (TEST_STRATEGY §3) must cover every `allow`.

## 7. Secrets and keys
| Secret | Storage | Access |
|---|---|---|
| Razorpay key secret, webhook secret | Secret Manager per project | bound to payment functions only |
| Razorpay key ID | function returns it at checkout | public by design |
| Google Maps server key (Geocoding) | Secret Manager | `resolveLocality` only; API-restricted |
| Places SDK Android key | `local.properties` → Secrets Gradle plugin | restricted to package + SHA-256 + Places API |
| Firebase web config | public | protected by App Check + rules |
| Android upload keystore | GitHub encrypted secrets / local only | Play App Signing holds the app signing key |
| Service accounts | Workload Identity Federation for GitHub Actions | no JSON keys in CI |
Never commit `.env*`, `google-services.json` for prod (CI injects), keystores or service-account JSON. `gitleaks` runs in CI.

## 8. Abuse prevention and fraud signals
VOTE_BURST (> N votes on a request from accounts < 7 days old within 1 h) · JOIN_CHURN (≥ 3 join/leave cycles across groups in 24 h) · DEVICE_REUSE (same FCM installation across > 3 accounts — V1) · AMOUNT_MISMATCH (webhook amount ≠ order) · BID_ANOMALY (below floor attempts; winning bid < 60% of median past awards for the deal category) · DEFAULT_PATTERN (≥ 2 payment defaults in 30 days → join requires trust ≥ threshold). Signals are reviewed by admins; automated action limited to rate limiting and soft blocks.

## 9. Privacy
Data minimisation: no precise location stored; group member lists not public; activity feed anonymised; phone numbers visible only to the user and admins; suppliers see a customer's first name + collection point only for their own orders. Retention and deletion policy and DPDP Act 2023 compliance require legal review (NOT VERIFIED). Analytics events contain no PII.

## 10. Logging and audit
Structured JSON logs (`requestId`, `uid`, `fn`, `groupId`, outcome, latency) without phone numbers or payment payloads. `adminActions` for every admin and system award/refund action. Log-based alerts: webhook signature failures, reconciliation exceptions, function error rate, refund failures, admin role changes.
