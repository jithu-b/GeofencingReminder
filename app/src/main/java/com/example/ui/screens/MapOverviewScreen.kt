package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Reminder
import com.example.ui.components.OpenStreetMapComponent
import com.example.ui.theme.BrandActive
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.ReminderViewModel

@Composable
fun MapOverviewScreen(
    viewModel: ReminderViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()

    var selectedReminderId by remember { mutableStateOf<Long?>(null) }
    val focusReminder = activeReminders.find { it.id == selectedReminderId } ?: activeReminders.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        // Full Map Canvas
        OpenStreetMapComponent(
            modifier = Modifier
                .fillMaxSize()
                .testTag("osm_overview_map"),
            latitude = focusReminder?.latitude ?: 37.7749,
            longitude = focusReminder?.longitude ?: -122.4194,
            radiusMeters = focusReminder?.radiusMeters ?: 500f,
            markerTitle = focusReminder?.title ?: "OpenStreetMap Geofences",
            isInteractive = true,
            userLatitude = userLocation?.latitude,
            userLongitude = userLocation?.longitude,
            allReminders = activeReminders
        )

        // Top Status Header Pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${activeReminders.count { it.isActive }} Active Geofences on OpenStreetMap",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Floating Action Buttons (Locate Me + Add)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.refreshLocation() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("map_locate_me_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center My Location")
            }

            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("map_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }

        // Bottom Carousel Cards
        if (activeReminders.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeReminders, key = { it.id }) { reminder ->
                    val isFocused = (selectedReminderId == reminder.id) || (selectedReminderId == null && reminder == activeReminders.firstOrNull())
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .clickable {
                                selectedReminderId = reminder.id
                            }
                            .testTag("map_carousel_card_${reminder.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) BrandPrimary else MaterialTheme.colorScheme.outline
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 6.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = reminder.formattedRadius,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = reminder.placeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    onClick = { onNavigateToDetail(reminder.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "View Details →",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
