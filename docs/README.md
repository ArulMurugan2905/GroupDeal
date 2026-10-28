# GroupDeal Architecture & Engineering Documentation

This directory contains the comprehensive system design specifications, Architectural Decision Records (ADRs), database schemas, security models, and implementation roadmaps for the **GroupDeal** platform.

## Complete Specification Index

| Document | Description |
|---|---|
| [`GROUPDEAL_PRODUCTION_BLUEPRINT.md`](./GROUPDEAL_PRODUCTION_BLUEPRINT.md) | Single master compiled production blueprint (Sections A–S + ADRs). |
| [`SYSTEM_ARCHITECTURE.md`](./SYSTEM_ARCHITECTURE.md) | High-level system architecture, trust boundaries, multi-tier flow, and component interactions. |
| [`PRODUCT_REQUIREMENTS.md`](./PRODUCT_REQUIREMENTS.md) | Functional Requirements (FR-01 to FR-20), user personas, and target metrics. |
| [`DATABASE_SCHEMA.md`](./DATABASE_SCHEMA.md) | Firestore collection schemas, sub-collections, field types (integer paise), indexes, and data lifecycles. |
| [`PRICING_ENGINE.md`](./PRICING_ENGINE.md) | The mathematical and computational specifications for tier pricing, quotes, and golden vectors. |
| [`STATE_MACHINES.md`](./STATE_MACHINES.md) | Formal finite state machines for Groups, Memberships, Orders, and Reverse Auctions. |
| [`SUPPLIER_AUCTION.md`](./SUPPLIER_AUCTION.md) | Reverse sealed-bid auction algorithms, ranking mechanics, and anchor supplier fallback. |
| [`PAYMENTS.md`](./PAYMENTS.md) | Escrow lifecycle, Razorpay order creation, server HMAC verification, webhooks, and refunds. |
| [`SECURITY.md`](./SECURITY.md) | Threat modeling, App Check Play Integrity enforcement, Firestore security rules, and audit logging. |
| [`API_SPECIFICATION.md`](./API_SPECIFICATION.md) | Cloud Functions callable interfaces, Zod request/response validation schemas, and error taxonomy. |
| [`FIREBASE_ARCHITECTURE.md`](./FIREBASE_ARCHITECTURE.md) | Firebase project configuration, Cloud Functions, Firestore indexes, and emulator setup. |
| [`ANDROID_ARCHITECTURE.md`](./ANDROID_ARCHITECTURE.md) | Jetpack Compose client architecture, MVVM layers, Hilt DI, and StateFlow contracts. |
| [`WEB_ARCHITECTURE.md`](./WEB_ARCHITECTURE.md) | Next.js supplier portal, admin dashboard, and web share pages. |
| [`DECISIONS.md`](./DECISIONS.md) | Architectural Decision Records (ADRs D-01 through D-20) governing system invariants. |
| [`FEATURE_PLAN.md`](./FEATURE_PLAN.md) | Phased product delivery plan from MVP through production releases. |
| [`IMPLEMENTATION_ROADMAP.md`](./IMPLEMENTATION_ROADMAP.md) | Detailed sprint-by-sprint development roadmap and milestone deliverables. |
| [`LAPTOP_START.md`](./LAPTOP_START.md) | Quickstart guide for setting up local development emulators and running first end-to-end flows. |
| [`PRODUCTION_CHECKLIST.md`](./PRODUCTION_CHECKLIST.md) | Production readiness checklist covering performance, reliability, billing, and observability. |
| [`PROJECT_AUDIT.md`](./PROJECT_AUDIT.md) | Technical audit of the prototype with identified defects and remediation paths. |
| [`REPORT_MATERIAL.md`](./REPORT_MATERIAL.md) | Technical reference material aligning project deliverables with academic PBL criteria. |
| [`RISKS.md`](./RISKS.md) | Risk analysis, failure modes, hot-document mitigation, and economic safeguards. |
| [`TEST_STRATEGY.md`](./TEST_STRATEGY.md) | Multi-tier test strategy: unit tests, golden vectors, concurrency harness, and rules tests. |
| [`UI_UX_SPECIFICATION.md`](./UI_UX_SPECIFICATION.md) | Visual design tokens, emerald color palette, typography, animations, and micro-interactions. |
| [`DEPLOYMENT.md`](./DEPLOYMENT.md) | CI/CD pipelines, Firebase App Hosting, Secret Manager, and release gates. |
