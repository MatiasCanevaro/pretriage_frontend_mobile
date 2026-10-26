package com.proyecto_final.triage

import com.proyecto_final.triage.network.ChatMensaje
import com.proyecto_final.triage.network.ErrorHttpChat
import com.proyecto_final.triage.network.ChatResponse
import com.proyecto_final.triage.network.RespuestaChatResponse
import com.proyecto_final.triage.network.ResultadoEnvioChat
import com.proyecto_final.triage.network.enviarConReconciliacion
import com.proyecto_final.triage.network.hayRespuestaBotNuevaDesde
import com.proyecto_final.triage.viewmodels.ChatUiState
import com.proyecto_final.triage.viewmodels.ChatClassificationPresentation
import com.proyecto_final.triage.viewmodels.aplicarChatReconciliado
import com.proyecto_final.triage.viewmodels.aplicarChatVerificadoPendiente
import com.proyecto_final.triage.viewmodels.aplicarFalloEnvioNoConfirmado
import com.proyecto_final.triage.viewmodels.presentacionClasificacionChat
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CancellationException
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatSendReconciliationTest {
    private val errorDeTransporte = IllegalStateException("respuesta HTTP perdida")
    private val texto = "Me duele la cabeza"

    @Test
    fun postFallidoYGetConParejaNuevaConfirmaSinDuplicarNiMostrarReintento() {
        val previo = listOf(ChatMensaje(" Hola ", "BOT"))
        val remoto = chat(listOf(ChatMensaje("Hola", "BOT"), ChatMensaje(texto, "PACIENTE"), ChatMensaje("¿Desde cuándo?", "BOT")))
        val contenidoEnviado = " $texto "
        val transporte = TransporteSimulado(listOf(Result.success(remoto)), contenidoEsperado = contenidoEnviado)

        val resultado = enviar(transporte, previo, contenidoEnviado)

        val confirmado = assertIs<ResultadoEnvioChat.Confirmado>(resultado)
        assertFalse(confirmado.procesando)
        assertEquals(1, transporte.envios)
        assertEquals(1, confirmado.chat.mensajes.count { it.autor == "PACIENTE" && it.contenido.trim() == texto })
        val estado = estadoOptimista(previo).aplicarChatReconciliado(confirmado.chat, confirmado.procesando)
        assertEquals(remoto.mensajes, estado.mensajes)
        assertEquals("", estado.borrador)
        assertTrue(estado.mensajesNoEnviados.isEmpty())
        assertNull(estado.error)
        assertFalse(estado.enviando)
    }
    @Test
    fun getInicialAntiguoLuegoCompletadaActualizaElChat() {
        val previo = listOf(ChatMensaje("Hola", "BOT"))
        val primeraLectura = chat(previo)
        val completado = chat(
            previo + listOf(ChatMensaje(texto, "PACIENTE"), ChatMensaje("Buscá atención hoy.", "BOT")),
            finalizado = true,
            estado = "COMPLETADA"
        )
        val transporte = TransporteSimulado(listOf(Result.success(primeraLectura), Result.success(completado)))

        val resultado = enviar(transporte, previo)

        val confirmado = assertIs<ResultadoEnvioChat.Confirmado>(resultado)
        assertEquals(2, transporte.lecturasRealizadas)
        assertTrue(confirmado.chat.finalizado)
        val estado = estadoOptimista(previo).aplicarChatReconciliado(confirmado.chat, confirmado.procesando)
        assertEquals("COMPLETADA", estado.estadoClasificacion)
        assertEquals(completado.mensajes, estado.mensajes)
        assertEquals("", estado.borrador)
        assertNull(estado.error)
    }

    @Test
    fun getSinConfirmacionConservaFalloParaReintentoManual() {
        val previo = listOf(ChatMensaje("Hola", "BOT"))
        val transporte = TransporteSimulado(listOf(Result.success(chat(previo))))

        val resultado = enviar(transporte, previo)

        val noConfirmado = assertIs<ResultadoEnvioChat.NoConfirmado>(resultado)
        assertEquals(errorDeTransporte, noConfirmado.error)
        assertEquals(1, transporte.envios)
        assertTrue(transporte.lecturasRealizadas > 0)
        val abierto = estadoOptimista(previo).aplicarFalloEnvioNoConfirmado(texto, noConfirmado.error)
        assertEquals(texto, abierto.borrador)
        assertTrue(texto in abierto.mensajesNoEnviados)
        assertTrue(abierto.error != null)
        assertFalse(abierto.enviando)
    }
    @Test
    fun errorDeAutorizacionNoHaceGetsYSeConservaComoErrorReal() {
        val transporte = TransporteSimulado(emptyList(), ErrorHttpChat(403, "Sin permiso"))
        val resultado = enviar(transporte, emptyList())
        val noConfirmado = assertIs<ResultadoEnvioChat.NoConfirmado>(resultado)
        assertIs<ErrorHttpChat>(noConfirmado.error)
        assertEquals(0, transporte.lecturasRealizadas)
        assertEquals(1, transporte.envios)
    }

    @Test
    fun cancelacionSePropagaSinConvertirseEnErrorDeEnvio() {
        val cancelacion = CancellationException("pantalla abandonada")
        var lecturas = 0
        val error = assertFailsWith<CancellationException> {
            runSuspend {
                enviarConReconciliacion(
                    chatId = 17,
                    contenido = texto,
                    mensajesAntes = emptyList(),
                    enviar = { _, _ -> throw cancelacion },
                    obtener = { _, _ ->
                        lecturas++
                        Result.success(chat(emptyList()))
                    },
                    esperar = { }
                )
            }
        }
        assertEquals(cancelacion, error)
        assertEquals(0, lecturas)
    }
    @Test
    fun textoIgualEnHistorialNoConfirmaEnvioNuevo() {
        val previo = listOf(ChatMensaje(texto, "PACIENTE"), ChatMensaje("Respuesta histórica", "BOT"))
        val transporte = TransporteSimulado(listOf(Result.success(chat(previo))))

        val resultado = enviar(transporte, previo)

        assertIs<ResultadoEnvioChat.NoConfirmado>(resultado)
        assertEquals(1, transporte.envios)
    }

    @Test
    fun mensajePacienteGuardadoSinBotEsProcesamientoPendienteTrasPollsAcotados() {
        val previo = listOf(ChatMensaje("Hola", "BOT"))
        val pacienteGuardado = chat(previo + ChatMensaje(texto, "PACIENTE"))
        val transporte = TransporteSimulado(listOf(Result.success(pacienteGuardado)))

        val resultado = enviar(transporte, previo)

        val confirmado = assertIs<ResultadoEnvioChat.Confirmado>(resultado)
        assertTrue(confirmado.procesando)
        assertEquals(4, transporte.lecturasRealizadas)
        val estado = estadoOptimista(previo).aplicarChatReconciliado(confirmado.chat, confirmado.procesando)
        assertTrue(estado.respuestaPendiente)
        assertEquals("", estado.borrador)
        assertNull(estado.error)
        assertFalse(estado.enviando)
        assertEquals(1, transporte.envios)
    }

    @Test
    fun getManualConRespuestaBotNuevaLiberaElEstadoPendiente() {
        val snapshot = listOf(ChatMensaje("Pregunta", "PACIENTE"))
        val actualizado = chat(snapshot + ChatMensaje("Respuesta", "BOT"))
        assertTrue(hayRespuestaBotNuevaDesde(snapshot, actualizado.mensajes))
        val state = ChatUiState(respuestaPendiente = true, verificandoRespuesta = true, mensajes = snapshot)
            .aplicarChatVerificadoPendiente(actualizado, respuestaRecibida = true)
        assertFalse(state.respuestaPendiente)
        assertFalse(state.verificandoRespuesta)
        assertEquals(actualizado.mensajes, state.mensajes)
    }

    @Test
    fun getManualAntiguoMantienePendienteSinSpinner() {
        val snapshot = listOf(ChatMensaje("Pregunta", "PACIENTE"))
        val actualizado = chat(snapshot)
        val state = ChatUiState(respuestaPendiente = true, verificandoRespuesta = true, mensajes = snapshot)
            .aplicarChatVerificadoPendiente(actualizado, respuestaRecibida = false)
        assertTrue(state.respuestaPendiente)
        assertFalse(state.verificandoRespuesta)
        assertEquals("", state.borrador)
    }
    @Test
    fun chatCerradoFallidoSeSincronizaYDejaDisponibleSoloReintentoDeClasificacion() {
        val previo = listOf(ChatMensaje("Hola", "BOT"))
        val fallido = chat(previo + listOf(ChatMensaje(texto, "PACIENTE"), ChatMensaje("No pude clasificar.", "BOT")), false, "FALLIDA")
        val transporte = TransporteSimulado(listOf(Result.success(fallido)))

        val resultado = enviar(transporte, previo)

        val confirmado = assertIs<ResultadoEnvioChat.Confirmado>(resultado)
        val estado = estadoOptimista(previo).aplicarChatReconciliado(confirmado.chat, confirmado.procesando)
        assertTrue(estado.finalizado)
        assertEquals(ChatClassificationPresentation.FALLIDA, presentacionClasificacionChat(estado.finalizado, estado.estadoClasificacion))
        assertEquals("", estado.borrador)
        assertTrue(estado.mensajesNoEnviados.isEmpty())
        assertEquals(1, transporte.envios)
        assertEquals(1, transporte.lecturasRealizadas)
    }

    private fun enviar(transporte: TransporteSimulado, previo: List<ChatMensaje>, contenido: String = texto): ResultadoEnvioChat = runSuspend {
        enviarConReconciliacion(
            chatId = 17,
            contenido = contenido,
            mensajesAntes = previo,
            enviar = transporte::enviar,
            obtener = transporte::obtener,
            esperar = { }
        )
    }

    private fun estadoOptimista(previo: List<ChatMensaje>) = ChatUiState(
        chatId = 17,
        enviando = true,
        mensajes = previo + ChatMensaje(texto, "PACIENTE"),
        mensajesNoEnviados = setOf(texto),
        borrador = "",
        error = "fallo temporal"
    )

    private fun chat(
        mensajes: List<ChatMensaje>,
        finalizado: Boolean = false,
        estado: String = "NO_SOLICITADA"
    ) = ChatResponse(id = 17, mensajes = mensajes, finalizado = finalizado, estadoClasificacion = estado)

    private inner class TransporteSimulado(private val lecturas: List<Result<ChatResponse>>, private val falloPost: Throwable = errorDeTransporte, private val contenidoEsperado: String = texto) {
        var envios = 0
        var lecturasRealizadas = 0

        suspend fun enviar(id: Long, contenido: String): Result<RespuestaChatResponse> {
            envios++
            assertEquals(17, id)
            assertEquals(contenidoEsperado, contenido)
            return Result.failure(falloPost)
        }

        suspend fun obtener(id: Long, timeoutMillis: Long): Result<ChatResponse> {
            lecturasRealizadas++
            assertEquals(17, id)
            assertTrue(timeoutMillis in 1..2_000)
            return lecturas.getOrElse(lecturasRealizadas - 1) { lecturas.last() }
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) {
                result = value
            }
        })
        return result?.getOrThrow() ?: error("Test suspend function unexpectedly suspended")
    }
}
