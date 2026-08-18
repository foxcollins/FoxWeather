package com.foxcode.foxweather.weather

/**
 * Condiciones meteorológicas del proveedor (WMO/Open-Meteo en FASE 3).
 * Cada condición se resuelve a un efecto de render (WeatherEffects).
 */
enum class WeatherCondition { CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, HEAVY_RAIN, THUNDERSTORM, SNOW, SLEET, HAIL }

/**
 * Estado de clima separado de la representación (AGENTS.md §Convenciones).
 * El RenderEngine nunca lee la API; consume este estado.
 */
data class WeatherState(
    val condition: WeatherCondition,
    val temperature: Float,
    val precipitation: Float,
    val windSpeed: Float,
    val humidity: Float,
    val cloudCover: Float,
    val timestamp: Long,
)

/** Tipo de precipitación que debe dibujar el sistema de partículas. */
enum class PrecipitationKind { NONE, DRIZZLE, RAIN, SNOW, HAIL }

/** Efecto resuelto para un estado de clima: partículas + nubes + niebla. */
data class WeatherEffect(
    val kind: PrecipitationKind,
    val intensity: com.foxcode.foxweather.rendering.RainIntensity,
    val cloudCover: Float,
    val fog: Boolean,
    val lightning: Boolean,
) {
    /** Viento previsto para la condición (define velocidad/deriva de partículas y nubes). */
    val wind: Float = when (kind) {
        PrecipitationKind.HAIL -> 1.4f
        PrecipitationKind.NONE -> 0.4f
        else -> 1f
    }
}

/** Mapeo condición -> efecto. Los thresholds se calibrarán en FASE 3. */
object WeatherEffects {

    fun resolve(ws: WeatherState): WeatherEffect {
        val p = ws.precipitation
        return when (ws.condition) {
            WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY ->
                WeatherEffect(PrecipitationKind.NONE, com.foxcode.foxweather.rendering.RainIntensity.LOW, ws.cloudCover, fog = false, lightning = false)

            WeatherCondition.OVERCAST ->
                WeatherEffect(PrecipitationKind.NONE, com.foxcode.foxweather.rendering.RainIntensity.LOW, 0.95f, fog = false, lightning = false)

            WeatherCondition.FOG ->
                WeatherEffect(PrecipitationKind.NONE, com.foxcode.foxweather.rendering.RainIntensity.LOW, 0.8f, fog = true, lightning = false)

            WeatherCondition.DRIZZLE ->
                WeatherEffect(PrecipitationKind.DRIZZLE, com.foxcode.foxweather.rendering.RainIntensity.LOW, 0.6f, fog = true, lightning = false)

            WeatherCondition.RAIN ->
                WeatherEffect(PrecipitationKind.RAIN, if (p > 2.5f) com.foxcode.foxweather.rendering.RainIntensity.MEDIUM else com.foxcode.foxweather.rendering.RainIntensity.LOW, 0.7f, fog = p > 1f, lightning = false)

            WeatherCondition.HEAVY_RAIN ->
                WeatherEffect(PrecipitationKind.RAIN, com.foxcode.foxweather.rendering.RainIntensity.HIGH, 0.9f, fog = false, lightning = false)

            WeatherCondition.THUNDERSTORM ->
                WeatherEffect(PrecipitationKind.RAIN, com.foxcode.foxweather.rendering.RainIntensity.HIGH, 1f, fog = false, lightning = true)

            WeatherCondition.SNOW ->
                WeatherEffect(PrecipitationKind.SNOW, com.foxcode.foxweather.rendering.RainIntensity.MEDIUM, 0.75f, fog = false, lightning = false)

            WeatherCondition.SLEET ->
                WeatherEffect(PrecipitationKind.SNOW, com.foxcode.foxweather.rendering.RainIntensity.LOW, 0.6f, fog = true, lightning = false)

            WeatherCondition.HAIL ->
                WeatherEffect(PrecipitationKind.HAIL, com.foxcode.foxweather.rendering.RainIntensity.MEDIUM, 0.85f, fog = false, lightning = false)
        }
    }
}