package com.foxcode.foxweather.astronomy

import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Cálculo solar local (algoritmo NOAA simplificado). Devuelve horas de
 * salida y puesta del sol y la elevación solar para una posición y fecha.
 * Sustituye la dependencia de la API meteorológica para la astronomía
 * (PROJECT.md §5, docs/ASTRONOMY_ENGINE.md).
 */
object SunCalculator {

    fun dayOfYear(date: ZonedDateTime): Double =
        date.dayOfYear + (date.hour + date.minute / 60.0) / 24.0

    /** Declinación solar en grados. */
    fun declination(dayOfYear: Double): Double =
        23.44 * sin(toRad(360.0 / 365.0 * (284.0 + dayOfYear)))

    /** Minutos (0..1440) locales de salida del sol. -1 si el sol no sale ese día. */
    fun sunriseMinutes(date: ZonedDateTime, lat: Double, lon: Double): Double =
        solveMinutes(date, lat, lon, rise = true)

    /** Minutos (0..1440) locales de puesta del sol. 1441 si el sol no se pone ese día. */
    fun sunsetMinutes(date: ZonedDateTime, lat: Double, lon: Double): Double =
        solveMinutes(date, lat, lon, rise = false)

    /**
     * Elevación del sol en grados (> 0 de día). Aproximación sinusoidal
     * sobre el arco diurno, suficiente para posicionar el sol en pantalla.
     */
    fun elevationMinutes(date: ZonedDateTime, lat: Double, lon: Double): Double {
        val rise = sunriseMinutes(date, lat, lon)
        val set = sunsetMinutes(date, lat, lon)
        if (rise < 0 || set < 0) return -5.0
        val m = date.hour * 60.0 + date.minute + date.second / 60.0
        val dayLen = set - rise
        if (m < rise || m > set) {
            val nightElapsed = if (m < rise) m + 1440 - set else m - set
            return -5.0 - nightElapsed * 0.02
        }
        val p = (m - rise) / dayLen
        val maxElev = 90.0 - abs(lat - declination(dayOfYear(date)))
        return maxElev * sin(PI * p)
    }

    private fun solveMinutes(date: ZonedDateTime, lat: Double, lon: Double, rise: Boolean): Double {
        val n = dayOfYear(date)
        val lngHour = lon / 15.0
        val t = if (rise) n + (6.0 - lngHour) / 24.0 else n + (18.0 - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        val l = (toDeg(m + 1.916 * sin(toRad(m)) + 0.020 * sin(toRad(2 * m)) + 282.634) % 360 + 360) % 360
        var ra = toDeg(atan(0.91764 * tan(toRad(l))))
        if (ra < 0) ra += 360
        ra += (floor(l / 90.0) - floor(ra / 90.0)) * 90.0
        ra /= 15.0
        val sinDec = 0.39782 * sin(toRad(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(toRad(90.833)) - sinDec * sin(toRad(lat))) / (cosDec * cos(toRad(lat)))
        if (cosH > 1 || cosH < -1) {
            // Sol que no sale (noche polar) o no se pone (día polar)
            return if (rise) -1.0 else -1.0
        }
        val h = if (rise) 360.0 - toDeg(acos(cosH)) else toDeg(acos(cosH))
        val ut = (h / 15.0 + ra - 0.06571 * t - 6.622 - lngHour + 48) % 24
        val offsetMinutes = date.offset.totalSeconds / 60.0
        return (ut * 60.0 + offsetMinutes + 1440) % 1440
    }

    private fun toRad(d: Double): Double = d * PI / 180.0
    private fun toDeg(r: Double): Double = r * 180.0 / PI
}