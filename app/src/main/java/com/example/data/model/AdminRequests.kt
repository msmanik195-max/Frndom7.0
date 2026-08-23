package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PaymentMethodItem(
    val id: String = "",
    val name: String = "",
    val accountNumber: String = "",
    val accountType: String = "Personal (Send Money)",
    val instructions: String = "1. Send Money to the number above.\n2. Copy the Transaction ID (TrxID) and submit it below.",
    val colorHex: String = "#E2136E",
    val isActive: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "accountNumber" to accountNumber,
        "accountType" to accountType,
        "instructions" to instructions,
        "colorHex" to colorHex,
        "isActive" to isActive
    )
}

@IgnoreExtraProperties
data class DepositRequestItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val amount: Double = 0.0,
    val methodName: String = "",
    val senderNumber: String = "",
    val transactionId: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val adminNote: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "amount" to amount,
        "methodName" to methodName,
        "senderNumber" to senderNumber,
        "transactionId" to transactionId,
        "status" to status,
        "createdAt" to createdAt,
        "adminNote" to adminNote
    )
}

@IgnoreExtraProperties
data class WithdrawRequestItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val amount: Double = 0.0,
    val methodName: String = "",
    val accountNumber: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val adminNote: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "amount" to amount,
        "methodName" to methodName,
        "accountNumber" to accountNumber,
        "status" to status,
        "createdAt" to createdAt,
        "adminNote" to adminNote
    )
}

@IgnoreExtraProperties
data class MonetizationRequestItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val viewsCount: Int = 0,
    val followersCount: Int = 0,
    val postsCount: Int = 0,
    val reelsCount: Int = 0,
    val accountAgeDays: Int = 0,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val adminNote: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "viewsCount" to viewsCount,
        "followersCount" to followersCount,
        "postsCount" to postsCount,
        "reelsCount" to reelsCount,
        "accountAgeDays" to accountAgeDays,
        "status" to status,
        "createdAt" to createdAt,
        "adminNote" to adminNote
    )
}

@IgnoreExtraProperties
data class VerificationRequestItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val planTitle: String = "",
    val durationDays: Int = 30,
    val price: Double = 0.0,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val adminNote: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "userPhone" to userPhone,
        "planTitle" to planTitle,
        "durationDays" to durationDays,
        "price" to price,
        "status" to status,
        "createdAt" to createdAt,
        "adminNote" to adminNote
    )
}
