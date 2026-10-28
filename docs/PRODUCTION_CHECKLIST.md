# Production Readiness Checklist

## Product & legal
- [ ] Terms of use, privacy policy, refund & cancellation policy published (legal review — NOT VERIFIED)
- [ ] DPDP Act 2023 obligations reviewed; data deletion flow working
- [ ] Supplier agreement (anchor obligation, delivery terms, settlement) signed
- [ ] FSSAI/GST requirements for food deals confirmed with an accountant

## Security
- [ ] App Check enforced on Functions, Firestore, Storage (staging and prod)
- [ ] Rules tests cover every allow path; all pass
- [ ] No secrets in repo (gitleaks clean); Secret Manager bindings least-privilege
- [ ] Admin allow-list set; first admin bootstrapped; script access revoked
- [ ] Maps/Places keys restricted (package + SHA-256 / API restrictions)
- [ ] Razorpay webhook secret configured; signature failures alert
- [ ] Rate limits configured; fraud signal triggers deployed

## Correctness
- [ ] Golden vectors pass on TS and Android
- [ ] Concurrency harness passes on staging (joins, leaves, bids, webhooks)
- [ ] Full purchase E2E in Razorpay test mode passes, including refund
- [ ] Reconciliation job: zero exceptions on staging for 7 consecutive days
- [ ] Sweeper idempotency verified (double invocation test)

## Operations
- [ ] Monitoring dashboards + alerts live; on-call contacts set
- [ ] Budget alerts on billing account
- [ ] PITR + scheduled backups enabled; restore drill done in staging
- [ ] Runbooks: payment incident, webhook outage, stuck sweeper, data restore, compromised admin
- [ ] Play App Signing enrolled; upload key stored safely; staged rollout plan

## UX & quality
- [ ] Loading/empty/error/offline states on every screen
- [ ] Accessibility pass (TalkBack, font scale 200%, contrast)
- [ ] Crash-free rate gate defined from baseline
- [ ] Localisation framework in place (English shipped)
