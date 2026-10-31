package com.proyecto_final.triage.audio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlEcoVozTest {
    @Test
    fun escuchaAntesDeLaPrimeraRespuesta() {
        assertFalse(ControlEcoVoz().debeSilenciar(0, 0))
    }

    @Test
    fun colaVaciaNoHabilitaMicrofonoMientrasAudioTrackTodaviaReproduce() {
        val control = ControlEcoVoz()
        control.encolar(24_000)
        assertTrue(control.debeSilenciar(0, 0))
        // Todo puede estar escrito en AudioTrack: todavía quedan frames por sonar.
        assertTrue(control.debeSilenciar(23_999, 10_000))
        assertTrue(control.debeSilenciar(24_000, 10_001))
        assertTrue(control.debeSilenciar(24_000, 10_300))
        assertFalse(control.debeSilenciar(24_000, 10_301))
    }

    @Test
    fun nuevaRespuestaDuranteMargenEsperaSuPropioDrenaje() {
        val control = ControlEcoVoz()
        control.encolar(100)
        assertTrue(control.debeSilenciar(100, 0))
        control.encolar(200)
        assertTrue(control.debeSilenciar(100, 400))
        assertTrue(control.debeSilenciar(300, 500))
        assertFalse(control.debeSilenciar(300, 800))
    }

    @Test
    fun interrupcionDescartaPendientesPeroConservaMargenDeEco() {
        val control = ControlEcoVoz()
        control.encolar(1_000)
        control.vaciar(100)
        assertTrue(control.debeSilenciar(0, 399))
        assertFalse(control.debeSilenciar(0, 400))
        control.encolar(50)
        assertTrue(control.debeSilenciar(0, 401))
        assertTrue(control.debeSilenciar(50, 500))
        assertFalse(control.debeSilenciar(50, 800))
    }

    @Test
    fun descartaLecturaQueComienzaDuranteEcoAunqueTermineDespues() {
        val control = ControlEcoVoz()
        control.encolar(100)
        assertTrue(control.debeSilenciar(100, 0))
        val antes = control.debeSilenciar(100, 299)
        val despues = control.debeSilenciar(100, 399)
        assertTrue(antes || despues)
        assertFalse(control.debeSilenciar(100, 400))
    }

    @Test
    fun parlanteRequiereAecHabilitadoYComunicacionConFoco() {
        assertEquals(ModoEntradaVoz.INTERRUMPIBLE,
            modoEntradaVoz(false, true, true, false, true, true))
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(false, true, true, false, true, false))
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(false, false, true, false, true, true))
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(false, true, false, false, true, true))
    }

    @Test
    fun auricularesPermitenInterrumpirSinAecPeroNuncaSinFoco() {
        assertEquals(ModoEntradaVoz.INTERRUMPIBLE,
            modoEntradaVoz(false, true, true, true, false, false))
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(false, true, false, true, false, false))
    }

    @Test
    fun rutaDesconocidaYProteccionManualSiempreUsanTurnos() {
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(false, true, true, false, false, true))
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(true, true, true, false, true, true))
        assertEquals(ModoEntradaVoz.POR_TURNOS,
            modoEntradaVoz(true, true, true, true, false, false))
    }

    @Test
    fun activarProteccionDuranteRespuestaConservaDrenajePendiente() {
        val control = ControlEcoVoz()
        control.encolar(1_000)
        // En dúplex se actualiza la política aunque no silencie el micrófono.
        assertTrue(control.debeSilenciar(500, 0))
        val protegido = modoEntradaVoz(true, true, true, false, true, true)
        assertEquals(ModoEntradaVoz.POR_TURNOS, protegido)
        assertTrue(control.debeSilenciar(999, 500))
        assertTrue(control.debeSilenciar(1_000, 600))
        assertFalse(control.debeSilenciar(1_000, 900))
    }
}
