package com.foxcode.foxweather.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.foxcode.foxweather.environment.TimeOfDay
import com.foxcode.foxweather.rendering.RainIntensity
import com.foxcode.foxweather.weather.WeatherCondition
import com.foxcode.foxweather.weather.WeatherState
import org.json.JSONObject

/**
 * Almacén de ajustes compartido entre la app (UI) y el WallpaperService.
 * Se usa SharedPreferences (suficiente para el prototipo); migrar a
 * DataStore cuando crezca la configuración (docs/DATA_MODEL.md).
 */
object SettingsStore {

    const val PREFS = "foxweather_settings"

    const val KEY_INTENSITY = "intensity"
    const val KEY_FPS = "fps"
    const val KEY_BG_MODE = "background_mode"
    const val KEY_SCENE = "scene_override"
    const val KEY_CUSTOM_WALLPAPER = "custom_wallpaper"
    const val KEY_WEATHER = "weather"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"
    const val KEY_CITY_NAME = "city_name"
    const val KEY_LIVE_WEATHER = "live_weather_json"

    const val BG_SCENE = "scene"
    const val BG_IMAGE = "image"

    /** Latitud/longitud por defecto (CDMX) hasta elegir ubicación real. */
    const val DEFAULT_LAT = 19.4326
    const val DEFAULT_LON = -99.1332

    fun location(context: Context): Pair<Double, Double> {
        val p = prefs(context)
        val lat = p.getFloat(KEY_LATITUDE, DEFAULT_LAT.toFloat()).toDouble()
        val lon = p.getFloat(KEY_LONGITUDE, DEFAULT_LON.toFloat()).toDouble()
        return lat to lon
    }

    fun cityName(context: Context): String? = prefs(context).getString(KEY_CITY_NAME, null)

    fun saveLocation(context: Context, lat: Double, lon: Double, city: String) {
        prefs(context).edit()
            .putFloat(KEY_LATITUDE, lat.toFloat())
            .putFloat(KEY_LONGITUDE, lon.toFloat())
            .putString(KEY_CITY_NAME, city)
            .apply()
    }

    /** Cache del WeatherState real (última respuesta de Open-Meteo). */
    fun saveLiveWeather(context: Context, ws: WeatherState) {
        val json = JSONObject().apply {
            put("condition", ws.condition.name)
            put("temperature", ws.temperature.toDouble())
            put("precipitation", ws.precipitation.toDouble())
            put("windSpeed", ws.windSpeed.toDouble())
            put("humidity", ws.humidity.toDouble())
            put("cloudCover", ws.cloudCover.toDouble())
            put("timestamp", ws.timestamp)
        }
        prefs(context).edit().putString(KEY_LIVE_WEATHER, json.toString()).apply()
    }

    fun liveWeather(context: Context): WeatherState? = runCatching {
        val raw = prefs(context).getString(KEY_LIVE_WEATHER, null) ?: return null
        val json = JSONObject(raw)
        WeatherState(
            condition = WeatherCondition.valueOf(json.optString("condition", "CLEAR")),
            temperature = json.optDouble("temperature", 20.0).toFloat(),
            precipitation = json.optDouble("precipitation", 0.0).toFloat(),
            windSpeed = json.optDouble("windSpeed", 0.0).toFloat(),
            humidity = json.optDouble("humidity", 50.0).toFloat(),
            cloudCover = json.optDouble("cloudCover", 0.1).toFloat(),
            timestamp = json.optLong("timestamp", 0L),
        )
    }.getOrNull()

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun intensity(context: Context): RainIntensity {
        val name = prefs(context).getString(KEY_INTENSITY, null) ?: return RainIntensity.MEDIUM
        return runCatching { RainIntensity.valueOf(name) }.getOrDefault(RainIntensity.MEDIUM)
    }

    fun fps(context: Context): Int = prefs(context).getInt(KEY_FPS, 30).coerceIn(1, 60)

    fun backgroundMode(context: Context): String =
        prefs(context).getString(KEY_BG_MODE, BG_SCENE)!!

    fun sceneOverride(context: Context): TimeOfDay? =
        when (prefs(context).getString(KEY_SCENE, null)) {
            "DIA" -> TimeOfDay.DAY
            "TARDE" -> TimeOfDay.DAY // TARDE se resuelve por elevación dentro de DAY
            "ATARDECER" -> TimeOfDay.SUNSET
            "NOCHE" -> TimeOfDay.NIGHT
            else -> null
        }

    fun hasCustomWallpaper(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CUSTOM_WALLPAPER, false)

    fun customWallpaperFile(context: Context): Context =
        context

    const val CUSTOM_WALLPAPER_FILE = "custom_wallpaper.jpg"
}