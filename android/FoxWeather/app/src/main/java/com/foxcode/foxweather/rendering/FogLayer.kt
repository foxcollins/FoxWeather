package com.foxcode.foxweather.rendering

import kotlin.random.Random

/**
 * Capa de niebla (Fog Layer de RENDERING_ENGINE.md). Baños translúcidos
 * horizontales que se desplazan lentamente hacia el viento y se acumulan en
 * el horizonte. Se pinta DESPUÉS de las montañas y ANTES de la lluvia para
 * dar profundidad (niebla de fondo frente al cielo).
 */
class FogLayer(seed: Int = 5) {

    private val rand = Random(seed)

    data class Band(
        val nx: Float,          // inicio x normalizado (-1..1 para desfase)
        val ny: Float,          // altura centro (horizonte arriba)
        val h: Float,           // grosor normalizado
        val alpha: Float,
        val speed: Float,
    )

    val bands = List(4) {
        Band(
            nx = rand.nextFloat() * 2f - 1f,
            ny = 0.62f + rand.nextFloat() * 0.22f,
            h = 0.06f + rand.nextFloat() * 0.10f,
            alpha = 0.10f + rand.nextFloat() * 0.12f,
            speed = 0.02f + rand.nextFloat() * 0.05f,
        )
    }

    fun update(dt: Float, wind: Float) {
        // La niebla es global; solo desplazamos las bandas internamente en update.
    }
}