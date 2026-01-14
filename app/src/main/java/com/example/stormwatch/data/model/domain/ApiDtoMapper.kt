package com.example.stormwatch.data.model.domain

import com.example.stormwatch.data.model.api.WeatherApiDto

fun WeatherApiDto.toDomain(): WeatherForecast{
    return WeatherForecast(
        city = name,
        temperature = main.temp.toInt(),
        condition = weather.firstOrNull()?.main ?: "Unknown"
    )
}