package com.foxcode.foxweather.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Lugar resultante de la búsqueda de ubicación. */
data class GeoPlace(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
) {
    override fun toString() = listOf(name, region, country).filter { it.isNotBlank() }.joinToString(", ")
}

/**
 * Cliente de Open-Meteo (FASE 3). Sin clave de API, uso no comercial.
 * - Geocoding: busca ciudades por nombre.
 * - Forecast: condiciones actuales para lat/lon.
 * Se ejecuta en Dispatchers.IO; las llamadas nunca bloquean el hilo principal.
 */
object OpenMeteoClient {

    private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val REVERSE_URL = "https://nominatim.openstreetmap.org/reverse"
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    private const val TIMEOUT_MS = 8000

    /** Busca hasta [limit] lugares que coincidan con [query]. */
    suspend fun searchCity(query: String, limit: Int = 6): List<GeoPlace> =
        withContext(Dispatchers.IO) {
            val url = URL(
                "$GEOCODING_URL?name=${enc(query)}&count=$limit&language=es&format=json",
            )
            val json = get(url) ?: return@withContext emptyList()
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val o = results.optJSONObject(i) ?: continue
                    val name = o.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val region = o.optString("admin1")
                    val country = o.optString("country")
                    val lat = o.optDouble("latitude", Double.NaN)
                    val lon = o.optDouble("longitude", Double.NaN)
                    if (lat.isNaN() || lon.isNaN()) continue
                    add(GeoPlace(name, region, country, lat, lon))
                }
            }
        }

    /** Lugar más cercano a unas coordenadas (para el botón de geolocalización). */
    suspend fun reverseGeocode(lat: Double, lon: Double): GeoPlace? =
        withContext(Dispatchers.IO) {
            // Nominatim (OSM) soporta reverse geocoding; Open-Meteo no.
            val url = URL(
                "$REVERSE_URL?lat=${enc(lat.toString())}&lon=${enc(lon.toString())}" +
                    "&format=json&zoom=10&addressdetails=1&accept-language=es",
            )
            val json = get(url) ?: return@withContext null
            val name = json.optString("name").takeIf { it.isNotBlank() }
                ?: json.optJSONObject("address")?.optString("city")?.takeIf { it.isNotBlank() }
                ?: json.optJSONObject("address")?.optString("town")
                ?: json.optString("display_name").substringBefore(',').takeIf { it.isNotBlank() }
                ?: return@withContext null
            val addr = json.optJSONObject("address")
            val region = addr?.optString("state") ?: addr?.optString("county") ?: ""
            val country = addr?.optString("country") ?: ""
            GeoPlace(name, region, country, lat, lon)
        }

    /** Estado de clima actual para [lat]/[lon] (datos del minuto actual). */
    suspend fun currentWeather(lat: Double, lon: Double): WeatherState =
        withContext(Dispatchers.IO) {
            val url = URL(
                "$FORECAST_URL?latitude=${enc(lat.toString())}&longitude=${enc(lon.toString())}" +
                    "&current=temperature_2m,relative_humidity_2m,precipitation," +
                    "weather_code,cloud_cover,wind_speed_10m&timezone=auto",
            )
            val json = get(url) ?: return@withContext defaultState()
            val current = json.optJSONObject("current")
            if (current == null) {
                // Algún parámetro faltante en la respuesta -> degradación segura.
                return@withContext defaultState()
            }
            val code = current.optInt("weather_code", 0)
            WeatherState(
                condition = WeatherMapper.conditionFromWmo(code),
                temperature = current.optDouble("temperature_2m", 20.0).toFloat(),
                precipitation = current.optDouble("precipitation", 0.0).toFloat(),
                windSpeed = current.optDouble("wind_speed_10m", 0.0).toFloat(),
                humidity = current.optDouble("relative_humidity_2m", 0.0).toFloat(),
                cloudCover = (current.optDouble("cloud_cover", 0.0) / 100.0).toFloat(),
                timestamp = System.currentTimeMillis(),
            )
        }

    private suspend fun defaultState() = WeatherState(
        condition = WeatherCondition.CLEAR,
        temperature = 20f,
        precipitation = 0f,
        windSpeed = 0f,
        humidity = 50f,
        cloudCover = 0.1f,
        timestamp = System.currentTimeMillis(),
    )

    private fun get(url: URL): JSONObject? {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            // Nominatim (OSM) exige User-Agent identificable (política de uso).
            setRequestProperty("User-Agent", "FoxWeatherPrototype/0.1 (android; contacto@foxcode.app)")
        }
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            JSONObject(text)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
