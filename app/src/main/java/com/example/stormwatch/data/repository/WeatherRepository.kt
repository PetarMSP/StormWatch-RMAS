package com.example.stormwatch.data.repository

import com.example.stormwatch.data.model.WeatherReport
import com.example.stormwatch.data.model.domain.WeatherForecast
import com.example.stormwatch.data.model.domain.toDomain
import com.example.stormwatch.data.remote.WeatherApi

class WeatherRepository(private val api: WeatherApi) {
    suspend fun getForecast(city: String): WeatherForecast{
        return api.getWeatherByCity(city,"e38c61f6c3f881aff4c3a63ba5ed1426").toDomain()
    }
}