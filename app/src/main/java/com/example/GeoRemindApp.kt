package com.example

import android.app.Application
import com.example.data.local.GeoRemindDatabase
import com.example.geofence.NotificationHelper

class GeoRemindApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels
        NotificationHelper(this)
        // Warm up Database
        GeoRemindDatabase.getInstance(this)
    }
}
