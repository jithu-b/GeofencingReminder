package com.example.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Reminder

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "georemind_proximity_alerts"
        const val CHANNEL_NAME = "Proximity Reminders"
        const val CHANNEL_DESC = "Triggers when you enter the geofence of a saved reminder"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showGeofenceAlert(reminder: Reminder) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminder.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "You're near your reminder: ${reminder.title}"
        val content = "${reminder.placeName} is within your ${reminder.formattedRadius} radius. Don't forget!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("GeoRemind • Near ${reminder.title}")
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "You're near your reminder.\n\n" +
                        "📍 ${reminder.title}\n" +
                        "${reminder.placeName} is within your ${reminder.formattedRadius} radius.\n\n" +
                        "Don't forget!"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(reminder.id.toInt(), notification)
    }
}
