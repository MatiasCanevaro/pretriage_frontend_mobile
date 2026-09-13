package com.proyecto_final.triage

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.proyecto_final.triage.screens.StartupState
import com.proyecto_final.triage.storage.AndroidTokenStorage
import com.proyecto_final.triage.storage.TokenStorageProvider
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

lateinit var appContext: Context

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)

        appContext = applicationContext

        TokenStorageProvider.instance = AndroidTokenStorage()

        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)

        enableEdgeToEdge()

        splash.setKeepOnScreenCondition {
            StartupState.loading
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}