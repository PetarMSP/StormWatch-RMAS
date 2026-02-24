package com.example.stormwatch.data.remote

import com.example.stormwatch.data.model.api.WeatherApiDto
import com.example.stormwatch.data.model.api.CityDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("data/3.0/onecall")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("exclude") exclude: String = "minutely,alerts",
        @Query("units") units: String = "metric"
    ): WeatherApiDto
    @GET("data/2.5/weather")
    suspend fun getCityData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): CityDto
}

