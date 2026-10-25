package com.proyecto_final.triage

import com.proyecto_final.triage.viewmodels.ChatClassificationPresentation
import com.proyecto_final.triage.viewmodels.presentacionClasificacionChat
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatStatusPresentationTest {
    @Test
    fun estadosDeClasificacionSeSeparanDelCierreDeEntrevista() {
        assertEquals(ChatClassificationPresentation.NO_SOLICITADA, presentacionClasificacionChat(false, "NO_SOLICITADA"))
        assertEquals(ChatClassificationPresentation.PROCESANDO, presentacionClasificacionChat(true, "PENDIENTE"))
        assertEquals(ChatClassificationPresentation.FALLIDA, presentacionClasificacionChat(true, "FALLIDA"))
        assertEquals(ChatClassificationPresentation.COMPLETADA, presentacionClasificacionChat(true, "COMPLETADA"))
    }

    @Test
    fun unaRespuestaLegacySinEstadoNoTomaCierreComoExito() {
        assertEquals(ChatClassificationPresentation.CERRADA_SIN_RESULTADO, presentacionClasificacionChat(true, null))
    }

    @Test
    fun falloDeEnvioNoEsFalloDeClasificacionHastaConfirmarRespuestasGuardadas() {
        // A failed POST with an open interview remains an ordinary message-send failure.
        assertEquals(ChatClassificationPresentation.NO_SOLICITADA, presentacionClasificacionChat(false, "NO_SOLICITADA"))
        // GET confirming a closed interview and FALLIDA selects the saved-answer retry card.
        assertEquals(ChatClassificationPresentation.FALLIDA, presentacionClasificacionChat(true, "FALLIDA"))
    }
}
