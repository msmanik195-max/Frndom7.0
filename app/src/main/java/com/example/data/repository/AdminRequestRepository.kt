package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.DepositRequestItem
import com.example.data.model.MonetizationRequestItem
import com.example.data.model.PaymentMethodItem
import com.example.data.model.VerificationRequestItem
import com.example.data.model.WithdrawRequestItem
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AdminRequestRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_admin_requests_prefs", Context.MODE_PRIVATE)

    // Flow states initialized from local cache for instant UI rendering
    private val _paymentMethodsFlow = MutableStateFlow<List<PaymentMethodItem>>(loadPaymentMethods())
    val paymentMethodsFlow: StateFlow<List<PaymentMethodItem>> = _paymentMethodsFlow.asStateFlow()

    private val _depositRequestsFlow = MutableStateFlow<List<DepositRequestItem>>(loadDepositRequests())
    val depositRequestsFlow: StateFlow<List<DepositRequestItem>> = _depositRequestsFlow.asStateFlow()

    private val _withdrawRequestsFlow = MutableStateFlow<List<WithdrawRequestItem>>(loadWithdrawRequests())
    val withdrawRequestsFlow: StateFlow<List<WithdrawRequestItem>> = _withdrawRequestsFlow.asStateFlow()

    private val _monetizationRequestsFlow = MutableStateFlow<List<MonetizationRequestItem>>(loadMonetizationRequests())
    val monetizationRequestsFlow: StateFlow<List<MonetizationRequestItem>> = _monetizationRequestsFlow.asStateFlow()

    private val _verificationRequestsFlow = MutableStateFlow<List<VerificationRequestItem>>(loadVerificationRequests())
    val verificationRequestsFlow: StateFlow<List<VerificationRequestItem>> = _verificationRequestsFlow.asStateFlow()

    // Firebase Database Reference
    private val adminDbRef: DatabaseReference? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference("admin_requests")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("AdminRequestRepository", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("AdminRequestRepository", "FirebaseFirestore not initialized: ${e.message}")
            null
        }
    }

    init {
        // Ensure default payment methods exist if local cache is completely empty
        if (_paymentMethodsFlow.value.isEmpty()) {
            val defaultMethods = getDefaultPaymentMethods()
            savePaymentMethodsLocally(defaultMethods)
            _paymentMethodsFlow.value = defaultMethods
        }

        // Start Firebase real-time listeners for all 5 domains
        listenToFirebasePaymentMethods()
        listenToFirebaseDepositRequests()
        listenToFirebaseWithdrawRequests()
        listenToFirebaseVerificationRequests()
        listenToFirebaseMonetizationRequests()
    }

    private fun getDefaultPaymentMethods(): List<PaymentMethodItem> {
        return listOf(
            PaymentMethodItem(
                id = "pm_bkash",
                name = "bKash",
                accountNumber = "01712345678",
                accountType = "Personal (Send Money)",
                instructions = "1. Go to your bKash app or dial *247#.\n2. Choose 'Send Money' option.\n3. Enter the bKash number: 01712345678.\n4. Complete payment and copy the Transaction ID (TrxID).\n5. Enter your phone number and TrxID below to verify.",
                colorHex = "#E2136E",
                isActive = true
            ),
            PaymentMethodItem(
                id = "pm_nagad",
                name = "Nagad",
                accountNumber = "01812345678",
                accountType = "Personal (Send Money)",
                instructions = "1. Open your Nagad app or dial *167#.\n2. Select 'Send Money' option.\n3. Enter the Nagad number: 01812345678.\n4. Complete the transfer and copy your TrxID.\n5. Enter your sender number and TrxID below.",
                colorHex = "#F7941D",
                isActive = true
            ),
            PaymentMethodItem(
                id = "pm_rocket",
                name = "Rocket",
                accountNumber = "01912345678-9",
                accountType = "Personal",
                instructions = "1. Open Rocket App or dial *322#.\n2. Choose 'Send Money' to 01912345678-9.\n3. Enter amount and confirm.\n4. Copy the Transaction ID and submit it below.",
                colorHex = "#8C3494",
                isActive = true
            ),
            PaymentMethodItem(
                id = "pm_bank",
                name = "Bank Transfer",
                accountNumber = "City Bank: 1102345678901",
                accountType = "Corporate / Savings",
                instructions = "Bank: City Bank PLC\nAccount Name: Frndom Services Ltd\nAccount Number: 1102345678901\nBranch: Gulshan 2, Dhaka\nRouting: 225271890\nSubmit your bank reference/transaction number below.",
                colorHex = "#008937",
                isActive = true
            )
        )
    }

    // =========================================================================
    // 1. PAYMENT METHODS (Firebase RTDB + Firestore + Local Cache)
    // =========================================================================

    private fun listenToFirebasePaymentMethods() {
        try {
            adminDbRef?.child("payment_methods")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PaymentMethodItem>()
                    for (child in snapshot.children) {
                        val item = child.getValue(PaymentMethodItem::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    }

                    if (list.isNotEmpty()) {
                        _paymentMethodsFlow.value = list
                        savePaymentMethodsLocally(list)
                    } else {
                        // If empty on Firebase, initialize default methods to Firebase
                        val defaults = getDefaultPaymentMethods()
                        _paymentMethodsFlow.value = defaults
                        savePaymentMethodsLocally(defaults)
                        defaults.forEach { pm ->
                            adminDbRef?.child("payment_methods")?.child(pm.id)?.setValue(pm.toMap())
                            firestore?.collection("payment_methods")?.document(pm.id)?.set(pm.toMap(), SetOptions.merge())
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "PaymentMethods listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun loadPaymentMethods(): List<PaymentMethodItem> {
        val json = prefs.getString("payment_methods_json", null) ?: return emptyList()
        val list = mutableListOf<PaymentMethodItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PaymentMethodItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        accountNumber = obj.optString("accountNumber", ""),
                        accountType = obj.optString("accountType", "Personal"),
                        instructions = obj.optString("instructions", ""),
                        colorHex = obj.optString("colorHex", "#E2136E"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun savePaymentMethodsLocally(list: List<PaymentMethodItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("accountNumber", item.accountNumber)
                    put("accountType", item.accountType)
                    put("instructions", item.instructions)
                    put("colorHex", item.colorHex)
                    put("isActive", item.isActive)
                }
                arr.put(obj)
            }
            prefs.edit().putString("payment_methods_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addOrUpdatePaymentMethod(item: PaymentMethodItem) {
        val current = _paymentMethodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        _paymentMethodsFlow.value = current
        savePaymentMethodsLocally(current)

        // Sync to Firebase Realtime Database and Firestore
        try {
            adminDbRef?.child("payment_methods")?.child(item.id)?.setValue(item.toMap())
            firestore?.collection("payment_methods")?.document(item.id)?.set(item.toMap(), SetOptions.merge())
        } catch (_: Exception) {}
    }

    fun togglePaymentMethod(id: String) {
        val current = _paymentMethodsFlow.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
        _paymentMethodsFlow.value = current
        savePaymentMethodsLocally(current)

        val updatedItem = current.firstOrNull { it.id == id }
        if (updatedItem != null) {
            try {
                adminDbRef?.child("payment_methods")?.child(id)?.child("isActive")?.setValue(updatedItem.isActive)
                firestore?.collection("payment_methods")?.document(id)?.update("isActive", updatedItem.isActive)
            } catch (_: Exception) {}
        }
    }

    fun deletePaymentMethod(id: String) {
        val current = _paymentMethodsFlow.value.filter { it.id != id }
        _paymentMethodsFlow.value = current
        savePaymentMethodsLocally(current)

        try {
            adminDbRef?.child("payment_methods")?.child(id)?.removeValue()
            firestore?.collection("payment_methods")?.document(id)?.delete()
        } catch (_: Exception) {}
    }

    // =========================================================================
    // 2. DEPOSIT REQUESTS (Firebase RTDB + Firestore + Local Cache)
    // =========================================================================

    private fun listenToFirebaseDepositRequests() {
        try {
            adminDbRef?.child("deposits")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<DepositRequestItem>()
                    for (child in snapshot.children) {
                        val item = child.getValue(DepositRequestItem::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.sortedByDescending { it.createdAt }
                    if (sorted.isNotEmpty()) {
                        _depositRequestsFlow.value = sorted
                        saveDepositRequestsLocally(sorted)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "DepositRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun loadDepositRequests(): List<DepositRequestItem> {
        val json = prefs.getString("deposit_requests_json", null) ?: return emptyList()
        val list = mutableListOf<DepositRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DepositRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        amount = obj.optDouble("amount", 0.0),
                        methodName = obj.optString("methodName", ""),
                        senderNumber = obj.optString("senderNumber", ""),
                        transactionId = obj.optString("transactionId", ""),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveDepositRequestsLocally(list: List<DepositRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("amount", item.amount)
                    put("methodName", item.methodName)
                    put("senderNumber", item.senderNumber)
                    put("transactionId", item.transactionId)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("deposit_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitDepositRequest(
        userId: String,
        userName: String,
        userEmail: String,
        amount: Double,
        methodName: String,
        senderNumber: String,
        transactionId: String
    ): DepositRequestItem {
        val item = DepositRequestItem(
            id = "dep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            amount = amount,
            methodName = methodName,
            senderNumber = senderNumber,
            transactionId = transactionId,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _depositRequestsFlow.value.filter { it.id != item.id }
        _depositRequestsFlow.value = updated
        saveDepositRequestsLocally(updated)

        // Save directly to Firebase Realtime Database & Firestore
        try {
            adminDbRef?.child("deposits")?.child(item.id)?.setValue(item.toMap())
            firestore?.collection("deposit_requests")?.document(item.id)?.set(item.toMap(), SetOptions.merge())
        } catch (_: Exception) {}

        // Record a single PENDING transaction in wallet repository so user sees it as Pending
        try {
            val walletRepo = WalletRepository.getInstance(context)
            walletRepo.recordPendingDeposit(
                requestId = item.id,
                amount = amount,
                method = methodName,
                trxId = transactionId,
                senderNumber = senderNumber
            )
        } catch (_: Exception) {}

        return item
    }

    fun approveDepositRequest(requestId: String, walletRepo: WalletRepository): Boolean {
        val current = _depositRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _depositRequestsFlow.value = current
        saveDepositRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("deposits")?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
            firestore?.collection("deposit_requests")?.document(requestId)?.update("status", "APPROVED")
        } catch (_: Exception) {}

        // Update the existing pending transaction to COMPLETED (single card updated, no duplicate!)
        walletRepo.approvePendingDeposit(
            depositId = req.id,
            amount = req.amount,
            method = req.methodName,
            trxId = req.transactionId
        )
        return true
    }

    fun rejectDepositRequest(requestId: String, adminNote: String = ""): Boolean {
        val current = _depositRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _depositRequestsFlow.value = current
        saveDepositRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("deposits")?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
            firestore?.collection("deposit_requests")?.document(requestId)?.update(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Update the existing pending transaction to REJECTED
        try {
            val walletRepo = WalletRepository.getInstance(context)
            walletRepo.rejectPendingDeposit(
                depositId = req.id,
                reason = adminNote,
                trxId = req.transactionId
            )
        } catch (_: Exception) {}
        return true
    }

    // =========================================================================
    // 3. WITHDRAW REQUESTS (Firebase RTDB + Firestore + Local Cache)
    // =========================================================================

    private fun listenToFirebaseWithdrawRequests() {
        try {
            adminDbRef?.child("withdraws")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<WithdrawRequestItem>()
                    for (child in snapshot.children) {
                        val item = child.getValue(WithdrawRequestItem::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.sortedByDescending { it.createdAt }
                    if (sorted.isNotEmpty()) {
                        _withdrawRequestsFlow.value = sorted
                        saveWithdrawRequestsLocally(sorted)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "WithdrawRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun loadWithdrawRequests(): List<WithdrawRequestItem> {
        val json = prefs.getString("withdraw_requests_json", null) ?: return emptyList()
        val list = mutableListOf<WithdrawRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WithdrawRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        amount = obj.optDouble("amount", 0.0),
                        methodName = obj.optString("methodName", ""),
                        accountNumber = obj.optString("accountNumber", ""),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveWithdrawRequestsLocally(list: List<WithdrawRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("amount", item.amount)
                    put("methodName", item.methodName)
                    put("accountNumber", item.accountNumber)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("withdraw_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitWithdrawRequest(
        userId: String,
        userName: String,
        userEmail: String,
        amount: Double,
        methodName: String,
        accountNumber: String
    ): WithdrawRequestItem {
        val item = WithdrawRequestItem(
            id = "wth_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            amount = amount,
            methodName = methodName,
            accountNumber = accountNumber,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _withdrawRequestsFlow.value.filter { it.id != item.id }
        _withdrawRequestsFlow.value = updated
        saveWithdrawRequestsLocally(updated)

        // Save directly to Firebase Realtime Database & Firestore
        try {
            adminDbRef?.child("withdraws")?.child(item.id)?.setValue(item.toMap())
            firestore?.collection("withdraw_requests")?.document(item.id)?.set(item.toMap(), SetOptions.merge())
        } catch (_: Exception) {}

        return item
    }

    fun approveWithdrawRequest(requestId: String): Boolean {
        val current = _withdrawRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _withdrawRequestsFlow.value = current
        saveWithdrawRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("withdraws")?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
            firestore?.collection("withdraw_requests")?.document(requestId)?.update("status", "APPROVED")
        } catch (_: Exception) {}

        return true
    }

    fun rejectWithdrawRequest(requestId: String, walletRepo: WalletRepository, adminNote: String = ""): Boolean {
        val current = _withdrawRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _withdrawRequestsFlow.value = current
        saveWithdrawRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("withdraws")?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
            firestore?.collection("withdraw_requests")?.document(requestId)?.update(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Refund money back to user's wallet!
        walletRepo.recharge(req.amount, "Withdrawal Refund (${req.methodName} rejected)")
        return true
    }

    // =========================================================================
    // 4. VERIFICATION REQUESTS (Firebase RTDB + Firestore + Local Cache)
    // =========================================================================

    private fun listenToFirebaseVerificationRequests() {
        try {
            adminDbRef?.child("verifications")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<VerificationRequestItem>()
                    for (child in snapshot.children) {
                        val item = child.getValue(VerificationRequestItem::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.sortedByDescending { it.createdAt }
                    if (sorted.isNotEmpty()) {
                        _verificationRequestsFlow.value = sorted
                        saveVerificationRequestsLocally(sorted)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "VerificationRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun loadVerificationRequests(): List<VerificationRequestItem> {
        val json = prefs.getString("verification_requests_json", null) ?: return emptyList()
        val list = mutableListOf<VerificationRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    VerificationRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        userPhone = obj.optString("userPhone", ""),
                        planTitle = obj.optString("planTitle", ""),
                        durationDays = obj.optInt("durationDays", 30),
                        price = obj.optDouble("price", 0.0),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveVerificationRequestsLocally(list: List<VerificationRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("userPhone", item.userPhone)
                    put("planTitle", item.planTitle)
                    put("durationDays", item.durationDays)
                    put("price", item.price)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("verification_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitVerificationRequest(
        userId: String,
        userName: String,
        userEmail: String,
        userPhone: String,
        planTitle: String,
        durationDays: Int,
        price: Double
    ): VerificationRequestItem {
        val item = VerificationRequestItem(
            id = "ver_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            userPhone = userPhone,
            planTitle = planTitle,
            durationDays = durationDays,
            price = price,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _verificationRequestsFlow.value.filter { it.id != item.id }
        _verificationRequestsFlow.value = updated
        saveVerificationRequestsLocally(updated)

        // Save directly to Firebase Realtime Database & Firestore
        try {
            adminDbRef?.child("verifications")?.child(item.id)?.setValue(item.toMap())
            firestore?.collection("verification_requests")?.document(item.id)?.set(item.toMap(), SetOptions.merge())
        } catch (_: Exception) {}

        return item
    }

    fun approveVerificationRequest(
        requestId: String,
        userRepo: UserRepository
    ): Boolean {
        val current = _verificationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _verificationRequestsFlow.value = current
        saveVerificationRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("verifications")?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
            firestore?.collection("verification_requests")?.document(requestId)?.update("status", "APPROVED")
        } catch (_: Exception) {}

        // Calculate verified until
        val durationMs = req.durationDays * 24L * 60L * 60L * 1000L
        val verifiedUntil = System.currentTimeMillis() + durationMs

        // Grant persistent verification across DB and Cache
        userRepo.savePersistentVerification(
            uid = req.userId,
            email = req.userEmail,
            phone = req.userPhone,
            verifiedUntil = verifiedUntil,
            verificationType = "GREEN_BADGE",
            planTitle = req.planTitle
        )

        return true
    }

    fun rejectVerificationRequest(
        requestId: String,
        walletRepo: WalletRepository,
        userRepo: UserRepository,
        adminNote: String = ""
    ): Boolean {
        val current = _verificationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _verificationRequestsFlow.value = current
        saveVerificationRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("verifications")?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
            firestore?.collection("verification_requests")?.document(requestId)?.update(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Revoke any active verification for this user
        userRepo.revokePersistentVerification(
            uid = req.userId,
            email = req.userEmail,
            phone = req.userPhone
        )

        // Refund money back to user's wallet!
        if (req.price > 0) {
            walletRepo.recharge(req.price, "Verification Refund (${req.planTitle} rejected)")
        }

        return true
    }

    // =========================================================================
    // 5. MONETIZATION REQUESTS (Firebase RTDB + Firestore + Local Cache)
    // =========================================================================

    private fun listenToFirebaseMonetizationRequests() {
        try {
            adminDbRef?.child("monetizations")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<MonetizationRequestItem>()
                    for (child in snapshot.children) {
                        val item = child.getValue(MonetizationRequestItem::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.sortedByDescending { it.createdAt }
                    if (sorted.isNotEmpty()) {
                        _monetizationRequestsFlow.value = sorted
                        saveMonetizationRequestsLocally(sorted)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "MonetizationRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun loadMonetizationRequests(): List<MonetizationRequestItem> {
        val json = prefs.getString("monetization_requests_json", null) ?: return emptyList()
        val list = mutableListOf<MonetizationRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MonetizationRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        viewsCount = obj.optInt("viewsCount", 0),
                        followersCount = obj.optInt("followersCount", 0),
                        postsCount = obj.optInt("postsCount", 0),
                        reelsCount = obj.optInt("reelsCount", 0),
                        accountAgeDays = obj.optInt("accountAgeDays", 0),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveMonetizationRequestsLocally(list: List<MonetizationRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("viewsCount", item.viewsCount)
                    put("followersCount", item.followersCount)
                    put("postsCount", item.postsCount)
                    put("reelsCount", item.reelsCount)
                    put("accountAgeDays", item.accountAgeDays)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("monetization_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitMonetizationRequest(
        userId: String,
        userName: String,
        userEmail: String,
        viewsCount: Int,
        followersCount: Int,
        postsCount: Int,
        reelsCount: Int,
        accountAgeDays: Int
    ): MonetizationRequestItem {
        // Mark applied in shared prefs
        context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("monetization_status_$userId", "PENDING")
            .apply()

        val item = MonetizationRequestItem(
            id = "mon_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            viewsCount = viewsCount,
            followersCount = followersCount,
            postsCount = postsCount,
            reelsCount = reelsCount,
            accountAgeDays = accountAgeDays,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _monetizationRequestsFlow.value.filter { it.userId != userId || it.status != "PENDING" }
        _monetizationRequestsFlow.value = updated
        saveMonetizationRequestsLocally(updated)

        // Save directly to Firebase Realtime Database & Firestore
        try {
            adminDbRef?.child("monetizations")?.child(item.id)?.setValue(item.toMap())
            firestore?.collection("monetization_requests")?.document(item.id)?.set(item.toMap(), SetOptions.merge())
        } catch (_: Exception) {}

        return item
    }

    fun getMonetizationStatusForUser(userId: String): String {
        if (userId.isBlank()) return "NONE"
        val fundPrefs = context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
        val explicitStatus = fundPrefs.getString("monetization_status_$userId", null)
        if (!explicitStatus.isNullOrBlank()) return explicitStatus

        val req = _monetizationRequestsFlow.value.firstOrNull { it.userId == userId }
        return req?.status ?: "NONE"
    }

    fun approveMonetizationRequest(requestId: String): Boolean {
        val current = _monetizationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _monetizationRequestsFlow.value = current
        saveMonetizationRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("monetizations")?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
            firestore?.collection("monetization_requests")?.document(requestId)?.update("status", "APPROVED")
        } catch (_: Exception) {}

        // Set user status to APPROVED
        context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("monetization_status_${req.userId}", "APPROVED")
            .apply()

        return true
    }

    fun rejectMonetizationRequest(requestId: String, adminNote: String = ""): Boolean {
        val current = _monetizationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _monetizationRequestsFlow.value = current
        saveMonetizationRequestsLocally(current)

        // Sync to Firebase RTDB and Firestore
        try {
            adminDbRef?.child("monetizations")?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
            firestore?.collection("monetization_requests")?.document(requestId)?.update(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Set user status to REJECTED
        context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("monetization_status_${req.userId}", "REJECTED")
            .apply()

        return true
    }

    companion object {
        @Volatile
        private var instance: AdminRequestRepository? = null

        fun getInstance(context: Context): AdminRequestRepository {
            return instance ?: synchronized(this) {
                instance ?: AdminRequestRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
