package com.example.data.repository

import android.content.Context
import com.example.data.local.ReminderDao
import com.example.data.model.Reminder
import com.example.geofence.GeofenceManager
import com.example.geofence.NotificationHelper
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val geofenceManager: GeofenceManager,
    private val notificationHelper: NotificationHelper
) {
    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveReminders()
    val completedReminders: Flow<List<Reminder>> = reminderDao.getCompletedReminders()
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    fun getReminderById(id: Long): Flow<Reminder?> = reminderDao.getReminderFlowById(id)

    suspend fun addReminder(reminder: Reminder): Long {
        val id = reminderDao.insertReminder(reminder)
        val saved = reminder.copy(id = id)
        if (saved.isActive && !saved.isCompleted) {
            geofenceManager.registerGeofence(saved)
        }
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
        if (reminder.isActive && !reminder.isCompleted) {
            geofenceManager.registerGeofence(reminder)
        } else {
            geofenceManager.removeGeofence(reminder)
        }
    }

    suspend fun setReminderActive(id: Long, isActive: Boolean) {
        reminderDao.setReminderActive(id, isActive)
        val reminder = reminderDao.getReminderById(id)
        if (reminder != null) {
            if (isActive && !reminder.isCompleted) {
                geofenceManager.registerGeofence(reminder)
            } else {
                geofenceManager.removeGeofence(reminder)
            }
        }
    }

    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        reminderDao.setReminderCompleted(id, isCompleted)
        val reminder = reminderDao.getReminderById(id)
        if (reminder != null) {
            if (isCompleted) {
                geofenceManager.removeGeofence(reminder)
            } else if (reminder.isActive) {
                geofenceManager.registerGeofence(reminder)
            }
        }
    }

    suspend fun deleteReminder(reminder: Reminder) {
        geofenceManager.removeGeofence(reminder)
        reminderDao.deleteReminder(reminder)
    }

    suspend fun clearCompleted() {
        reminderDao.clearCompleted()
    }

    suspend fun simulateGeofenceTrigger(reminder: Reminder) {
        // Triggers the real notification and records the trigger event
        notificationHelper.showGeofenceAlert(reminder)
        reminderDao.updateTriggeredTimestamp(reminder.id, System.currentTimeMillis())
    }

    suspend fun registerAllActiveGeofences() {
        val candidates = reminderDao.getActiveGeofenceCandidates()
        for (reminder in candidates) {
            geofenceManager.registerGeofence(reminder)
        }
    }
}
