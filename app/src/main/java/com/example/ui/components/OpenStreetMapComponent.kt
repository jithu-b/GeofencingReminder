package com.example.ui.components

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Reminder
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun OpenStreetMapComponent(
    modifier: Modifier = Modifier,
    latitude: Double = 37.7749,
    longitude: Double = -122.4194,
    radiusMeters: Float = 500f,
    markerTitle: String = "Reminder Location",
    isInteractive: Boolean = true,
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    allReminders: List<Reminder>? = null,
    onLocationSelected: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.filesDir
            osmdroidTileCache = java.io.File(context.filesDir, "osmdroid/tiles").apply { mkdirs() }
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(isInteractive)
            setBuiltInZoomControls(false)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    DisposableEffect(Unit) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null && onLocationSelected != null) {
                    onLocationSelected(p.latitude, p.longitude)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        if (isInteractive) {
            mapView.overlays.add(0, eventsOverlay)
        }
        onDispose {
            mapView.onDetach()
        }
    }

    LaunchedEffect(latitude, longitude, radiusMeters, markerTitle, allReminders) {
        val eventsOverlays = mapView.overlays.filterIsInstance<MapEventsOverlay>()
        mapView.overlays.clear()
        mapView.overlays.addAll(eventsOverlays)

        if (!allReminders.isNullOrEmpty()) {
            allReminders.forEach { r ->
                val point = GeoPoint(r.latitude, r.longitude)
                val marker = Marker(mapView)
                marker.position = point
                marker.title = r.title
                marker.snippet = r.placeName
                mapView.overlays.add(marker)

                val circle = Polygon(mapView)
                circle.points = Polygon.pointsAsCircle(point, r.radiusMeters.toDouble())
                circle.fillColor = Color.argb(50, 37, 99, 235)
                circle.strokeColor = Color.rgb(37, 99, 235)
                circle.strokeWidth = 3f
                mapView.overlays.add(circle)
            }
        } else {
            val point = GeoPoint(latitude, longitude)
            val marker = Marker(mapView)
            marker.position = point
            marker.title = markerTitle
            mapView.overlays.add(marker)

            val circle = Polygon(mapView)
            circle.points = Polygon.pointsAsCircle(point, radiusMeters.toDouble())
            circle.fillColor = Color.argb(50, 37, 99, 235)
            circle.strokeColor = Color.rgb(37, 99, 235)
            circle.strokeWidth = 3f
            mapView.overlays.add(circle)

            mapView.controller.setCenter(point)
        }

        if (userLatitude != null && userLongitude != null) {
            val userPoint = GeoPoint(userLatitude, userLongitude)
            val userMarker = Marker(mapView)
            userMarker.title = "You"
            userMarker.position = userPoint
            mapView.overlays.add(userMarker)
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        if (isInteractive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { mapView.controller.zoomIn() },
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    containerColor = ComposeColor.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = ComposeColor.Black)
                }
                FloatingActionButton(
                    onClick = { mapView.controller.zoomOut() },
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    containerColor = ComposeColor.White
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = ComposeColor.Black)
                }
            }
        }
    }
}
