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

### Tiempo estimado de atención en vivo

Mientras la entrada de la consulta permanece `EN_COLA`, la app abre una conexión SSE autenticada con `GET /api/atencion/tiempos/suscribirse/{consultaId}`. Recibe los eventos `tiempo-estimado` y `heartbeat`; al salir de la cola, el backend deja de enviar actualizaciones del tiempo estimado.

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


La entrevista terminada permanece cerrada mientras el backend procesa y si agota sus reintentos. `finalizado` indica que la entrevista cerró; `estadoClasificacion` indica por separado si la clasificación está `NO_SOLICITADA`, `PENDIENTE`, `FALLIDA` o `COMPLETADA`. Una respuesta antigua que omita `estadoClasificacion` permanece como cierre desconocido, porque `finalizado` solo indica que la entrevista cerró y una estimación de cola puede existir antes de clasificar. `PENDIENTE` muestra un aviso breve mientras el backend realiza sus reintentos automáticos; el cliente consulta el estado por GET durante un plazo acotado. `FALLIDA` es terminal: la app informa que las respuestas quedaron guardadas, sin pedir al paciente que reintente el procesamiento. El origen `CIERRE_SIN_DATOS` identifica un cierre administrativo sin clasificación: el cliente sincroniza el chat por GET, mantiene `finalizado=true` y `estadoClasificacion=NO_SOLICITADA`, y no muestra una confirmación de envío, un fallo de procesamiento ni una tarjeta de atención. En voz, el evento `triage_finalizado` conserva `origenRespuesta` para aplicar la misma regla. El cierre de una clasificación completada no anuncia prioridad ni promete una derivación ya realizada.


### Estado de espera y cierre de la entrevista por voz

El evento WebSocket procesando_respuesta lleva activo=true mientras el backend
interpreta una respuesta y activo=false cuando vuelve a esperar una respuesta
útil. La app mantiene visible «Procesando tu respuesta…» durante ese intervalo;
los fragmentos de transcripción, audio y turno_completo no lo limpian por sí
solos. El micrófono y la interrupción siguen disponibles. Al recibirse una
pregunta útil, la pantalla indica que está escuchando.

Cuando el backend acepta el cierre, la app muestra el procesamiento mientras la
sesión de voz sigue activa. Si el socket termina antes de confirmar un resultado,
la entrevista queda cerrada con estado desconocido y un control para consultar
el estado. Los errores de una entrevista abierta ofrecen retomar voz o
continuar por texto; una entrevista cerrada no ofrece volver a responder ni
reprocesar manualmente la clasificación.


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

El envío de texto incluye un UUID de solicitud. Un HTTP 202 confirma que el backend
guardó el turno y continuará procesándolo; el cliente obtiene el historial canónico
del DTO y consulta su estado sin agregar una burbuja de respuesta propia. Ante un
resultado ambiguo, la app consulta por GET y nunca repite automáticamente el POST.
Un 401/403 requiere revisar la sesión, 404 indica un chat no encontrado y 429 pide
esperar. Si no se puede confirmar el estado después de 120 segundos, la pantalla deja
de mostrar el indicador de escritura y ofrece una consulta de estado que solo usa GET.
`NO_DISPONIBLE` informa que el asistente no pudo continuar y deshabilita texto y voz,
sin marcar la entrevista como cerrada.

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

Verificado el 25 de octubre de 2026: `:composeApp:assembleDebug` y
`:composeApp:testDebugUnitTest --tests com.proyecto_final.triage.audio.ControlEcoVozTest`
finalizaron correctamente; pasaron las nueve pruebas de política de audio. Se usó
un override local de `BACKEND_BASE_URL`, log temporal y detención del daemon.
La compilación comprueba Android; la regresión acústica física indicada arriba y
la compilación iOS requieren sus respectivos dispositivos/entornos.

### Reconciliación de mensajes del chat

`POST /api/chat/{id}/mensajes` envía un UUID `solicitudId`. El backend responde HTTP
202 con el DTO completo después de guardar el mensaje del paciente y aceptar el turno
para procesamiento. `GET /api/chat/{id}` devuelve el mismo estado: `estadoTurnoTexto`
(`SIN_SOLICITAR`, `PENDIENTE`, `COMPLETADA` o `NO_DISPONIBLE`), el UUID asociado y la
cantidad de intentos. La solicitud UUID y el texto se conservan en la copia de
navegación en memoria hasta un estado terminal.

Después de HTTP 202, y también al volver a abrir un chat, la app restaura el progreso
desde GET y consulta cada 1,5 s durante un máximo de 120 s. Solo reemplaza el historial
con una respuesta terminal del servidor. Un POST con respuesta perdida se reconcilia
por GET usando el UUID; el cliente nunca repite ese POST. Si el estado sigue sin
confirmarse tras el plazo, deja de mostrar la escritura y ofrece «Consultar estado».
Ese control solo hace GET. Mientras el turno esté pendiente o su estado sea desconocido,
texto y voz permanecen bloqueados para impedir mensajes duplicados.

`NO_DISPONIBLE` es terminal para el turno de texto: la app muestra que el asistente
no está disponible y deshabilita todos los controles del chat. No cambia
`finalizado` ni `estadoClasificacion`; la entrevista no se marca como cerrada por
este error. La respuesta HTTP 200 del backend anterior sigue aceptándose como una
respuesta inmediata con su burbuja BOT tradicional.

En el cierre de voz, `entrevista_finalizada` muestra un único aviso breve de procesamiento.
La app consulta automáticamente `GET /api/chat/{id}` cada 1,5 s, hasta 80 lecturas, con
timeout de 2 s por lectura y un plazo total de 120 s. No se muestra una recarga durante
la espera normal. El seguimiento automático permanece acotado a 120 s, para cubrir
los dos intentos máximos del backend y su espera entre intentos. Si no confirma el resultado en ese
plazo, ofrece «Consultar estado»; esto solo hace GET y no reenvía respuestas.
`PENDIENTE` permanece mientras el backend reintenta. `FALLIDA` aparece únicamente
cuando los intentos automáticos se agotaron y se presenta como resultado terminal,
sin una acción de reintento para el paciente. Al agotar la espera, la app sale del estado de voz, confirma
los fragmentos locales y cancela únicamente el canal agotado del cliente; el backend
continúa clasificando. Un resultado desconocido ofrece «Consultar estado» sin volver
al spinner cuando termina el socket. Estas reglas también se aplican al reingresar.

Al salir se conserva en memoria la última conversación visible, incluidos los fragmentos
de transcripción abiertos y el estado de cierre. Al volver al mismo chat se restaura
antes del GET; una lectura antigua o pendiente con menos mensajes no recorta el historial
ni reabre una entrevista cerrada. Un resultado terminal usa el historial del servidor.
Esta copia no escribe transcripciones en disco y se limpia al cerrar sesión o limpiar
el chat. Si termina el proceso de la app, se depende del historial ya guardado por el
backend. Un fallo de red al recuperar no descarta el chat ni crea otro automáticamente.

El error recuperable de voz muestra «Se interrumpió la voz.» con «Retomar voz»;
la entrada escrita permanece disponible. Un resultado completado limpia el error y
tiene precedencia sobre fallos posteriores del socket.

Pruebas de reconciliación (Android unit tests):
`.\gradlew.bat :composeApp:testDebugUnitTest --tests com.proyecto_final.triage.ChatSendReconciliationTest`
La recuperación durable de turnos de texto se cubre con
`com.proyecto_final.triage.ChatTextTurnRecoveryTest`.
Verificado el 25 de octubre de 2026: `assembleDebug` y `testDebugUnitTest`
completados, con 24 pruebas aprobadas (10 de reconciliación). Se usó un log
 temporal y se detuvo Gradle al terminar. El APK de prueba se generó con el
 override local `-PBACKEND_BASE_URL=http://192.168.100.241:8080`; no se cambió
 `local.properties` ni se guardaron credenciales en el repositorio.

Verificado el 28/10/2026 después de corregir el cierre y reingreso de voz:
`assembleDebug` y `testDebugUnitTest` pasaron, con 38 pruebas aprobadas y sin
fallos, errores ni omisiones. Las nueve regresiones de recuperación cubren
historial antiguo/incompleto, copia temporal de navegación, estados terminales,
fallos transitorios de clasificación, errores de lectura, límite de consultas y
cancelación al salir. El APK usa `http://192.168.100.241:8080`; se guardó el log
en un archivo temporal y se detuvo Gradle. No se probó una nueva grabación en
el teléfono ni se compiló iOS. La pregunta repetida tras la primera negativa
requiere además el ajuste de reconocimiento del backend actualizado.

Verificación adicional del 28/10/2026 para el indicador atascado: se reprodujeron
antes de la corrección dos casos que ocultaban `FALLIDA` mientras el socket seguía
en `PROCESANDO`. El build final y las 42 pruebas unitarias pasaron sin fallos,
errores ni omisiones; Gradle quedó detenido. Cuatro regresiones nuevas cubren
fallo confirmado con socket abierto, fallo persistido al agotar la espera, salida
del estado de voz y resultado desconocido sin reabrir la entrevista. El APK se
generó a las 11:29 -03:00 con la misma URL LAN. No se probó un teléfono ni iOS.

Verificación final del cierre controlado por el servidor (28/10/2026):
`assembleDebug` y `testDebugUnitTest` completados, con 43 pruebas aprobadas, cero
fallos/errores/omisiones y Gradle detenido. El APK usa la URL LAN existente.
La clasificación pendiente se sigue automáticamente por GET también cuando un
POST de texto se reconcilia con un cierre guardado. No se muestra un botón de
reprocesar; los reintentos técnicos son responsabilidad del backend. La espera
usa hasta 80 lecturas y un límite total de 120 s, seguido de una consulta de estado
si el resultado sigue sin confirmarse. El backend pasó su suite aislada de 748
pruebas (730 aprobadas, 18 opcionales omitidas). Falta la prueba física de voz.

Verificación de turnos de texto durables (28/10/2026, 14:59 -03:00):
`assembleDebug` y las 52 pruebas Android pasaron sin fallos, errores ni omisiones.
Se probaron HTTP 202, recuperación al volver al chat, resultado desconocido sin
reenviar, bloqueo por indisponibilidad, UUID anterior recibido tarde, historial
terminal y clasificación final que sigue pendiente durante un reintento. El APK
usa `http://192.168.100.241:8080`; el log quedó en un archivo temporal y Gradle se
detuvo al finalizar. Hace falta reiniciar el backend actualizado e instalar este
APK. No se probó un teléfono ni se compiló iOS.

Si vence la espera durante la clasificación final de un turno de texto, la app
lo presenta como estado desconocido y detiene el aviso de procesamiento. La
consulta de estado sigue disponible sin reenviar el mensaje.
