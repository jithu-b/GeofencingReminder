package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Reminder
import com.example.location.Coordinates
import com.example.location.LocationHelper
import com.example.ui.theme.BrandActive
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandWarning

@Composable
fun ReminderCard(
    reminder: Reminder,
    userLocation: Coordinates?,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onToggleComplete: () -> Unit,
    onSimulateTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceText = userLocation?.let {
        val distance = LocationHelper.calculateDistanceMeters(
            it.latitude, it.longitude,
            reminder.latitude, reminder.longitude
        )
        LocationHelper.formatDistance(distance, reminder.radiusMeters)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (reminder.isActive && !reminder.isCompleted) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Task title + Completion check
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = reminder.placeName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.testTag("complete_button_${reminder.id}")
                ) {
                    Icon(
                        imageVector = if (reminder.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Mark Complete",
                        tint = if (reminder.isCompleted) BrandActive else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Middle: Tags / Radius / Proximity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radius pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${reminder.formattedRadius} radius",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Interactive Active status toggle pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        reminder.isCompleted -> Color(0xFF6B7280).copy(alpha = 0.12f)
                        reminder.isActive -> BrandActive.copy(alpha = 0.12f)
                        else -> BrandWarning.copy(alpha = 0.12f)
                    },
                    onClick = { onToggleActive(!reminder.isActive) },
                    modifier = Modifier.testTag("toggle_active_${reminder.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        reminder.isCompleted -> Color(0xFF6B7280)
                                        reminder.isActive -> BrandActive
                                        else -> BrandWarning
                                    }
                                )
                        )
                        Text(
                            text = when {
                                reminder.isCompleted -> "Completed"
                                reminder.isActive -> "Armed"
                                else -> "Paused"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                reminder.isCompleted -> Color(0xFF6B7280)
                                reminder.isActive -> BrandActive
                                else -> BrandWarning
                            }
                        )
                    }
                }
            }

            if (reminder.notes.isNotBlank()) {
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom row: Distance indicator (if known) + Quick test proximity action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (distanceText != null) {
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (distanceText.contains("Inside")) BrandActive else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (distanceText.contains("Inside")) FontWeight.Bold else FontWeight.Normal
                    )
                } else {
                    Text(
                        text = "Geofence armed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Simulate proximity button
                Surface(
                    onClick = onSimulateTrigger,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("test_trigger_${reminder.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Trigger",
                            modifier = Modifier.size(13.dp),
                            tint = BrandPrimary
                        )
                        Text(
                            text = "Test Alert",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }
    }
}
