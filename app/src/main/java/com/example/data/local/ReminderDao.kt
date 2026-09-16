package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderFlowById(id: Long): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND isCompleted = 0")
    suspend fun getActiveGeofenceCandidates(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isActive = :isActive WHERE id = :id")
    suspend fun setReminderActive(id: Long, isActive: Boolean)

    @Query("UPDATE reminders SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean)

    @Query("UPDATE reminders SET lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun updateTriggeredTimestamp(id: Long, timestamp: Long)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun clearCompleted()
}
