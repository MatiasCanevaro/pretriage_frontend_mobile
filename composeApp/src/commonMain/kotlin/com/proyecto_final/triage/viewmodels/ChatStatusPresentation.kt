package com.proyecto_final.triage.viewmodels

/** Classification state for display; interview closure is tracked separately. */
enum class ChatClassificationPresentation {
    NO_SOLICITADA,
    PROCESANDO,
    FALLIDA,
    COMPLETADA,
    CERRADA_SIN_RESULTADO
}

/**
 * Maps the backend classification status to UI. Legacy responses can omit the status;
 * an interview closure or queue estimate alone cannot establish triage success.
 */
fun presentacionClasificacionChat(
    finalizado: Boolean,
    estadoClasificacion: String?
): ChatClassificationPresentation = when (estadoClasificacion?.uppercase()) {
    "NO_SOLICITADA" -> ChatClassificationPresentation.NO_SOLICITADA
    "PENDIENTE" -> ChatClassificationPresentation.PROCESANDO
    "FALLIDA" -> ChatClassificationPresentation.FALLIDA
    "COMPLETADA" -> ChatClassificationPresentation.COMPLETADA
    else -> if (finalizado) {
        ChatClassificationPresentation.CERRADA_SIN_RESULTADO
    } else {
        ChatClassificationPresentation.NO_SOLICITADA
    }
}
