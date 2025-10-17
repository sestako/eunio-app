package com.eunio.healthapp.platform.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * BroadcastReceiver that handles scheduled notification alarms on Android.
 * This receiver is triggered by AlarmManager when it's time to show a notification.
 */
class NotificationReceiver : BroadcastReceiver() {
    
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notification_id") ?: return
        val title = intent.getStringExtra("notification_title") ?: "Health Reminder"
        val body = intent.getStringExtra("notification_body") ?: "Don't forget to log your health data"
        
        showNotification(context, notificationId, title, body)
    }
    
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(context: Context, id: String, title: String, body: String) {
        try {
            val channelId = getChannelIdForNotification(id)
            val notificationManager = NotificationManagerCompat.from(context)
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(id.hashCode(), notification)
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }
    
    private fun getChannelIdForNotification(id: String): String {
        return when {
            id.contains("daily_logging") -> "health_tracking"
            id.contains("period") || id.contains("ovulation") -> "cycle_tracking"
            id.contains("insight") -> "insights"
            else -> "health_tracking"
        }
    }
}