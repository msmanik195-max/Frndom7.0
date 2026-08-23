package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CommentItem(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val emojiSticker: String = "",
    val parentCommentId: String = "",
    val replyToAuthorName: String = "",
    val likesCount: Int = 0,
    val likedByMap: Map<String, Boolean> = emptyMap(),
    val isAuthorVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "postId" to postId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatarUrl" to authorAvatarUrl,
            "text" to text,
            "emojiSticker" to emojiSticker,
            "parentCommentId" to parentCommentId,
            "replyToAuthorName" to replyToAuthorName,
            "likesCount" to likesCount,
            "likedByMap" to likedByMap,
            "isAuthorVerified" to isAuthorVerified,
            "createdAt" to createdAt
        )
    }
}
