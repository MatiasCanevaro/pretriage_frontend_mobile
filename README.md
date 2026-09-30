This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Local setup after cloning

Copy [local.properties.example](local.properties.example) to `local.properties` if
you do not already have one. If IntelliJ has created it, add the missing entries
while keeping your existing SDK path and credentials. Set `sdk.dir`, `MAPS_API_KEY`
and `AUTH0_CLIENT_ID` for your environment; `DEV_TOKEN` may remain empty for normal
sign-in. `local.properties` is ignored by Git and must never be committed.

### Backend URL

Set the backend address in your own `local.properties`, without editing Kotlin:

```properties
BACKEND_BASE_URL=http://10.0.2.2:8080
```

| Target | Example URL |
| --- | --- |
| Android Emulator; backend on the host PC | `http://10.0.2.2:8080` |
| Physical phone on the same LAN | `http://192.168.1.100:8080` (replace with your PC's IP) |
| iOS Simulator; backend on the same Mac | `http://localhost:8080` |
| Shared remote backend | `https://api.example.com` (replace with the actual server) |

The address must start with `http://` or `https://`, without credentials, query
parameters or a fragment. A trailing slash is removed automatically.

Precedence is: nonblank Gradle property `BACKEND_BASE_URL`, nonblank
`BACKEND_BASE_URL` in `local.properties`, then `http://10.0.2.2:8080`.
For a one-off build, you can override it without changing the local file:

```powershell
.\gradlew.bat :composeApp:assembleDebug "-PBACKEND_BASE_URL=https://api.example.com"
```

Gradle generates a common Kotlin configuration consumed by `AppConfig`, so the
setting applies to both Android and iOS and to both DEV and PROD. It is embedded
in the application at build time. After changing it, rebuild and install/run the
app from IntelliJ; reopening an already installed APK does not update its URL.
Do not edit generated files under `build/`.

Start the backend at the selected address before running the app. For a physical
phone, allow the backend port through the host firewall and use the same network.

The initial `SplashScreen` requests `/`. An unauthenticated HTTP 401 response means
the backend is reachable; the app can continue to sign-in. A connection timeout
or refused connection triggers the generic “Algo salió mal” dialog.

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
