package com.foxcode.foxweather.rendering

import com.foxcode.foxweather.weather.PrecipitationKind
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
 * Sistema de partículas de precipitación multi-condición (lluvia, llovizna,
 * nieve, granizo) con Canvas. Sustituye al prototipo solo-lluvia: recibe el
 * [PrecipitationKind] que el WeatherEffects resuelve desde el WeatherState.
 */
class RainParticleSystem {

    private val random = Random.Default
    private var spawnAccum = 0f
    private var kind = PrecipitationKind.RAIN

    val particles = ArrayList<RainParticle>()

    val count: Int get() = particles.size
    val currentKind: PrecipitationKind get() = kind

    fun update(dt: Float, width: Float, height: Float, intensity: RainIntensity, newKind: PrecipitationKind) {
        if (width <= 0f || height <= 0f) return
        kind = newKind
        val cfg = config(intensity)

        // Cielo despejado: sin precipitación y sin partículas residuales.
        if (newKind == PrecipitationKind.NONE) {
            particles.clear()
            spawnAccum = 0f
            return
        }

        // Spawn
        spawnAccum += cfg.spawnPerSecond * dt
        var spawns = spawnAccum.toInt()
        spawnAccum -= spawns
        while (spawns-- > 0 && particles.size < cfg.maxParticles) {
            particles += newParticle(width, intensity)
        }

        // Movimiento (dt acotado para evitar saltos tras frames omitidos)
        val cappedDt = min(dt, 0.05f)
        for (p in particles) {
            p.x += (p.vx + p.sway) * cappedDt
            p.y += p.vy * cappedDt
        }

        // Reciclar
        val limit = height + 60f
        particles.removeAll { it.y > limit || it.x < -40f || it.x > width + 40f }
    }

    private fun config(intensity: RainIntensity) = when (kind) {
        PrecipitationKind.NONE -> RainIntensity.LOW
        PrecipitationKind.DRIZZLE -> RainIntensity.LOW
        PrecipitationKind.RAIN -> intensity
        PrecipitationKind.SNOW -> RainIntensity.MEDIUM
        PrecipitationKind.HAIL -> RainIntensity.HIGH
    }

    private fun newParticle(width: Float, intensity: RainIntensity): RainParticle {
        val speed = random.nextFloat() * (intensity.maxSpeed - intensity.minSpeed) + intensity.minSpeed
        return when (kind) {
            PrecipitationKind.SNOW -> RainParticle(
                x = random.nextFloat() * width,
                y = -random.nextFloat() * 120f,
                vx = 0f,
                vy = speed * 0.18f,
                length = 2.5f + random.nextFloat() * 2f,
                opacity = random.nextFloat() * 0.3f + 0.65f,
                sway = 18f + random.nextFloat() * 26f,
            )
            PrecipitationKind.HAIL -> RainParticle(
                x = random.nextFloat() * width,
                y = -random.nextFloat() * 120f,
                vx = speed * 0.04f,
                vy = speed * 1.35f,
                length = 3f + random.nextFloat() * 3f,
                opacity = 0.55f + random.nextFloat() * 0.35f,
            )
            PrecipitationKind.DRIZZLE -> RainParticle(
                x = random.nextFloat() * width,
                y = -random.nextFloat() * 120f,
                vx = speed * 0.04f,
                vy = speed * 0.55f,
                length = (random.nextFloat() * 8f + 4f) * 0.4f,
                opacity = random.nextFloat() * 0.2f + 0.3f,
            )
            else -> RainParticle(
                x = random.nextFloat() * width,
                y = -random.nextFloat() * 120f,
                vx = speed * 0.05f,
                vy = speed,
                length = (random.nextFloat() * 14f + 8f) * intensity.lengthFactor,
                opacity = random.nextFloat() * 0.25f + 0.35f,
            )
        }
    }
}