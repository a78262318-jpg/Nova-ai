package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeoCity>? = null
)

@JsonClass(generateAdapter = true)
data class GeoCity(
    val name: String,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double
)

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val current: CurrentWeather? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val humidity: Int? = null,
    @Json(name = "wind_speed_10m") val windSpeed: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int = 0
)

@JsonClass(generateAdapter = true)
data class WikiSummaryResponse(
    val title: String? = null,
    val extract: String? = null,
    val description: String? = null
)

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 1
    ): GeocodingResponse
}

interface WeatherForecastApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
    ): WeatherResponse
}

interface WikipediaApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getSummary(
        @Path("title") title: String
    ): WikiSummaryResponse
}

data class WeatherReport(
    val city: String,
    val country: String,
    val temperatureC: Double,
    val humidityPercent: Int?,
    val windKmh: Double?,
    val condition: String
)

class OnlineInfoRepository {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val geocodingApi: GeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeocodingApi::class.java)
    }

    private val weatherApi: WeatherForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherForecastApi::class.java)
    }

    private val wikipediaApi: WikipediaApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaApi::class.java)
    }

    suspend fun fetchWeather(cityQuery: String): Result<WeatherReport> = runCatching {
        val target = cityQuery.ifBlank { "Dhaka" }
        val geo = geocodingApi.searchCity(target).results?.firstOrNull()
            ?: throw IllegalStateException("City '$target' not found")
        val weather = weatherApi.getCurrentWeather(geo.latitude, geo.longitude).current
            ?: throw IllegalStateException("Weather data unavailable")
        WeatherReport(
            city = geo.name,
            country = geo.country.orEmpty(),
            temperatureC = weather.temperature2m,
            humidityPercent = weather.humidity,
            windKmh = weather.windSpeed,
            condition = mapWmoCode(weather.weatherCode)
        )
    }

    suspend fun fetchWebSummary(topic: String): Result<String> = runCatching {
        val sanitized = topic.trim().replace(" ", "_")
        val summary = wikipediaApi.getSummary(sanitized)
        val extract = summary.extract?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("No online summary found")
        "${summary.title ?: topic}: $extract"
    }

    private fun mapWmoCode(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Foggy"
        51, 53, 55, 61, 63, 65 -> "Rainy"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Rain showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Variable"
    }
}
