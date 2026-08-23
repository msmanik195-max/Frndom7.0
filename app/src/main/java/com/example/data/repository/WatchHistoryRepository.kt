package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PostItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class WatchHistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("frndom_watch_history", Context.MODE_PRIVATE)

    private val _historyFlow = MutableStateFlow<List<PostItem>>(emptyList())
    val historyFlow: StateFlow<List<PostItem>> = _historyFlow.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val json = prefs.getString("watched_posts", null)
        if (!json.isNullOrBlank()) {
            val list = mutableListOf<PostItem>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        PostItem(
                            id = obj.optString("id", ""),
                            authorId = obj.optString("authorId", ""),
                            authorName = obj.optString("authorName", ""),
                            authorAvatarUrl = obj.optString("authorAvatarUrl", ""),
                            content = obj.optString("content", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            mediaType = obj.optString("mediaType", "video"),
                            likesCount = obj.optInt("likesCount", 0),
                            commentsCount = obj.optInt("commentsCount", 0),
                            sharesCount = obj.optInt("sharesCount", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
            _historyFlow.value = list
        }
    }

    fun recordWatch(post: PostItem) {
        if (post.id.isBlank()) return
        val current = _historyFlow.value.filter { it.id != post.id }
        val updated = (listOf(post) + current).take(20)
        _historyFlow.value = updated

        val arr = JSONArray()
        for (item in updated) {
            arr.put(JSONObject(item.toMap()))
        }
        prefs.edit().putString("watched_posts", arr.toString()).apply()
    }

    fun clearHistory() {
        _historyFlow.value = emptyList()
        prefs.edit().remove("watched_posts").apply()
    }
}
