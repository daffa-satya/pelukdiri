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

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val LEGACY_CHANNEL_ID = "daily_usage_status_channel"
        private const val STATUS_CHANNEL_ID = "daily_usage_status_channel_v2"
        private const val REMINDER_CHANNEL_ID = "pelukdiri_reminders_channel"
        private const val STATUS_NOTIFICATION_ID = 1001
        private const val DAILY_SUMMARY_NOTIFICATION_ID = 1101
        private const val WEEKLY_REFLECTION_NOTIFICATION_ID = 1102
        private const val LIMIT_REMINDER_NOTIFICATION_ID = 1103
        private const val INTERVENTION_REMINDER_NOTIFICATION_ID = 1104
    }

    init {
        createNotificationChannels()
        removeLegacyNotifications()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val statusChannel = NotificationChannel(
                STATUS_CHANNEL_ID,
                context.getString(R.string.notification_daily_usage_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_daily_usage_channel_description)
            }
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(R.string.notification_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_reminder_channel_description)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(statusChannel, reminderChannel))
        }
    }

    private fun removeLegacyNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.activeNotifications
            .filter { it.notification.channelId == LEGACY_CHANNEL_ID }
            .forEach { notificationManager.cancel(it.tag, it.id) }
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
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

        val notification = createNotificationBuilder(STATUS_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setOngoing(true) // Status notification should stay there
            .build()

        notify(STATUS_NOTIFICATION_ID, notification)
    }

    fun showDailySummaryNotification(totalUsageMillis: Long) {
        val usageStr = formatDuration(totalUsageMillis)
        val templates = listOf(
            R.string.notification_daily_summary_title_1,
            R.string.notification_daily_summary_title_2,
            R.string.notification_daily_summary_title_3,
            R.string.notification_daily_summary_title_4
        )
        val title = context.getString(templates.random())
        val contentText = context.getString(R.string.notification_usage_label, usageStr)

        val notification = createNotificationBuilder(REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
    }

    fun showWeeklyReflectionNotification() {
        val templates = listOf(
            R.string.notification_weekly_reflection_title_1,
            R.string.notification_weekly_reflection_title_2,
            R.string.notification_weekly_reflection_title_3,
            R.string.notification_weekly_reflection_title_4
        )
        val title = context.getString(templates.random())
        val contentText = context.getString(R.string.notification_weekly_reflection_content)

        val notification = createNotificationBuilder(REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notify(WEEKLY_REFLECTION_NOTIFICATION_ID, notification)
    }

    fun showLimitReminderNotification() {
        val templates = listOf(
            R.string.notification_limit_reminder_title_1,
            R.string.notification_limit_reminder_title_2,
            R.string.notification_limit_reminder_title_3,
            R.string.notification_limit_reminder_title_4
        )
        val title = context.getString(templates.random())
        val contentText = context.getString(R.string.notification_limit_reminder_content)

        val notification = createNotificationBuilder(REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notify(LIMIT_REMINDER_NOTIFICATION_ID, notification)
    }

    fun showInterventionReminderNotification() {
        val templates = listOf(
            R.string.notification_intervention_title_1,
            R.string.notification_intervention_title_2,
            R.string.notification_intervention_title_3,
            R.string.notification_intervention_title_4
        )
        val title = context.getString(templates.random())
        val contentText = context.getString(R.string.notification_intervention_content)

        val notification = createNotificationBuilder(REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notify(INTERVENTION_REMINDER_NOTIFICATION_ID, notification)
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
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
