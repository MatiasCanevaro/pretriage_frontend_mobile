package com.proyecto_final.triage.filesaver

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import android.content.Intent
import androidx.core.content.FileProvider

actual class PlatformContext(val context: Context)

actual object FileSaver {
    actual suspend fun saveToDownloads(
        context: PlatformContext,
        fileName: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.context.contentResolver   // 👈 ojo el .context.context

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeTypeFor(fileName))
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IOException("No se pudo crear el archivo"))

                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: return@withContext Result.failure(IOException("No se pudo abrir el archivo"))

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(downloadsDir, fileName)
                file.writeBytes(bytes)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mimeTypeFor(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "")
        return when (ext.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }
}

actual object FileViewer {

    actual suspend fun archivoExisteLocalmente(
        context: PlatformContext,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        archivoLocal(context.context, fileName).exists()
    }

    actual suspend fun guardarLocalmente(
        context: PlatformContext,
        fileName: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            archivoLocal(context.context, fileName).writeBytes(bytes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun abrirArchivo(
        context: PlatformContext,
        fileName: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val ctx = context.context
            val file = archivoLocal(ctx, fileName)

            if (!file.exists()) {
                return@withContext Result.failure(IOException("El archivo no existe localmente"))
            }

            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )

            val mimeType = when (fileName.substringAfterLast('.', "").lowercase()) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            ctx.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun archivoLocal(context: Context, fileName: String): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, fileName)
    }
}

@Composable
actual fun rememberPlatformContext(): PlatformContext = PlatformContext(LocalContext.current)