package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SearchHistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("frndom_search_history", Context.MODE_PRIVATE)

    private val _recentSearchesFlow = MutableStateFlow<List<String>>(emptyList())
    val recentSearchesFlow: StateFlow<List<String>> = _recentSearchesFlow.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val json = prefs.getString("recent_keywords", null)
        if (!json.isNullOrBlank()) {
            val list = mutableListOf<String>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (_: Exception) {}
            _recentSearchesFlow.value = list
        } else {
            _recentSearchesFlow.value = emptyList()
        }
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _recentSearchesFlow.value.filter { !it.equals(trimmed, ignoreCase = true) }
        val updated = (listOf(trimmed) + current).take(10)
        _recentSearchesFlow.value = updated

        val arr = JSONArray()
        for (item in updated) {
            arr.put(item)
        }
        prefs.edit().putString("recent_keywords", arr.toString()).apply()
    }

    fun removeSearchQuery(query: String) {
        val updated = _recentSearchesFlow.value.filter { it != query }
        _recentSearchesFlow.value = updated

        val arr = JSONArray()
        for (item in updated) {
            arr.put(item)
        }
        prefs.edit().putString("recent_keywords", arr.toString()).apply()
    }

    fun clearAll() {
        _recentSearchesFlow.value = emptyList()
        prefs.edit().remove("recent_keywords").apply()
    }
}
