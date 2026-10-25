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

### Chat after cancelling a consultation

Mobile clears its saved chat ID after a successful cancellation
(`POST /api/paciente/consulta/cancelar`, with state `CANCELADA`) and after a
successful new hospital selection (`POST /api/atencion/hospital`, HTTP 204).
The next chat screen creates a new interview, including when the same hospital
and specialty are selected again. Failed requests preserve the saved chat ID;
leaving and reopening the chat within the same consultation still restores it.
The previous chat and its messages remain in the backend as history.

Manual regression: select a hospital, complete or partially answer the chat,
cancel the consultation, select the same hospital/specialty again and open chat.
It must show only the initial greeting and allow writing. Leave and reopen this
new chat to confirm that its messages are restored. Check that a rejected
cancellation or hospital selection does not discard the existing chat.

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…


### Mensajes del chat de voz

La app muestra el registro de respuestas y la información de atención, sin anunciar preclasificaciones ni niveles de prioridad al paciente. El backend comparte esa regla con Gemini para texto, audio y cierre. Al activar voz con el saludo inicial ya visible, el backend pide una transición breve («Te escucho») y no envía otra transcripción del saludo. Las preguntas posteriores y las respuestas del paciente siguen llegando normalmente. Los avisos de atención urgente se mantienen.


La entrevista terminada permanece cerrada si falla el procesamiento. `estadoClasificacion` permite distinguir el cierre de un resultado completado. El botón «Reintentar» usa `POST /api/chat/{id}/clasificacion` con las respuestas ya guardadas; no agrega un «ok» ni habilita nuevamente el micrófono o las preguntas.


### Interrupciones de voz y protección contra eco (Android)

La app intenta permitir que el paciente hable mientras responde el asistente. El
modo efectivo se obtiene del audio nativo y se actualiza al iniciar o cambiar la
salida: con una ruta de auriculares compatible o cancelación de eco habilitada,
la pantalla indica «Podés interrumpir hablando». Se conserva la detección de voz de
Gemini y, al recibir `interrumpido`, se corta inmediatamente la reproducción y se
vacían tanto la cola local como el buffer nativo. Las escrituras previas quedan
invalidadas para que no reaparezca audio del turno interrumpido.

La cancelación de eco nativa depende del teléfono y de su ruta de audio; que se
pueda habilitar no garantiza un resultado acústico perfecto. La selección de modo
es automática y no expone un control manual: el modo por turnos se activa cuando
faltan las condiciones mínimas para escuchar y reproducir a la vez. La pantalla
sigue indicando el modo efectivo.

En modo por turnos, se reemplaza por PCM silencioso la captura mientras hay audio
pendiente en la cola o en `AudioTrack`. La escucha vuelve cuando el contador real
`playbackHeadPosition` alcanza los frames encolados y pasan 300 ms de margen. Se
comprueba antes y después de leer cada fragmento para descartar lecturas que cruzan
ese límite. No se usa `turno_completo` para liberar el micrófono, porque puede
quedar audio por reproducir. No se envía `fin_audio` por cada fragmento silenciado.
La interfaz muestra el modo efectivo y conserva la opción de terminar la voz.
Si otra aplicación o llamada toma el foco de audio durante la captura, la sesión
se cierra y se informa el motivo; no queda mostrando «Te escucho» con el micrófono
apagado. El cierre indica «Terminando la conversación de voz...». No se cancela
el procesamiento de una entrevista ya terminada por un cambio de foco posterior.

Verificación automática de la política: `./gradlew.bat :composeApp:testDebugUnitTest
--tests com.proyecto_final.triage.audio.ControlEcoVozTest` (usar el procedimiento de
log temporal y `--stop` de AGENTS.md). No requiere backend ni base de datos.

Regresión física pendiente en un teléfono real:

- Con parlante, dejar terminar tres respuestas sin hablar: no deben aparecer
  transcripciones del bot como paciente ni interrupciones espontáneas.
- En modo que permite interrumpir, hablar durante una respuesta: debe detenerse
  el audio pendiente, transcribirse la voz del paciente y contestarse la nueva frase.
- En una ruta sin soporte de interrupciones, comprobar la selección automática
  del modo por turnos y su indicación en pantalla.
- Repetir con volumen alto y con auriculares; conectar/desconectar auriculares
  durante la sesión y verificar que el mensaje de la pantalla sigue el modo nativo.
- Detener y reabrir voz, salir de la pantalla y repetir: sin audio pendiente,
  micrófono retenido ni cambios persistentes de la ruta/modo de audio del sistema.


### Errores al enviar por texto

Un error HTTP del backend se distingue de una falla de transporte: un 5xx indica
que el asistente no pudo responder, 401/403 requieren revisar la sesión, 404 indica
un chat no encontrado y 429 pide esperar antes de reintentar. Las excepciones de
transporte conservan el aviso de conexión. La app mantiene el borrador y el reintento
manual, sin reenvíos automáticos de POST que puedan duplicar una respuesta.
Si el backend ya cerró la entrevista, se conserva la recuperación por GET y el
reintento de procesamiento sin volver a enviar respuestas. La ausencia de un mensaje
fallido en la base no permite reconstruir su causa: revisar los logs del mismo horario.

### Disposición de la UI y barras del sistema

El contenedor de `App` dibuja el fondo con `Surface` y reserva `safeDrawing`
antes de la navegación. Las pantallas quedan dentro del área disponible de las
barras de estado, navegación y teclado. Los modificadores de insets descendientes
consumen solo el espacio restante; no agregar márgenes fijos para compensar la
barra de Android. Se conserva `imePadding()` del chat. Esta composición sigue el
[manejo de insets de Compose](https://developer.android.com/develop/ui/compose/system/insets-ui).

Las tarjetas del perfil distribuyen el ancho entre etiqueta y valor con separación.
Con poco ancho o fuentes grandes pasan a etiqueta arriba y valor debajo, para que
un correo largo no comprima la etiqueta en una columna de letras. No se recortan
los datos para forzar una altura fija.

Los filtros de hospitales (distancia, tiempo de atención y mejor valorados)
comparten ancho y altura calculada a partir de su texto. La grilla adapta la
cantidad de columnas al ancho disponible y la escala de fuente; en pantallas
angostas se apilan conservando la altura común. El espacio del indicador de selección se
mantiene al cambiar de filtro para evitar saltos de tamaño.

Regresión manual: revisar perfil con correo largo y elección de hospital a 320 dp
y con fuente normal, 1.5 y 2 veces mayor; probar orientación horizontal. Con
navegación Android por tres botones y por gestos, recorrer inicio de sesión,
perfil, hospitales, consulta y chat, verificando que los últimos botones sean
visibles y pulsables. Abrir/cerrar el teclado del chat y edición del perfil para
comprobar que no se duplique el espacio inferior ni quede tapado el campo activo.


### Integración con desarrollo (25 de octubre de 2026)

La rama de chat de voz incorpora el cambio de contraseña autenticado, el orden
«Mejor valorados» y los ajustes de ubicación/carga de hospitales de `desarrollo`.
La URL del backend sigue configurándose mediante `BACKEND_BASE_URL`; una dirección
local de otro equipo no reemplaza esa configuración. El cambio de contraseña usa
`POST /api/auth/cambio-contrasenia/actual` y conserva los avisos de bloqueo y
validación del backend. El selector mantiene los tres órdenes con altura común.
