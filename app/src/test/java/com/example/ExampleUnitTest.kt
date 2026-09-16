package com.example

import com.example.data.model.Reminder
import com.example.location.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun reminder_formattedRadius_returns_correct_units() {
        val r100 = Reminder(title = "Test", placeName = "Place", latitude = 0.0, longitude = 0.0, radiusMeters = 100f)
        assertEquals("100 m", r100.formattedRadius)

        val r500 = Reminder(title = "Test", placeName = "Place", latitude = 0.0, longitude = 0.0, radiusMeters = 500f)
        assertEquals("500 m", r500.formattedRadius)

        val r1000 = Reminder(title = "Test", placeName = "Place", latitude = 0.0, longitude = 0.0, radiusMeters = 1000f)
        assertEquals("1 km", r1000.formattedRadius)
    }

    @Test
    fun locationHelper_formatDistance_inside_and_outside() {
        val inside = LocationHelper.formatDistance(150f, 250f)
        assertTrue(inside.contains("Inside geofence"))

        val outside = LocationHelper.formatDistance(800f, 500f)
        assertEquals("800 m away", outside)

        val farOutside = LocationHelper.formatDistance(2400f, 500f)
        assertEquals("2.4 km away", farOutside)
    }

    @Test
    fun reminder_filtering_and_sorting_logic() {
        val r1 = Reminder(id = 1, title = "Haircut", placeName = "ABC Salon", latitude = 37.77, longitude = -122.41, radiusMeters = 500f, isActive = true, isCompleted = false)
        val r2 = Reminder(id = 2, title = "Groceries", placeName = "Market", latitude = 37.78, longitude = -122.42, radiusMeters = 200f, isActive = true, isCompleted = true)
        val r3 = Reminder(id = 3, title = "Dentist", placeName = "Dental Clinic", latitude = 37.79, longitude = -122.43, radiusMeters = 800f, isActive = false, isCompleted = false)

        val all = listOf(r1, r2, r3)
        val activeOnly = all.filter { it.isActive && !it.isCompleted }
        assertEquals(1, activeOnly.size)
        assertEquals("Haircut", activeOnly[0].title)

        val completedOnly = all.filter { it.isCompleted }
        assertEquals(1, completedOnly.size)
        assertEquals("Groceries", completedOnly[0].title)

        val sortedByRadius = all.sortedBy { it.radiusMeters }
        assertEquals(200f, sortedByRadius[0].radiusMeters, 0.01f)
        assertEquals(800f, sortedByRadius[2].radiusMeters, 0.01f)
    }
}
