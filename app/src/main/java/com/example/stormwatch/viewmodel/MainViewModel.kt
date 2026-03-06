package com.example.stormwatch.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stormwatch.data.model.LocalWeatherReport
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.domain.UserProfile
import com.example.stormwatch.data.model.domain.WeatherResult
import com.example.stormwatch.data.model.floorToHour
import com.example.stormwatch.data.remote.RetrofitInstance
import com.example.stormwatch.data.repository.AuthRepository
import com.example.stormwatch.data.repository.LocalReportRepository
import com.example.stormwatch.data.repository.UserRepository
import com.example.stormwatch.data.repository.WeatherRepository
import com.example.stormwatch.ui.screen.distanceMeters
import com.example.stormwatch.util.showStormNotification
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val weatherRepository = WeatherRepository(RetrofitInstance.api)
    private val localReportRepository = LocalReportRepository()

    // Location
    private val _userLocation = MutableStateFlow<android.location.Location?>(null)
    val userLocation: StateFlow<android.location.Location?> = _userLocation

    fun setUserLocation(loc: android.location.Location) {
        _userLocation.value = loc
    }

    // Auth

    fun currentUserId(): String? = authRepository.currentUserId()


    val currentUserProfile =
        authRepository.currentUserIdFlow()
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(null)
                else userRepository.getUserFlow(uid)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    val currentUsername: StateFlow<String> =
        currentUserProfile
            .map { it?.username.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Weather Api
    private val _weather = MutableStateFlow<WeatherResult?>(null)
    val weather: StateFlow<WeatherResult?> = _weather

    // Local Reports
    private val _localReports = MutableStateFlow<List<LocalWeatherReport>>(emptyList())
    val localReports: StateFlow<List<LocalWeatherReport>> = _localReports

    private val _selectedReport = MutableStateFlow<LocalWeatherReport?>(null)
    val selectedReport: StateFlow<LocalWeatherReport?> = _selectedReport

    private val _selectedReportOwner = MutableStateFlow<UserProfile?>(null)
    val selectedReportOwner: StateFlow<UserProfile?> = _selectedReportOwner

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers

    init {
        observeLocalReports()
        loadWeather()
        loadAllUsers()
    }

    private fun observeLocalReports() {
        viewModelScope.launch {
            localReportRepository.getActiveReports().collect {
                _localReports.value = it
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("users").get().await()
            _allUsers.value = snapshot.toObjects(UserProfile::class.java)
        }
    }

    fun openReport(reportId: String) {
        viewModelScope.launch {
            val r = localReportRepository.getReportById(reportId)
            _selectedReport.value = r

            if (r != null) {
                _selectedReportOwner.value = userRepository.getUserByUsername(r.userName)
            }
        }
    }

    fun likeReport(reportId: String) {
        viewModelScope.launch { localReportRepository.like(reportId, currentUserId()) }
    }

    fun dislikeReport(reportId: String) {
        viewModelScope.launch { localReportRepository.dislike(reportId, currentUserId()) }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch { localReportRepository.delete(reportId) }
    }

    fun loadWeather() {
        viewModelScope.launch {
            try {
                val result = weatherRepository.getWeather(
                    lat = 43.23,
                    lon = 21.59
                )
                _weather.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addLocalReport(
        lat: Double,
        lon: Double,
        parameter: WeatherParameter,
        customName: String?,
        description: String,
        endTime: Long,
        clickedTimestamp: Long
    ) {
        viewModelScope.launch {
            val cleanStart = floorToHour(clickedTimestamp)
            val cleanEnd = floorToHour(endTime)

            val diffMs = cleanEnd - cleanStart
            val calculatedDuration = (diffMs / 3600000L).toInt().coerceAtLeast(1)

            val currentUid = currentUserId() ?: return@launch
            val currentUserProfile = userRepository.getUser(currentUid)

            val report = LocalWeatherReport(
                id = "",
                userName = currentUserProfile?.username ?: "Anoniman",
                authorPhotoUrl = currentUserProfile?.photoUrl ?: "",
                latitude = lat,
                longitude = lon,
                parametar = parameter,
                customParameterName = if (parameter == WeatherParameter.OTHER) customName else null,
                description = description,
                startTime = cleanStart,
                durationHours = calculatedDuration,
                isActive = true,
                isProcessed = false,
            )

            localReportRepository.addReport(report)
        }
    }

    private fun calculatePoints(likes: Int, dislikes: Int): Int {
        var score = 0.0
        if (likes > 0) {
            score += if (likes <= 50) likes * 0.1 else 5.0
        }
        if (dislikes > 0) {
            val penalty = (dislikes * 0.1).coerceAtMost(2.0)
            score -= penalty
        }
        return kotlin.math.round(score).toInt()
    }

    fun finalizeWeeklyCompetition() {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()

            val unprocessedReports = db.collection("local_reports")
                .whereEqualTo("isProcessed", false)
                .get().await()
                .toObjects(LocalWeatherReport::class.java)
                .filter { (it.startTime + it.durationHours * 3600000L) < now }

            if (unprocessedReports.isEmpty()) return@launch

            val userPointMap = mutableMapOf<String, Int>()
            unprocessedReports.forEach { report ->
                val points = calculatePoints(report.likes, report.dislikes)
                userPointMap[report.userName] = userPointMap.getOrDefault(report.userName, 0) + points
            }

            userPointMap.forEach { (username, points) ->
                val userProfile = userRepository.getUserByUsername(username)
                userProfile?.let {
                    db.collection("users").document(it.uid)
                        .update(
                            "weeklyScore",
                            com.google.firebase.firestore.FieldValue.increment(points.toLong())
                        )
                        .await()
                }
            }

            db.runBatch { batch ->
                unprocessedReports.forEach { report ->
                    batch.update(db.collection("local_reports").document(report.id), "isProcessed", true)
                }
            }.await()

            val allUsers = db.collection("users")
                .get().await()
                .toObjects(UserProfile::class.java)
                .sortedByDescending { it.weeklyScore }

            if (allUsers.isNotEmpty()) {
                db.collection("users").document(allUsers[0].uid)
                    .update("goldTrophies", com.google.firebase.firestore.FieldValue.increment(1)).await()

                if (allUsers.size > 1) {
                    db.collection("users").document(allUsers[1].uid)
                        .update("silverTrophies", com.google.firebase.firestore.FieldValue.increment(1)).await()
                }

                if (allUsers.size > 2) {
                    db.collection("users").document(allUsers[2].uid)
                        .update("bronzeTrophies", com.google.firebase.firestore.FieldValue.increment(1)).await()
                }
            }

            val allUsersAgain = db.collection("users").get().await()
            db.runBatch { batch ->
                allUsersAgain.documents.forEach { doc ->
                    batch.update(doc.reference, "weeklyScore", 0)
                }
            }.await()
        }
    }

    private var lastCheckedReportId: String? = null

    fun startObservingNewReports(context: Context) {
        viewModelScope.launch {
            localReports.collect { reports ->
                val newestReport = reports.maxByOrNull { it.startTime } ?: return@collect
                val myLoc = userLocation.value ?: return@collect

                if (newestReport.id != lastCheckedReportId && newestReport.userName != currentUsername.value) {
                    val reportLatLng = LatLng(newestReport.latitude, newestReport.longitude)
                    val myLatLng = LatLng(myLoc.latitude, myLoc.longitude)

                    val distance = distanceMeters(myLatLng, reportLatLng)

                    if (distance <= 5000) {
                        lastCheckedReportId = newestReport.id

                        showStormNotification(
                            context = context,
                            message = "Novi izveštaj: ${newestReport.parametar.name} u vašoj blizini!",
                            reportId = newestReport.id
                        )
                    }
                }
            }
        }
    }
    fun updateMyProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            val uid = currentUserId() ?: return@launch
            val url = userRepository.uploadProfilePhoto(uid, uri)
            userRepository.updatePhoto(uid, url)
        }
    }

}