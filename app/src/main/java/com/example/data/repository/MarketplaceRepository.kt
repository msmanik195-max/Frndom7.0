package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.MarketplaceItem
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MarketplaceRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("marketplace_prefs", Context.MODE_PRIVATE)

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().reference.child("marketplace_items")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "FirebaseDatabase not ready: ${e.message}")
            null
        }
    }

    private val _localItems = MutableStateFlow<List<MarketplaceItem>>(emptyList())
    val localItems = _localItems.asStateFlow()

    private val _savedProductIdsFlow = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    init {
        loadCachedItems()
    }

    private fun loadCachedItems() {
        val json = prefs.getString("cached_marketplace_items", null)
        if (!json.isNullOrBlank()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<MarketplaceItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val sellerId = obj.optString("sellerId")
                    // Strictly exclude any seed/demo items
                    if (id.startsWith("item_seed_") || sellerId.startsWith("seed_seller_")) {
                        continue
                    }
                    val imgArray = obj.optJSONArray("imageUrls")
                    val images = mutableListOf<String>()
                    if (imgArray != null) {
                        for (j in 0 until imgArray.length()) {
                            images.add(imgArray.getString(j))
                        }
                    }
                    list.add(
                        MarketplaceItem(
                            id = id,
                            title = obj.optString("title"),
                            price = obj.optDouble("price", 0.0),
                            category = obj.optString("category", "Electronics"),
                            condition = obj.optString("condition", "Used - Good"),
                            location = obj.optString("location", "Dhaka, Bangladesh"),
                            description = obj.optString("description", ""),
                            imageUrls = images,
                            coverImageUrl = obj.optString("coverImageUrl"),
                            sellerId = sellerId,
                            sellerName = obj.optString("sellerName", "Seller"),
                            sellerAvatarUrl = obj.optString("sellerAvatarUrl"),
                            isSold = obj.optBoolean("isSold", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _localItems.value = list
                saveCachedItems(list)
            } catch (e: Exception) {
                Log.e("MarketplaceRepository", "Error parsing cached items: ${e.message}")
                _localItems.value = emptyList()
            }
        } else {
            _localItems.value = emptyList()
        }
    }

    private fun saveCachedItems(list: List<MarketplaceItem>) {
        val cleanList = list.filterNot { it.id.startsWith("item_seed_") || it.sellerId.startsWith("seed_seller_") }
        _localItems.value = cleanList
        try {
            val array = JSONArray()
            cleanList.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("price", item.price)
                    put("category", item.category)
                    put("condition", item.condition)
                    put("location", item.location)
                    put("description", item.description)
                    val imgArr = JSONArray()
                    item.imageUrls.forEach { imgArr.put(it) }
                    put("imageUrls", imgArr)
                    put("coverImageUrl", item.coverImageUrl)
                    put("sellerId", item.sellerId)
                    put("sellerName", item.sellerName)
                    put("sellerAvatarUrl", item.sellerAvatarUrl)
                    put("isSold", item.isSold)
                    put("createdAt", item.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString("cached_marketplace_items", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("MarketplaceRepository", "Error saving cached items: ${e.message}")
        }
    }

    fun getItemsFlow(): Flow<List<MarketplaceItem>> = callbackFlow {
        if (dbRef == null) {
            trySend(_localItems.value)
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MarketplaceItem>()
                for (child in snapshot.children) {
                    val map = child.value as? Map<String, Any?>
                    if (map != null) {
                        val item = MarketplaceItem.fromMap(map)
                        // Exclude demo/seed items
                        if (!item.id.startsWith("item_seed_") && !item.sellerId.startsWith("seed_seller_")) {
                            list.add(item)
                        }
                    }
                }
                val sorted = list.sortedByDescending { it.createdAt }
                saveCachedItems(sorted)
                trySend(sorted)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(_localItems.value)
            }
        }

        dbRef?.addValueEventListener(listener)
        awaitClose {
            dbRef?.removeEventListener(listener)
        }
    }

    fun createListing(item: MarketplaceItem) {
        val updated = listOf(item) + _localItems.value.filter { it.id != item.id }
        saveCachedItems(updated)
        dbRef?.child(item.id)?.setValue(item.toMap())
    }

    fun updateListing(item: MarketplaceItem) {
        val updated = _localItems.value.map { if (it.id == item.id) item else it }
        saveCachedItems(updated)
        dbRef?.child(item.id)?.setValue(item.toMap())
    }

    fun deleteListing(itemId: String) {
        val updated = _localItems.value.filter { it.id != itemId }
        saveCachedItems(updated)
        dbRef?.child(itemId)?.removeValue()
    }

    fun markAsSold(itemId: String, isSold: Boolean) {
        val updated = _localItems.value.map {
            if (it.id == itemId) it.copy(isSold = isSold) else it
        }
        saveCachedItems(updated)
        dbRef?.child(itemId)?.child("isSold")?.setValue(isSold)
    }

    // -------------------------------------------------------------
    // SAVED MARKETPLACE PRODUCTS (PERSISTENT PER USER)
    // -------------------------------------------------------------
    fun getSavedProductIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        val set = prefs.getStringSet("saved_products_$userId", emptySet()) ?: emptySet()
        return set
    }

    fun isProductSaved(userId: String, productId: String): Boolean {
        if (userId.isBlank() || productId.isBlank()) return false
        return getSavedProductIds(userId).contains(productId)
    }

    fun toggleSaveProduct(userId: String, productId: String): Boolean {
        if (userId.isBlank() || productId.isBlank()) return false
        val currentSet = getSavedProductIds(userId).toMutableSet()
        val isNowSaved = if (currentSet.contains(productId)) {
            currentSet.remove(productId)
            false
        } else {
            currentSet.add(productId)
            true
        }
        prefs.edit().putStringSet("saved_products_$userId", currentSet).apply()
        val currentMap = _savedProductIdsFlow.value.toMutableMap()
        currentMap[userId] = currentSet
        _savedProductIdsFlow.value = currentMap
        return isNowSaved
    }

    fun getSavedProducts(userId: String): List<MarketplaceItem> {
        val savedIds = getSavedProductIds(userId)
        return _localItems.value.filter { savedIds.contains(it.id) }
    }
}
