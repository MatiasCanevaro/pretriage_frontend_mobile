package com.proyecto_final.triage.filesaver

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.Foundation.*
import platform.UIKit.*

actual class PlatformContext

actual object FileSaver {
    actual suspend fun saveToDownloads(
        context: PlatformContext,
        fileName: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val fileManager = NSFileManager.defaultManager
            val documentsUrl = fileManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            ).firstOrNull() as? platform.Foundation.NSURL
                ?: return@withContext Result.failure(IOException("No se encontró Documentos"))

            val fileUrl = documentsUrl.URLByAppendingPathComponent(fileName)
                ?: return@withContext Result.failure(IOException("Ruta inválida"))

            val nsData = bytes.toNSData()
            val ok = nsData.writeToURL(fileUrl, true)

            if (ok) Result.success(Unit) else Result.failure(IOException("No se pudo escribir"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned {
    NSData.create(bytes = it.addressOf(0), length = this.size.toULong())
}

actual object FileViewer {

    actual suspend fun archivoExisteLocalmente(
        context: PlatformContext,
        fileName: String
    ): Boolean = withContext(Dispatchers.Default) {
        NSFileManager.defaultManager.fileExistsAtPath(rutaLocal(fileName))
    }

    actual suspend fun guardarLocalmente(
        context: PlatformContext,
        fileName: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val ok = bytes.toNSData().writeToFile(rutaLocal(fileName), true)
            if (ok) Result.success(Unit) else Result.failure(Exception("No se pudo guardar"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun abrirArchivo(
        context: PlatformContext,
        fileName: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val url = NSURL.fileURLWithPath(rutaLocal(fileName))
            val activityVC = UIActivityViewController(listOf(url), null)
            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(activityVC, true, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun rutaLocal(fileName: String): String {
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).first() as String
        return "$documentsPath/$fileName"
    }
}

@Composable
actual fun rememberPlatformContext(): PlatformContext = PlatformContext()