package com.example.stormwatch.data.model.domain

data class WeatherResult(
    val current: WeatherForecast,
    val daily: List<DailyForecast>,
    val hourly: List<HourlyForecast>
)