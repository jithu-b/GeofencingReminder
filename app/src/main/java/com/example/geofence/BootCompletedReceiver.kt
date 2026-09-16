package com.example.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.GeoRemindDatabase
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Geofences registered via GeofencingClient do not survive a device reboot,
 * so we re-add them for every active reminder once the system finishes booting.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val geofenceManager = GeofenceManager(context)
        if (!geofenceManager.hasLocationPermission()) return

        val database = GeoRemindDatabase.getInstance(context)
        val repository = ReminderRepository(
            reminderDao = database.reminderDao(),
            geofenceManager = geofenceManager,
            notificationHelper = NotificationHelper(context)
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.registerAllActiveGeofences()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
