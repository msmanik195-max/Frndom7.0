package com.example.data.model

data class PageItem(
    val id: String = "",
    val name: String = "",
    val category: String = "Creator",
    val description: String = "",
    val coverUrl: String = "",
    val avatarUrl: String = "",
    val creatorId: String = "",
    val followersCount: Int = 1,
    val likesCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "category" to category,
        "description" to description,
        "coverUrl" to coverUrl,
        "avatarUrl" to avatarUrl,
        "creatorId" to creatorId,
        "followersCount" to followersCount,
        "likesCount" to likesCount,
        "createdAt" to createdAt
    )
}
