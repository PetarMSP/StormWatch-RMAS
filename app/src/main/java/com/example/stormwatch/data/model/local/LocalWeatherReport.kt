package com.example.stormwatch.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class LocalWeatherReport(
    val id: String = "",
    val userID: String = "",

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    val parametar: WeatherParameter = WeatherParameter.RAIN,
    val isActive: Boolean = true,

    val durationHours: Int = 1,
    val startTime: Long = System.currentTimeMillis(),

    val endTime: Long = startTime + durationHours * 60L * 60L * 1000L,

    val description: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0
)

enum class WeatherParameter {
    RAIN,
    TEMPERATURE,
    WIND,
    SNOW,
    ICE
}
