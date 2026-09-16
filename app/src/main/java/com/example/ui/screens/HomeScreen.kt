package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Reminder
import com.example.ui.components.LocationPermissionBanner
import com.example.ui.components.ReminderCard
import com.example.ui.theme.BrandActive
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.ReminderFilter
import com.example.ui.viewmodel.ReminderSort
import com.example.ui.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReminderViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Reactive Task / Reminder Database Observation via ViewModel
    val displayedReminders by viewModel.filteredReminders.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val completedReminders by viewModel.completedReminders.collectAsStateWithLifecycle()
    val nearbyCount by viewModel.nearbyCount.collectAsStateWithLifecycle()

    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val searchQuery by viewModel.listSearchQuery.collectAsStateWithLifecycle()

    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val isRefreshingLocation by viewModel.isRefreshingLocation.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }

    // Smooth spinning animation for GPS refresh
    val infiniteTransition = rememberInfiniteTransition(label = "gps_refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gps_spin_angle"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_reminder_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar with Brand & Live Geofencing Status
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "GEOREMIND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(BrandActive)
                            )
                            Text(
                                text = "Radar Armed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Live Location Refresh button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            onClick = { viewModel.refreshLocationWithFeedback() },
                            modifier = Modifier.testTag("refresh_location_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh GPS",
                                    tint = BrandPrimary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .rotate(if (isRefreshingLocation) spinAngle else 0f)
                                )
                                Text(
                                    text = if (isRefreshingLocation) "Locating..." else if (userLocation != null) "GPS Live" else "Get GPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Remember it where it matters.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Geofenced tasks trigger alerts when you arrive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Permissions prompt if missing
            item {
                LocationPermissionBanner(
                    hasLocationPermission = hasLocationPermission,
                    hasNotificationPermission = true,
                    onPermissionsUpdated = { viewModel.refreshPermissions() }
                )
            }

            // Map-inspired Hero Card with Armed Stats
            item {
                MapHeroCard(
                    activeCount = activeReminders.count { it.isActive },
                    totalCount = allReminders.size,
                    onExploreMap = onNavigateToMap
                )
            }

            // Search Bar for Smooth Filtering
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setListSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_search_input"),
                    placeholder = {
                        Text(
                            "Search tasks or places...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setListSearchQuery("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Filter Chips and Sort Menu Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Horizontal scrollable filter pills
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFilter == ReminderFilter.ACTIVE,
                            onClick = { viewModel.setFilter(ReminderFilter.ACTIVE) },
                            label = { Text("Active (${activeReminders.size})") },
                            leadingIcon = if (selectedFilter == ReminderFilter.ACTIVE) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_active")
                        )

                        FilterChip(
                            selected = selectedFilter == ReminderFilter.NEARBY,
                            onClick = { viewModel.setFilter(ReminderFilter.NEARBY) },
                            label = { Text("Nearby ($nearbyCount)") },
                            leadingIcon = {
                                Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandActive.copy(alpha = 0.15f),
                                selectedLabelColor = BrandActive
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_nearby")
                        )

                        FilterChip(
                            selected = selectedFilter == ReminderFilter.ALL,
                            onClick = { viewModel.setFilter(ReminderFilter.ALL) },
                            label = { Text("All (${allReminders.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_all")
                        )

                        FilterChip(
                            selected = selectedFilter == ReminderFilter.COMPLETED,
                            onClick = { viewModel.setFilter(ReminderFilter.COMPLETED) },
                            label = { Text("Done (${completedReminders.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_completed")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sort menu button
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort Reminders",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when (selectedSort) {
                                        ReminderSort.NEWEST -> "Newest"
                                        ReminderSort.NEAREST -> "Nearest"
                                        ReminderSort.RADIUS -> "Radius"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = {
                                    viewModel.setSort(ReminderSort.NEWEST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nearest distance") },
                                onClick = {
                                    viewModel.setSort(ReminderSort.NEAREST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Smallest radius") },
                                onClick = {
                                    viewModel.setSort(ReminderSort.RADIUS)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Section Header with count and swipe hint
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedFilter) {
                            ReminderFilter.ACTIVE -> "Active Geofences"
                            ReminderFilter.NEARBY -> "Nearby Reminders"
                            ReminderFilter.ALL -> "All Reminders"
                            ReminderFilter.COMPLETED -> "Completed Tasks"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Swipe right to complete • left to delete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Dynamic Empty State Handling
            if (displayedReminders.isEmpty()) {
                item {
                    when {
                        searchQuery.isNotBlank() -> {
                            SearchEmptyCard(
                                query = searchQuery,
                                onClear = { viewModel.setListSearchQuery("") }
                            )
                        }
                        selectedFilter == ReminderFilter.NEARBY -> {
                            NearbyEmptyCard(
                                onSwitchToAll = { viewModel.setFilter(ReminderFilter.ACTIVE) }
                            )
                        }
                        selectedFilter == ReminderFilter.COMPLETED -> {
                            CompletedEmptyCard()
                        }
                        else -> {
                            EmptyRemindersCard(onAddClicked = onNavigateToAdd)
                        }
                    }
                }
            } else {
                // List of Reminder Cards with SwipeToDismissBox and Modifier.animateItem()
                items(displayedReminders, key = { it.id }) { reminder ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            when (dismissValue) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    // Mark complete / incomplete
                                    viewModel.toggleReminderComplete(reminder)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (reminder.isCompleted) "Re-activated ${reminder.title}" else "Completed ${reminder.title}",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.toggleReminderComplete(reminder)
                                        }
                                    }
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    // Delete reminder
                                    viewModel.deleteReminder(reminder)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${reminder.title}",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreReminder(reminder)
                                        }
                                    }
                                    true
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.animateItem(),
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val backgroundColor by animateColorAsState(
                                targetValue = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> BrandActive
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                },
                                label = "swipe_bg"
                            )
                            val alignment = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                SwipeToDismissBoxValue.Settled -> Alignment.Center
                            }
                            val icon = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                SwipeToDismissBoxValue.Settled -> null
                            }
                            val label = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> if (reminder.isCompleted) "Activate" else "Complete"
                                SwipeToDismissBoxValue.EndToStart -> "Delete"
                                SwipeToDismissBoxValue.Settled -> ""
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(backgroundColor)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                if (icon != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        ReminderCard(
                            reminder = reminder,
                            userLocation = userLocation,
                            onClick = { onNavigateToDetail(reminder.id) },
                            onToggleActive = { viewModel.toggleReminderActive(reminder) },
                            onToggleComplete = {
                                viewModel.toggleReminderComplete(reminder)
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = if (reminder.isCompleted) "Re-activated ${reminder.title}" else "Completed ${reminder.title}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.toggleReminderComplete(reminder)
                                    }
                                }
                            },
                            onSimulateTrigger = {
                                viewModel.simulateTrigger(reminder)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Alert triggered: Near ${reminder.title}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapHeroCard(
    activeCount: Int,
    totalCount: Int,
    onExploreMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExploreMap)
            .testTag("hero_map_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A),
                            Color(0xFF2563EB),
                            Color(0xFF0284C7)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "GEOFENCE RADAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$activeCount active perimeter${if (activeCount == 1) "" else "s"} armed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Open Map",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Text(
                    text = "GeoRemind monitors your physical perimeter using OpenStreetMap coordinates so you never miss an errand.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun SearchEmptyCard(
    query: String,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_empty_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No Reminders Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "No tasks or places matched \"$query\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = onClear,
                colors = ButtonDefaults.textButtonColors(contentColor = BrandPrimary)
            ) {
                Text("Clear Search Query", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NearbyEmptyCard(
    onSwitchToAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nearby_empty_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BrandPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = "No Nearby Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You are not currently approaching any active geofences. GeoRemind will trigger automatically when you arrive within radius.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Button(
                onClick = onSwitchToAll,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Active Tasks", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CompletedEmptyCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("completed_empty_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "No Completed Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Swipe tasks right from the list or tap the checkmark when you finish an errand to see them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyRemindersCard(
    onAddClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_reminders_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BrandPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "No Active Geofences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Pick a task, drop a pin on OpenStreetMap, set your proximity radius, and let GeoRemind alert you when you arrive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Button(
                onClick = onAddClicked,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Reminder", fontWeight = FontWeight.Bold)
            }
        }
    }
}
