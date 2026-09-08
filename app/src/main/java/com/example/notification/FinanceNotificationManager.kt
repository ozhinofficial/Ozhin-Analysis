package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.SubscriptionEntity
import com.example.util.CurrencyManager

class FinanceNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_BILLS_ID = "channel_bills_reminders"
        const val CHANNEL_BALANCE_ID = "channel_balance_warnings"
        const val NOTIFICATION_ID_BILL = 1001
        const val NOTIFICATION_ID_BALANCE = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // Channel 1: Upcoming Bill Due Reminders
            val billsChannel = NotificationChannel(
                CHANNEL_BILLS_ID,
                "Upcoming Bill Due Dates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notification alerts for upcoming subscription and recurring bill due dates"
                enableVibration(true)
            }

            // Channel 2: Account Balance Warnings
            val balanceChannel = NotificationChannel(
                CHANNEL_BALANCE_ID,
                "Account Balance Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notifications for low account balance alerts"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(billsChannel)
            notificationManager.createNotificationChannel(balanceChannel)
        }
    }

    fun notifyUpcomingBill(
        subscriptionName: String,
        amountFormatted: String,
        daysUntilDue: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dueMessage = when {
            daysUntilDue <= 0 -> "is due TODAY!"
            daysUntilDue == 1L -> "is due tomorrow ($amountFormatted)"
            else -> "is due in $daysUntilDue days ($amountFormatted)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_BILLS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Upcoming Bill Alert: $subscriptionName")
            .setContentText("$subscriptionName $dueMessage. Tap to review.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Reminder: Recurring payment for $subscriptionName ($amountFormatted) $dueMessage. Keep sufficient balance.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BILL + subscriptionName.hashCode(),
                builder.build()
            )
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun notifyBalanceWarning(currentBalanceFormatted: String, thresholdFormatted: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_BALANCE_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Low Account Balance Warning")
            .setContentText("Your current balance ($currentBalanceFormatted) is below your safety threshold of $thresholdFormatted.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Attention: Your total balance is now $currentBalanceFormatted, which is below your designated safe limit of $thresholdFormatted. Review your recent spending.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BALANCE, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun checkAndAlertBills(
        subscriptions: List<SubscriptionEntity>,
        baseCurrency: String
    ) {
        val now = System.currentTimeMillis()
        subscriptions.filter { it.isActive }.forEach { sub ->
            val diffMillis = sub.nextDueDate - now
            val days = (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            if (days <= sub.reminderDaysBefore) {
                val convertedAmount = CurrencyManager.convert(sub.amount, sub.currency, baseCurrency)
                val formatted = CurrencyManager.formatAmount(convertedAmount, baseCurrency)
                notifyUpcomingBill(sub.name, formatted, days)
            }
        }
    }

    fun checkBalance(
        currentBalanceInBase: Double,
        thresholdInBase: Double,
        baseCurrency: String
    ) {
        if (currentBalanceInBase < thresholdInBase) {
            val balanceStr = CurrencyManager.formatAmount(currentBalanceInBase, baseCurrency)
            val thresholdStr = CurrencyManager.formatAmount(thresholdInBase, baseCurrency)
            notifyBalanceWarning(balanceStr, thresholdStr)
        }
    }
}
