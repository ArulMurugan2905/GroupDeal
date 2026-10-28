# O. Academic Report–Ready Technical Material

The submitted PBL report (102-page build, September 2026) documents the **prototype** and the **proposed** system. When the production build exists, update the report using the labels below. Never report results that were not measured.

Labels: **IMPLEMENTED** (in prototype source today) · **DESIGNED** (fully specified in this blueprint, not coded) · **PROPOSED** (direction agreed, details open) · **FUTURE**.

| Report section | Material | Status today | Source doc |
|---|---|---|---|
| Introduction / problem | Fragmented household demand; bulk prices inaccessible; no price–demand visibility | — | PRODUCT_REQUIREMENTS |
| Objectives | FR-01..20 condensed to 8 objectives | — | PRODUCT_REQUIREMENTS §5 |
| Literature themes | group-buying price discovery; quantity discounts; dynamic pricing (contrast: rule-based vs optimisation); reverse auctions; social commerce; trust/reputation; mobile architecture | refs already verified in report ch. 13 | — |
| Existing system | individual retail, e-commerce per-order pricing, informal chat pooling | — | report ch. 3 |
| Proposed system | deal vs group model; pay-after-lock; sealed reverse auction; anchor supplier | DESIGNED | DECISIONS D-04..D-09 |
| Methodology | iterative PBL; prototype → audit → blueprint → staged build | IMPLEMENTED (process) | ROADMAP |
| Architecture | Android MVVM/Clean + Firebase serverless + web portal; trust boundaries | DESIGNED | SYSTEM_ARCHITECTURE |
| Database design | Firestore schema with reasoning | DESIGNED | DATABASE_SCHEMA |
| Algorithms | tier pricing (integer paise), membership transaction, auction ranking, payment verification | pricing engine **IMPLEMENTED in TypeScript and tested** (6/6); others DESIGNED | PRICING_ENGINE, SUPPLIER_AUCTION, PAYMENTS |
| Implementation | prototype modules (IMPLEMENTED); production modules per stage | mixed | PROJECT_AUDIT, ROADMAP |
| Testing methodology | matrix with risk levels; concurrency harness; rules tests | DESIGNED | TEST_STRATEGY |
| Results | Only measured facts: golden-vector results; later E2E/concurrency results from staging runs with dates | partial | — |
| Applications, advantages | from report ch. 10–11 | — | — |
| Limitations | hot-document limits, non-payment risk, supplier liquidity, locality density, external dependencies | — | RISKS |
| Future scope | FEATURE_PLAN FUTURE rows; iOS; courier integration; ML (clearly future) | FUTURE | FEATURE_PLAN |

**References to verify before citing:** Firebase docs pages for transactions, App Check, App Hosting, PITR/backups; Razorpay docs for orders API, signature verification and webhooks; Google Places SDK docs; Firestore write-rate guidance (AS-3). Cite URLs with access dates.

**Screenshot / diagram insertion points:** Fig. architecture (SYSTEM_ARCHITECTURE §1), purchase lifecycle (§5.4), join sequence (§5.2), payment sequence (§5.6), group state machine (STATE_MACHINES §2), payment state machine (PAYMENTS §3), ER-style schema (DATABASE_SCHEMA), device screenshots listed in UI_UX §7.
