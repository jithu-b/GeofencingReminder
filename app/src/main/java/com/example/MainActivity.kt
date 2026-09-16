package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.GeoRemindNavigation
import com.example.ui.theme.GeoRemindTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reminderId = intent?.getLongExtra("reminder_id", -1L)?.takeIf { it != -1L }

        setContent {
            GeoRemindTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GeoRemindNavigation(initialReminderId = reminderId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
