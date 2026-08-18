package com.foxcode.foxweather.rendering

import kotlin.math.abs
import kotlin.random.Random

/**
 * Capa de nubes (Cloud Layer de RENDERING_ENGINE.md). Genera nubes como
 * elipses compuestas en coordenadas normalizadas 0..1 (por "isla"), que se
 * desplazan horizontalmente según el viento. La densidad/número depende del
 * cloudCover (0=despejado, 1=cubierto) que resuelve WeatherEffects.
 */
class CloudSystem(seed: Int = 3) {

    private val rand = Random(seed)

    data class Cloud(
        var nx: Float,
        var ny: Float,
        val w: Float,
        val h: Float,
        val speed: Float,
        val alpha: Float,
        val puffs: List<Float>, // radios de los puffs relativos a w
    )

    val clouds = ArrayList<Cloud>()

    val count: Int get() = clouds.size

    /** Reajusta la cantidad objetivo según la cobertura (solo crece/reduce suavemente). */
    fun setCover(cover: Float) {
        val target = (cover.coerceIn(0f, 1f) * 6).toInt()
        while (clouds.size < target) clouds += newCloud()
        while (clouds.size > target) clouds.removeAt(clouds.size - 1)
    }

    fun update(dt: Float, wind: Float) {
        for (c in clouds) {
            c.nx += c.speed * wind * dt * 0.003f
            c.ny += abs(c.speed) * wind * dt * 0.0004f // leve deriva vertical
            if (c.nx > 1.3f) c.nx = -0.3f
            if (c.nx < -0.3f) c.nx = 1.3f
        }
    }

    fun clear() = clouds.clear()

    private fun newCloud(): Cloud {
        val w = 0.16f + rand.nextFloat() * 0.22f
        val puffs = (2 + rand.nextInt(3)).let { n ->
            List(n) { 0.18f + rand.nextFloat() * 0.22f }
        }
        return Cloud(
            nx = rand.nextFloat() * 1.4f - 0.2f,
            ny = 0.06f + rand.nextFloat() * 0.45f,
            w = w,
            h = w * (0.32f + rand.nextFloat() * 0.14f),
            speed = 0.08f + rand.nextFloat() * 0.25f,
            alpha = 0.5f + rand.nextFloat() * 0.35f,
            puffs = puffs,
        )
    }
}