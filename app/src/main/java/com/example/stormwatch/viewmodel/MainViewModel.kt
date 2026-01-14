package com.example.stormwatch.viewmodel

import androidx.lifecycle.ViewModel
import com.example.stormwatch.data.model.WeatherReport
import com.example.stormwatch.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val repository = ReportRepository()

    private val _weather = MutableStateFlow<WeatherReport?>(null)
    val weather: StateFlow<WeatherReport?> = _weather

    init {
        loadWeather()
    }

    private fun loadWeather() {
        _weather.value = repository.getWeatherReport()
    }


}