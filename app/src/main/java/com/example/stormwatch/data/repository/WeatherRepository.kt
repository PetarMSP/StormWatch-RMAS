package com.example.stormwatch.data.repository


import com.example.stormwatch.data.model.domain.WeatherResult
import com.example.stormwatch.data.model.domain.toCurrentForecast
import com.example.stormwatch.data.model.domain.toDailyForecast
import com.example.stormwatch.data.model.domain.toHourlyForecast
import com.example.stormwatch.data.remote.WeatherApi
private const val API_KEY = "e38c61f6c3f881aff4c3a63ba5ed1426"

class WeatherRepository(private val api: WeatherApi) {

    suspend fun getWeather(lat: Double, lon: Double): WeatherResult {

        val cityDto = api.getCityData(
            lat = lat,
            lon = lon,
            apiKey = API_KEY
        )

        val forecastDto = api.getWeatherForecast(
            lat = lat,
            lon = lon,
            apiKey = API_KEY
        )

        return WeatherResult(
            current = cityDto.toCurrentForecast(),
            daily = forecastDto.daily.map { it.toDailyForecast() },
            hourly = forecastDto.hourly.map { it.toHourlyForecast() }
        )
    }
}
