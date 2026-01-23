package com.example.stormwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stormwatch.data.model.LocalWeatherReport
import com.example.stormwatch.data.model.WeatherParameter
import com.example.stormwatch.data.model.domain.WeatherForecast
import com.example.stormwatch.data.remote.RetrofitInstance
import com.example.stormwatch.data.repository.AuthRepository
import com.example.stormwatch.data.repository.LocalReportRepository
import com.example.stormwatch.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val weatherRepository = WeatherRepository(RetrofitInstance.api)

    private val localReportRepository = LocalReportRepository()

    //Auth
    private val _isLoggedIn = MutableStateFlow(authRepository.currentUserId() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    //Weather Api
    private val _forecast = MutableStateFlow<WeatherForecast?>(null)
    val forecast: StateFlow<WeatherForecast?> = _forecast

    //Local Reports
    private val _localReports = MutableStateFlow<List<LocalWeatherReport>>(emptyList())
    val localReports: StateFlow<List<LocalWeatherReport>> = _localReports

    init {
        observeLocalReports()
        loadWeather("Prokuplje")
    }

    private fun observeLocalReports(){
        viewModelScope.launch {
            localReportRepository.getActiveReports().collect {
                _localReports.value = it
            }
        }
    }

    fun loadWeather(city: String){
        viewModelScope.launch {
            try {
                val data = weatherRepository.getForecast(city)
                _forecast.value = data
            }catch (e: Exception){
                println("Weather load failed: ${e.message}")
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