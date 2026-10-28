# GroupDeal Android Client Application

Modern Jetpack Compose native Android customer application for **GroupDeal** — locality-based group buying commerce platform with dynamic tier-based pricing.

## Overview
The Android application enables neighborhood consumers to discover hyper-local group deals, join active purchasing pools, observe live tier transitions with instant price drops, and complete escrow-secured orders.

## Architecture
- **Pattern:** MVVM (Model-View-ViewModel) + Clean Architecture
- **UI Framework:** Jetpack Compose (Material 3) with custom design system tokens
- **Asynchronous Flow:** Kotlin Coroutines & `StateFlow`
- **Dependency Injection:** Hilt
- **Local Persistence & Cache:** Room Database & Jetpack DataStore
- **Networking & Backend:** Firebase Cloud Functions Client (HTTP callable functions with idempotency keys)
- **Security & Integrity:** Firebase App Check with Play Integrity provider
- **Payment Gateway:** Razorpay Checkout SDK

## Directory Layout
```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── core/
│   │   │   │   │   ├── data/          # Repositories & locality services
│   │   │   │   │   ├── engine/        # Kotlin port of PricingEngine
│   │   │   │   │   └── model/         # Domain models (GroupDeal, Order, User)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── auth/          # Login & Onboarding screens
│   │   │   │   │   ├── components/    # Reusable Compose UI elements & badges
│   │   │   │   │   ├── detail/        # Deal detail & tier roadmap
│   │   │   │   │   ├── discovery/     # Hyperlocal feed & deal cards
│   │   │   │   │   ├── mygroups/      # User active group memberships
│   │   │   │   │   ├── navigation/    # Type-safe Compose navigation
│   │   │   │   │   ├── orders/        # Order history & status tracking
│   │   │   │   │   ├── profile/       # User profile & savings metrics
│   │   │   │   │   └── theme/         # Color palettes, typography & themes
│   │   │   │   └── MainActivity.kt    # Root Android Activity
│   │   │   └── res/                   # Drawables, mipmaps, strings, XML configs
│   │   └── test/                      # Unit tests & Roborazzi screenshot tests
│   └── build.gradle.kts               # App-level build script & dependencies
├── gradle/                            # Gradle wrapper binaries & configuration
├── build.gradle.kts                   # Project-level Gradle build configuration
├── settings.gradle.kts                # Subproject and plugin management
└── gradle.properties                  # JVM & Gradle compilation settings
```

## Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1+) or newer
- **JDK:** 17 LTS
- **Android SDK:** Compile SDK 36, Min SDK 24

### Local Setup
1. Open Android Studio.
2. Select **Open** and choose the `android/` directory.
3. Allow Gradle to sync dependencies and project configuration.
4. Copy `.env.example` to `.env` and configure your API keys if needed:
   ```bash
   cp .env.example .env
   ```
5. Connect an Android emulator or physical device running Android 7.0+ (API 24+).
6. Click **Run > Run 'app'** or execute:
   ```bash
   ./gradlew installDebug
   ```

## Running Unit Tests
```bash
./gradlew test
```
