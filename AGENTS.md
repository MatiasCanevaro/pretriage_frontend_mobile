# AGENTS.md

Compose Multiplatform app (Android + iOS) targeting `:composeApp`, the only Gradle module. All product code lives under `composeApp/src`.

## Build

- Windows dev machine: Android only. iOS targets are configured but require Xcode/macOS.
- **Always load the `build-output-temp` skill before running any Gradle build**: redirect output to a temp log, show the tail, then run `.\gradlew.bat --stop`. Without it the huge Gradle/Kotlin daemon output keeps the session busy.
- Command: `.\gradlew.bat :composeApp:assembleDebug`
- No separate lint/typecheck/test scripts exist; a passing debug build is the verification step.

## Secrets & generated code (build fails without these)

- `local.properties` (gitignored) is required: keys `sdk.dir`, `DEV_TOKEN`, `MAPS_API_KEY`, `AUTH0_CLIENT_ID`. Missing keys break the build. Never commit, log, or echo these values.
- `generateDevToken` task reads `DEV_TOKEN` and generates `composeApp/build/generated/devToken/.../DevToken.kt`, which is added to the commonMain source set. Never hand-edit anything under `build/` — rebuild to regenerate.
- `MAPS_API_KEY` → AndroidManifest meta-data; `AUTH0_CLIENT_ID` → BuildConfig.

## Architecture

- Package root `com.proyecto_final.triage` under `commonMain/kotlin`:
  - `screens/` + `viewmodels/` + `components/` — Compose UI, navigation via Voyager
  - `network/` — Ktor client (kotlinx.serialization JSON), Auth0 password-realm auth, repositories + DTOs
  - `theme/`, `config/`, `filesaver/`, `utils/`
- Platform APIs use expect/actual: `Platform.kt`, `network/TokenStorage.kt`, `filesaver/PlatformContext.kt`, `utils/Emergency.kt`. New platform-specific code needs an expect/actual pair; Android-only UI (Google Maps screen) lives in `androidMain`.
- Backend base URL is hardcoded in `config/AppConfig.kt` (`http://192.168.0.7:8080`, same for DEV and PROD; manifest allows cleartext). Network failures = backend not reachable on the LAN.

## Toolchain

- Kotlin 2.3.21, AGP 8.11.2, compileSdk 36, minSdk 24, JVM 11; configuration cache and build cache enabled.
- Dependencies go in `gradle/libs.versions.toml` (version catalog), then referenced via `libs.*` in `composeApp/build.gradle.kts`.
