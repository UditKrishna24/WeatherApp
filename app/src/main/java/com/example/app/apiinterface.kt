package com.example.app

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface apiinterface {

    // Weather by city name (used in SearchView)
    @GET("weather")
    fun getWeatherData(
        @Query("q") city: String,
        @Query("appid") appid: String,
        @Query("units") units: String
    ): Call<weatherapp>

    // Weather by coordinates (used in GPS-based fetch)
    @GET("weather")
    fun getWeatherDataByCoord(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String,
        @Query("units") units: String
    ): Call<weatherapp>
}
