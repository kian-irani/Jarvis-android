package com.kianirani.jarvis.core.weather

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Real weather (PRD §, "weather واقعی"). Pure parsing + WMO-code → description for the home
 * weather chip, fed by **Open-Meteo** (no API key, free). The HTTP GET is the device half; this
 * decodes the `current_weather` JSON and maps the numeric WMO weather code to a label/emoji-free
 * description. Pure → JVM-tested.
 */
@Serializable
data class Weather(val temperatureC: Double, val windKmh: Double, val code: Int, val isDay: Boolean) {
    val description: String get() = WeatherCodes.describe(code)
}

object WeatherCodes {
    /** WMO weather interpretation codes → short human label (Open-Meteo `weathercode`). */
    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain showers"
        in 85..86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Unknown"
    }
}

object WeatherParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** The Open-Meteo current-weather endpoint for [lat],[lon] (no key needed). */
    fun endpoint(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"

    /** Parse Open-Meteo's response into [Weather], or null on malformed/missing data. */
    fun parse(body: String): Weather? = runCatching {
        val cur = json.decodeFromString(JsonObject.serializer(), body)["current_weather"]?.jsonObject ?: return null
        Weather(
            temperatureC = cur["temperature"]?.jsonPrimitive?.content?.toDouble() ?: return null,
            windKmh = cur["windspeed"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            code = cur["weathercode"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            isDay = (cur["is_day"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1) == 1,
        )
    }.getOrNull()
}
