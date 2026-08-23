package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.R2StorageConfig
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class StorageRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("r2_storage_prefs", Context.MODE_PRIVATE)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val dbRef: DatabaseReference? by lazy {
        try {
            FirebaseDatabase.getInstance().getReference("storage_configs")
        } catch (e: Exception) {
            Log.e("StorageRepository", "FirebaseDatabase not ready: ${e.message}")
            null
        }
    }

    init {
        // Initialize default Cloudflare R2 Account if none exists
        if (getLocalConfigs().isEmpty()) {
            val defaultConfig = R2StorageConfig(
                id = UUID.randomUUID().toString(),
                label = "Cloudflare R2 Storage",
                bucketName = "social-image-video",
                accountId = "2bc16e75370a4b1efde11c17a12c81d5",
                accessKeyId = "bb23db527d18408910c6aebdcc794d5c",
                secretAccessKey = "1dc1bc90c4ab70c625b01194cbc5b7d8264dc46218870278d25e38850115cfcd",
                publicEndpoint = "https://pub-c98a2e409ad94dbb8aded428cf2952a1.r2.dev",
                isActive = true
            )
            saveLocalConfigs(listOf(defaultConfig))
        }
    }

    fun getLocalConfigs(): List<R2StorageConfig> {
        val jsonString = prefs.getString("configs_json", null) ?: return emptyList()
        val list = mutableListOf<R2StorageConfig>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    R2StorageConfig(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.optString("label", "Cloudflare R2 Storage"),
                        bucketName = obj.optString("bucketName", "social-image-video"),
                        accountId = obj.optString("accountId", ""),
                        accessKeyId = obj.optString("accessKeyId", ""),
                        secretAccessKey = obj.optString("secretAccessKey", ""),
                        publicEndpoint = obj.optString("publicEndpoint", ""),
                        isActive = obj.optBoolean("isActive", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error parsing local configs: ${e.message}")
        }
        return list
    }

    fun saveLocalConfigs(configs: List<R2StorageConfig>) {
        try {
            val array = JSONArray()
            for (cfg in configs) {
                val obj = JSONObject().apply {
                    put("id", cfg.id)
                    put("label", cfg.label)
                    put("bucketName", cfg.bucketName)
                    put("accountId", cfg.accountId)
                    put("accessKeyId", cfg.accessKeyId)
                    put("secretAccessKey", cfg.secretAccessKey)
                    put("publicEndpoint", cfg.publicEndpoint)
                    put("isActive", cfg.isActive)
                    put("createdAt", cfg.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString("configs_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving configs: ${e.message}")
        }
    }

    fun getActiveConfig(): R2StorageConfig? {
        val list = getLocalConfigs()
        return list.firstOrNull { it.isActive } ?: list.firstOrNull()
    }

    fun saveOrUpdateConfig(config: R2StorageConfig, userId: String = "global") {
        val currentList = getLocalConfigs().toMutableList()
        val configId = if (config.id.isBlank()) UUID.randomUUID().toString() else config.id
        val finalizedConfig = config.copy(id = configId)

        if (finalizedConfig.isActive) {
            // Set all other configs to inactive
            for (i in currentList.indices) {
                currentList[i] = currentList[i].copy(isActive = false)
            }
        }

        val existingIndex = currentList.indexOfFirst { it.id == finalizedConfig.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = finalizedConfig
        } else {
            currentList.add(finalizedConfig)
        }

        // Ensure at least one is active if list not empty
        if (currentList.none { it.isActive } && currentList.isNotEmpty()) {
            currentList[0] = currentList[0].copy(isActive = true)
        }

        saveLocalConfigs(currentList)

        // Sync with Firebase Realtime Database
        try {
            dbRef?.child(userId)?.child(finalizedConfig.id)?.setValue(finalizedConfig.toMap())
        } catch (e: Exception) {
            Log.e("StorageRepository", "Firebase sync error: ${e.message}")
        }
    }

    fun setActiveConfig(configId: String, userId: String = "global") {
        val currentList = getLocalConfigs().map {
            it.copy(isActive = (it.id == configId))
        }
        saveLocalConfigs(currentList)

        try {
            currentList.forEach { cfg ->
                dbRef?.child(userId)?.child(cfg.id)?.child("isActive")?.setValue(cfg.isActive)
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Firebase sync active error: ${e.message}")
        }
    }

    fun deleteConfig(configId: String, userId: String = "global") {
        val currentList = getLocalConfigs().filter { it.id != configId }.toMutableList()
        if (currentList.none { it.isActive } && currentList.isNotEmpty()) {
            currentList[0] = currentList[0].copy(isActive = true)
        }
        saveLocalConfigs(currentList)

        try {
            dbRef?.child(userId)?.child(configId)?.removeValue()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Firebase delete error: ${e.message}")
        }
    }

    suspend fun testConnection(config: R2StorageConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val rawEndpoint = config.publicEndpoint.trim().ifBlank {
            "https://${config.accountId}.r2.cloudflarestorage.com/${config.bucketName}"
        }
        val targetUrl = if (!rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://")) {
            "https://$rawEndpoint"
        } else {
            rawEndpoint
        }

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .head()
                .build()

            httpClient.newCall(request).execute().use { response ->
                // If the endpoint is reachable (including 200, 301, 302, 403, 404 from Cloudflare edge)
                if (response.code in 200..404) {
                    Pair(true, "Server Status: Connected Successfully")
                } else {
                    Pair(false, "Server Error: HTTP ${response.code} (${response.message.ifBlank { "Invalid response" }})")
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Network test error: ${e.message}", e)
            Pair(false, "Server Error: ${e.localizedMessage ?: "Connection Timed Out or Unreachable"}")
        }
    }
}
