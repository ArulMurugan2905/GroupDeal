# Web Architecture (Supplier Portal, Admin Console, Public Share Pages)

## 1. Stack
Next.js (App Router) · TypeScript (strict) · React · Tailwind CSS · Firebase JS SDK (Auth, Firestore, Functions, App Check reCAPTCHA Enterprise) · Firebase Admin SDK on the server (read-only use) · zod · TanStack Query for callable mutations · Vitest + Testing Library · Playwright E2E. Hosting: Firebase App Hosting (D-18).

## 2. Route map
| Route | Access | Rendering |
|---|---|---|
| `/` | public | static marketing page |
| `/g/[groupId]` | public | SSR (Admin SDK read of the group snapshot), Open Graph tags for sharing; CTA "Open in app" (Android App Link) |
| `/login` | public | client |
| `/supplier/onboarding`, `/supplier/verification` | supplier (any status) | client |
| `/supplier` (dashboard), `/supplier/opportunities`, `/supplier/auctions/[gid]`, `/supplier/bids`, `/supplier/fulfilment`, `/supplier/analytics`, `/supplier/profile` | verified supplier | client + server components |
| `/admin` and `/admin/{users,suppliers,localities,deals,groups,requests,auctions,orders,payments,disputes,moderation,fraud,analytics,settings}` | admin | server components for lists (session cookie verified), client for actions |

## 3. Auth model
Client signs in with Firebase Auth; a route handler exchanges the ID token for a **Firebase session cookie** (httpOnly, secure, SameSite=Strict, 5 days). `middleware.ts` redirects unauthenticated users; server components verify the cookie with the Admin SDK and check claims (`role`, `supplierStatus`). **All mutations still go through callable functions** (the web server never writes business data), keeping one authorisation path for Android and web.

## 4. Structure
```
web/
  app/(public)/… app/(supplier)/supplier/… app/(admin)/admin/… app/api/session/route.ts
  components/ui (Button, Table, Badge, Dialog…) components/domain (TierRoadmap, AuctionCard, BidForm, OrderTable…)
  lib/firebase/{client.ts,admin.ts} lib/callables.ts (typed wrappers + requestId) lib/money.ts (paise formatting)
  lib/pricing → re-exports @groupdeal/pricing
```

## 5. UX rules
Responsive (mobile-first for suppliers on phones; admin optimised for ≥ 1280 px). Tables with server pagination. Every destructive admin action requires a reason dialog. Dark mode via Tailwind `class` strategy. Accessibility: semantic HTML, focus management in dialogs, axe checks in CI.

## 6. Security
CSP with nonces, `X-Frame-Options: DENY`, strict referrer policy; no secrets in `NEXT_PUBLIC_*` except the Firebase web config and reCAPTCHA site key; Admin SDK credentials via App Hosting service identity (no JSON keys).
