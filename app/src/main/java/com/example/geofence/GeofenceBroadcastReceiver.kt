package com.example.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.GeoRemindDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"

        // Avoid re-notifying for GPS-jitter re-triggers of the same geofence.
        private const val COOLDOWN_MS = 5 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error code: ${geofencingEvent.errorCode}, message: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
            val notificationHelper = NotificationHelper(context)
            val database = GeoRemindDatabase.getInstance(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (geofence in triggeringGeofences) {
                        val requestId = geofence.requestId
                        // Format: "georemind_ID"
                        val reminderId = requestId.removePrefix("georemind_").toLongOrNull()
                        if (reminderId != null) {
                            val reminder = database.reminderDao().getReminderById(reminderId)
                            val sinceLastTrigger = reminder?.lastTriggeredAt?.let {
                                System.currentTimeMillis() - it
                            }
                            val onCooldown = sinceLastTrigger != null && sinceLastTrigger < COOLDOWN_MS

                            if (reminder != null && reminder.isActive && !reminder.isCompleted && !onCooldown) {
                                notificationHelper.showGeofenceAlert(reminder)
                                database.reminderDao().updateTriggeredTimestamp(
                                    reminderId,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling geofence transition", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
