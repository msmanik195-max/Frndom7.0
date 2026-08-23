package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class R2StorageConfig(
    val id: String = "",
    val label: String = "Cloudflare R2 Storage",
    val bucketName: String = "social-image-video",
    val accountId: String = "2bc16e75370a4b1efde11c17a12c81d5",
    val accessKeyId: String = "bb23db527d18408910c6aebdcc794d5c",
    val secretAccessKey: String = "1dc1bc90c4ab70c625b01194cbc5b7d8264dc46218870278d25e38850115cfcd",
    val publicEndpoint: String = "https://pub-c98a2e409ad94dbb8aded428cf2952a1.r2.dev",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "label" to label,
            "bucketName" to bucketName,
            "accountId" to accountId,
            "accessKeyId" to accessKeyId,
            "secretAccessKey" to secretAccessKey,
            "publicEndpoint" to publicEndpoint,
            "isActive" to isActive,
            "createdAt" to createdAt
        )
    }
}
