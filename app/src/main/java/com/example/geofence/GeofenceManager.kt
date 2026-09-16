package com.example.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.Reminder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    companion object {
        private const val TAG = "GeofenceManager"
    }

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerGeofence(reminder: Reminder, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot register geofence: ACCESS_FINE_LOCATION not granted")
            onResult(false, "Location permission is required to register geofence")
            return
        }

        try {
            val geofence = Geofence.Builder()
                .setRequestId(reminder.geofenceRequestId)
                .setCircularRegion(
                    reminder.latitude,
                    reminder.longitude,
                    reminder.radiusMeters
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered geofence for ${reminder.title} at ${reminder.placeName}")
                    onResult(true, null)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to register geofence for ${reminder.title}", exception)
                    onResult(false, exception.localizedMessage)
                }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException registering geofence", se)
            onResult(false, se.localizedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Exception registering geofence", e)
            onResult(false, e.localizedMessage)
        }
    }

    fun removeGeofence(reminder: Reminder, onResult: (Boolean) -> Unit = {}) {
        geofencingClient.removeGeofences(listOf(reminder.geofenceRequestId))
            .addOnSuccessListener {
                Log.d(TAG, "Removed geofence for ${reminder.geofenceRequestId}")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence for ${reminder.geofenceRequestId}", e)
                onResult(false)
            }
    }

    fun removeAllGeofences(onResult: (Boolean) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
