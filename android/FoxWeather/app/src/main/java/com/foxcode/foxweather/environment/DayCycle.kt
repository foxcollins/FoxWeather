package com.foxcode.foxweather.environment

import com.foxcode.foxweather.astronomy.SunCalculator
import java.time.ZoneId
import java.time.ZonedDateTime

/** Momento del día derivado del ciclo solar real. */
enum class TimeOfDay { NIGHT, SUNRISE, DAY, SUNSET }

/**
 * Ciclo día/noche local: resuelve el [TimeOfDay] y horas solares para una
 * latitud/longitud. Cachea el cálculo por minuto (evita trigonometría por
 * frame). La posición se usará del Fused Location Provider en FASE 3.
 */
class DayCycle(
    val lat: Double,
    val lon: Double,
    val zone: ZoneId = ZoneId.systemDefault(),
) {

    private var cacheKey = -1L
    private var cachedRise = -1.0
    private var cachedSet = -1.0

    fun sunrise(now: ZonedDateTime): Double {
        ensureCache(now)
        return cachedRise
    }

    fun sunset(now: ZonedDateTime): Double {
        ensureCache(now)
        return cachedSet
    }

    fun timeOfDay(now: ZonedDateTime): TimeOfDay {
        val m = minutes(now)
        val rise = sunrise(now)
        val set = sunset(now)
        if (rise < 0 || set < 0) return TimeOfDay.NIGHT
        return when {
            m in (rise - 45.0)..(rise + 35.0) -> TimeOfDay.SUNRISE
            m in (set - 50.0)..(set + 35.0) -> TimeOfDay.SUNSET
            m in rise..set -> TimeOfDay.DAY
            else -> TimeOfDay.NIGHT
        }
    }

    /** Progreso diurno 0..1 entre salida y puesta (para posicionar el sol). */
    fun dayProgress(now: ZonedDateTime): Float {
        val rise = sunrise(now)
        val set = sunset(now)
        if (rise < 0 || set < 0) return 0f
        val len = set - rise
        if (len <= 0) return 0f
        return (((minutes(now) - rise) / len).toFloat()).coerceIn(0f, 1f)
    }

    private fun ensureCache(now: ZonedDateTime) {
        val key = now.toLocalDate().toEpochDay() * 1440 + now.hour * 60 + now.minute
        if (key == cacheKey) return
        cacheKey = key
        cachedRise = SunCalculator.sunriseMinutes(now, lat, lon)
        cachedSet = SunCalculator.sunsetMinutes(now, lat, lon)
    }

    private fun minutes(now: ZonedDateTime): Double =
        now.hour * 60.0 + now.minute + now.second / 60.0
}