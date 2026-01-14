package com.example.stormwatch.data.model

data class LocalWeatherReport(
    val id: String = "",
    val userID: String = "",

    val latitude: Double,
    val longitude: Double,

    val parametar: String,

    val isActive: Boolean,

    val durationHours: Int,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = startTime + durationHours * 60 * 60 * 1000,

    val likes: Int = 0,
    val dislikes: Int = 0

)