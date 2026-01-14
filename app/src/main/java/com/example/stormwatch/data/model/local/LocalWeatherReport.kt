package com.example.stormwatch.data.model

data class WeatherReport(
    val city: String,
    val temperature: Double,
    val description: String,
    val windSpeed: Double,
    val humidity: Int
)