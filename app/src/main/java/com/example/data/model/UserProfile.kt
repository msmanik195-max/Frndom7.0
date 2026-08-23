package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val identifierType: String = "email", // "email" or "phone"
    val email: String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val birthDay: Int = 1,
    val birthMonth: Int = 1,
    val birthYear: Int = 2000,
    val formattedBirthDate: String = "",
    val profilePictureUrl: String = "",
    val coverPictureUrl: String = "",
    val bio: String = "",
    val work: String = "",
    val education: String = "",
    val currentCity: String = "",
    val hometown: String = "",
    val relationshipStatus: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val followersMap: Map<String, Boolean> = emptyMap(),
    val followingMap: Map<String, Boolean> = emptyMap(),
    val isVerified: Boolean = false,
    val verifiedUntil: Long = 0L,
    val verificationType: String = "GREEN_BADGE",
    val verificationPlanTitle: String = "",
    val isBlocked: Boolean = false,
    val isMonetized: Boolean = false,
    val isOnline: Boolean = false,
    val lastActiveAt: Long = System.currentTimeMillis(),
    val walletBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    fun isVerificationActive(): Boolean {
        if (!isVerified) return false
        if (verifiedUntil <= 0L) return isVerified
        return System.currentTimeMillis() <= verifiedUntil
    }

    fun isUserOnline(): Boolean {
        if (isBlocked) return false
        if (isOnline) return true
        val now = System.currentTimeMillis()
        if (lastActiveAt > 0 && (now - lastActiveAt) < 15 * 60 * 1000) return true
        if (lastLoginAt > 0 && (now - lastLoginAt) < 20 * 60 * 1000) return true
        return false
    }

    fun getRemainingDays(): Int {
        if (!isVerificationActive()) return 0
        if (verifiedUntil <= 0L) return 365
        val diff = verifiedUntil - System.currentTimeMillis()
        if (diff <= 0) return 0
        return ((diff + (24L * 60 * 60 * 1000 - 1)) / (24L * 60 * 60 * 1000)).toInt()
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "fullName" to fullName,
            "identifierType" to identifierType,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "gender" to gender,
            "birthDay" to birthDay,
            "birthMonth" to birthMonth,
            "birthYear" to birthYear,
            "formattedBirthDate" to formattedBirthDate,
            "profilePictureUrl" to profilePictureUrl,
            "coverPictureUrl" to coverPictureUrl,
            "bio" to bio,
            "work" to work,
            "education" to education,
            "currentCity" to currentCity,
            "hometown" to hometown,
            "relationshipStatus" to relationshipStatus,
            "followersCount" to followersCount,
            "followingCount" to followingCount,
            "followersMap" to followersMap,
            "followingMap" to followingMap,
            "isVerified" to isVerified,
            "verifiedUntil" to verifiedUntil,
            "verificationType" to verificationType,
            "verificationPlanTitle" to verificationPlanTitle,
            "isBlocked" to isBlocked,
            "isMonetized" to isMonetized,
            "isOnline" to isOnline,
            "lastActiveAt" to lastActiveAt,
            "walletBalance" to walletBalance,
            "createdAt" to createdAt,
            "lastLoginAt" to lastLoginAt
        )
    }
}
