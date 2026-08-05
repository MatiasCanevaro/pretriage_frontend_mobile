package com.proyecto_final.triage.viewmodels

import androidx.compose.runtime.staticCompositionLocalOf

val LocalStudiesViewModel = staticCompositionLocalOf<StudiesViewModel> {
    error("StudiesViewModel no fue provisto. Envolvé tu Navigator con CompositionLocalProvider.")
}