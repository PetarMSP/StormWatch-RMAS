package com.example.stormwatch.data.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherApiDto(
    val hourly: List<HourlyDto>,
    val daily: List<DailyDto>
)
@JsonClass(generateAdapter = true)
data class CityDto(
    val name: String,
    val main: MainDto,
    val weather: List<WeatherDescDto>
)

@JsonClass(generateAdapter = true)
data class MainDto(
    val temp: Double
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    val dt: Long,
    val temp: Double,
    val weather: List<WeatherDescDto>
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    val dt: Long,
    val temp: TempDto,
    val weather: List<WeatherDescDto>
)

@JsonClass(generateAdapter = true)
data class TempDto(
    val min: Double,
    val max: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDescDto(
    val main: String,
    val description: String
)