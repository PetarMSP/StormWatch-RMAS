package com.example.stormwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stormwatch.data.model.LocalWeatherReport
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.domain.WeatherResult
import com.example.stormwatch.data.model.domain.UserProfile
import com.example.stormwatch.data.repository.WeatherRepository
import com.example.stormwatch.data.repository.UserRepository
import com.example.stormwatch.data.remote.RetrofitInstance
import com.example.stormwatch.data.repository.AuthRepository
import com.example.stormwatch.data.repository.LocalReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val weatherRepository = WeatherRepository(RetrofitInstance.api)

    private val localReportRepository = LocalReportRepository()

    //Auth
    private val _isLoggedIn = MutableStateFlow(authRepository.currentUserId() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    //Weather Api
    private val _weather = MutableStateFlow<WeatherResult?>(null)

    val weather: StateFlow<WeatherResult?> = _weather

    //Local Reports
    private val _localReports = MutableStateFlow<List<LocalWeatherReport>>(emptyList())
    val localReports: StateFlow<List<LocalWeatherReport>> = _localReports

    private val _selectedReport = MutableStateFlow<LocalWeatherReport?>(null)
    val selectedReport: StateFlow<LocalWeatherReport?> = _selectedReport

    private val _selectedReportOwner = MutableStateFlow<UserProfile?>(null)
    val selectedReportOwner: StateFlow<UserProfile?> = _selectedReportOwner

    fun currentUserId(): String? = authRepository.currentUserId()

    init {
        observeLocalReports()
        loadWeather()
    }

    private fun observeLocalReports(){
        viewModelScope.launch {
            localReportRepository.getActiveReports().collect {
                _localReports.value = it
            }
        }
    }

    fun openReport(reportId: String) {
        viewModelScope.launch {
            val r = localReportRepository.getReportById(reportId)
            _selectedReport.value = r

            if (r != null) {
                _selectedReportOwner.value = userRepository.getUser(r.userID)
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
                println("WEATHER RESULT: $result")
                _weather.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addLocalReport(lat: Double, lon: Double, parameter: WeatherParameter, duration: Int,description: String) {
        viewModelScope.launch {
            val report = LocalWeatherReport(
                latitude = lat,
                longitude = lon,
                parametar = parameter,
                isActive = true,
                durationHours = duration,
                description = description
            )
            localReportRepository.addReport(report)
        }
    }


    fun logout(){
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.value = false
        }
    }
}