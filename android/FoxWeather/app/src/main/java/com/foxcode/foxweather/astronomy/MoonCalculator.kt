package com.foxcode.foxweather.astronomy

import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.cos

/**
 * Cálculo lunar local basado en la edad del ciclo sinódico (29.530588853 d)
 * con época en una luna nueva conocida (2000-01-06). Devuelve la fracción
 * de fase (0 = nueva, 0.5 = llena) y la fracción del disco iluminado.
 */
object MoonCalculator {

    val SYNODIC_DAYS = 29.530588853

    fun ageFraction(now: ZonedDateTime): Double {
        val epoch = ZonedDateTime.of(2000, 1, 6, 18, 14, 0, 0, now.zone)
        var age = Duration.between(epoch, now).toSeconds() / 86400.0 % SYNODIC_DAYS
        if (age < 0) age += SYNODIC_DAYS
        return age / SYNODIC_DAYS
    }

    /** Fracción del disco iluminado (0..1). */
    fun illumination(fraction: Double): Double =
        (1 - cos(2 * kotlin.math.PI * fraction)) / 2

    /** true = creciente (luz hacia la derecha), false = menguante. */
    fun waxing(fraction: Double): Boolean = fraction < 0.5
}