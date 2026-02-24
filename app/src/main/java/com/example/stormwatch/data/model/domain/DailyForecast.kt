package com.example.stormwatch.data.model.domain

data class DailyForecast(
    val date: Long,
    val minTemp: Int,
    val maxTemp: Int,
    val condition: String,
    val dayName: String
)
