package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.StoryItem
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

class StoryRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_stories_prefs", Context.MODE_PRIVATE)

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference("stories")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("StoryRepository", "FirebaseDatabase not ready: ${e.message}")
            null
        }
    }

    private val _storiesFlow = MutableStateFlow<List<StoryItem>>(getLocalStories())
    val storiesFlow = _storiesFlow.asStateFlow()

    init {
        listenToFirebaseStories()
    }

    private fun listenToFirebaseStories() {
        try {
            dbRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<StoryItem>()
                    val now = System.currentTimeMillis()
                    for (child in snapshot.children) {
                        try {
                            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                            val userId = child.child("userId").getValue(String::class.java) ?: ""
                            val userName = child.child("userName").getValue(String::class.java) ?: "User"
                            val userAvatar = child.child("userAvatar").getValue(String::class.java) ?: ""
                            val mediaUrl = child.child("mediaUrl").getValue(String::class.java) ?: ""
                            val mediaType = child.child("mediaType").getValue(String::class.java) ?: "image"
                            val caption = child.child("caption").getValue(String::class.java) ?: ""
                            val backgroundStyle = child.child("backgroundStyle").getValue(String::class.java) ?: "none"
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: now
                            val expiresAt = child.child("expiresAt").getValue(Long::class.java) ?: (now + 86400000)

                            val viewersMap = mutableMapOf<String, com.example.data.model.StoryViewerInfo>()
                            val viewersSnap = child.child("viewers")
                            for (vSnap in viewersSnap.children) {
                                val vUid = vSnap.child("userId").getValue(String::class.java) ?: vSnap.key ?: ""
                                val vName = vSnap.child("userName").getValue(String::class.java) ?: ""
                                val vAvatar = vSnap.child("userAvatar").getValue(String::class.java) ?: ""
                                val vReaction = vSnap.child("reaction").getValue(String::class.java) ?: ""
                                val vTime = vSnap.child("viewedAt").getValue(Long::class.java) ?: now
                                if (vUid.isNotBlank()) {
                                    viewersMap[vUid] = com.example.data.model.StoryViewerInfo(
                                        userId = vUid,
                                        userName = vName,
                                        userAvatar = vAvatar,
                                        reaction = vReaction,
                                        viewedAt = vTime
                                    )
                                }
                            }

                            if (expiresAt > now) {
                                list.add(
                                    StoryItem(
                                        id = id,
                                        userId = userId,
                                        userName = userName,
                                        userAvatar = userAvatar,
                                        mediaUrl = mediaUrl,
                                        mediaType = mediaType,
                                        caption = caption,
                                        backgroundStyle = backgroundStyle,
                                        createdAt = createdAt,
                                        expiresAt = expiresAt,
                                        viewers = viewersMap
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w("StoryRepository", "Error parsing story snapshot: ${e.message}")
                        }
                    }
                    val sorted = list.sortedByDescending { it.createdAt }
                    saveLocalStories(sorted)
                    _storiesFlow.value = sorted
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("StoryRepository", "Firebase stories cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error setting up stories listener: ${e.message}")
        }
    }

    fun getLocalStories(): List<StoryItem> {
        val json = prefs.getString("cached_stories", null) ?: return emptyList()
        val list = mutableListOf<StoryItem>()
        val now = System.currentTimeMillis()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val expiresAt = obj.optLong("expiresAt", now + 86400000)
                if (expiresAt > now) {
                    val viewersMap = mutableMapOf<String, com.example.data.model.StoryViewerInfo>()
                    val viewersJson = obj.optJSONObject("viewers")
                    if (viewersJson != null) {
                        val keys = viewersJson.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val vObj = viewersJson.optJSONObject(k)
                            if (vObj != null) {
                                viewersMap[k] = com.example.data.model.StoryViewerInfo(
                                    userId = vObj.optString("userId", k),
                                    userName = vObj.optString("userName", ""),
                                    userAvatar = vObj.optString("userAvatar", ""),
                                    reaction = vObj.optString("reaction", ""),
                                    viewedAt = vObj.optLong("viewedAt", now)
                                )
                            }
                        }
                    }

                    list.add(
                        StoryItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = obj.optString("userId", ""),
                            userName = obj.optString("userName", "User"),
                            userAvatar = obj.optString("userAvatar", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            mediaType = obj.optString("mediaType", "image"),
                            caption = obj.optString("caption", ""),
                            backgroundStyle = obj.optString("backgroundStyle", "none"),
                            createdAt = obj.optLong("createdAt", now),
                            expiresAt = expiresAt,
                            viewers = viewersMap
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error parsing stories: ${e.message}")
        }
        return list.sortedByDescending { it.createdAt }
    }

    fun saveLocalStories(stories: List<StoryItem>) {
        try {
            val arr = JSONArray()
            for (s in stories) {
                val viewersObj = JSONObject()
                for ((vId, vInfo) in s.viewers) {
                    val vObj = JSONObject().apply {
                        put("userId", vInfo.userId)
                        put("userName", vInfo.userName)
                        put("userAvatar", vInfo.userAvatar)
                        put("reaction", vInfo.reaction)
                        put("viewedAt", vInfo.viewedAt)
                    }
                    viewersObj.put(vId, vObj)
                }

                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("userId", s.userId)
                    put("userName", s.userName)
                    put("userAvatar", s.userAvatar)
                    put("mediaUrl", s.mediaUrl)
                    put("mediaType", s.mediaType)
                    put("caption", s.caption)
                    put("backgroundStyle", s.backgroundStyle)
                    put("createdAt", s.createdAt)
                    put("expiresAt", s.expiresAt)
                    put("viewers", viewersObj)
                }
                arr.put(obj)
            }
            prefs.edit().putString("cached_stories", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error saving cached stories: ${e.message}")
        }
    }

    fun createStory(story: StoryItem) {
        val newStory = if (story.id.isBlank()) story.copy(id = UUID.randomUUID().toString()) else story
        val current = _storiesFlow.value.toMutableList()
        current.add(0, newStory)
        _storiesFlow.value = current
        saveLocalStories(current)

        try {
            dbRef?.child(newStory.id)?.setValue(newStory.toMap())
        } catch (e: Exception) {
            Log.e("StoryRepository", "Firebase create story error: ${e.message}")
        }
    }

    fun markStoryViewed(storyId: String, viewer: com.example.data.model.StoryViewerInfo) {
        if (viewer.userId.isBlank() || storyId.isBlank()) return
        val current = _storiesFlow.value.map { story ->
            if (story.id == storyId) {
                val existingViewer = story.viewers[viewer.userId]
                val updatedViewer = if (existingViewer != null && existingViewer.reaction.isNotBlank() && viewer.reaction.isBlank()) {
                    existingViewer.copy(viewedAt = System.currentTimeMillis())
                } else {
                    viewer
                }
                val updatedViewers = story.viewers.toMutableMap().apply {
                    put(viewer.userId, updatedViewer)
                }
                story.copy(viewers = updatedViewers)
            } else {
                story
            }
        }
        _storiesFlow.value = current
        saveLocalStories(current)

        try {
            dbRef?.child(storyId)?.child("viewers")?.child(viewer.userId)?.setValue(viewer.toMap())
        } catch (e: Exception) {
            Log.e("StoryRepository", "Firebase mark viewer error: ${e.message}")
        }
    }

    fun reactToStory(storyId: String, viewer: com.example.data.model.StoryViewerInfo, reaction: String) {
        if (viewer.userId.isBlank() || storyId.isBlank()) return
        val updatedViewer = viewer.copy(reaction = reaction, viewedAt = System.currentTimeMillis())
        val current = _storiesFlow.value.map { story ->
            if (story.id == storyId) {
                val updatedViewers = story.viewers.toMutableMap().apply {
                    put(viewer.userId, updatedViewer)
                }
                story.copy(viewers = updatedViewers)
            } else {
                story
            }
        }
        _storiesFlow.value = current
        saveLocalStories(current)

        try {
            dbRef?.child(storyId)?.child("viewers")?.child(viewer.userId)?.setValue(updatedViewer.toMap())
        } catch (e: Exception) {
            Log.e("StoryRepository", "Firebase react story error: ${e.message}")
        }
    }

    fun refreshStories() {
        val cached = getLocalStories()
        if (cached.isNotEmpty()) {
            _storiesFlow.value = cached
        }
        try {
            dbRef?.get()?.addOnSuccessListener { snapshot ->
                val list = mutableListOf<StoryItem>()
                val now = System.currentTimeMillis()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        val userId = child.child("userId").getValue(String::class.java) ?: ""
                        val userName = child.child("userName").getValue(String::class.java) ?: "User"
                        val userAvatar = child.child("userAvatar").getValue(String::class.java) ?: ""
                        val mediaUrl = child.child("mediaUrl").getValue(String::class.java) ?: ""
                        val mediaType = child.child("mediaType").getValue(String::class.java) ?: "image"
                        val caption = child.child("caption").getValue(String::class.java) ?: ""
                        val backgroundStyle = child.child("backgroundStyle").getValue(String::class.java) ?: "none"
                        val createdAt = child.child("createdAt").getValue(Long::class.java) ?: now
                        val expiresAt = child.child("expiresAt").getValue(Long::class.java) ?: (now + 86400000)

                        val viewersMap = mutableMapOf<String, com.example.data.model.StoryViewerInfo>()
                        val viewersSnap = child.child("viewers")
                        for (vSnap in viewersSnap.children) {
                            val vUid = vSnap.child("userId").getValue(String::class.java) ?: vSnap.key ?: ""
                            val vName = vSnap.child("userName").getValue(String::class.java) ?: ""
                            val vAvatar = vSnap.child("userAvatar").getValue(String::class.java) ?: ""
                            val vReaction = vSnap.child("reaction").getValue(String::class.java) ?: ""
                            val vTime = vSnap.child("viewedAt").getValue(Long::class.java) ?: now
                            if (vUid.isNotBlank()) {
                                viewersMap[vUid] = com.example.data.model.StoryViewerInfo(
                                    userId = vUid,
                                    userName = vName,
                                    userAvatar = vAvatar,
                                    reaction = vReaction,
                                    viewedAt = vTime
                                )
                            }
                        }

                        if (expiresAt > now) {
                            list.add(
                                StoryItem(
                                    id = id,
                                    userId = userId,
                                    userName = userName,
                                    userAvatar = userAvatar,
                                    mediaUrl = mediaUrl,
                                    mediaType = mediaType,
                                    caption = caption,
                                    backgroundStyle = backgroundStyle,
                                    createdAt = createdAt,
                                    expiresAt = expiresAt,
                                    viewers = viewersMap
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("StoryRepository", "Error parsing refreshed story snapshot: ${e.message}")
                    }
                }
                val sorted = list.sortedByDescending { it.createdAt }
                saveLocalStories(sorted)
                _storiesFlow.value = sorted
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error refreshing stories: ${e.message}")
        }
    }

    fun updateStoriesUserAvatar(userId: String, newAvatar: String, newName: String = "") {
        if (userId.isBlank()) return
        val current = _storiesFlow.value.map { story ->
            if (story.userId == userId) {
                story.copy(
                    userAvatar = if (newAvatar.isNotBlank()) newAvatar else story.userAvatar,
                    userName = if (newName.isNotBlank()) newName else story.userName
                )
            } else {
                story
            }
        }
        _storiesFlow.value = current
        saveLocalStories(current)

        try {
            current.filter { it.userId == userId }.forEach { story ->
                if (newAvatar.isNotBlank()) {
                    dbRef?.child(story.id)?.child("userAvatar")?.setValue(newAvatar)
                }
                if (newName.isNotBlank()) {
                    dbRef?.child(story.id)?.child("userName")?.setValue(newName)
                }
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Firebase update user avatar error: ${e.message}")
        }
    }

    fun deleteStory(storyId: String) {
        if (storyId.isBlank()) return
        val current = _storiesFlow.value.filter { it.id != storyId }
        _storiesFlow.value = current
        saveLocalStories(current)
        try {
            dbRef?.child(storyId)?.removeValue()
        } catch (e: Exception) {
            Log.e("StoryRepository", "Firebase delete story error: ${e.message}")
        }
    }
}
