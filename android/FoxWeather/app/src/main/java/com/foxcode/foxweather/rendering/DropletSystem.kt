package com.foxcode.foxweather.rendering

import kotlin.math.cbrt
import kotlin.math.min
import kotlin.random.Random

/**
 * Gota de agua sobre un cristal. Comportamiento físico simplificado:
 *  - nace como micro-gota (condensación);
 *  - coalesce al acercarse a otra (volumen conservado);
 *  - desliza más rápido cuanto más grande;
 *  - al llegar al borde inferior se desprende y cae (gota corriente);
 *  - deja residuos (estela) que se evaporan.
 */
class Droplet(
    var x: Float,
    var y: Float,
    var r: Float,
    var vy: Float = 0f,
    var falling: Boolean = false,
    var evaporating: Boolean = false,
    var wobble: Float = 0f,
)

class DropletSystem {

    private val random = Random.Default
    private var spawnAcc = 0f
    private var trailAcc = 0f

    val drops = ArrayList<Droplet>()

    val count: Int get() = drops.size

    fun update(dt: Float, width: Float, height: Float, intensity: RainIntensity) {
        if (width <= 0f || height <= 0f || dt <= 0f) return
        val cappedDt = min(dt, 0.05f)
        val density = when (intensity) {
            RainIntensity.LOW -> 0.5f
            RainIntensity.MEDIUM -> 1f
            RainIntensity.HIGH -> 1.8f
        }

        // Condensación: micro-gotas
        spawnAcc += density * 16f * cappedDt
        while (spawnAcc >= 1f && drops.size < 280) {
            spawnAcc -= 1f
            drops += Droplet(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height * 0.7f,
                r = random.nextFloat() * 2.2f + 1.4f,
                wobble = random.nextFloat() * (2f * Math.PI).toFloat(),
            )
        }

        for (d in drops) {
            if (d.evaporating) {
                d.r -= (2f + density) * cappedDt
                if (d.r <= 0.35f) d.r = -1f // señal de muerte
                continue
            }

            if (!d.falling) {
                // Deslizamiento proporcional al exceso de radio (~>3px se mueve)
                d.vy = (d.r - 3f).coerceAtLeast(0f) * 3.2f
                d.y += d.vy * cappedDt

                // Crecer/evaporar lentamente en reposo
                if (random.nextFloat() < 6f * density * cappedDt) d.r += 0.07f
                if (random.nextFloat() < 3f * cappedDt) d.r *= 0.998f

                // Desprender al llegar al borde inferior
                if (d.y + d.r * 1.4f >= height - 2f) d.falling = true
            } else {
                // Caída libre (gota corriente)
                d.vy += 2600f * cappedDt
                d.r *= 0.998f
                d.y += d.vy * cappedDt
            }
        }

        // Estela: las gotas grandes que deslizan dejan residuos que se evaporan
        trailAcc += cappedDt
        if (trailAcc >= 0.15f) {
            trailAcc = 0f
            val residue = ArrayList<Droplet>()
            for (d in drops) {
                if (!d.falling && d.vy > 4f && random.nextFloat() < 0.25f) {
                    residue += Droplet(
                        x = d.x + (random.nextFloat() - 0.5f) * d.r * 0.6f,
                        y = d.y - d.r * 0.6f,
                        r = d.r * (random.nextFloat() * 0.3f + 0.2f),
                        evaporating = true,
                    )
                }
            }
            drops += residue
        }

        coalesce()
        drops.removeAll { it.r <= 0f }
    }

    /** Junta gotas cercanas: la mayor absorbe a la menor conservando volumen. */
    private fun coalesce() {
        val n = drops.size
        for (i in 0 until n) {
            val a = drops[i]
            if (a.falling || a.evaporating) continue
            for (j in i + 1 until n) {
                val b = drops[j]
                if (b.falling || b.evaporating) continue
                val dx = a.x - b.x
                val dy = a.y - b.y
                val reach = a.r + b.r
                if (dx * dx + dy * dy > reach * reach * 0.81f) continue
                val keep = if (a.r >= b.r) a else b
                val absorb = if (a.r >= b.r) b else a
                keep.r = cbrt(keep.r * keep.r * keep.r + absorb.r * absorb.r * absorb.r).toFloat()
                absorb.r = -1f
            }
        }
    }
}