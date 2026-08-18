package com.foxcode.foxweather.rendering

import kotlin.math.sin
import kotlin.random.Random

/**
 * Capa de relámpagos (Lightning Layer de RENDERING_ENGINE.md).
 * Máquina de estados: WAIT (espera aleatoria) -> BUILD -> FLASH (iluminación
 * del cielo) -> STRIKE (trazado del rayo) -> FADE. El destello ilumina la
 * escena globalmente; el rayo es una polilínea fractal ramificada.
 */
class LightningSystem(seed: Int = 11) {

    private val rand = Random(seed)

    enum class Phase { IDLE, STRIKE, FADE }

    var phase = Phase.IDLE
    var t = 0f
    var strikeDuration = 0f

    /** Destello global 0..1 (multiplicador de luz sobre el cielo). */
    var flashAlpha = 0f
        private set

    /** Puntos normalizados del rayo (x 0..1, y 0..1). */
    var bolt: List<Pair<Float, Float>> = emptyList()
        private set

    private var waitTime = nextWait()

    fun update(dt: Float) {
        t += dt
        when (phase) {
            Phase.IDLE -> if (t >= waitTime) startStrike()
            Phase.STRIKE -> {
                if (t >= strikeDuration) {
                    phase = Phase.FADE
                    t = 0f
                }
                // Flash intenso mientras hay rayo (con microparpadeo)
                val flicker = 0.75f + 0.25f * sin(t * 90f)
                flashAlpha = 0.55f * flicker
            }
            Phase.FADE -> {
                flashAlpha = (0.55f * (1f - t / 0.4f)).coerceAtLeast(0f)
                if (t >= 0.4f) {
                    phase = Phase.IDLE
                    t = 0f
                    waitTime = nextWait()
                }
            }
        }
    }

    private fun startStrike() {
        phase = Phase.STRIKE
        t = 0f
        strikeDuration = 0.12f + rand.nextFloat() * 0.08f
        bolt = buildBolt()
    }

    private fun nextWait(): Float = 2f + rand.nextFloat() * 7f

    /** Polilínea fractal: divide verticalmente la caída del rayo. */
    private fun buildBolt(): List<Pair<Float, Float>> {
        val x0 = 0.7f + rand.nextFloat() * 0.2f
        val points = ArrayList<Pair<Float, Float>>()
        points += x0 to 0.06f
        var y = 0.06f
        var x = x0
        while (y < 0.72f) {
            val jump = 0.08f + rand.nextFloat() * 0.10f
            y += jump
            x += (rand.nextFloat() - 0.5f) * 0.22f
            points += x.coerceIn(0.05f, 0.95f) to y
        }
        return points
    }

    val active: Boolean get() = phase != Phase.IDLE
}