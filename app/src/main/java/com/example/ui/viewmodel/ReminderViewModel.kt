package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GeoRemindDatabase
import com.example.data.model.Reminder
import com.example.data.repository.ReminderRepository
import com.example.geofence.GeofenceManager
import com.example.geofence.NotificationHelper
import com.example.location.Coordinates
import com.example.location.LocationHelper
import com.example.location.NominatimService
import com.example.location.PlaceSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReminderFilter {
    ALL,
    ACTIVE,
    NEARBY,
    COMPLETED
}

enum class ReminderSort {
    NEWEST,
    NEAREST,
    RADIUS
}

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GeoRemindDatabase.getInstance(application)
    private val geofenceManager = GeofenceManager(application)
    private val notificationHelper = NotificationHelper(application)
    private val repository = ReminderRepository(database.reminderDao(), geofenceManager, notificationHelper)
    private val locationHelper = LocationHelper(application)
    private val nominatimService = NominatimService()

    val activeReminders: StateFlow<List<Reminder>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders: StateFlow<List<Reminder>> = repository.completedReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List Filtering & Sorting State
    private val _selectedFilter = MutableStateFlow(ReminderFilter.ACTIVE)
    val selectedFilter: StateFlow<ReminderFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(ReminderSort.NEWEST)
    val selectedSort: StateFlow<ReminderSort> = _selectedSort.asStateFlow()

    private val _listSearchQuery = MutableStateFlow("")
    val listSearchQuery: StateFlow<String> = _listSearchQuery.asStateFlow()

    // Location refreshing state for smooth UI feedback
    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation: StateFlow<Boolean> = _isRefreshingLocation.asStateFlow()

    // Current user location
    private val _userLocation = MutableStateFlow<Coordinates?>(null)
    val userLocation: StateFlow<Coordinates?> = _userLocation.asStateFlow()

    // Derived reactive list of reminders observing Room Database
    val filteredReminders: StateFlow<List<Reminder>> = combine(
        repository.allReminders,
        _selectedFilter,
        _listSearchQuery,
        _selectedSort,
        _userLocation
    ) { reminders, filter, query, sort, location ->
        var list = when (filter) {
            ReminderFilter.ALL -> reminders
            ReminderFilter.ACTIVE -> reminders.filter { it.isActive && !it.isCompleted }
            ReminderFilter.NEARBY -> {
                if (location != null) {
                    reminders.filter {
                        !it.isCompleted && LocationHelper.calculateDistanceMeters(
                            location.latitude, location.longitude,
                            it.latitude, it.longitude
                        ) <= (it.radiusMeters * 2.5f)
                    }
                } else {
                    reminders.filter { it.isActive && !it.isCompleted }
                }
            }
            ReminderFilter.COMPLETED -> reminders.filter { it.isCompleted }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                        it.placeName.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }

        when (sort) {
            ReminderSort.NEWEST -> list.sortedByDescending { it.createdAt }
            ReminderSort.RADIUS -> list.sortedBy { it.radiusMeters }
            ReminderSort.NEAREST -> {
                if (location != null) {
                    list.sortedBy {
                        LocationHelper.calculateDistanceMeters(
                            location.latitude, location.longitude,
                            it.latitude, it.longitude
                        )
                    }
                } else {
                    list.sortedByDescending { it.createdAt }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Counts for UI summary badges
    val nearbyCount: StateFlow<Int> = combine(
        repository.allReminders,
        _userLocation
    ) { reminders, location ->
        if (location == null) 0
        else {
            reminders.count {
                !it.isCompleted && LocationHelper.calculateDistanceMeters(
                    location.latitude, location.longitude,
                    it.latitude, it.longitude
                ) <= it.radiusMeters
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Permission states
    private val _hasLocationPermission = MutableStateFlow(locationHelper.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PlaceSearchResult>>(emptyList())
    val searchResults: StateFlow<List<PlaceSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Creation State
    private val _newTitle = MutableStateFlow("")
    val newTitle: StateFlow<String> = _newTitle.asStateFlow()

    private val _newPlaceName = MutableStateFlow("")
    val newPlaceName: StateFlow<String> = _newPlaceName.asStateFlow()

    private val _newLatitude = MutableStateFlow(37.7749)
    val newLatitude: StateFlow<Double> = _newLatitude.asStateFlow()

    private val _newLongitude = MutableStateFlow(-122.4194)
    val newLongitude: StateFlow<Double> = _newLongitude.asStateFlow()

    private val _newRadius = MutableStateFlow(500f)
    val newRadius: StateFlow<Float> = _newRadius.asStateFlow()

    // Last created reminder for Confirmation screen
    private val _lastCreatedReminder = MutableStateFlow<Reminder?>(null)
    val lastCreatedReminder: StateFlow<Reminder?> = _lastCreatedReminder.asStateFlow()

    private var searchJob: Job? = null

    init {
        refreshPermissions()
        refreshLocation()
    }

    fun refreshPermissions() {
        _hasLocationPermission.value = locationHelper.hasLocationPermission()
        if (_hasLocationPermission.value) {
            refreshLocation()
            viewModelScope.launch {
                repository.registerAllActiveGeofences()
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                _userLocation.value = loc
            }
        }
    }

    // Add Reminder Flow actions
    fun setNewTitle(title: String) {
        _newTitle.value = title
    }

    fun setNewPlace(placeName: String, lat: Double, lon: Double) {
        _newPlaceName.value = placeName
        _newLatitude.value = lat
        _newLongitude.value = lon
    }

    fun setNewRadius(radius: Float) {
        _newRadius.value = radius
    }

    fun onMapLocationPicked(lat: Double, lon: Double) {
        _newLatitude.value = lat
        _newLongitude.value = lon
        viewModelScope.launch {
            val res = nominatimService.reverseGeocode(lat, lon)
            if (res != null) {
                _newPlaceName.value = res.title
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(350) // debounce
            val results = nominatimService.searchPlaces(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun selectSearchResult(result: PlaceSearchResult) {
        _newPlaceName.value = result.title
        _newLatitude.value = result.latitude
        _newLongitude.value = result.longitude
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun useCurrentLocationAsTarget() {
        viewModelScope.launch {
            val loc = _userLocation.value ?: locationHelper.getCurrentLocation()
            if (loc != null) {
                _newLatitude.value = loc.latitude
                _newLongitude.value = loc.longitude
                val res = nominatimService.reverseGeocode(loc.latitude, loc.longitude)
                _newPlaceName.value = res?.title ?: "Current Location"
            }
        }
    }

    fun createReminder(onSuccess: (Reminder) -> Unit) {
        val title = _newTitle.value.trim().ifEmpty { "My Reminder" }
        val place = _newPlaceName.value.trim().ifEmpty { "Selected Location" }

        val reminder = Reminder(
            title = title,
            placeName = place,
            latitude = _newLatitude.value,
            longitude = _newLongitude.value,
            radiusMeters = _newRadius.value,
            isActive = true
        )

        viewModelScope.launch {
            val id = repository.addReminder(reminder)
            val saved = reminder.copy(id = id)
            _lastCreatedReminder.value = saved
            // Reset fields for next time
            _newTitle.value = ""
            _newPlaceName.value = ""
            _newRadius.value = 500f
            onSuccess(saved)
        }
    }

    fun toggleReminderActive(reminder: Reminder) {
        viewModelScope.launch {
            repository.setReminderActive(reminder.id, !reminder.isActive)
        }
    }

    fun toggleReminderComplete(reminder: Reminder) {
        viewModelScope.launch {
            repository.setReminderCompleted(reminder.id, !reminder.isCompleted)
        }
    }

    fun updateReminderRadius(reminder: Reminder, newRadius: Float) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(radiusMeters = newRadius))
        }
    }

    fun updateReminderDetails(reminder: Reminder, newTitle: String, newPlace: String, newRadius: Float) {
        viewModelScope.launch {
            repository.updateReminder(
                reminder.copy(
                    title = newTitle,
                    placeName = newPlace,
                    radiusMeters = newRadius
                )
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun simulateTrigger(reminder: Reminder) {
        viewModelScope.launch {
            repository.simulateGeofenceTrigger(reminder)
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    fun setFilter(filter: ReminderFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: ReminderSort) {
        _selectedSort.value = sort
    }

    fun setListSearchQuery(query: String) {
        _listSearchQuery.value = query
    }

    fun restoreReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.addReminder(reminder)
        }
    }

    fun refreshLocationWithFeedback() {
        viewModelScope.launch {
            _isRefreshingLocation.value = true
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                _userLocation.value = loc
            }
            delay(500)
            _isRefreshingLocation.value = false
        }
    }
}
