package com.example.voice

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

sealed interface KwsModelState {
    data object NotDownloaded : KwsModelState
    data class Downloading(val progressPercent: Int) : KwsModelState
    data object Extracting : KwsModelState
    data class Ready(val modelDir: File) : KwsModelState
    data class Error(val message: String) : KwsModelState
}

/**
 * Manages the lightweight offline Vosk acoustic model (~40 MB) used by [HeyNovaKeywordSpotter]
 * for continuous grammar-constrained "Hey NOVA" keyword spotting inside [com.example.service.NovaVoiceService].
 */
object VoskModelManager {

    const val MODEL_NAME = "vosk-model-small-en-us-0.15"
    const val MODEL_DOWNLOAD_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _state = MutableStateFlow<KwsModelState>(KwsModelState.NotDownloaded)
    val state: StateFlow<KwsModelState> = _state.asStateFlow()

    @Volatile
    private var downloadJob: Job? = null

    fun getModelsRootDir(context: Context): File =
        File(context.applicationContext.filesDir, "kws_models")

    fun getModelDir(context: Context): File =
        File(getModelsRootDir(context), MODEL_NAME)

    fun isValidModelDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val hasModelConf = File(dir, "conf/model.conf").isFile || File(dir, "conf/mfcc.conf").isFile
        val hasAcousticModel = File(dir, "am/final.mdl").isFile
        return hasModelConf && hasAcousticModel
    }

    fun refreshState(context: Context): KwsModelState {
        val current = _state.value
        if (current is KwsModelState.Downloading || current is KwsModelState.Extracting) {
            return current
        }
        val dir = getModelDir(context)
        val next = if (isValidModelDirectory(dir)) {
            KwsModelState.Ready(dir)
        } else {
            KwsModelState.NotDownloaded
        }
        _state.value = next
        return next
    }

    fun getReadyModelDirOrNull(context: Context): File? {
        val dir = getModelDir(context)
        return if (isValidModelDirectory(dir)) dir else null
    }

    fun startDownloadIfNeeded(context: Context, onReady: ((File) -> Unit)? = null) {
        val appContext = context.applicationContext
        val existing = getReadyModelDirOrNull(appContext)
        if (existing != null) {
            _state.value = KwsModelState.Ready(existing)
            onReady?.invoke(existing)
            return
        }
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch {
            val result = downloadAndExtract(appContext)
            if (result != null) {
                onReady?.invoke(result)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        if (_state.value is KwsModelState.Downloading || _state.value is KwsModelState.Extracting) {
            _state.value = KwsModelState.NotDownloaded
        }
    }

    fun deleteModel(context: Context) {
        cancelDownload()
        val dir = getModelDir(context.applicationContext)
        runCatching { dir.deleteRecursively() }
        _state.value = KwsModelState.NotDownloaded
    }

    private suspend fun downloadAndExtract(context: Context): File? = withContext(Dispatchers.IO) {
        val rootDir = getModelsRootDir(context).apply { mkdirs() }
        val tempZip = File(rootDir, "$MODEL_NAME.zip.part")
        val targetDir = getModelDir(context)

        try {
            _state.value = KwsModelState.Downloading(0)
            val request = Request.Builder().url(MODEL_DOWNLOAD_URL).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _state.value = KwsModelState.Error("Model download failed (HTTP ${response.code}).")
                    return@withContext null
                }
                val body = response.body ?: run {
                    _state.value = KwsModelState.Error("Empty response while downloading KWS model.")
                    return@withContext null
                }
                val totalBytes = body.contentLength()
                var downloaded = 0L
                var lastReported = -1

                body.byteStream().use { input ->
                    FileOutputStream(tempZip).use { output ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                if (pct != lastReported) {
                                    lastReported = pct
                                    _state.value = KwsModelState.Downloading(pct)
                                }
                            }
                        }
                    }
                }
            }

            _state.value = KwsModelState.Extracting
            if (targetDir.exists()) targetDir.deleteRecursively()
            extractZipSafely(tempZip, rootDir)
            tempZip.delete()

            if (isValidModelDirectory(targetDir)) {
                _state.value = KwsModelState.Ready(targetDir)
                targetDir
            } else {
                _state.value = KwsModelState.Error("Extracted KWS model is incomplete.")
                null
            }
        } catch (ce: CancellationException) {
            tempZip.delete()
            _state.value = KwsModelState.NotDownloaded
            throw ce
        } catch (e: Exception) {
            tempZip.delete()
            _state.value = KwsModelState.Error("KWS model error: ${e.localizedMessage ?: "Network unavailable"}")
            null
        }
    }

    private fun extractZipSafely(zipFile: File, destDir: File) {
        val canonicalDest = destDir.canonicalPath + File.separator
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8 * 1024)
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalDest)) {
                    throw SecurityException("Blocked unsafe zip entry path: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            fos.write(buffer, 0, count)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
