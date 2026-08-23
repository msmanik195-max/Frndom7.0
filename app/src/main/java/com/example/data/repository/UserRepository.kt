package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UserRepository(private val context: Context) {

    companion object {
        private val allUsersMapState = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
        private val verifiedUsersMap = MutableStateFlow<Map<String, Triple<Long, String, String>>>(emptyMap())
        private val deletedUidsSet = MutableStateFlow<Set<String>>(emptySet())
        private var isListenersInitialized = false
    }

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference("users")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    private val verificationsDbRef: DatabaseReference? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference("verifications")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("UserRepository", "Verifications FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    init {
        initializeDataAndListeners()
    }

    @Synchronized
    private fun initializeDataAndListeners() {
        val prefs = context.getSharedPreferences("frndom_user_cache", Context.MODE_PRIVATE)
        val savedDeleted = prefs.getStringSet("permanently_deleted_uids", emptySet()) ?: emptySet()
        deletedUidsSet.value = savedDeleted

        // 1. Initial seed from local storage if state is empty
        if (allUsersMapState.value.isEmpty()) {
            val initialMap = LinkedHashMap<String, UserProfile>()
            val deleted = deletedUidsSet.value
            getSavedAccounts().forEach { user ->
                if (user.uid.isNotBlank() && !deleted.contains(user.uid) && !deleted.contains(user.email.trim().lowercase()) && !deleted.contains(user.phoneNumber.trim())) {
                    initialMap[user.uid] = user
                }
            }
            if (initialMap.isNotEmpty()) {
                allUsersMapState.value = initialMap
            }
        }

        if (isListenersInitialized) return
        isListenersInitialized = true

        // 2. Listen to verifications
        listenToFirebaseVerifications()

        // 3. Listen to Realtime Database users
        listenToRealtimeDbUsers()

        // 4. Listen to Firestore users
        listenToFirestoreUsers()
    }

    private fun listenToFirebaseVerifications() {
        try {
            verificationsDbRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<String, Triple<Long, String, String>>()
                    val deleted = deletedUidsSet.value
                    for (child in snapshot.children) {
                        val uid = child.key ?: child.child("uid").getValue(String::class.java) ?: continue
                        if (deleted.contains(uid)) continue

                        val verifiedUntil = child.child("verifiedUntil").getValue(Long::class.java) ?: 0L
                        val isVerified = child.child("isVerified").getValue(Boolean::class.java) ?: (verifiedUntil > System.currentTimeMillis())
                        val verificationType = child.child("verificationType").getValue(String::class.java) ?: "GREEN_BADGE"
                        val planTitle = child.child("verificationPlanTitle").getValue(String::class.java) ?: ""
                        val email = child.child("email").getValue(String::class.java) ?: ""
                        val phone = child.child("phone").getValue(String::class.java) ?: ""

                        if (deleted.contains(email.trim().lowercase()) || deleted.contains(phone.trim())) continue

                        if (isVerified && (verifiedUntil > System.currentTimeMillis() || verifiedUntil <= 0L)) {
                            val triple = Triple(verifiedUntil, verificationType, planTitle)
                            map[uid] = triple
                            if (email.isNotBlank()) map[email.trim().lowercase()] = triple
                            if (phone.isNotBlank()) map[phone.trim().replace(" ", "").replace("-", "")] = triple
                        }
                    }
                    verifiedUsersMap.value = map

                    // Re-enrich all current users with updated verifications
                    val currentMap = allUsersMapState.value
                    if (currentMap.isNotEmpty()) {
                        val updated = currentMap.mapValues { (_, user) -> enrichProfileWithVerification(user) }
                        allUsersMapState.value = updated
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("UserRepository", "Verifications listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun listenToRealtimeDbUsers() {
        try {
            dbRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newMap = LinkedHashMap<String, UserProfile>()
                    val deleted = deletedUidsSet.value
                    for (child in snapshot.children) {
                        val user = child.getValue(UserProfile::class.java)
                        if (user != null && user.uid.isNotBlank()) {
                            if (deleted.contains(user.uid) || deleted.contains(user.email.trim().lowercase()) || deleted.contains(user.phoneNumber.trim())) {
                                continue
                            }
                            newMap[user.uid] = enrichProfileWithVerification(user)
                        }
                    }
                    allUsersMapState.value = newMap
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("UserRepository", "RTDB users listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun listenToFirestoreUsers() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance().collection("users")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val newMap = LinkedHashMap(allUsersMapState.value)
                        val deleted = deletedUidsSet.value
                        for (doc in snapshot.documents) {
                            val user = doc.toObject(UserProfile::class.java)
                            if (user != null && user.uid.isNotBlank()) {
                                if (deleted.contains(user.uid) || deleted.contains(user.email.trim().lowercase()) || deleted.contains(user.phoneNumber.trim())) {
                                    continue
                                }
                                val existing = newMap[user.uid]
                                val merged = if (existing != null) {
                                    // Merge latest fields
                                    existing.copy(
                                        fullName = user.fullName.ifBlank { existing.fullName },
                                        firstName = user.firstName.ifBlank { existing.firstName },
                                        lastName = user.lastName.ifBlank { existing.lastName },
                                        email = user.email.ifBlank { existing.email },
                                        phoneNumber = user.phoneNumber.ifBlank { existing.phoneNumber },
                                        profilePictureUrl = user.profilePictureUrl.ifBlank { existing.profilePictureUrl },
                                        bio = user.bio.ifBlank { existing.bio },
                                        isBlocked = user.isBlocked,
                                        isMonetized = user.isMonetized,
                                        walletBalance = if (user.walletBalance > 0) user.walletBalance else existing.walletBalance,
                                        isVerified = user.isVerified || existing.isVerified,
                                        verifiedUntil = maxOf(user.verifiedUntil, existing.verifiedUntil)
                                    )
                                } else {
                                    user
                                }
                                newMap[user.uid] = enrichProfileWithVerification(merged)
                            }
                        }
                        allUsersMapState.value = newMap.filterKeys { !deleted.contains(it) }
                    }
            }
        } catch (_: Exception) {}
    }

    private fun savePersistentVerificationLocally(
        uid: String,
        email: String = "",
        phone: String = "",
        verifiedUntil: Long,
        verificationType: String = "GREEN_BADGE",
        planTitle: String = ""
    ) {
        val prefs = context.getSharedPreferences("frndom_verified_accounts", Context.MODE_PRIVATE)
        val json = org.json.JSONObject().apply {
            put("uid", uid)
            put("email", email.trim().lowercase())
            put("phone", phone.trim().replace(" ", "").replace("-", ""))
            put("verifiedUntil", verifiedUntil)
            put("verificationType", verificationType)
            put("planTitle", planTitle)
            put("savedAt", System.currentTimeMillis())
        }.toString()

        val editor = prefs.edit()
        if (uid.isNotBlank()) {
            editor.putString("v_uid_$uid", json)
        }
        if (email.isNotBlank()) {
            editor.putString("v_email_${email.trim().lowercase()}", json)
        }
        if (phone.isNotBlank()) {
            editor.putString("v_phone_${phone.trim().replace(" ", "").replace("-", "")}", json)
        }
        editor.apply()
    }

    fun savePersistentVerification(
        uid: String,
        email: String = "",
        phone: String = "",
        verifiedUntil: Long,
        verificationType: String = "GREEN_BADGE",
        planTitle: String = ""
    ) {
        if (verifiedUntil <= 0L && uid.isBlank()) return
        savePersistentVerificationLocally(uid, email, phone, verifiedUntil, verificationType, planTitle)

        // Update in-memory state
        val currentMap = verifiedUsersMap.value.toMutableMap()
        val triple = Triple(verifiedUntil, verificationType, planTitle)
        if (uid.isNotBlank()) currentMap[uid] = triple
        if (email.isNotBlank()) currentMap[email.trim().lowercase()] = triple
        if (phone.isNotBlank()) currentMap[phone.trim().replace(" ", "").replace("-", "")] = triple
        verifiedUsersMap.value = currentMap

        // Also push to Firebase Realtime Database and Cloud Firestore
        try {
            if (uid.isNotBlank() && FirebaseApp.getApps(context).isNotEmpty()) {
                val map = mapOf(
                    "uid" to uid,
                    "email" to email,
                    "phone" to phone,
                    "isVerified" to true,
                    "verifiedUntil" to verifiedUntil,
                    "verificationType" to verificationType,
                    "verificationPlanTitle" to planTitle,
                    "updatedAt" to System.currentTimeMillis()
                )
                FirebaseFirestore.getInstance()
                    .collection("verifications")
                    .document(uid)
                    .set(map, SetOptions.merge())

                verificationsDbRef?.child(uid)?.setValue(map)

                // Also update users node so it is in sync
                val userVerificationPatch = mapOf(
                    "isVerified" to true,
                    "verifiedUntil" to verifiedUntil,
                    "verificationType" to verificationType,
                    "verificationPlanTitle" to planTitle
                )
                dbRef?.child(uid)?.updateChildren(userVerificationPatch)
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(userVerificationPatch, SetOptions.merge())
            }
        } catch (_: Exception) {}

        // Update local user profile cache if exists
        val currentProfile = if (uid.isNotBlank()) getLocalUserProfile(uid) else getCurrentUser()
        if (currentProfile != null && (currentProfile.uid == uid || (email.isNotBlank() && currentProfile.email.equals(email, ignoreCase = true)))) {
            val updated = currentProfile.copy(
                isVerified = true,
                verifiedUntil = verifiedUntil,
                verificationType = verificationType,
                verificationPlanTitle = planTitle
            )
            saveLocalUserProfile(updated)
        }
    }

    fun revokePersistentVerification(
        uid: String,
        email: String = "",
        phone: String = ""
    ) {
        // 1. Remove from in-memory map
        val currentMap = verifiedUsersMap.value.toMutableMap()
        if (uid.isNotBlank()) currentMap.remove(uid)
        if (email.isNotBlank()) currentMap.remove(email.trim().lowercase())
        if (phone.isNotBlank()) currentMap.remove(phone.trim().replace(" ", "").replace("-", ""))
        verifiedUsersMap.value = currentMap

        // 2. Remove from SharedPreferences
        val prefs = context.getSharedPreferences("frndom_verified_accounts", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (uid.isNotBlank()) editor.remove("v_uid_$uid")
        if (email.isNotBlank()) editor.remove("v_email_${email.trim().lowercase()}")
        if (phone.isNotBlank()) editor.remove("v_phone_${phone.trim().replace(" ", "").replace("-", "")}")
        editor.apply()

        // 3. Remove from Firebase
        try {
            if (uid.isNotBlank() && FirebaseApp.getApps(context).isNotEmpty()) {
                verificationsDbRef?.child(uid)?.removeValue()
                FirebaseFirestore.getInstance().collection("verifications").document(uid).delete()

                val userRevokePatch = mapOf(
                    "isVerified" to false,
                    "verifiedUntil" to 0L,
                    "verificationType" to "",
                    "verificationPlanTitle" to ""
                )
                dbRef?.child(uid)?.updateChildren(userRevokePatch)
                FirebaseFirestore.getInstance().collection("users").document(uid).set(userRevokePatch, SetOptions.merge())
            }
        } catch (_: Exception) {}

        // 4. Update local user profile cache
        val currentProfile = if (uid.isNotBlank()) getLocalUserProfile(uid) else getCurrentUser()
        if (currentProfile != null && (currentProfile.uid == uid || (email.isNotBlank() && currentProfile.email.equals(email, ignoreCase = true)))) {
            val updated = currentProfile.copy(
                isVerified = false,
                verifiedUntil = 0L,
                verificationType = "",
                verificationPlanTitle = ""
            )
            saveLocalUserProfile(updated)
        }
    }

    fun getPersistentVerification(uid: String, email: String = "", phone: String = ""): Triple<Long, String, String>? {
        // 1. Check in-memory map
        val memEntry = if (uid.isNotBlank()) verifiedUsersMap.value[uid] else null
        if (memEntry != null && (memEntry.first > System.currentTimeMillis() || memEntry.first <= 0L)) {
            return memEntry
        }
        val memEmailEntry = if (email.isNotBlank()) verifiedUsersMap.value[email.trim().lowercase()] else null
        if (memEmailEntry != null && (memEmailEntry.first > System.currentTimeMillis() || memEmailEntry.first <= 0L)) {
            return memEmailEntry
        }

        // 2. Check SharedPreferences
        val prefs = context.getSharedPreferences("frndom_verified_accounts", Context.MODE_PRIVATE)
        var json = if (uid.isNotBlank()) prefs.getString("v_uid_$uid", null) else null
        if (json == null && email.isNotBlank()) {
            json = prefs.getString("v_email_${email.trim().lowercase()}", null)
        }
        if (json == null && phone.isNotBlank()) {
            json = prefs.getString("v_phone_${phone.trim().replace(" ", "").replace("-", "")}", null)
        }
        if (json == null) return null

        return try {
            val obj = org.json.JSONObject(json)
            val until = obj.optLong("verifiedUntil", 0L)
            val type = obj.optString("verificationType", "GREEN_BADGE")
            val plan = obj.optString("planTitle", "")
            if (until > System.currentTimeMillis() || until <= 0L) {
                Triple(until, type, plan)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun enrichProfileWithVerification(profile: UserProfile): UserProfile {
        val stored = getPersistentVerification(profile.uid, profile.email, profile.phoneNumber)
        return if (stored != null && (stored.first > System.currentTimeMillis() || stored.first <= 0L)) {
            profile.copy(
                isVerified = true,
                verifiedUntil = if (stored.first > 0L) maxOf(profile.verifiedUntil, stored.first) else if (profile.verifiedUntil > 0L) profile.verifiedUntil else stored.first,
                verificationType = if (profile.verificationType.isNotBlank()) profile.verificationType else stored.second,
                verificationPlanTitle = if (profile.verificationPlanTitle.isNotBlank()) profile.verificationPlanTitle else stored.third
            )
        } else if (profile.isVerificationActive()) {
            profile
        } else {
            profile.copy(
                isVerified = false,
                verifiedUntil = 0L,
                verificationType = "",
                verificationPlanTitle = ""
            )
        }
    }

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> {
        return allUsersMapState.map { map ->
            val profile = map[uid] ?: getLocalUserProfile(uid)
            profile?.let { enrichProfileWithVerification(it) }
        }.distinctUntilChanged()
    }

    fun getAllUsersFlow(): Flow<List<UserProfile>> {
        return allUsersMapState.map { map ->
            val deleted = deletedUidsSet.value
            val list = mutableListOf<UserProfile>()
            val seenUids = mutableSetOf<String>()

            // 1. Add all from synchronized map
            for (user in map.values) {
                if (user.uid.isNotBlank() && !deleted.contains(user.uid) && !deleted.contains(user.email.trim().lowercase()) && !deleted.contains(user.phoneNumber.trim()) && seenUids.add(user.uid)) {
                    list.add(enrichProfileWithVerification(user))
                }
            }

            // 2. Add any saved local accounts that might not yet be in map
            for (saved in getSavedAccounts()) {
                if (saved.uid.isNotBlank() && !deleted.contains(saved.uid) && !deleted.contains(saved.email.trim().lowercase()) && !deleted.contains(saved.phoneNumber.trim()) && seenUids.add(saved.uid)) {
                    list.add(enrichProfileWithVerification(saved))
                }
            }

            list
        }.distinctUntilChanged()
    }

    fun getLocalUserProfile(uid: String): UserProfile? {
        val prefs = context.getSharedPreferences("frndom_user_cache", Context.MODE_PRIVATE)
        val json = prefs.getString("user_$uid", null) ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            val raw = UserProfile(
                uid = obj.optString("uid", uid),
                firstName = obj.optString("firstName", ""),
                lastName = obj.optString("lastName", ""),
                fullName = obj.optString("fullName", ""),
                email = obj.optString("email", ""),
                phoneNumber = obj.optString("phoneNumber", ""),
                profilePictureUrl = obj.optString("profilePictureUrl", ""),
                coverPictureUrl = obj.optString("coverPictureUrl", ""),
                bio = obj.optString("bio", ""),
                isVerified = obj.optBoolean("isVerified", false),
                verifiedUntil = obj.optLong("verifiedUntil", 0L),
                verificationType = obj.optString("verificationType", "GREEN_BADGE"),
                verificationPlanTitle = obj.optString("verificationPlanTitle", ""),
                isBlocked = obj.optBoolean("isBlocked", false),
                isMonetized = obj.optBoolean("isMonetized", false),
                isOnline = obj.optBoolean("isOnline", false),
                lastActiveAt = obj.optLong("lastActiveAt", System.currentTimeMillis()),
                walletBalance = obj.optDouble("walletBalance", 0.0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                lastLoginAt = obj.optLong("lastLoginAt", System.currentTimeMillis())
            )
            enrichProfileWithVerification(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun saveLocalUserProfile(profile: UserProfile) {
        val enriched = enrichProfileWithVerification(profile)

        val prefs = context.getSharedPreferences("frndom_user_cache", Context.MODE_PRIVATE)
        val json = org.json.JSONObject().apply {
            put("uid", enriched.uid)
            put("firstName", enriched.firstName)
            put("lastName", enriched.lastName)
            put("fullName", enriched.fullName)
            put("email", enriched.email)
            put("phoneNumber", enriched.phoneNumber)
            put("profilePictureUrl", enriched.profilePictureUrl)
            put("coverPictureUrl", enriched.coverPictureUrl)
            put("bio", enriched.bio)
            put("isVerified", enriched.isVerified)
            put("verifiedUntil", enriched.verifiedUntil)
            put("verificationType", enriched.verificationType)
            put("verificationPlanTitle", enriched.verificationPlanTitle)
            put("isBlocked", enriched.isBlocked)
            put("isMonetized", enriched.isMonetized)
            put("isOnline", enriched.isOnline)
            put("lastActiveAt", enriched.lastActiveAt)
            put("walletBalance", enriched.walletBalance)
            put("createdAt", enriched.createdAt)
            put("lastLoginAt", enriched.lastLoginAt)
        }.toString()
        prefs.edit().putString("user_${enriched.uid}", json).apply()

        // Update in-memory flow immediately
        allUsersMapState.value = allUsersMapState.value.toMutableMap().apply {
            put(enriched.uid, enriched)
        }

        // Also add to saved accounts set
        val existingAccounts = getSavedAccounts().toMutableList()
        val index = existingAccounts.indexOfFirst { it.uid == enriched.uid }
        if (index >= 0) {
            existingAccounts[index] = enriched
        } else {
            existingAccounts.add(enriched)
        }
        val arr = org.json.JSONArray()
        existingAccounts.forEach { acc ->
            arr.put(org.json.JSONObject().apply {
                put("uid", acc.uid)
                put("firstName", acc.firstName)
                put("lastName", acc.lastName)
                put("fullName", acc.fullName)
                put("email", acc.email)
                put("phoneNumber", acc.phoneNumber)
                put("profilePictureUrl", acc.profilePictureUrl)
                put("coverPictureUrl", acc.coverPictureUrl)
                put("bio", acc.bio)
                put("isVerified", acc.isVerified)
                put("verifiedUntil", acc.verifiedUntil)
                put("verificationType", acc.verificationType)
                put("verificationPlanTitle", acc.verificationPlanTitle)
                put("isBlocked", acc.isBlocked)
                put("isMonetized", acc.isMonetized)
                put("isOnline", acc.isOnline)
                put("lastActiveAt", acc.lastActiveAt)
                put("walletBalance", acc.walletBalance)
                put("createdAt", acc.createdAt)
                put("lastLoginAt", acc.lastLoginAt)
            })
        }
        prefs.edit().putString("saved_accounts_list", arr.toString()).apply()

        // Synchronize updated profile picture & name across all active stories immediately
        if (enriched.uid.isNotBlank() && (enriched.profilePictureUrl.isNotBlank() || enriched.fullName.isNotBlank())) {
            try {
                StoryRepository(context).updateStoriesUserAvatar(
                    userId = enriched.uid,
                    newAvatar = enriched.profilePictureUrl,
                    newName = enriched.fullName
                )
            } catch (_: Exception) {}
        }
    }

    fun getCurrentUser(): UserProfile? {
        try {
            val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (fbUser != null) {
                val cached = getLocalUserProfile(fbUser.uid)
                if (cached != null) return cached
                return UserProfile(
                    uid = fbUser.uid,
                    firstName = fbUser.displayName?.split(" ")?.firstOrNull() ?: "User",
                    lastName = fbUser.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                    fullName = fbUser.displayName ?: "User",
                    email = fbUser.email ?: ""
                )
            }
        } catch (_: Exception) {}
        return getSavedAccounts().firstOrNull()
    }

    fun getSavedAccounts(): List<UserProfile> {
        val prefs = context.getSharedPreferences("frndom_user_cache", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_accounts_list", null) ?: return emptyList()
        val list = mutableListOf<UserProfile>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val raw = UserProfile(
                    uid = obj.optString("uid", ""),
                    firstName = obj.optString("firstName", ""),
                    lastName = obj.optString("lastName", ""),
                    fullName = obj.optString("fullName", ""),
                    email = obj.optString("email", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    profilePictureUrl = obj.optString("profilePictureUrl", ""),
                    coverPictureUrl = obj.optString("coverPictureUrl", ""),
                    bio = obj.optString("bio", ""),
                    isVerified = obj.optBoolean("isVerified", false),
                    verifiedUntil = obj.optLong("verifiedUntil", 0L),
                    verificationType = obj.optString("verificationType", "GREEN_BADGE"),
                    verificationPlanTitle = obj.optString("verificationPlanTitle", ""),
                    isBlocked = obj.optBoolean("isBlocked", false),
                    isMonetized = obj.optBoolean("isMonetized", false),
                    isOnline = obj.optBoolean("isOnline", false),
                    lastActiveAt = obj.optLong("lastActiveAt", System.currentTimeMillis()),
                    walletBalance = obj.optDouble("walletBalance", 0.0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    lastLoginAt = obj.optLong("lastLoginAt", System.currentTimeMillis())
                )
                list.add(enrichProfileWithVerification(raw))
            }
        } catch (_: Exception) {}
        return list
    }

    fun updateUserProfile(profile: UserProfile, onComplete: (Boolean) -> Unit = {}) {
        val enriched = enrichProfileWithVerification(profile)
        saveLocalUserProfile(enriched)
        if (enriched.uid.isBlank()) {
            onComplete(true)
            return
        }

        // 1. Sync to Cloud Firestore
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(enriched.uid)
                    .set(enriched.toMap(), SetOptions.merge())
            }
        } catch (_: Exception) {}

        // 2. Sync to Realtime Database
        if (dbRef == null) {
            onComplete(true)
            return
        }
        dbRef?.child(enriched.uid)?.setValue(enriched.toMap())
            ?.addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    // ==========================================
    // ADMIN USER MANAGEMENT METHODS
    // ==========================================

    fun setUserBlocked(uid: String, isBlocked: Boolean, onComplete: (Boolean) -> Unit = {}) {
        val cached = getLocalUserProfile(uid)
        if (cached != null) {
            saveLocalUserProfile(cached.copy(isBlocked = isBlocked))
        }

        val updates = mapOf<String, Any>("isBlocked" to isBlocked)
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid).update(updates)
        } catch (_: Exception) {}

        if (dbRef != null) {
            dbRef?.child(uid)?.updateChildren(updates)?.addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
        } else {
            onComplete(true)
        }
    }

    fun setUserMonetized(uid: String, isMonetized: Boolean, onComplete: (Boolean) -> Unit = {}) {
        val cached = getLocalUserProfile(uid)
        if (cached != null) {
            saveLocalUserProfile(cached.copy(isMonetized = isMonetized))
        }

        val updates = mapOf<String, Any>("isMonetized" to isMonetized)
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid).update(updates)
        } catch (_: Exception) {}

        if (dbRef != null) {
            dbRef?.child(uid)?.updateChildren(updates)?.addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
        } else {
            onComplete(true)
        }
    }

    fun setUserVerification(
        uid: String,
        isVerified: Boolean,
        verifiedUntil: Long = 0L,
        planTitle: String = "Admin Verified",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val email = getLocalUserProfile(uid)?.email ?: ""
        val phone = getLocalUserProfile(uid)?.phoneNumber ?: ""
        
        if (isVerified) {
            val targetUntil = if (verifiedUntil <= 0L) System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) else verifiedUntil
            savePersistentVerification(
                uid = uid,
                email = email,
                phone = phone,
                verifiedUntil = targetUntil,
                verificationType = "GREEN_BADGE",
                planTitle = planTitle
            )
        } else {
            revokePersistentVerification(uid, email, phone)
        }
        onComplete(true)
    }

    fun extendUserVerification(
        uid: String,
        additionalDays: Int,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val user = getLocalUserProfile(uid)
        val currentExpiry = if (user != null && user.verifiedUntil > System.currentTimeMillis()) {
            user.verifiedUntil
        } else {
            System.currentTimeMillis()
        }
        val newExpiry = currentExpiry + (additionalDays.toLong() * 24 * 60 * 60 * 1000)
        setUserVerification(
            uid = uid,
            isVerified = true,
            verifiedUntil = newExpiry,
            planTitle = "+${additionalDays} Days Extended",
            onComplete = onComplete
        )
    }

    fun editUserDetails(
        uid: String,
        fullName: String,
        email: String,
        phone: String,
        bio: String,
        balance: Double,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cached = getLocalUserProfile(uid) ?: UserProfile(uid = uid)
        val nameParts = fullName.trim().split(" ")
        val updated = cached.copy(
            fullName = fullName.trim(),
            firstName = nameParts.firstOrNull() ?: "",
            lastName = nameParts.drop(1).joinToString(" "),
            email = email.trim(),
            phoneNumber = phone.trim(),
            bio = bio.trim(),
            walletBalance = balance
        )
        updateUserProfile(updated, onComplete)
    }

    fun deleteUserCompletely(uid: String, onComplete: (Boolean) -> Unit = {}) {
        if (uid.isBlank()) {
            onComplete(false)
            return
        }

        // Get user details before deletion if present
        val existingProfile = getLocalUserProfile(uid) ?: allUsersMapState.value[uid]
        val email = existingProfile?.email?.trim()?.lowercase() ?: ""
        val phone = existingProfile?.phoneNumber?.trim() ?: ""

        // 1. Add to permanent deleted blacklist
        val prefs = context.getSharedPreferences("frndom_user_cache", Context.MODE_PRIVATE)
        val currentDeleted = (prefs.getStringSet("permanently_deleted_uids", emptySet()) ?: emptySet()).toMutableSet()
        currentDeleted.add(uid)
        if (email.isNotBlank()) currentDeleted.add(email)
        if (phone.isNotBlank()) currentDeleted.add(phone)
        prefs.edit().putStringSet("permanently_deleted_uids", currentDeleted).apply()
        deletedUidsSet.value = currentDeleted

        // 2. Remove from local caches
        prefs.edit().remove("user_$uid").apply()

        val saved = getSavedAccounts().filter { it.uid != uid && (email.isBlank() || !it.email.equals(email, ignoreCase = true)) }
        val arr = org.json.JSONArray()
        saved.forEach { acc ->
            arr.put(org.json.JSONObject().apply {
                put("uid", acc.uid)
                put("firstName", acc.firstName)
                put("lastName", acc.lastName)
                put("fullName", acc.fullName)
                put("email", acc.email)
                put("phoneNumber", acc.phoneNumber)
                put("profilePictureUrl", acc.profilePictureUrl)
            })
        }
        prefs.edit().putString("saved_accounts_list", arr.toString()).apply()

        // 3. Remove immediately from in-memory state
        allUsersMapState.value = allUsersMapState.value.toMutableMap().apply {
            remove(uid)
        }

        // 4. Remove verification completely
        revokePersistentVerification(uid, email, phone)

        // 5. Cascade delete from Cloud Firestore
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(uid).delete()
                firestore.collection("verifications").document(uid).delete()
                firestore.collection("wallets").document(uid).delete()

                firestore.collection("verification_requests").whereEqualTo("userId", uid).get().addOnSuccessListener { snap ->
                    for (doc in snap.documents) doc.reference.delete()
                }
                firestore.collection("admin_requests").whereEqualTo("userId", uid).get().addOnSuccessListener { snap ->
                    for (doc in snap.documents) doc.reference.delete()
                }
            }
        } catch (_: Exception) {}

        // 6. Cascade delete from Realtime Database (users, verifications, wallets, notifications, posts, stories)
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val rtdb = FirebaseDatabase.getInstance()
                rtdb.getReference("users").child(uid).removeValue()
                rtdb.getReference("verifications").child(uid).removeValue()
                rtdb.getReference("wallets").child(uid).removeValue()
                rtdb.getReference("notifications").child(uid).removeValue()
                rtdb.getReference("user_settings").child(uid).removeValue()

                // Delete posts created by this user
                rtdb.getReference("posts").orderByChild("authorId").equalTo(uid).get().addOnSuccessListener { snap ->
                    for (child in snap.children) {
                        child.ref.removeValue()
                    }
                }

                // Delete stories created by this user
                rtdb.getReference("stories").orderByChild("userId").equalTo(uid).get().addOnSuccessListener { snap ->
                    for (child in snap.children) {
                        child.ref.removeValue()
                    }
                }
            }
        } catch (_: Exception) {}

        // 7. If currently authenticated user is deleted user, sign them out
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser?.uid == uid) {
                auth.signOut()
            }
        } catch (_: Exception) {}

        // 8. RTDB users delete callback
        if (dbRef != null) {
            dbRef?.child(uid)?.removeValue()?.addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
        } else {
            onComplete(true)
        }
    }

    fun toggleFollow(currentUserId: String, targetUserId: String) {
        if (currentUserId.isBlank() || targetUserId.isBlank() || currentUserId == targetUserId || dbRef == null) return

        val currentUserFollowingRef = dbRef?.child(currentUserId)?.child("followingMap")?.child(targetUserId)
        val targetUserFollowerRef = dbRef?.child(targetUserId)?.child("followersMap")?.child(currentUserId)

        currentUserFollowingRef?.get()?.addOnSuccessListener { snapshot ->
            val isFollowing = snapshot.exists() && snapshot.getValue(Boolean::class.java) == true

            if (isFollowing) {
                // Unfollow
                currentUserFollowingRef.removeValue()
                targetUserFollowerRef?.removeValue()

                // Update count
                dbRef?.child(currentUserId)?.child("followingCount")?.get()?.addOnSuccessListener { snap ->
                    val count = (snap.getValue(Int::class.java) ?: 1) - 1
                    dbRef?.child(currentUserId)?.child("followingCount")?.setValue(count.coerceAtLeast(0))
                }
                dbRef?.child(targetUserId)?.child("followersCount")?.get()?.addOnSuccessListener { snap ->
                    val count = (snap.getValue(Int::class.java) ?: 1) - 1
                    dbRef?.child(targetUserId)?.child("followersCount")?.setValue(count.coerceAtLeast(0))
                }
            } else {
                // Follow
                currentUserFollowingRef.setValue(true)
                targetUserFollowerRef?.setValue(true)

                // Trigger follow notification
                val sender: UserProfile? = getLocalUserProfile(currentUserId)
                val senderName = if (sender != null && sender.fullName.isNotBlank()) {
                    sender.fullName
                } else if (sender != null && (sender.firstName.isNotBlank() || sender.lastName.isNotBlank())) {
                    "${sender.firstName} ${sender.lastName}".trim()
                } else {
                    "Someone"
                }
                val senderAvatar = sender?.profilePictureUrl ?: ""
                NotificationRepository(context).addNotification(
                    com.example.data.model.NotificationItem(
                        recipientId = targetUserId,
                        senderId = currentUserId,
                        senderName = senderName,
                        senderAvatarUrl = senderAvatar,
                        type = "follow",
                        content = "started following you.",
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Update count
                dbRef?.child(currentUserId)?.child("followingCount")?.get()?.addOnSuccessListener { snap ->
                    val count = (snap.getValue(Int::class.java) ?: 0) + 1
                    dbRef?.child(currentUserId)?.child("followingCount")?.setValue(count)
                }
                dbRef?.child(targetUserId)?.child("followersCount")?.get()?.addOnSuccessListener { snap ->
                    val count = (snap.getValue(Int::class.java) ?: 0) + 1
                    dbRef?.child(targetUserId)?.child("followersCount")?.setValue(count)
                }
            }
        }
    }
}
