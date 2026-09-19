package com.proyecto_final.triage.filesaver

import androidx.compose.runtime.Composable

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformContext

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object FileSaver {
    suspend fun saveToDownloads(
        context: PlatformContext,
        fileName: String,
        bytes: ByteArray
    ): Result<Unit>
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object FileViewer {
    suspend fun archivoExisteLocalmente(context: PlatformContext, fileName: String): Boolean
    suspend fun guardarLocalmente(context: PlatformContext, fileName: String, bytes: ByteArray): Result<Unit>
    suspend fun abrirArchivo(context: PlatformContext, fileName: String): Result<Unit>
}

@Composable
expect fun rememberPlatformContext(): PlatformContext