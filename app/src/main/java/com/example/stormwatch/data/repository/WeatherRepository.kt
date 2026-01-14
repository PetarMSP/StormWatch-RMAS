package com.example.stormwatch.data.repository

import com.example.stormwatch.data.model.WeatherReport

class ReportRepository {
    fun getWeatherReport(): WeatherReport{
        return WeatherReport(
            city = "Beograd",
            temperature = 24.5,
            description = "Delimicno oblacno",
            windSpeed = 5.2,
            humidity = 60
        )
    }
}