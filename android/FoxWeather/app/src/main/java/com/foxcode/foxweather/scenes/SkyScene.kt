package com.foxcode.foxweather.scenes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Escena unificada Time/Sky (PROJECT.md §12): geometría determinista en
 * coordenadas normalizadas 0..1 para dibujarla igual con android.graphics
 * (wallpaper) o Compose DrawScope (preview). Cubre todos los momentos del
 * día: la posición del sol y de la luna se derivan de la luz del día real
 * y de la fase lunar (SunCalculator / MoonCalculator).
 */
class SkyScene(seed: Int = 7) {

    private val rand = Random(seed)

    data class StarPoint(
        val nx: Float,
        val ny: Float,
        val r: Float,
        val phase: Float,
        val speed: Float,
    )

    data class P(val nx: Float, val ny: Float)

    val stars: List<StarPoint> = (0 until 42).map {
        StarPoint(
            nx = rand.nextFloat(),
            ny = rand.nextFloat() * 0.72f,
            r = 1f + rand.nextFloat() * 0.9f,
            phase = rand.nextFloat() * 6.28f,
            speed = 0.6f + rand.nextFloat() * 1.4f,
        )
    }

    val backCrest: List<P> = crest(0.62f, 0.05f)
    val frontCrest: List<P> = crest(0.78f, 0.10f)

    /** Parpadeo de estrella en 0..1 según el tiempo (en segundos). */
    fun twinkle(star: StarPoint, t: Float): Float {
        val v = abs(sin(t * star.speed + star.phase))
        return 0.25f + 0.55f * v
    }

    /** Posición normalizada del sol a lo largo del arco diurno (0..1). */
    fun sun(dayProgress: Float): P {
        val p = dayProgress.coerceIn(0f, 1f)
        val nx = 0.08f + p * 0.84f
        val ny = 0.74f - 0.62f * sin(p * PI.toFloat())
        return P(nx.coerceIn(0.02f, 0.98f), ny.coerceIn(0.06f, 0.9f))
    }

    /**
     * Posición normalizada de la luna según su edad (0=newa, 0.5=llena).
     * En luna nueva la luna sale y se pone con el sol; la aproximamos
     * recorriendo el cielo de día a noche según la fase.
     */
    fun moon(moonAgeFraction: Float): P {
        val f = moonAgeFraction.coerceIn(0f, 1f)
        val nx = 0.20f + 0.62f * f
        val ny = 0.10f + 0.10f * sin(f * PI.toFloat())
        return P(nx.coerceIn(0.05f, 0.95f), ny.coerceIn(0.05f, 0.35f))
    }

    private fun crest(baseY: Float, amplitude: Float): List<P> {
        val points = ArrayList<P>(9)
        points += P(0f, baseY + rand.nextFloat() * amplitude)
        for (i in 1..7) {
            val nx = i / 8f
            val peak = (rand.nextFloat() - 0.5f) * amplitude
            val interp = (0.5f - abs(nx - 0.5f)) * 0.4f
            points += P(nx, baseY + interp + peak)
        }
        points += P(1f, baseY + rand.nextFloat() * amplitude)
        return points
    }
}