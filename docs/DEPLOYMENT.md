# Deployment Architecture

## 1. Environments
| Env | Firebase project | Android flavor / App ID | Web | Razorpay | Data |
|---|---|---|---|---|---|
| dev | `groupdeal-dev` + local emulators | `dev` / `in.groupdeal.app.dev` | localhost / dev backend | stub / test | synthetic |
| staging | `groupdeal-staging` | `staging` / `in.groupdeal.app.staging` | `staging.<domain>` | **test** keys | synthetic |
| prod | `groupdeal-prod` | `prod` / `in.groupdeal.app` | `<domain>` | **live** keys | real |
The package name `in.groupdeal.app` and domain are placeholders until the team registers a domain (decision needed — see LAPTOP START).

## 2. CI/CD (GitHub Actions)
| Workflow | Trigger | Steps |
|---|---|---|
| `pricing.yml` | PR touching `packages/pricing` | test, publish vectors artifact |
| `functions.yml` | PR | lint, typecheck, unit, emulator integration, rules tests |
| `android.yml` | PR | ktlint/detekt, unit, golden parity, lint, assembleDevDebug |
| `web.yml` | PR | lint, typecheck, unit, build |
| `security.yml` | PR + weekly | gitleaks, npm audit, Gradle dependency check |
| `deploy-staging.yml` | merge to `main` | deploy firestore rules/indexes, functions, web to staging; build staging AAB → Play internal track |
| `deploy-prod.yml` | tag `v*` + manual approval (GitHub Environment `production`) | same to prod; AAB → Play closed/production track |
Auth to Google Cloud from Actions via **Workload Identity Federation** (no JSON keys). Deploy order: rules/indexes → functions → web → Android rollout.

## 3. Secrets
Secret Manager per project (Razorpay key secret, webhook secret, Maps server key). GitHub Environment secrets: upload keystore (base64) + passwords, Play service account via WIF, `google-services.json` per flavor. Rotation: Razorpay secrets rotated on any suspected exposure and at least yearly.

## 4. Android release
Play App Signing (Google holds the app signing key; team keeps the upload key). Versioning: `versionCode` from CI run number, `versionName` from tag. Staged rollout 10% → 50% → 100% with Crashlytics crash-free gate (threshold set after baseline). Data safety form completed from SECURITY.md §9.

## 5. Web release
Firebase App Hosting backends per environment connected to the repo; HTTPS certificates managed by Firebase; custom domain via DNS verification. Rollback = redeploy the previous build from the App Hosting rollout history.

## 6. Functions and Firestore release
Backward-compatible changes only: new fields optional; clients tolerate unknown fields; callables versioned by adding new names (`joinGroupV2`) when contracts break; old ones removed after the minimum supported app version moves. Index deployments before code that needs them.

## 7. Data migration strategy
Migrations are idempotent scripts in `/scripts/migrations/NNNN_name.ts`, run first on staging, recorded in `config/migrations`. Pattern: expand (add field, dual-write) → backfill (batched, resumable) → contract (remove old field) across two releases.

## 8. Observability
Crashlytics (Android), Firebase Performance, Google Analytics for Firebase (no PII), Cloud Logging structured logs, Error Reporting, log-based metrics + Cloud Monitoring alerts: function error rate, webhook signature failures, reconciliation exceptions, refund failures, payment verification failures, sweeper lag (oldest due group age), budget alerts on the billing account. On-call: the two maintainers; alert channel email + phone push.

## 9. Backups and disaster recovery
Firestore **point-in-time recovery** (7-day window) and **scheduled backups** (daily, 14-day retention) in prod — availability and cost to confirm in console (NOT VERIFIED). Storage bucket object versioning for supplier documents. DR runbook: restore backup to a new database, point functions at it via config, verify reconciliation against Razorpay, then switch clients. Recovery objectives to be set after a restore drill in staging.

## 10. Release process
1. Feature flags off by default. 2. Merge → staging auto-deploy. 3. Run E2E + concurrency + reconciliation on staging. 4. Tag → prod approval. 5. Staged Android rollout. 6. Post-release checks (dashboard, error rates, payment success path). 7. Rollback plan per component (functions redeploy previous tag, web rollback, Play halt rollout).
