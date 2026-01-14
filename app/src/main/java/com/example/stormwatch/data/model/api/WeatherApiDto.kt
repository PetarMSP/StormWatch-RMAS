package com.example.stormwatch.data.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherApiDto(
    val name: String,
    val main: MainDto,
    val weather: List<WeatherDescDto>
)

@JsonClass(generateAdapter = true)
data class MainDto(
    val temp: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDescDto(
    val main: String,
    val description: String
)