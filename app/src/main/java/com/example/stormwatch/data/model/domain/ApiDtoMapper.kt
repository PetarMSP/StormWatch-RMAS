package com.example.stormwatch.data.model.domain

import com.example.stormwatch.data.model.api.CityDto
import com.example.stormwatch.data.model.api.DailyDto
import com.example.stormwatch.data.model.api.HourlyDto
import java.util.Calendar
import java.util.Locale

fun DailyDto.toDailyForecast(): DailyForecast {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dt * 1000
    }

    val DayName = cal.getDisplayName(
        Calendar.DAY_OF_WEEK,
        Calendar.SHORT,
        Locale.getDefault()
    ) ?: ""
    return DailyForecast(
        date = dt * 1000,
        minTemp = temp.min.toInt(),
        maxTemp = temp.max.toInt(),
        condition = weather.firstOrNull()?.main ?: "Unknown",
        dayName = DayName
    )
}

fun HourlyDto.toHourlyForecast(): HourlyForecast {
    val ts = dt * 1000L
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val hour = cal.get(Calendar.HOUR_OF_DAY)

    return HourlyForecast(
        timestamp = ts,
        hour = hour,
        temp = temp.toInt(),
        isDay = hour in 6..18
    )
}
fun CityDto.toCurrentForecast(): WeatherForecast {
    return WeatherForecast(
        city = name,
        temperature = main.temp.toInt(),
        condition = weather.firstOrNull()?.main ?: "Unknown"
    )
}

