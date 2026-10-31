package com.proyecto_final.triage.audio

/** Política por turnos. El llamador sincroniza el acceso y usa un reloj monotónico. */
internal class ControlEcoVoz(private val margenEcoMs: Long = 300L) {
    private var fotogramasEncolados = 0L
    private var esperandoDrenaje = false
    private var silenciarHastaMs = Long.MIN_VALUE

    fun encolar(fotogramas: Long) {
        require(fotogramas > 0)
        fotogramasEncolados += fotogramas
        esperandoDrenaje = true
    }

    fun debeSilenciar(fotogramasReproducidos: Long, ahoraMs: Long): Boolean {
        if (fotogramasReproducidos < fotogramasEncolados) return true
        if (esperandoDrenaje) {
            esperandoDrenaje = false
            silenciarHastaMs = ahoraMs + margenEcoMs
        }
        return ahoraMs < silenciarHastaMs
    }

    fun vaciar(ahoraMs: Long) {
        fotogramasEncolados = 0L
        esperandoDrenaje = false
        silenciarHastaMs = ahoraMs + margenEcoMs
    }
}


/** AEC confirmado es capacidad, no una promesa de supresión acústica perfecta. */
internal fun modoEntradaVoz(
    forzarPorTurnos: Boolean,
    comunicacionActiva: Boolean,
    focoObtenido: Boolean,
    auriculares: Boolean,
    salidaConocida: Boolean,
    cancelacionEcoActiva: Boolean
): ModoEntradaVoz = if (!forzarPorTurnos && comunicacionActiva && focoObtenido &&
    (auriculares || (salidaConocida && cancelacionEcoActiva))) {
    ModoEntradaVoz.INTERRUMPIBLE
} else ModoEntradaVoz.POR_TURNOS
