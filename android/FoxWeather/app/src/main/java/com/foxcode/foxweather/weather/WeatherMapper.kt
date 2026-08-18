package com.foxcode.foxweather.weather

/**
 * Mapeo de códigos WMO de Open-Meteo a [WeatherCondition].
 * Tabla oficial WMO-4677 usada por el proveedor (docs/API.md FASE 3).
 */
object WeatherMapper {

    fun conditionFromWmo(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.OVERCAST
        45, 48 -> WeatherCondition.FOG
        51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE
        61, 63, 65, 80, 81, 82 -> WeatherCondition.RAIN
        66, 67 -> WeatherCondition.SLEET
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95 -> WeatherCondition.THUNDERSTORM
        96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLEAR
    }
}
