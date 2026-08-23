package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class WalletTxItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String,
    val amount: Double,
    val isPositive: Boolean,
    val balanceAfter: Double,
    val status: String = "COMPLETED", // "PENDING", "COMPLETED", "REJECTED"
    val referenceId: String = "",
    val method: String = "",
    val trxId: String = ""
)

class WalletRepository(context: Context) {
    private val prefs = context.getSharedPreferences("frndom_wallet_prefs", Context.MODE_PRIVATE)

    private val _balanceFlow = MutableStateFlow(prefs.getFloat(KEY_BALANCE, 0.0f).toDouble())
    val balanceFlow: StateFlow<Double> = _balanceFlow.asStateFlow()

    private val _totalInFlow = MutableStateFlow(prefs.getFloat(KEY_TOTAL_IN, 0.0f).toDouble())
    val totalInFlow: StateFlow<Double> = _totalInFlow.asStateFlow()

    private val _totalOutFlow = MutableStateFlow(prefs.getFloat(KEY_TOTAL_OUT, 0.0f).toDouble())
    val totalOutFlow: StateFlow<Double> = _totalOutFlow.asStateFlow()

    private val _transactionsFlow = MutableStateFlow<List<WalletTxItem>>(loadTransactions())
    val transactionsFlow: StateFlow<List<WalletTxItem>> = _transactionsFlow.asStateFlow()

    init {
        // Clean any old demo transactions if present
        val cleanList = _transactionsFlow.value.filter { it.id != "tx_init_1" }
        if (cleanList.size != _transactionsFlow.value.size) {
            saveTransactions(cleanList)
            _transactionsFlow.value = cleanList
        }
    }

    private fun loadTransactions(): List<WalletTxItem> {
        val json = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val list = mutableListOf<WalletTxItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WalletTxItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Transaction"),
                        subtitle = obj.optString("subtitle", ""),
                        date = obj.optString("date", ""),
                        amount = obj.optDouble("amount", 0.0),
                        isPositive = obj.optBoolean("isPositive", true),
                        balanceAfter = obj.optDouble("balanceAfter", 0.0),
                        status = obj.optString("status", "COMPLETED"),
                        referenceId = obj.optString("referenceId", ""),
                        method = obj.optString("method", ""),
                        trxId = obj.optString("trxId", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    private fun saveTransactions(list: List<WalletTxItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("subtitle", item.subtitle)
                    put("date", item.date)
                    put("amount", item.amount)
                    put("isPositive", item.isPositive)
                    put("balanceAfter", item.balanceAfter)
                    put("status", item.status)
                    put("referenceId", item.referenceId)
                    put("method", item.method)
                    put("trxId", item.trxId)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_TRANSACTIONS, arr.toString()).apply()
        } catch (e: Exception) {
            // fallback
        }
    }

    fun recordPendingDeposit(
        requestId: String,
        amount: Double,
        method: String,
        trxId: String,
        senderNumber: String
    ): WalletTxItem {
        val tx = WalletTxItem(
            id = requestId,
            title = "Deposit ($method)",
            subtitle = "TrxID: $trxId • $senderNumber (Pending)",
            date = getFormattedDate(System.currentTimeMillis()),
            amount = amount,
            isPositive = true,
            balanceAfter = _balanceFlow.value,
            status = "PENDING",
            referenceId = requestId,
            method = method,
            trxId = trxId
        )

        val updated = listOf(tx) + _transactionsFlow.value.filter { it.id != requestId && it.referenceId != requestId }
        _transactionsFlow.value = updated
        saveTransactions(updated)
        return tx
    }

    fun approvePendingDeposit(
        depositId: String,
        amount: Double,
        method: String = "",
        trxId: String = ""
    ): Boolean {
        val newBal = _balanceFlow.value + amount
        val newTotalIn = _totalInFlow.value + amount
        _balanceFlow.value = newBal
        _totalInFlow.value = newTotalIn

        prefs.edit()
            .putFloat(KEY_BALANCE, newBal.toFloat())
            .putFloat(KEY_TOTAL_IN, newTotalIn.toFloat())
            .apply()

        val currentList = _transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.id == depositId ||
            (it.referenceId.isNotBlank() && it.referenceId == depositId) ||
            (trxId.isNotBlank() && it.trxId == trxId) ||
            (trxId.isNotBlank() && it.subtitle.contains(trxId, ignoreCase = true))
        }

        val completedSubtitle = if (trxId.isNotBlank()) {
            "$method deposit (Completed • TrxID: $trxId)".trim()
        } else {
            "$method deposit (Completed)".trim()
        }

        if (index >= 0) {
            val oldItem = currentList[index]
            val updatedItem = oldItem.copy(
                title = if (method.isNotBlank()) "Deposit ($method)" else oldItem.title,
                subtitle = completedSubtitle,
                date = getFormattedDate(System.currentTimeMillis()),
                amount = amount,
                isPositive = true,
                balanceAfter = newBal,
                status = "COMPLETED",
                method = if (method.isNotBlank()) method else oldItem.method,
                trxId = if (trxId.isNotBlank()) trxId else oldItem.trxId
            )
            currentList[index] = updatedItem
        } else {
            val tx = WalletTxItem(
                id = depositId.ifBlank { UUID.randomUUID().toString() },
                title = if (method.isNotBlank()) "Deposit ($method)" else "Recharge",
                subtitle = completedSubtitle,
                date = getFormattedDate(System.currentTimeMillis()),
                amount = amount,
                isPositive = true,
                balanceAfter = newBal,
                status = "COMPLETED",
                referenceId = depositId,
                method = method,
                trxId = trxId
            )
            currentList.add(0, tx)
        }

        _transactionsFlow.value = currentList
        saveTransactions(currentList)
        return true
    }

    fun rejectPendingDeposit(
        depositId: String,
        reason: String = "",
        trxId: String = ""
    ): Boolean {
        val currentList = _transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.id == depositId ||
            (it.referenceId.isNotBlank() && it.referenceId == depositId) ||
            (trxId.isNotBlank() && it.trxId == trxId) ||
            (trxId.isNotBlank() && it.subtitle.contains(trxId, ignoreCase = true))
        }

        if (index >= 0) {
            val oldItem = currentList[index]
            val rejectedSubtitle = if (reason.isNotBlank()) {
                "Rejected: $reason"
            } else {
                "Deposit Rejected by Admin"
            }
            val updatedItem = oldItem.copy(
                subtitle = rejectedSubtitle,
                status = "REJECTED"
            )
            currentList[index] = updatedItem
            _transactionsFlow.value = currentList
            saveTransactions(currentList)
            return true
        }
        return false
    }

    fun recharge(amount: Double, method: String): Boolean {
        if (amount <= 0) return false
        val newBal = _balanceFlow.value + amount
        val newTotalIn = _totalInFlow.value + amount
        _balanceFlow.value = newBal
        _totalInFlow.value = newTotalIn

        prefs.edit()
            .putFloat(KEY_BALANCE, newBal.toFloat())
            .putFloat(KEY_TOTAL_IN, newTotalIn.toFloat())
            .apply()

        val tx = WalletTxItem(
            id = UUID.randomUUID().toString(),
            title = "Recharge",
            subtitle = "$method recharge (completed)",
            date = getFormattedDate(System.currentTimeMillis()),
            amount = amount,
            isPositive = true,
            balanceAfter = newBal,
            status = "COMPLETED",
            method = method
        )

        val updated = listOf(tx) + _transactionsFlow.value
        _transactionsFlow.value = updated
        saveTransactions(updated)
        return true
    }

    fun withdraw(amount: Double, method: String, accountNo: String): Boolean {
        if (amount <= 0 || amount > _balanceFlow.value) return false
        val newBal = _balanceFlow.value - amount
        val newTotalOut = _totalOutFlow.value + amount
        _balanceFlow.value = newBal
        _totalOutFlow.value = newTotalOut

        prefs.edit()
            .putFloat(KEY_BALANCE, newBal.toFloat())
            .putFloat(KEY_TOTAL_OUT, newTotalOut.toFloat())
            .apply()

        val tx = WalletTxItem(
            id = UUID.randomUUID().toString(),
            title = "Withdraw",
            subtitle = "$method payout to $accountNo",
            date = getFormattedDate(System.currentTimeMillis()),
            amount = amount,
            isPositive = false,
            balanceAfter = newBal
        )

        val updated = listOf(tx) + _transactionsFlow.value
        _transactionsFlow.value = updated
        saveTransactions(updated)
        return true
    }

    fun deduct(amount: Double, title: String, subtitle: String): Boolean {
        if (amount <= 0 || amount > _balanceFlow.value) return false
        val newBal = _balanceFlow.value - amount
        val newTotalOut = _totalOutFlow.value + amount
        _balanceFlow.value = newBal
        _totalOutFlow.value = newTotalOut

        prefs.edit()
            .putFloat(KEY_BALANCE, newBal.toFloat())
            .putFloat(KEY_TOTAL_OUT, newTotalOut.toFloat())
            .apply()

        val tx = WalletTxItem(
            id = UUID.randomUUID().toString(),
            title = title,
            subtitle = subtitle,
            date = getFormattedDate(System.currentTimeMillis()),
            amount = amount,
            isPositive = false,
            balanceAfter = newBal
        )

        val updated = listOf(tx) + _transactionsFlow.value
        _transactionsFlow.value = updated
        saveTransactions(updated)
        return true
    }

    private fun getFormattedDate(timeMs: Long): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timeMs))
    }

    companion object {
        private const val KEY_BALANCE = "wallet_balance"
        private const val KEY_TOTAL_IN = "wallet_total_in"
        private const val KEY_TOTAL_OUT = "wallet_total_out"
        private const val KEY_TRANSACTIONS = "wallet_transactions"

        @Volatile
        private var instance: WalletRepository? = null

        fun getInstance(context: Context): WalletRepository {
            return instance ?: synchronized(this) {
                instance ?: WalletRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
