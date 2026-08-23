package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.UUID

@IgnoreExtraProperties
data class MarketplaceItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val price: Double = 0.0, // Strictly in BDT (৳)
    val category: String = "All", // e.g. "Electronics", "Mobiles", "Vehicles", "Property", "Apparel", "Home Goods", "Hobbies"
    val condition: String = "Brand New", // "Brand New", "Used - Like New", "Used - Good", "Used - Fair"
    val location: String = "Dhaka, Bangladesh",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val coverImageUrl: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerAvatarUrl: String = "",
    val isSold: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "price" to price,
            "category" to category,
            "condition" to condition,
            "location" to location,
            "description" to description,
            "imageUrls" to imageUrls,
            "coverImageUrl" to coverImageUrl,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "sellerAvatarUrl" to sellerAvatarUrl,
            "isSold" to isSold,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): MarketplaceItem {
            @Suppress("UNCHECKED_CAST")
            val rawImages = map["imageUrls"] as? List<String> ?: emptyList()
            return MarketplaceItem(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                category = map["category"] as? String ?: "Other",
                condition = map["condition"] as? String ?: "Used - Good",
                location = map["location"] as? String ?: "Dhaka, Bangladesh",
                description = map["description"] as? String ?: "",
                imageUrls = rawImages,
                coverImageUrl = map["coverImageUrl"] as? String ?: rawImages.firstOrNull() ?: "",
                sellerId = map["sellerId"] as? String ?: "",
                sellerName = map["sellerName"] as? String ?: "Seller",
                sellerAvatarUrl = map["sellerAvatarUrl"] as? String ?: "",
                isSold = map["isSold"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
