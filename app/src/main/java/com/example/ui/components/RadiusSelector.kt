package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandPrimary

fun formatRadiusOption(meters: Float): String {
    return when {
        meters >= 1000f -> "${(meters / 1000f)} km"
        else -> "${meters.toInt()} m"
    }
}

fun getRadiusDescription(meters: Float): String {
    return when {
        meters <= 150f -> "Immediate doorstep proximity (~1 block)"
        meters <= 350f -> "Standard neighborhood block (~2 min walk)"
        meters <= 650f -> "Quarter-mile arrival zone (recommended)"
        else -> "Wide perimeter / driving approach (~0.6 mi)"
    }
}

@Composable
fun RadiusSelector(
    selectedRadius: Float,
    onRadiusSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Geofence Radius",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatRadiusOption(selectedRadius),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
        }

        Slider(
            value = selectedRadius,
            onValueChange = { onRadiusSelected(it) },
            valueRange = 100f..1000f,
            steps = 17,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("radius_slider"),
            colors = SliderDefaults.colors(
                thumbColor = BrandPrimary,
                activeTrackColor = BrandPrimary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "100 m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "1 km",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Adjust,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.padding(2.dp)
            )
            Text(
                text = getRadiusDescription(selectedRadius),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
