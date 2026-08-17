package com.foxcode.foxweather.rendering

import kotlin.math.min
import kotlin.random.Random

/** Niveles de intensidad del prototipo (visuales, no carga real del dispositivo). */
enum class RainIntensity(
    val label: String,
    val maxParticles: Int,
    val spawnPerSecond: Int,
    val minSpeed: Float,
    val maxSpeed: Float,
    val lengthFactor: Float,
) {
    LOW("LOW", 60, 12, 260f, 340f, 0.4f),
    MEDIUM("MED", 160, 36, 340f, 460f, 0.7f),
    HIGH("HIGH", 320, 90, 440f, 620f, 1.0f),
}

/**
 * Sistema de partículas de lluvia para el prototipo (Sprint 0).
 * Sin texturas: las gotas se dibujan como líneas con Canvas.
 */
class RainParticleSystem {

    private val random = Random.Default
    private var spawnAccum = 0f

    val particles = ArrayList<RainParticle>()

    val count: Int get() = particles.size

    fun update(dt: Float, width: Float, height: Float, intensity: RainIntensity) {
        if (width <= 0f || height <= 0f) return

        // Spawn
        spawnAccum += intensity.spawnPerSecond * dt
        var spawns = spawnAccum.toInt()
        spawnAccum -= spawns
        while (spawns-- > 0 && particles.size < intensity.maxParticles) {
            particles += newParticle(width, intensity)
        }

        // Movimiento (dt acotado para evitar saltos tras frames omitidos)
        val cappedDt = min(dt, 0.05f)
        for (p in particles) {
            p.x += p.vx * cappedDt
            p.y += p.vy * cappedDt
        }

        // Reciclar
        particles.removeAll { it.y > height + it.length }
    }

    private fun newParticle(width: Float, intensity: RainIntensity): RainParticle {
        val speed = random.nextFloat() * (intensity.maxSpeed - intensity.minSpeed) + intensity.minSpeed
        return RainParticle(
            x = random.nextFloat() * width,
            y = -random.nextFloat() * 120f,
            vx = speed * 0.05f,
            vy = speed,
            length = (random.nextFloat() * 14f + 8f) * intensity.lengthFactor,
            opacity = random.nextFloat() * 0.25f + 0.35f,
        )
    }
}