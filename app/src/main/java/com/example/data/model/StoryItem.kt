package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class StoryViewerInfo(
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val reaction: String = "", // "LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY" or ""
    val viewedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "userAvatar" to userAvatar,
            "reaction" to reaction,
            "viewedAt" to viewedAt
        )
    }
}

@IgnoreExtraProperties
data class StoryItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image", "video", "text"
    val caption: String = "",
    val backgroundStyle: String = "none",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
    val viewers: Map<String, StoryViewerInfo> = emptyMap()
) {
    val viewersCount: Int
        get() = viewers.size

    val viewersList: List<StoryViewerInfo>
        get() = viewers.values.sortedByDescending { it.viewedAt }

    fun toMap(): Map<String, Any> {
        val viewersMap = viewers.mapValues { it.value.toMap() }
        return mapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userAvatar" to userAvatar,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption,
            "backgroundStyle" to backgroundStyle,
            "createdAt" to createdAt,
            "expiresAt" to expiresAt,
            "viewers" to viewersMap
        )
    }
}

