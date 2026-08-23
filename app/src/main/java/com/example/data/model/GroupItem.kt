package com.example.data.model

data class GroupItem(
    val id: String = "",
    val name: String = "",
    val privacy: String = "Public",
    val description: String = "",
    val coverUrl: String = "",
    val creatorId: String = "",
    val membersCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "privacy" to privacy,
        "description" to description,
        "coverUrl" to coverUrl,
        "creatorId" to creatorId,
        "membersCount" to membersCount,
        "createdAt" to createdAt
    )
}
