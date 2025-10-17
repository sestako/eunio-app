package com.eunio.healthapp.platform.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import kotlinx.datetime.LocalTime
import java.util.Calendar

/**
 * Android-specific implementation of PlatformNotificationService.
 * Handles Android notification channels, AlarmManager for scheduling,
 * and permission management.
 */
class AndroidNotificationService(
    private val context: Context
) : PlatformNotificationService {
    
    companion object {
        private const val CHANNEL_ID_HEALTH_TRACKING = "health_tracking"
        private const val CHANNEL_ID_CYCLE_TRACKING = "cycle_tracking"
        private const val CHANNEL_ID_INSIGHTS = "insights"
        
        private const val CHANNEL_NAME_HEALTH_TRACKING = "Health Tracking"
        private const val CHANNEL_NAME_CYCLE_TRACKING = "Cycle Tracking"
        private const val CHANNEL_NAME_INSIGHTS = "Health Insights"
        
        private const val CHANNEL_DESC_HEALTH_TRACKING = "Daily logging reminders and health tracking notifications"
        private const val CHANNEL_DESC_CYCLE_TRACKING = "Period and ovulation alerts"
        private const val CHANNEL_DESC_INSIGHTS = "Health insights and pattern notifications"
    }
    
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            createNotificationChannels()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to initialize Android notification service: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun scheduleRepeatingNotification(
        id: String,
        title: String,
        body: String,
        time: LocalTime,
        repeatInterval: RepeatInterval
    ): Result<Unit> {
        return try {
            if (!hasNotificationPermission()) {
                return Result.failure(
                    AppError.PermissionError(
                        message = "Notification permission not granted",
                        requiredPermission = Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
            
            val intent = createNotificationIntent(id, title, body)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            when (repeatInterval) {
                RepeatInterval.DAILY -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }
                RepeatInterval.WEEKLY -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                }
                RepeatInterval.MONTHLY -> {
                    // For monthly, we'll use a custom approach since AlarmManager doesn't have monthly intervals
                    scheduleMonthlyNotification(calendar.timeInMillis, pendingIntent)
                }
                RepeatInterval.NONE -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to schedule repeating notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun scheduleOneTimeNotification(
        id: String,
        title: String,
        body: String,
        triggerTimeMillis: Long
    ): Result<Unit> {
        return try {
            if (!hasNotificationPermission()) {
                return Result.failure(
                    AppError.PermissionError(
                        message = "Notification permission not granted",
                        requiredPermission = Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
            
            val intent = createNotificationIntent(id, title, body)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to schedule one-time notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun cancelNotification(id: String): Result<Unit> {
        return try {
            val intent = createNotificationIntent(id, "", "")
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            notificationManager.cancel(id.hashCode())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to cancel notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            notificationManager.cancelAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to cancel all notifications: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun requestPermission(): Result<Boolean> {
        return try {
            // On Android 13+, we need to request POST_NOTIFICATIONS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    Result.success(true)
                } else {
                    // Permission request needs to be handled by the Activity
                    // This method just checks current status
                    Result.success(false)
                }
            } else {
                // On older Android versions, notifications are enabled by default
                Result.success(areNotificationsEnabled())
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.PermissionError(
                    message = "Failed to request notification permission: ${e.message}",
                    requiredPermission = Manifest.permission.POST_NOTIFICATIONS,
                    cause = e
                )
            )
        }
    }
    
    override suspend fun getPermissionStatus(): NotificationPermissionStatus {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                    PackageManager.PERMISSION_GRANTED -> NotificationPermissionStatus.GRANTED
                    PackageManager.PERMISSION_DENIED -> {
                        // Check if we can still request permission or if it's permanently denied
                        // This would need to be checked in the Activity context
                        NotificationPermissionStatus.DENIED
                    }
                    else -> NotificationPermissionStatus.UNKNOWN
                }
            } else {
                // On older versions, check if notifications are enabled
                if (areNotificationsEnabled()) {
                    NotificationPermissionStatus.GRANTED
                } else {
                    NotificationPermissionStatus.DENIED
                }
            }
        } catch (e: Exception) {
            NotificationPermissionStatus.UNKNOWN
        }
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return try {
            notificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun openNotificationSettings(): Result<Unit> {
        return try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to open notification settings: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showTestNotification(
        id: String,
        title: String,
        body: String
    ): Result<Unit> {
        return try {
            if (!hasNotificationPermission()) {
                return Result.failure(
                    AppError.PermissionError(
                        message = "Notification permission not granted",
                        requiredPermission = Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
            
            val channelId = getChannelIdForNotification(id)
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(id.hashCode(), notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to show test notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun getScheduledNotificationIds(): List<String> {
        // Android doesn't provide a direct way to get scheduled alarms
        // This would need to be tracked separately in app state
        return emptyList()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_HEALTH_TRACKING,
                    CHANNEL_NAME_HEALTH_TRACKING,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC_HEALTH_TRACKING
                },
                NotificationChannel(
                    CHANNEL_ID_CYCLE_TRACKING,
                    CHANNEL_NAME_CYCLE_TRACKING,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC_CYCLE_TRACKING
                },
                NotificationChannel(
                    CHANNEL_ID_INSIGHTS,
                    CHANNEL_NAME_INSIGHTS,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_DESC_INSIGHTS
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun createNotificationIntent(id: String, title: String, body: String): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_id", id)
            putExtra("notification_title", title)
            putExtra("notification_body", body)
        }
    }
    
    private fun getChannelIdForNotification(id: String): String {
        return when {
            id.contains("daily_logging") -> CHANNEL_ID_HEALTH_TRACKING
            id.contains("period") || id.contains("ovulation") -> CHANNEL_ID_CYCLE_TRACKING
            id.contains("insight") -> CHANNEL_ID_INSIGHTS
            else -> CHANNEL_ID_HEALTH_TRACKING
        }
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }
    
    private fun scheduleMonthlyNotification(triggerTime: Long, pendingIntent: PendingIntent) {
        // For monthly notifications, we'll schedule the next occurrence
        // This is a simplified approach - in a real app, you might want to use WorkManager
        val calendar = Calendar.getInstance().apply {
            timeInMillis = triggerTime
            add(Calendar.MONTH, 1)
        }
        
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}