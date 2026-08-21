package com.makhp.pelukdiri.core.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.makhp.pelukdiri.MainActivity
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.components.formatDuration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "daily_usage_status_channel"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_daily_usage_channel_name)
            val descriptionText = context.getString(R.string.notification_daily_usage_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateDailyUsageNotification(totalUsageMillis: Long, adaptiveLimitMinutes: Int?) {
        val usageStr = formatDuration(totalUsageMillis)
        val limitStr = if (adaptiveLimitMinutes != null) {
            formatDuration(adaptiveLimitMinutes * 60_000L)
        } else {
            context.getString(R.string.notification_insufficient_data)
        }

        val usageMessage = context.getString(R.string.notification_usage_label, usageStr)
        val limitMessage = context.getString(R.string.notification_limit_label, limitStr)
        val contentText = "$usageMessage\n$limitMessage"

        val notification = createNotificationBuilder(CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true) // Status notification should stay there
            .build()

        notify(NOTIFICATION_ID, notification)
    }

    fun showDailySummaryNotification(totalUsageMillis: Long) {
        val usageStr = formatDuration(totalUsageMillis)
        val templates = listOf(
            "Your day at a glance 🌱",
            "Here's your screen-time summary.",
            "Take a moment to check today's screen time.",
            "See how your day went."
        )
        val title = templates.random()
        val contentText = context.getString(R.string.notification_usage_label, usageStr)

        val notification = createNotificationBuilder(CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notify(Random.nextInt(), notification)
    }

    fun showWeeklyReflectionNotification() {
        val templates = listOf(
            "Time for your weekly reflection.",
            "See how your screen-time habits changed this week.",
            "Your weekly progress is ready to review.",
            "Take a look at your progress this week."
        )
        val title = templates.random()
        val contentText = "Tap to review your weekly digital wellbeing progress."

        val notification = createNotificationBuilder(CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notify(Random.nextInt(), notification)
    }

    fun showLimitReminderNotification() {
        val templates = listOf(
            "You're getting close to your screen-time limit.",
            "You're approaching today's adaptive limit.",
            "Your screen-time limit is getting closer.",
            "Take a short pause and check your screen time."
        )
        val title = templates.random()

        val notification = createNotificationBuilder(CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Mindful usage is key. Take a break if needed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notify(Random.nextInt(), notification)
    }

    fun showInterventionReminderNotification() {
        val templates = listOf(
            "Your next cognitive challenge is ready.",
            "Take a moment for a quick mental challenge.",
            "Ready for a short challenge?",
            "A quick challenge is waiting for you."
        )
        val title = templates.random()

        val notification = createNotificationBuilder(CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("A quick exercise to help you stay focused.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notify(Random.nextInt(), notification)
    }

    private fun createNotificationBuilder(channelId: String): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            android.util.Log.d("NotificationHelper", "notify: id=$id, permissionGranted=$granted")
            if (granted) {
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        } else {
            android.util.Log.d("NotificationHelper", "notify: id=$id (Legacy API)")
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
