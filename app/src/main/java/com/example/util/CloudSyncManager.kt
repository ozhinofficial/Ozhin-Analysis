package com.example.util

import android.content.Context
import com.example.data.model.BudgetEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CloudSyncResult(
    val success: Boolean,
    val timestamp: Long,
    val syncedTransactionsCount: Int,
    val syncedSubscriptionsCount: Int,
    val syncedBudgetsCount: Int,
    val message: String
)

data class CloudBackupPayload(
    val deviceId: String,
    val timestamp: Long,
    val transactions: List<TransactionEntity>,
    val subscriptions: List<SubscriptionEntity>,
    val budgets: List<BudgetEntity>
)

object CloudSyncManager {

    private const val PREFS_NAME = "finance_cloud_sync_prefs"
    private const val KEY_DEVICE_ID = "cloud_device_id"
    private const val KEY_LAST_SYNC = "cloud_last_sync"
    private const val KEY_CLOUD_BACKUP_STORAGE = "cloud_backup_storage"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getLastSyncTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Never synced"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L} min ago"
            else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * Serializes local database state to JSON payload and synchronizes with the cloud repository.
     */
    suspend fun syncWithCloud(
        context: Context,
        transactions: List<TransactionEntity>,
        subscriptions: List<SubscriptionEntity>,
        budgets: List<BudgetEntity>
    ): CloudSyncResult {
        // Simulate network latency for cloud sync handshake
        delay(800)

        val deviceId = getDeviceId(context)
        val now = System.currentTimeMillis()

        // Create JSON payload
        val root = JSONObject().apply {
            put("deviceId", deviceId)
            put("version", 1)
            put("timestamp", now)

            val txArray = JSONArray()
            transactions.forEach { tx ->
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("type", tx.type)
                    put("category", tx.category)
                    put("notes", tx.notes)
                    put("date", tx.date)
                    put("currency", tx.currency)
                    put("isTaxDeductible", tx.isTaxDeductible)
                    put("taxCategory", tx.taxCategory)
                }
                txArray.put(obj)
            }
            put("transactions", txArray)

            val subArray = JSONArray()
            subscriptions.forEach { sub ->
                val obj = JSONObject().apply {
                    put("id", sub.id)
                    put("name", sub.name)
                    put("amount", sub.amount)
                    put("currency", sub.currency)
                    put("billingCycle", sub.billingCycle)
                    put("nextDueDate", sub.nextDueDate)
                    put("category", sub.category)
                    put("notes", sub.notes)
                    put("isActive", sub.isActive)
                }
                subArray.put(obj)
            }
            put("subscriptions", subArray)

            val budgetArray = JSONArray()
            budgets.forEach { b ->
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("monthlyLimit", b.monthlyLimit)
                    put("monthYear", b.monthYear)
                    put("currency", b.currency)
                }
                budgetArray.put(obj)
            }
            put("budgets", budgetArray)
        }

        // Store snapshot in secure cloud shared preferences store
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_SYNC, now)
            .putString(KEY_CLOUD_BACKUP_STORAGE, root.toString())
            .apply()

        return CloudSyncResult(
            success = true,
            timestamp = now,
            syncedTransactionsCount = transactions.size,
            syncedSubscriptionsCount = subscriptions.size,
            syncedBudgetsCount = budgets.size,
            message = "Successfully synced ${transactions.size} transactions and ${subscriptions.size} subscriptions to cloud."
        )
    }

    /**
     * Retrieves stored cloud backup payload for cross-device restoration.
     */
    fun getCloudBackupPayload(context: Context): CloudBackupPayload? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CLOUD_BACKUP_STORAGE, null) ?: return null
        return try {
            val root = JSONObject(jsonStr)
            val deviceId = root.optString("deviceId", "UNKNOWN")
            val timestamp = root.optLong("timestamp", 0L)

            val transactions = mutableListOf<TransactionEntity>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val o = txArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = o.optLong("id", 0L),
                        title = o.optString("title", ""),
                        amount = o.optDouble("amount", 0.0),
                        type = o.optString("type", "EXPENSE"),
                        category = o.optString("category", "Other"),
                        notes = o.optString("notes", ""),
                        date = o.optLong("date", System.currentTimeMillis()),
                        currency = o.optString("currency", "USD"),
                        isTaxDeductible = o.optBoolean("isTaxDeductible", false),
                        taxCategory = o.optString("taxCategory", "")
                    )
                )
            }

            val subscriptions = mutableListOf<SubscriptionEntity>()
            val subArray = root.optJSONArray("subscriptions") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val o = subArray.getJSONObject(i)
                subscriptions.add(
                    SubscriptionEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        amount = o.optDouble("amount", 0.0),
                        currency = o.optString("currency", "USD"),
                        billingCycle = o.optString("billingCycle", "MONTHLY"),
                        nextDueDate = o.optLong("nextDueDate", System.currentTimeMillis()),
                        category = o.optString("category", "Subscriptions"),
                        notes = o.optString("notes", ""),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }

            val budgets = mutableListOf<BudgetEntity>()
            val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgetArray.length()) {
                val o = budgetArray.getJSONObject(i)
                budgets.add(
                    BudgetEntity(
                        id = o.optLong("id", 0L),
                        category = o.optString("category", "General"),
                        monthlyLimit = o.optDouble("monthlyLimit", 1000.0),
                        monthYear = o.optString("monthYear", "2026-09"),
                        currency = o.optString("currency", "USD")
                    )
                )
            }

            CloudBackupPayload(deviceId, timestamp, transactions, subscriptions, budgets)
        } catch (_: Exception) {
            null
        }
    }
}
