package com.kianirani.jarvis.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Weather acceptance: WMO code mapping, Open-Meteo parse, endpoint. Pure. */
class WeatherTest {

    @Test fun `wmo codes map to labels`() {
        assertEquals("Clear", WeatherCodes.describe(0))
        assertEquals("Rain", WeatherCodes.describe(63))
        assertEquals("Thunderstorm", WeatherCodes.describe(95))
        assertEquals("Unknown", WeatherCodes.describe(200))
    }

    @Test fun `parses open-meteo current weather`() {
        val body = """{"current_weather":{"temperature":18.4,"windspeed":12.0,"weathercode":61,"is_day":1}}"""
        val w = WeatherParser.parse(body)!!
        assertEquals(18.4, w.temperatureC, 1e-9)
        assertEquals("Rain", w.description)
        assertTrue(w.isDay)
    }

    @Test fun `malformed body parses to null`() {
        assertNull(WeatherParser.parse("{not json"))
        assertNull(WeatherParser.parse("{}"))
    }

    @Test fun `endpoint includes coordinates`() {
        assertTrue(WeatherParser.endpoint(35.7, 51.4).contains("latitude=35.7"))
        assertTrue(WeatherParser.endpoint(35.7, 51.4).contains("current_weather=true"))
    }
}
