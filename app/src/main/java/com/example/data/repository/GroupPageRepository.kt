package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.GroupItem
import com.example.data.model.PageItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class GroupPageRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("frndom_groups_pages", Context.MODE_PRIVATE)

    private val dbRef: DatabaseReference? by lazy {
        try {
            FirebaseDatabase.getInstance().reference
        } catch (e: Throwable) {
            Log.w("GroupPageRepository", "Firebase Database unavailable: ${e.message}")
            null
        }
    }

    private val _pagesFlow = MutableStateFlow<List<PageItem>>(emptyList())
    val pagesFlow: StateFlow<List<PageItem>> = _pagesFlow.asStateFlow()

    private val _groupsFlow = MutableStateFlow<List<GroupItem>>(emptyList())
    val groupsFlow: StateFlow<List<GroupItem>> = _groupsFlow.asStateFlow()

    init {
        loadLocalData()
        listenToFirebase()
    }

    private fun loadLocalData() {
        val pagesJson = prefs.getString("saved_pages", null)
        val defaultPages = if (!pagesJson.isNullOrBlank()) {
            parsePages(pagesJson).filterNot {
                it.id in listOf("page_tech", "page_travel", "page_fitness")
            }
        } else {
            emptyList()
        }
        _pagesFlow.value = defaultPages

        val groupsJson = prefs.getString("saved_groups", null)
        val defaultGroups = if (!groupsJson.isNullOrBlank()) {
            parseGroups(groupsJson).filterNot {
                it.id in listOf("grp_android", "grp_photo", "grp_travel")
            }
        } else {
            emptyList()
        }
        _groupsFlow.value = defaultGroups
    }

    private fun listenToFirebase() {
        dbRef?.child("pages")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PageItem>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                    if (id.isBlank() || id in listOf("page_tech", "page_travel", "page_fitness")) continue
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val category = child.child("category").getValue(String::class.java) ?: "Creator"
                    val desc = child.child("description").getValue(String::class.java) ?: ""
                    val cover = child.child("coverUrl").getValue(String::class.java) ?: ""
                    val avatar = child.child("avatarUrl").getValue(String::class.java) ?: ""
                    val creatorId = child.child("creatorId").getValue(String::class.java) ?: ""
                    val followers = child.child("followersCount").getValue(Int::class.java) ?: 1
                    val likes = child.child("likesCount").getValue(Int::class.java) ?: 1
                    val created = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                    list.add(PageItem(id, name, category, desc, cover, avatar, creatorId, followers, likes, created))
                }
                _pagesFlow.value = list
                savePagesLocally(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        dbRef?.child("groups")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<GroupItem>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                    if (id.isBlank() || id in listOf("grp_android", "grp_photo", "grp_travel")) continue
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val privacy = child.child("privacy").getValue(String::class.java) ?: "Public"
                    val desc = child.child("description").getValue(String::class.java) ?: ""
                    val cover = child.child("coverUrl").getValue(String::class.java) ?: ""
                    val creatorId = child.child("creatorId").getValue(String::class.java) ?: ""
                    val members = child.child("membersCount").getValue(Int::class.java) ?: 1
                    val created = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                    list.add(GroupItem(id, name, privacy, desc, cover, creatorId, members, created))
                }
                _groupsFlow.value = list
                saveGroupsLocally(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun createPage(page: PageItem) {
        val newPage = if (page.id.isBlank()) page.copy(id = "page_" + UUID.randomUUID().toString().take(8)) else page
        val updated = listOf(newPage) + _pagesFlow.value.filter { it.id != newPage.id }
        _pagesFlow.value = updated
        savePagesLocally(updated)
        try {
            dbRef?.child("pages")?.child(newPage.id)?.setValue(newPage.toMap())
        } catch (_: Exception) {}
    }

    fun updatePage(page: PageItem) {
        val updated = _pagesFlow.value.map { if (it.id == page.id) page else it }
        _pagesFlow.value = updated
        savePagesLocally(updated)
        try {
            dbRef?.child("pages")?.child(page.id)?.setValue(page.toMap())
        } catch (_: Exception) {}
    }

    fun deletePage(pageId: String) {
        val updated = _pagesFlow.value.filter { it.id != pageId }
        _pagesFlow.value = updated
        savePagesLocally(updated)
        try {
            dbRef?.child("pages")?.child(pageId)?.removeValue()
        } catch (_: Exception) {}
    }

    fun createGroup(group: GroupItem) {
        val newGroup = if (group.id.isBlank()) group.copy(id = "grp_" + UUID.randomUUID().toString().take(8)) else group
        val updated = listOf(newGroup) + _groupsFlow.value.filter { it.id != newGroup.id }
        _groupsFlow.value = updated
        saveGroupsLocally(updated)
        try {
            dbRef?.child("groups")?.child(newGroup.id)?.setValue(newGroup.toMap())
        } catch (_: Exception) {}
    }

    fun updateGroup(group: GroupItem) {
        val updated = _groupsFlow.value.map { if (it.id == group.id) group else it }
        _groupsFlow.value = updated
        saveGroupsLocally(updated)
        try {
            dbRef?.child("groups")?.child(group.id)?.setValue(group.toMap())
        } catch (_: Exception) {}
    }

    fun deleteGroup(groupId: String) {
        val updated = _groupsFlow.value.filter { it.id != groupId }
        _groupsFlow.value = updated
        saveGroupsLocally(updated)
        try {
            dbRef?.child("groups")?.child(groupId)?.removeValue()
        } catch (_: Exception) {}
    }

    private fun savePagesLocally(pages: List<PageItem>) {
        val arr = JSONArray()
        for (p in pages) {
            arr.put(JSONObject(p.toMap()))
        }
        prefs.edit().putString("saved_pages", arr.toString()).apply()
    }

    private fun saveGroupsLocally(groups: List<GroupItem>) {
        val arr = JSONArray()
        for (g in groups) {
            arr.put(JSONObject(g.toMap()))
        }
        prefs.edit().putString("saved_groups", arr.toString()).apply()
    }

    private fun parsePages(json: String): List<PageItem> {
        val list = mutableListOf<PageItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PageItem(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        category = obj.optString("category", "Creator"),
                        description = obj.optString("description", ""),
                        coverUrl = obj.optString("coverUrl", ""),
                        avatarUrl = obj.optString("avatarUrl", ""),
                        creatorId = obj.optString("creatorId", ""),
                        followersCount = obj.optInt("followersCount", 1),
                        likesCount = obj.optInt("likesCount", 1),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseGroups(json: String): List<GroupItem> {
        val list = mutableListOf<GroupItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GroupItem(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        privacy = obj.optString("privacy", "Public"),
                        description = obj.optString("description", ""),
                        coverUrl = obj.optString("coverUrl", ""),
                        creatorId = obj.optString("creatorId", ""),
                        membersCount = obj.optInt("membersCount", 1),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
