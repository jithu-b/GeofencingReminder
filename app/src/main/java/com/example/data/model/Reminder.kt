package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 500f,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val category: String = "General",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
) {
    val geofenceRequestId: String
        get() = "georemind_$id"

    val formattedRadius: String
        get() = when {
            radiusMeters >= 1000f -> "${(radiusMeters / 1000f).toInt()} km"
            else -> "${radiusMeters.toInt()} m"
        }
}
