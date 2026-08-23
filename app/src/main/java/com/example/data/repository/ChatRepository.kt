package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AudioCallSession
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class ChatRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_local_prefs", Context.MODE_PRIVATE)

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().reference
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("ChatRepository", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    private fun getConversationId(userA: String, userB: String): String {
        return if (userA < userB) "${userA}_$userB" else "${userB}_$userA"
    }

    private fun isDeletedLocallyForUser(userId: String, messageId: String): Boolean {
        return prefs.getBoolean("deleted_${userId}_$messageId", false)
    }

    private fun markDeletedLocally(userId: String, messageId: String) {
        prefs.edit().putBoolean("deleted_${userId}_$messageId", true).apply()
    }

    fun getConversationsFlow(currentUserId: String): Flow<List<ChatConversation>> = callbackFlow {
        if (currentUserId.isBlank() || dbRef == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val conversationsRef = dbRef?.child("user_conversations")?.child(currentUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatConversation>()
                for (child in snapshot.children) {
                    val peerId = child.key ?: ""
                    val peerName = child.child("peerName").getValue(String::class.java) ?: "User"
                    val peerAvatarUrl = child.child("peerAvatarUrl").getValue(String::class.java) ?: ""
                    val lastMsg = child.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                    list.add(
                        ChatConversation(
                            peerId = peerId,
                            peerName = peerName,
                            peerAvatarUrl = peerAvatarUrl,
                            lastMessage = lastMsg,
                            lastMessageTime = lastTime
                        )
                    )
                }
                trySend(list.sortedByDescending { it.lastMessageTime })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        conversationsRef?.addValueEventListener(listener)
        awaitClose {
            conversationsRef?.removeEventListener(listener)
        }
    }

    fun getMessagesFlow(currentUserId: String, peerId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (currentUserId.isBlank() || peerId.isBlank() || dbRef == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val convId = getConversationId(currentUserId, peerId)
        val messagesRef = dbRef?.child("messages")?.child(convId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val map = child.value as? Map<String, Any?>
                    val msg = if (map != null) {
                        ChatMessage.fromMap(map)
                    } else {
                        child.getValue(ChatMessage::class.java)
                    }

                    if (msg != null) {
                        // Check if deleted for everyone or deleted for this user
                        val isDeletedForMe = msg.deletedForUserIds.contains(currentUserId) ||
                                isDeletedLocallyForUser(currentUserId, msg.id)
                        if (!msg.isDeletedForEveryone && !isDeletedForMe) {
                            list.add(msg)
                        }
                    }
                }
                trySend(list.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        messagesRef?.addValueEventListener(listener)
        awaitClose {
            messagesRef?.removeEventListener(listener)
        }
    }

    fun getTotalMessagesCountFlow(): Flow<Int> = callbackFlow {
        val cachedCount = prefs.getInt("total_messages_count_cache", 0)
        trySend(cachedCount)

        if (dbRef == null) {
            close()
            return@callbackFlow
        }

        val allMessagesRef = dbRef?.child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                for (conv in snapshot.children) {
                    total += conv.childrenCount.toInt()
                }
                prefs.edit().putInt("total_messages_count_cache", total).apply()
                trySend(total)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(prefs.getInt("total_messages_count_cache", 0))
            }
        }

        allMessagesRef?.addValueEventListener(listener)
        awaitClose {
            allMessagesRef?.removeEventListener(listener)
        }
    }

    fun sendMessage(
        senderId: String,
        senderName: String,
        senderAvatar: String,
        receiverId: String,
        receiverName: String,
        receiverAvatar: String,
        text: String,
        mediaType: String = "text",
        mediaUrl: String = "",
        fileName: String = "",
        fileSize: Long = 0L
    ) {
        if (senderId.isBlank() || receiverId.isBlank() || dbRef == null) return

        val convId = getConversationId(senderId, receiverId)
        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val msg = ChatMessage(
            id = msgId,
            senderId = senderId,
            senderName = senderName,
            receiverId = receiverId,
            text = text,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            fileName = fileName,
            fileSize = fileSize,
            timestamp = timestamp
        )

        // 1. Save to messages node
        dbRef?.child("messages")?.child(convId)?.child(msgId)?.setValue(msg.toMap())

        val displaySnippet = when (mediaType) {
            "image" -> "📷 Sent a photo"
            "video" -> "🎥 Sent a video"
            "audio" -> "🎤 Sent a voice message"
            "apk" -> "📦 Sent an APK: ${fileName.ifBlank { "app.apk" }}"
            "file" -> "📎 Sent a file: ${fileName.ifBlank { "document" }}"
            "call" -> "📞 $text"
            else -> text
        }

        // 2. Update conversation for sender
        dbRef?.child("user_conversations")?.child(senderId)?.child(receiverId)?.setValue(
            mapOf(
                "peerName" to receiverName,
                "peerAvatarUrl" to receiverAvatar,
                "lastMessage" to displaySnippet,
                "lastMessageTime" to timestamp
            )
        )

        // 3. Update conversation for receiver
        dbRef?.child("user_conversations")?.child(receiverId)?.child(senderId)?.setValue(
            mapOf(
                "peerName" to senderName,
                "peerAvatarUrl" to senderAvatar,
                "lastMessage" to displaySnippet,
                "lastMessageTime" to timestamp
            )
        )
    }

    /**
     * Delete message for current user only ("Delete for me")
     */
    fun deleteMessageForMe(currentUserId: String, peerId: String, messageId: String) {
        markDeletedLocally(currentUserId, messageId)
        if (dbRef == null) return
        val convId = getConversationId(currentUserId, peerId)
        val msgRef = dbRef?.child("messages")?.child(convId)?.child(messageId)
        msgRef?.child("deletedForUserIds")?.get()?.addOnSuccessListener { snap ->
            val currentList = (snap.value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (!currentList.contains(currentUserId)) {
                val updated = currentList + currentUserId
                msgRef.child("deletedForUserIds").setValue(updated)
            }
        }
    }

    /**
     * Delete message for everyone ("Delete for Everyone" - permanently removed from database)
     */
    fun deleteMessageForEveryone(currentUserId: String, peerId: String, messageId: String) {
        if (dbRef == null) return
        val convId = getConversationId(currentUserId, peerId)
        // Permanently remove from database as requested
        dbRef?.child("messages")?.child(convId)?.child(messageId)?.removeValue()
    }

    // -------------------------------------------------------------
    // REAL-TIME AUDIO CALLING METHODS
    // -------------------------------------------------------------
    fun initiateAudioCall(
        callerId: String,
        callerName: String,
        callerAvatar: String,
        receiverId: String,
        receiverName: String,
        receiverAvatar: String
    ): String {
        val callId = "call_${UUID.randomUUID()}"
        val session = AudioCallSession(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            callerAvatarUrl = callerAvatar,
            receiverId = receiverId,
            receiverName = receiverName,
            receiverAvatarUrl = receiverAvatar,
            status = "RINGING",
            timestamp = System.currentTimeMillis(),
            startedAt = 0L
        )

        dbRef?.child("audio_calls")?.child(callId)?.setValue(session.toMap())
        // Set user active incoming call indicator
        dbRef?.child("active_calls")?.child(receiverId)?.setValue(callId)
        dbRef?.child("active_calls")?.child(callerId)?.setValue(callId)

        return callId
    }

    fun listenToCallSession(callId: String): Flow<AudioCallSession?> = callbackFlow {
        if (callId.isBlank() || dbRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val callRef = dbRef?.child("audio_calls")?.child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                val map = snapshot.value as? Map<String, Any?>
                if (map != null) {
                    val session = AudioCallSession(
                        callId = map["callId"] as? String ?: callId,
                        callerId = map["callerId"] as? String ?: "",
                        callerName = map["callerName"] as? String ?: "",
                        callerAvatarUrl = map["callerAvatarUrl"] as? String ?: "",
                        receiverId = map["receiverId"] as? String ?: "",
                        receiverName = map["receiverName"] as? String ?: "",
                        receiverAvatarUrl = map["receiverAvatarUrl"] as? String ?: "",
                        status = map["status"] as? String ?: "RINGING",
                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0L
                    )
                    trySend(session)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        callRef?.addValueEventListener(listener)
        awaitClose {
            callRef?.removeEventListener(listener)
        }
    }

    fun listenForIncomingCalls(userId: String): Flow<AudioCallSession?> = callbackFlow {
        if (userId.isBlank() || dbRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val activeCallRef = dbRef?.child("active_calls")?.child(userId)
        var currentCallListener: ValueEventListener? = null
        var currentCallRef: DatabaseReference? = null

        val activeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val callId = snapshot.getValue(String::class.java)
                if (callId.isNullOrBlank()) {
                    trySend(null)
                    return
                }

                currentCallListener?.let { currentCallRef?.removeEventListener(it) }
                currentCallRef = dbRef?.child("audio_calls")?.child(callId)
                currentCallListener = object : ValueEventListener {
                    override fun onDataChange(callSnap: DataSnapshot) {
                        val map = callSnap.value as? Map<String, Any?>
                        if (map != null) {
                            val status = map["status"] as? String ?: ""
                            val receiverId = map["receiverId"] as? String ?: ""
                            if (status == "RINGING" && receiverId == userId) {
                                val session = AudioCallSession(
                                    callId = map["callId"] as? String ?: callId,
                                    callerId = map["callerId"] as? String ?: "",
                                    callerName = map["callerName"] as? String ?: "",
                                    callerAvatarUrl = map["callerAvatarUrl"] as? String ?: "",
                                    receiverId = receiverId,
                                    receiverName = map["receiverName"] as? String ?: "",
                                    receiverAvatarUrl = map["receiverAvatarUrl"] as? String ?: "",
                                    status = status,
                                    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                )
                                trySend(session)
                            } else {
                                trySend(null)
                            }
                        } else {
                            trySend(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        trySend(null)
                    }
                }
                currentCallRef?.addValueEventListener(currentCallListener!!)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        activeCallRef?.addValueEventListener(activeListener)
        awaitClose {
            activeCallRef?.removeEventListener(activeListener)
            currentCallListener?.let { currentCallRef?.removeEventListener(it) }
        }
    }

    fun acceptAudioCall(callId: String) {
        if (dbRef == null || callId.isBlank()) return
        val updates = mapOf(
            "status" to "CONNECTED",
            "startedAt" to System.currentTimeMillis()
        )
        dbRef?.child("audio_calls")?.child(callId)?.updateChildren(updates)
    }

    fun endAudioCall(session: AudioCallSession) {
        if (dbRef == null || session.callId.isBlank()) return
        dbRef?.child("audio_calls")?.child(session.callId)?.child("status")?.setValue("ENDED")
        dbRef?.child("active_calls")?.child(session.callerId)?.removeValue()
        dbRef?.child("active_calls")?.child(session.receiverId)?.removeValue()

        // Log call ending in conversation
        val durationSec = if (session.startedAt > 0) {
            (System.currentTimeMillis() - session.startedAt) / 1000
        } else {
            0
        }
        val durationFormatted = String.format("%02d:%02d", durationSec / 60, durationSec % 60)
        val textMsg = if (durationSec > 0) "Audio call ended ($durationFormatted)" else "Missed audio call"

        sendMessage(
            senderId = session.callerId,
            senderName = session.callerName,
            senderAvatar = session.callerAvatarUrl,
            receiverId = session.receiverId,
            receiverName = session.receiverName,
            receiverAvatar = session.receiverAvatarUrl,
            text = textMsg,
            mediaType = "call"
        )
    }

    fun rejectAudioCall(session: AudioCallSession) {
        if (dbRef == null || session.callId.isBlank()) return
        dbRef?.child("audio_calls")?.child(session.callId)?.child("status")?.setValue("REJECTED")
        dbRef?.child("active_calls")?.child(session.callerId)?.removeValue()
        dbRef?.child("active_calls")?.child(session.receiverId)?.removeValue()

        sendMessage(
            senderId = session.callerId,
            senderName = session.callerName,
            senderAvatar = session.callerAvatarUrl,
            receiverId = session.receiverId,
            receiverName = session.receiverName,
            receiverAvatar = session.receiverAvatarUrl,
            text = "Declined audio call",
            mediaType = "call"
        )
    }
}
