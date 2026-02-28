package com.example.stormwatch.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Calendar

data class LocalWeatherReport(
    val id: String = "",
    val userName: String = "",
    val authorPhotoUrl: String = "",

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val parametar: WeatherParameter = WeatherParameter.RAIN,
    val customParameterName: String? = null,
    val isActive: Boolean = true,

    val durationHours: Int = 1,
    val startTime: Long = System.currentTimeMillis(),

    val description: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,

    @get:PropertyName("isProcessed")
    @set:PropertyName("isProcessed")
    var isProcessed: Boolean = false
)

enum class WeatherParameter {
    RAIN,
    TEMPERATURE,
    WIND,
    SNOW,
    ICE,
    FOG,
    OTHER
}

fun floorToHour(ts: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}