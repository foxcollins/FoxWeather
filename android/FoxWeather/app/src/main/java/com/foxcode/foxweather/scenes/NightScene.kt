package com.foxcode.foxweather.scenes

import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Escena nocturna de referencia (capa Time/Sky de PROJECT.md §12).
 * Genera geometría determinista (estrellas, montañas, luna) en coordenadas
 * normalizadas 0..1 para poder dibujarla igual en android.graphics.Canvas
 * (wallpaper) o en Compose DrawScope (preview).
 */
class NightScene(seed: Int = 7) {

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

    val moonX = 0.78f
    val moonY = 0.16f

    val backCrest: List<P> = crest(0.62f, 0.05f)
    val frontCrest: List<P> = crest(0.78f, 0.10f)

    /** Parpadeo de estrella en 0..1 según el tiempo (en segundos). */
    fun twinkle(star: StarPoint, t: Float): Float {
        val v = abs(sin(t * star.speed + star.phase))
        return 0.25f + 0.55f * v
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