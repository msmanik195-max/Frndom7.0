package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

enum class ReactionType(val key: String, val label: String, val emoji: String, val colorHex: Long) {
    LIKE("LIKE", "Like", "👍", 0xFF1877F2),
    LOVE("LOVE", "Love", "❤️", 0xFFFA383E),
    CARE("CARE", "Care", "🥰", 0xFFF7B125),
    HAHA("HAHA", "Haha", "😆", 0xFFF7B125),
    WOW("WOW", "Wow", "😮", 0xFFF7B125),
    SAD("SAD", "Sad", "😢", 0xFFF7B125),
    ANGRY("ANGRY", "Angry", "😡", 0xFFE04E35);

    companion object {
        fun fromKey(key: String?): ReactionType? {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        }
    }
}

@IgnoreExtraProperties
data class PostItem(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val backgroundStyle: String = "none",
    val fontSize: Int = 24,
    val textAlign: String = "center",
    val mediaType: String = "text", // "text", "photo", "video", "reel"
    val mediaUrl: String = "",
    val mediaUrls: List<String> = emptyList(), // Multi-photo upload up to 10 photos
    val audience: String = "Public",
    val groupId: String = "",
    val groupName: String = "",
    val pageId: String = "",
    val pageName: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val likedByMap: Map<String, Boolean> = emptyMap(),
    val reactionsMap: Map<String, String> = emptyMap(), // userId -> "LIKE", "LOVE", etc.
    val isAuthorVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getAllMediaUrls(): List<String> {
        return if (mediaUrls.isNotEmpty()) mediaUrls else if (mediaUrl.isNotBlank()) listOf(mediaUrl) else emptyList()
    }

    fun getUserReaction(userId: String): ReactionType? {
        val rKey = reactionsMap[userId]
        if (rKey != null) {
            return ReactionType.fromKey(rKey)
        }
        if (likedByMap[userId] == true) {
            return ReactionType.LIKE
        }
        return null
    }

    fun getTopReactions(): List<ReactionType> {
        val allReactions = mutableListOf<ReactionType>()
        reactionsMap.values.forEach { key ->
            ReactionType.fromKey(key)?.let { allReactions.add(it) }
        }
        likedByMap.forEach { (userId, isLiked) ->
            if (isLiked && !reactionsMap.containsKey(userId)) {
                allReactions.add(ReactionType.LIKE)
            }
        }
        
        if (allReactions.isEmpty() && likesCount > 0) {
            return listOf(ReactionType.LIKE)
        }
        
        return allReactions.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatarUrl" to authorAvatarUrl,
            "content" to content,
            "backgroundStyle" to backgroundStyle,
            "fontSize" to fontSize,
            "textAlign" to textAlign,
            "mediaType" to mediaType,
            "mediaUrl" to mediaUrl,
            "mediaUrls" to mediaUrls,
            "audience" to audience,
            "groupId" to groupId,
            "groupName" to groupName,
            "pageId" to pageId,
            "pageName" to pageName,
            "likesCount" to likesCount,
            "commentsCount" to commentsCount,
            "sharesCount" to sharesCount,
            "likedByMap" to likedByMap,
            "reactionsMap" to reactionsMap,
            "isAuthorVerified" to isAuthorVerified,
            "createdAt" to createdAt
        )
    }
}
