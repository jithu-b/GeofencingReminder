package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Coordinates(val latitude: Double, val longitude: Double)

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Coordinates? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        continuation.resume(Coordinates(loc.latitude, loc.longitude))
                    } else {
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                continuation.resume(Coordinates(lastLoc.latitude, lastLoc.longitude))
                            } else {
                                continuation.resume(null)
                            }
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    companion object {
        fun calculateDistanceMeters(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLng, endLat, endLng, results)
            return results[0]
        }

        fun formatDistance(meters: Float, geofenceRadius: Float): String {
            val distStr = if (meters >= 1000f) {
                String.format("%.1f km", meters / 1000f)
            } else {
                "${meters.toInt()} m"
            }

            return if (meters <= geofenceRadius) {
                "Inside geofence ($distStr away)"
            } else {
                "$distStr away"
            }
        }
    }
}
