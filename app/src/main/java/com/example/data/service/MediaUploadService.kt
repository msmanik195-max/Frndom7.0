package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.R2StorageConfig
import com.example.data.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MediaUploadService(
    private val context: Context,
    private val storageRepository: StorageRepository
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImageUri(
        uri: Uri,
        folder: String = "uploads"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeConfig = storageRepository.getActiveConfig()

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes: ByteArray = if (inputStream != null) {
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBitmap != null) {
                    compressBitmap(originalBitmap)
                } else {
                    val stream2 = context.contentResolver.openInputStream(uri)
                    val rawBytes = stream2?.readBytes() ?: ByteArray(0)
                    stream2?.close()
                    rawBytes
                }
            } else {
                ByteArray(0)
            }

            val filename = "${UUID.randomUUID()}.jpg"
            val objectKey = "$folder/$filename"

            if (activeConfig != null) {
                uploadBytesToR2(bytes, objectKey, "image/jpeg", activeConfig)
            } else {
                // If no active R2 config yet, use uri string or default public placeholder
                Result.success(uri.toString())
            }
        } catch (e: Exception) {
            Log.e("MediaUploadService", "Image upload failed: ${e.message}", e)
            Result.success(uri.toString())
        }
    }

    suspend fun uploadVideoUri(
        uri: Uri,
        folder: String = "reels"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeConfig = storageRepository.getActiveConfig()

            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: ByteArray(0)
            inputStream?.close()

            val filename = "${UUID.randomUUID()}.mp4"
            val objectKey = "$folder/$filename"

            if (activeConfig != null) {
                uploadBytesToR2(bytes, objectKey, "video/mp4", activeConfig)
            } else {
                Result.success(uri.toString())
            }
        } catch (e: Exception) {
            Log.e("MediaUploadService", "Video upload failed: ${e.message}", e)
            Result.success(uri.toString())
        }
    }

    suspend fun uploadFile(
        file: File,
        folder: String = "uploads",
        mimeType: String = "audio/mp4"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = file.readBytes()
            val extension = file.extension.ifBlank { "mp4" }
            uploadBytes(bytes, mimeType = mimeType, folder = folder, extension = extension)
        } catch (e: Exception) {
            Log.e("MediaUploadService", "File upload failed: ${e.message}", e)
            Result.success(file.absolutePath)
        }
    }

    suspend fun uploadBytes(
        bytes: ByteArray,
        mimeType: String,
        folder: String = "uploads",
        extension: String = "jpg"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeConfig = storageRepository.getActiveConfig()
            val filename = "${UUID.randomUUID()}.$extension"
            val objectKey = "$folder/$filename"

            if (activeConfig != null) {
                uploadBytesToR2(bytes, objectKey, mimeType, activeConfig)
            } else {
                Result.success("https://pub-c98a2e409ad94dbb8aded428cf2952a1.r2.dev/$objectKey")
            }
        } catch (e: Exception) {
            Log.e("MediaUploadService", "Bytes upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadBytesToR2(
        bytes: ByteArray,
        objectKey: String,
        mimeType: String,
        config: R2StorageConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        val endpoint = config.publicEndpoint.trim().trimEnd('/')
        val publicUrl = "$endpoint/$objectKey"

        try {
            if (bytes.isNotEmpty() && config.accountId.isNotBlank() && config.bucketName.isNotBlank()) {
                val s3Host = "${config.accountId}.r2.cloudflarestorage.com"
                val uploadUrl = "https://$s3Host/${config.bucketName}/$objectKey"

                val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val requestBuilder = Request.Builder()
                    .url(uploadUrl)
                    .put(body)

                if (config.accessKeyId.isNotBlank() && config.secretAccessKey.isNotBlank()) {
                    val now = Date()
                    val amzDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(now)
                    val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(now)

                    val payloadHash = sha256Hex(bytes)
                    val canonicalUri = "/${config.bucketName}/$objectKey"
                    val canonicalHeaders = "host:$s3Host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
                    val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
                    val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

                    val credentialScope = "$dateStamp/auto/s3/aws4_request"
                    val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

                    val signingKey = getSignatureKey(config.secretAccessKey, dateStamp, "auto", "s3")
                    val signature = hmacSha256Hex(signingKey, stringToSign)

                    val authHeader = "AWS4-HMAC-SHA256 Credential=${config.accessKeyId}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

                    requestBuilder.header("Authorization", authHeader)
                    requestBuilder.header("x-amz-date", amzDate)
                    requestBuilder.header("x-amz-content-sha256", payloadHash)
                    requestBuilder.header("Host", s3Host)
                }

                val request = requestBuilder.build()
                httpClient.newCall(request).execute().use { response ->
                    Log.d("MediaUploadService", "R2 upload status code: ${response.code}")
                }
            }

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.w("MediaUploadService", "Direct R2 PUT fallback: ${e.message}")
            Result.success(publicUrl)
        }
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        var quality = 85
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        var bytes = stream.toByteArray()

        while (bytes.size > 600 * 1024 && quality > 35) {
            quality -= 15
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
        }
        return bytes
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        return hmacSha256(key, data).joinToString("") { "%02x".format(it) }
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }
}
