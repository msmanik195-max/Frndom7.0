package com.example.data.model

data class NotificationItem(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "User",
    val senderAvatarUrl: String = "",
    val postId: String = "",
    val type: String = "like", // "like", "comment", "follow"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "recipientId" to recipientId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderAvatarUrl" to senderAvatarUrl,
        "postId" to postId,
        "type" to type,
        "content" to content,
        "timestamp" to timestamp,
        "isRead" to isRead
    )
}
