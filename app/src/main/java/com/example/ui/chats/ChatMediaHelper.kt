package com.example.ui.chats

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ChatMediaHelper {

    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0L) return ""
        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
            else -> "$sizeInBytes B"
        }
    }

    fun downloadMediaFile(
        context: Context,
        url: String,
        suggestedName: String = "frndom_media",
        isVideo: Boolean = false,
        isApkOrDoc: Boolean = false
    ) {
        if (url.isBlank()) {
            Toast.makeText(context, "Cannot download: Invalid URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = when {
                isApkOrDoc -> suggestedName.ifBlank { "file_${System.currentTimeMillis()}.apk" }
                isVideo -> "${suggestedName}_${System.currentTimeMillis()}.mp4"
                else -> "${suggestedName}_${System.currentTimeMillis()}.jpg"
            }
            val uri = Uri.parse(url)

            val destDir = when {
                isApkOrDoc -> Environment.DIRECTORY_DOWNLOADS
                isVideo -> Environment.DIRECTORY_MOVIES
                else -> Environment.DIRECTORY_PICTURES
            }

            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("Downloading from Frndom Chat")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(destDir, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                manager.enqueue(request)
                Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()
            } else {
                downloadDirectly(context, url, fileName, isVideo, isApkOrDoc)
            }
        } catch (e: Exception) {
            Log.e("ChatMediaHelper", "DownloadManager failed: ${e.message}")
            downloadDirectly(context, url, suggestedName, isVideo, isApkOrDoc)
        }
    }

    fun downloadAndOpenApkOrFile(context: Context, url: String, fileName: String) {
        if (url.isBlank()) return
        Toast.makeText(context, "Downloading $fileName...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resolvedName = fileName.ifBlank { "app_${System.currentTimeMillis()}.apk" }
                val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val file = File(targetDir, resolvedName)

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.doInput = true
                conn.connect()

                val input = conn.inputStream
                val output = FileOutputStream(file)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
                output.close()
                input.close()

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            if (resolvedName.endsWith(".apk", ignoreCase = true)) {
                                setDataAndType(fileUri, "application/vnd.android.package-archive")
                            } else {
                                setDataAndType(fileUri, "*/*")
                            }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Downloaded to: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatMediaHelper", "Error downloading/opening file: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    downloadMediaFile(context, url, fileName, isApkOrDoc = true)
                }
            }
        }
    }

    private fun downloadDirectly(
        context: Context,
        fileUrl: String,
        fileName: String,
        isVideo: Boolean,
        isApkOrDoc: Boolean = false
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                val dir = when {
                    isApkOrDoc -> context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                    isVideo -> context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                    else -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
                }
                val file = File(dir, fileName)
                val input = connection.inputStream
                val output = FileOutputStream(file)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
                output.close()
                input.close()

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Saved to device: ${file.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ChatMediaHelper", "Direct download error: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class AudioRecordingManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var recordingStartTime = 0L

    fun startRecording(): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "audio_notes").apply { mkdirs() }
            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentAudioFile = file

            recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recordingStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Failed to start recording: ${e.message}")
            false
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            val duration = System.currentTimeMillis() - recordingStartTime
            if (duration >= 500 && currentAudioFile != null && currentAudioFile!!.exists() && currentAudioFile!!.length() > 0) {
                currentAudioFile
            } else {
                currentAudioFile?.delete()
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Failed to stop recording: ${e.message}")
            currentAudioFile?.delete()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            currentAudioFile?.delete()
            currentAudioFile = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}

class VoicePlayerManager(private val context: Context? = null) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUrl: String? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress = _currentProgress.asStateFlow()

    private var progressJob: Job? = null

    fun playOrPause(url: String, scope: CoroutineScope) {
        if (url.isBlank()) return

        if (currentPlayingUrl == url && mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            } else {
                mediaPlayer!!.start()
                _isPlaying.value = true
                startProgressTracker(scope)
            }
            return
        }

        stop()
        try {
            currentPlayingUrl = url
            val targetPath = if (url.startsWith("data:audio")) {
                // Decode base64 data to local temp cache file for MediaPlayer
                val base64Content = url.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
                val tempFile = File.createTempFile("voice_note_", ".m4a")
                tempFile.deleteOnExit()
                FileOutputStream(tempFile).use { it.write(decodedBytes) }
                tempFile.absolutePath
            } else {
                url
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(targetPath)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker(scope)
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentProgress.value = 0f
                    progressJob?.cancel()
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    progressJob?.cancel()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("VoicePlayerManager", "Audio playback error: ${e.message}")
        }
    }

    private fun startProgressTracker(scope: CoroutineScope) {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && mediaPlayer != null && _isPlaying.value) {
                try {
                    val current = mediaPlayer?.currentPosition ?: 0
                    val total = mediaPlayer?.duration ?: 1
                    if (total > 0) {
                        _currentProgress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(100)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        currentPlayingUrl = null
        _isPlaying.value = false
        _currentProgress.value = 0f
    }
}
