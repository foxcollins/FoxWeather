package com.foxcode.foxweather.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.foxcode.foxweather.environment.TimeOfDay
import com.foxcode.foxweather.rendering.RainIntensity

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

    const val BG_SCENE = "scene"
    const val BG_IMAGE = "image"

    /** Latitud/longitud por defecto (CDMX) hasta integrar Fused Location. */
    const val DEFAULT_LAT = 19.4326
    const val DEFAULT_LON = -99.1332

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