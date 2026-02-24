package com.example.stormwatch.data.model.domain

data class HourlyForecast(
    val timestamp: Long,
    val hour: Int,
    val temp: Int,
    val isDay: Boolean
)
