package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val mediaType: String = "text", // "text", "image", "video", "audio", "call"
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deletedForUserIds: List<String> = emptyList(),
    val isDeletedForEveryone: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "senderId" to senderId,
            "senderName" to senderName,
            "receiverId" to receiverId,
            "text" to text,
            "mediaType" to mediaType,
            "mediaUrl" to mediaUrl,
            "timestamp" to timestamp,
            "deletedForUserIds" to deletedForUserIds,
            "isDeletedForEveryone" to isDeletedForEveryone
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ChatMessage {
            @Suppress("UNCHECKED_CAST")
            val deletedList = map["deletedForUserIds"] as? List<String> ?: emptyList()
            return ChatMessage(
                id = map["id"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                text = map["text"] as? String ?: "",
                mediaType = map["mediaType"] as? String ?: "text",
                mediaUrl = map["mediaUrl"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                deletedForUserIds = deletedList,
                isDeletedForEveryone = map["isDeletedForEveryone"] as? Boolean ?: false
            )
        }
    }
}

@IgnoreExtraProperties
data class ChatConversation(
    val peerId: String = "",
    val peerName: String = "",
    val peerAvatarUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

@IgnoreExtraProperties
data class AudioCallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatarUrl: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatarUrl: String = "",
    val status: String = "RINGING", // "RINGING", "CONNECTED", "ENDED", "REJECTED", "MISSED"
    val timestamp: Long = System.currentTimeMillis(),
    val startedAt: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "callId" to callId,
            "callerId" to callerId,
            "callerName" to callerName,
            "callerAvatarUrl" to callerAvatarUrl,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "receiverAvatarUrl" to receiverAvatarUrl,
            "status" to status,
            "timestamp" to timestamp,
            "startedAt" to startedAt
        )
    }
}
