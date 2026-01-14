package com.example.stormwatch.data.remote

import com.example.stormwatch.data.model.api.WeatherApiDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather") suspend fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherApiDto
}