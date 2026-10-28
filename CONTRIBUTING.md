# Contributing to GroupDeal

Thank you for your interest in **GroupDeal**! GroupDeal is a locality-based group buying commerce platform engineered with strict architectural standards, integer-paise financial accuracy, and clean MVVM layering. All source code is proprietary to Arul Murugan M.

## Code of Conduct
We are committed to providing a welcoming, diverse, and harassment-free environment for everyone. Please be respectful and constructive in all interactions.

## Architectural Source of Truth
Before making architectural or business logic changes, review:
1. [`CLAUDE.md`](./CLAUDE.md) – Core engineering guidelines and non-negotiables.
2. [`docs/DECISIONS.md`](./docs/DECISIONS.md) – Architectural Decision Records (ADRs).
3. [`docs/SYSTEM_ARCHITECTURE.md`](./docs/SYSTEM_ARCHITECTURE.md) – System overview and trust boundaries.
4. [`docs/PRICING_ENGINE.md`](./docs/PRICING_ENGINE.md) – Single source of truth for dynamic pricing rules.

### Non-Negotiable Rules
- **Money is integer paise:** Never use floating point (`Float`/`Double`) for monetary calculations. ₹1 = 100 paise.
- **Client Trust Boundary:** Clients NEVER write business data (groups, members, orders, payments, auction bids) directly. All mutations occur via authenticated Cloud Functions transactions with idempotency keys.
- **Golden Vector Parity:** Any change to pricing logic must update `packages/pricing/test/golden-vectors.json` first, and both TypeScript and Kotlin implementations must pass all golden tests.

---

## Development Workflow

### 1. Branching
- `main`: Production-ready, stable code.
- Feature branches: `feat/<feature-name>`, `fix/<bug-name>`, `docs/<topic>`.

### 2. Testing
- **Pricing Engine:** Run `npm test` in `packages/pricing`.
- **Android App:** Run `./gradlew test` in `android/`.
- Ensure all CI checks pass before submitting a Pull Request.

### 3. Commit Conventions
We follow conventional commit format:
- `feat(android): add tier roadmap composable`
- `fix(pricing): prevent negative membersToNextTier on max tier`
- `docs: update system architecture diagram`
- `test: add golden vector for edge case N=0`

---

## Reporting Issues
Please report bugs or feature proposals using GitHub Issues. Provide clear reproduction steps, platform details (OS, Android version, Node version), and relevant logs.
