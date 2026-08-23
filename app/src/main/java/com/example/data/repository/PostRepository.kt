package com.example.data.repository

import kotlinx.coroutines.channels.awaitClose

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import android.content.Context
import android.util.Log
import com.example.data.model.PostItem
import com.example.data.model.ReactionType
import com.example.data.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PostRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_posts_prefs", Context.MODE_PRIVATE)
    private val notificationRepository = NotificationRepository(context)

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference("posts")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("PostRepository", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    private val _postsFlow = MutableStateFlow<List<PostItem>>(getLocalPosts())
    val postsFlow = _postsFlow.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        listenToFirebasePosts()
    }

    private fun listenToFirebasePosts() {
        try {
            dbRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PostItem>()
                    for (child in snapshot.children) {
                        val post = child.getValue(PostItem::class.java)
                        if (post != null) {
                            list.add(post)
                        }
                    }
                    val sortedList = list.sortedByDescending { it.createdAt }
                    saveLocalPosts(sortedList)
                    _postsFlow.value = sortedList
                    _isRefreshing.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("PostRepository", "Firebase posts cancelled: ${error.message}")
                    _isRefreshing.value = false
                }
            })
        } catch (e: Exception) {
            Log.e("PostRepository", "Error setting up posts listener: ${e.message}")
        }
    }

    fun refreshPosts() {
        _isRefreshing.value = true
        try {
            dbRef?.get()?.addOnSuccessListener { snapshot ->
                val list = mutableListOf<PostItem>()
                for (child in snapshot.children) {
                    val post = child.getValue(PostItem::class.java)
                    if (post != null) {
                        list.add(post)
                    }
                }
                val sortedList = list.sortedByDescending { it.createdAt }
                
                // Add a small artificial delay to make the shimmer effect visible and feel like a real refresh
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(1200)
                    saveLocalPosts(sortedList)
                    _postsFlow.value = sortedList
                    _isRefreshing.value = false
                }
            }?.addOnFailureListener {
                _isRefreshing.value = false
            } ?: run {
                _isRefreshing.value = false
            }
        } catch (e: Exception) {
            _isRefreshing.value = false
        }
    }

    fun getLocalPosts(): List<PostItem> {
        val json = prefs.getString("cached_posts", null) ?: return emptyList()
        val list = mutableListOf<PostItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val likedByMap = mutableMapOf<String, Boolean>()
                if (obj.has("likedByMap")) {
                    val likedObj = obj.getJSONObject("likedByMap")
                    val keys = likedObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        likedByMap[k] = likedObj.getBoolean(k)
                    }
                }

                val reactionsMap = mutableMapOf<String, String>()
                if (obj.has("reactionsMap")) {
                    val reactObj = obj.getJSONObject("reactionsMap")
                    val keys = reactObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        reactionsMap[k] = reactObj.getString(k)
                    }
                }

                val mediaUrlsList = mutableListOf<String>()
                if (obj.has("mediaUrls")) {
                    val mArr = obj.getJSONArray("mediaUrls")
                    for (m in 0 until mArr.length()) {
                        mediaUrlsList.add(mArr.getString(m))
                    }
                }

                list.add(
                    PostItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        authorId = obj.optString("authorId", ""),
                        authorName = obj.optString("authorName", "User"),
                        authorAvatarUrl = obj.optString("authorAvatarUrl", ""),
                        content = obj.optString("content", ""),
                        backgroundStyle = obj.optString("backgroundStyle", "none"),
                        fontSize = obj.optInt("fontSize", 24),
                        textAlign = obj.optString("textAlign", "center"),
                        mediaType = obj.optString("mediaType", "text"),
                        mediaUrl = obj.optString("mediaUrl", ""),
                        mediaUrls = mediaUrlsList,
                        audience = obj.optString("audience", "Public"),
                        groupId = obj.optString("groupId", ""),
                        groupName = obj.optString("groupName", ""),
                        pageId = obj.optString("pageId", ""),
                        pageName = obj.optString("pageName", ""),
                        likesCount = obj.optInt("likesCount", 0),
                        commentsCount = obj.optInt("commentsCount", 0),
                        sharesCount = obj.optInt("sharesCount", 0),
                        likedByMap = likedByMap,
                        reactionsMap = reactionsMap,
                        isAuthorVerified = obj.optBoolean("isAuthorVerified", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error parsing cached posts: ${e.message}")
        }
        return list.sortedByDescending { it.createdAt }
    }

    fun saveLocalPosts(posts: List<PostItem>) {
        try {
            val arr = JSONArray()
            for (p in posts) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("authorId", p.authorId)
                    put("authorName", p.authorName)
                    put("authorAvatarUrl", p.authorAvatarUrl)
                    put("content", p.content)
                    put("backgroundStyle", p.backgroundStyle)
                    put("fontSize", p.fontSize)
                    put("textAlign", p.textAlign)
                    put("mediaType", p.mediaType)
                    put("mediaUrl", p.mediaUrl)
                    val mArr = JSONArray()
                    p.mediaUrls.forEach { mArr.put(it) }
                    put("mediaUrls", mArr)
                    put("audience", p.audience)
                    put("groupId", p.groupId)
                    put("groupName", p.groupName)
                    put("pageId", p.pageId)
                    put("pageName", p.pageName)
                    put("likesCount", p.likesCount)
                    put("commentsCount", p.commentsCount)
                    put("sharesCount", p.sharesCount)
                    put("createdAt", p.createdAt)
                    val likedObj = JSONObject()
                    p.likedByMap.forEach { (k, v) -> likedObj.put(k, v) }
                    put("likedByMap", likedObj)
                    val reactObj = JSONObject()
                    p.reactionsMap.forEach { (k, v) -> reactObj.put(k, v) }
                    put("reactionsMap", reactObj)
                    put("isAuthorVerified", p.isAuthorVerified)
                }
                arr.put(obj)
            }
            prefs.edit().putString("cached_posts", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error saving cached posts: ${e.message}")
        }
    }

    fun createPost(post: PostItem) {
        val newPost = if (post.id.isBlank()) post.copy(id = UUID.randomUUID().toString()) else post
        val current = _postsFlow.value.toMutableList()
        current.add(0, newPost)
        _postsFlow.value = current
        saveLocalPosts(current)

        try {
            dbRef?.child(newPost.id)?.setValue(newPost.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase create post error: ${e.message}")
        }
    }

    fun setReaction(postId: String, userId: String, reaction: ReactionType?) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val prevReaction = post.getUserReaction(userId)
            val updatedReactionsMap = post.reactionsMap.toMutableMap()
            val updatedLikedByMap = post.likedByMap.toMutableMap()

            var newLikesCount = post.likesCount

            if (reaction == null) {
                // Remove reaction
                if (prevReaction != null) {
                    newLikesCount = (newLikesCount - 1).coerceAtLeast(0)
                }
                updatedReactionsMap.remove(userId)
                updatedLikedByMap.remove(userId)
            } else {
                // Add or update reaction
                if (prevReaction == null) {
                    newLikesCount += 1
                }
                updatedReactionsMap[userId] = reaction.key
                updatedLikedByMap[userId] = true
            }

            val updatedPost = post.copy(
                likesCount = newLikesCount,
                likedByMap = updatedLikedByMap,
                reactionsMap = updatedReactionsMap
            )
            current[index] = updatedPost
            _postsFlow.value = current
            saveLocalPosts(current)

            // Trigger notification if reaction added
            if (reaction != null && post.authorId.isNotBlank()) {
                val userRepo = UserRepository(context)
                val senderProfile: UserProfile? = userRepo.getLocalUserProfile(userId)
                val senderName = if (senderProfile != null && senderProfile.fullName.isNotBlank()) {
                    senderProfile.fullName
                } else if (senderProfile != null && (senderProfile.firstName.isNotBlank() || senderProfile.lastName.isNotBlank())) {
                    "${senderProfile.firstName} ${senderProfile.lastName}".trim()
                } else {
                    "Someone"
                }
                val senderAvatar = senderProfile?.profilePictureUrl ?: ""
                val postType = if (post.mediaType == "reel" || post.mediaType == "video") "reel" else "post"

                notificationRepository.addNotification(
                    com.example.data.model.NotificationItem(
                        recipientId = post.authorId,
                        senderId = userId,
                        senderName = senderName,
                        senderAvatarUrl = senderAvatar,
                        postId = post.id,
                        type = "like",
                        content = "liked your $postType.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            try {
                dbRef?.child(postId)?.child("likesCount")?.setValue(newLikesCount)
                dbRef?.child(postId)?.child("reactionsMap")?.child(userId)?.setValue(reaction?.key)
                dbRef?.child(postId)?.child("likedByMap")?.child(userId)?.setValue(if (reaction != null) true else null)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase setReaction error: ${e.message}")
            }
        }
    }

    fun toggleLike(postId: String, userId: String) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val hasReacted = post.getUserReaction(userId) != null
            if (hasReacted) {
                setReaction(postId, userId, null)
            } else {
                setReaction(postId, userId, ReactionType.LIKE)
            }
        }
    }

    fun addComment(postId: String) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val updated = post.copy(commentsCount = post.commentsCount + 1)
            current[index] = updated
            _postsFlow.value = current
            saveLocalPosts(current)

            try {
                dbRef?.child(postId)?.child("commentsCount")?.setValue(updated.commentsCount)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase comment error: ${e.message}")
            }
        }
    }

    fun incrementShare(postId: String) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val updated = post.copy(sharesCount = post.sharesCount + 1)
            current[index] = updated
            _postsFlow.value = current
            saveLocalPosts(current)

            try {
                dbRef?.child(postId)?.child("sharesCount")?.setValue(updated.sharesCount)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase share error: ${e.message}")
            }
        }

    }

    fun addDetailedComment(comment: com.example.data.model.CommentItem) {
        // 1. Immediately save to local persistent cache
        saveLocalComment(comment)

        // 2. Increment post comments count
        addComment(comment.postId)

        // 3. Trigger notification for post author
        val post = _postsFlow.value.firstOrNull { it.id == comment.postId }
        if (post != null && post.authorId.isNotBlank()) {
            val postType = if (post.mediaType == "reel" || post.mediaType == "video") "reel" else "post"
            val commentSnippet = if (comment.text.isNotBlank()) ": \"${comment.text.take(30)}\"" else ""
            notificationRepository.addNotification(
                com.example.data.model.NotificationItem(
                    recipientId = post.authorId,
                    senderId = comment.authorId,
                    senderName = comment.authorName,
                    senderAvatarUrl = comment.authorAvatarUrl,
                    postId = post.id,
                    type = "comment",
                    content = "commented on your $postType$commentSnippet",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // 4. Save to Firebase Database
        try {
            val commentsRef = FirebaseDatabase.getInstance().getReference("post_comments").child(comment.postId)
            commentsRef.child(comment.id).setValue(comment.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase addDetailedComment error: ${e.message}")
        }
    }

    fun getLocalComments(postId: String): List<com.example.data.model.CommentItem> {
        val json = prefs.getString("comments_$postId", null) ?: return emptyList()
        val list = mutableListOf<com.example.data.model.CommentItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val likedByMap = mutableMapOf<String, Boolean>()
                if (obj.has("likedByMap")) {
                    val likedObj = obj.getJSONObject("likedByMap")
                    val keys = likedObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        likedByMap[k] = likedObj.getBoolean(k)
                    }
                }
                list.add(
                    com.example.data.model.CommentItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        postId = obj.optString("postId", postId),
                        authorId = obj.optString("authorId", ""),
                        authorName = obj.optString("authorName", "User"),
                        authorAvatarUrl = obj.optString("authorAvatarUrl", ""),
                        text = obj.optString("text", ""),
                        emojiSticker = obj.optString("emojiSticker", ""),
                        parentCommentId = obj.optString("parentCommentId", ""),
                        replyToAuthorName = obj.optString("replyToAuthorName", ""),
                        likesCount = obj.optInt("likesCount", 0),
                        likedByMap = likedByMap,
                        isAuthorVerified = obj.optBoolean("isAuthorVerified", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error parsing cached comments for $postId: ${e.message}")
        }
        return list.sortedBy { it.createdAt }
    }

    fun saveLocalComment(comment: com.example.data.model.CommentItem) {
        val current = getLocalComments(comment.postId).toMutableList()
        val index = current.indexOfFirst { it.id == comment.id }
        if (index >= 0) {
            current[index] = comment
        } else {
            current.add(comment)
        }
        saveAllLocalComments(comment.postId, current)
    }

    fun saveAllLocalComments(postId: String, comments: List<com.example.data.model.CommentItem>) {
        try {
            val arr = JSONArray()
            for (c in comments) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("postId", c.postId)
                    put("authorId", c.authorId)
                    put("authorName", c.authorName)
                    put("authorAvatarUrl", c.authorAvatarUrl)
                    put("text", c.text)
                    put("emojiSticker", c.emojiSticker)
                    put("parentCommentId", c.parentCommentId)
                    put("replyToAuthorName", c.replyToAuthorName)
                    put("likesCount", c.likesCount)
                    put("createdAt", c.createdAt)
                    put("isAuthorVerified", c.isAuthorVerified)
                    val likedObj = JSONObject()
                    c.likedByMap.forEach { (k, v) -> likedObj.put(k, v) }
                    put("likedByMap", likedObj)
                }
                arr.put(obj)
            }
            prefs.edit().putString("comments_$postId", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error saving cached comments for $postId: ${e.message}")
        }
    }

    fun getCommentsFlow(postId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.CommentItem>> = kotlinx.coroutines.flow.callbackFlow {
        // Send initial local cached comments immediately
        val initialList = getLocalComments(postId)
        trySend(initialList)

        val commentsRef = try {
            FirebaseDatabase.getInstance().getReference("post_comments").child(postId)
        } catch (e: Exception) {
            null
        }

        if (commentsRef == null) {
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fbList = mutableListOf<com.example.data.model.CommentItem>()
                for (child in snapshot.children) {
                    child.getValue(com.example.data.model.CommentItem::class.java)?.let { fbList.add(it) }
                }
                // Merge Firebase comments with local cached comments
                val mergedMap = LinkedHashMap<String, com.example.data.model.CommentItem>()
                getLocalComments(postId).forEach { mergedMap[it.id] = it }
                fbList.forEach { mergedMap[it.id] = it }
                val mergedList = mergedMap.values.toList().sortedBy { it.createdAt }

                saveAllLocalComments(postId, mergedList)
                trySend(mergedList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep local cached comments on error
                trySend(getLocalComments(postId))
            }
        }

        commentsRef.addValueEventListener(listener)
        awaitClose { commentsRef.removeEventListener(listener) }
    }

    // ==========================================
    // Post Editing, Deleting, Saving & Reporting
    // ==========================================

    fun updatePost(updatedPost: PostItem) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == updatedPost.id }
        if (index >= 0) {
            current[index] = updatedPost
        } else {
            current.add(0, updatedPost)
        }
        _postsFlow.value = current
        saveLocalPosts(current)

        try {
            dbRef?.child(updatedPost.id)?.setValue(updatedPost.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase updatePost error: ${e.message}")
        }
    }

    fun deletePost(postId: String) {
        val current = _postsFlow.value.toMutableList()
        current.removeAll { it.id == postId }
        _postsFlow.value = current
        saveLocalPosts(current)

        try {
            dbRef?.child(postId)?.removeValue()
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase deletePost error: ${e.message}")
        }
    }

    fun isPostSaved(userId: String, postId: String): Boolean {
        if (userId.isBlank() || postId.isBlank()) return false
        val savedSet = prefs.getStringSet("saved_posts_$userId", emptySet()) ?: emptySet()
        return savedSet.contains(postId)
    }

    fun toggleSavePost(userId: String, postId: String): Boolean {
        if (userId.isBlank() || postId.isBlank()) return false
        val savedSet = prefs.getStringSet("saved_posts_$userId", emptySet())?.toMutableSet() ?: mutableSetOf()
        val isNowSaved = if (savedSet.contains(postId)) {
            savedSet.remove(postId)
            false
        } else {
            savedSet.add(postId)
            true
        }
        prefs.edit().putStringSet("saved_posts_$userId", savedSet).apply()

        try {
            val savedRef = FirebaseDatabase.getInstance().getReference("user_saved_posts").child(userId).child(postId)
            if (isNowSaved) {
                savedRef.setValue(System.currentTimeMillis())
            } else {
                savedRef.removeValue()
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase toggleSavePost error: ${e.message}")
        }
        return isNowSaved
    }

    fun getSavedPosts(userId: String): List<PostItem> {
        if (userId.isBlank()) return emptyList()
        val savedSet = prefs.getStringSet("saved_posts_$userId", emptySet()) ?: emptySet()
        return _postsFlow.value.filter { savedSet.contains(it.id) }
    }

    fun reportPost(postId: String, reporterId: String, reason: String, details: String = "") {
        try {
            val reportMap = mapOf(
                "id" to UUID.randomUUID().toString(),
                "postId" to postId,
                "reporterId" to reporterId,
                "reason" to reason,
                "details" to details,
                "timestamp" to System.currentTimeMillis()
            )
            FirebaseDatabase.getInstance().getReference("post_reports").push().setValue(reportMap)
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase reportPost error: ${e.message}")
        }
    }
}
