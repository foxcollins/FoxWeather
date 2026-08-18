package com.foxcode.foxweather.rendering

import kotlin.math.cbrt
import kotlin.math.min
import kotlin.random.Random

/**
 * Gota de agua sobre vidrio (Wet Glass, FASE 6). Comportamiento físico:
 *  - pocas gotas nacen por condensación en el tercio superior;
 *  - las chicas se quedan quietas (casi transparentes, casi puntuales);
 *  - las que crecen cruzan el umbral de tensión superficial y empiezan a
 *    deslizar, cada vez más rápido cuanto más grandes;
 *  - una gota "corriente" se estira y deja una estela que lava el cristal;
 *  - al llegar al borde inferior se desprenden y caen.
 */
class Droplet(
    var x: Float,
    var y: Float,
    var r: Float,
    var running: Boolean = false,     // desliza como corriente
    var stretch: Float = 0f,          // elongación vertical 0..1
    var vy: Float = 0f,
    var falling: Boolean = false,
    var evaporating: Boolean = false,
    var wobble: Float = 0f,
)

/** Estela vertical que una gota corriente ha lavado en el vidrio. */
class Trail(
    var x: Float,
    var topY: Float,
    var length: Float,
    var width: Float,
    var alpha: Float,
)

class DropletSystem {

    private val random = Random.Default
    private var spawnAcc = 0f
    private var trailAcc = 0f

    val drops = ArrayList<Droplet>()
    val trails = ArrayList<Trail>()

    val count: Int get() = drops.size

    fun update(dt: Float, width: Float, height: Float, intensity: RainIntensity, maxDrops: Int = 320) {
        if (width <= 0f || height <= 0f || dt <= 0f) return
        val cappedDt = min(dt, 0.05f)
        val density = when (intensity) {
            RainIntensity.LOW -> 0.7f
            RainIntensity.MEDIUM -> 1.3f
            RainIntensity.HIGH -> 1.9f
        }
        val cap = maxDrops.coerceIn(40, 320)

        // Condensación tipo "cristal mojado": gotas grandes y medianas por todo
        // el cristal (más denso arriba), bien separadas entre sí.
        spawnAcc += density * 9f * cappedDt
        while (spawnAcc >= 1f && drops.size < cap) {
            spawnAcc -= 1f
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height * 0.82f
            // Distribución de tamaño: mayoría medianas + algunas grandes.
            val r = when (val roll = random.nextFloat()) {
                in 0f..0.12f -> 8f + random.nextFloat() * 6f      // grandes
                in 0.12f..0.45f -> 4f + random.nextFloat() * 4f   // medianas
                else -> 2f + random.nextFloat() * 2f              // pequeñas
            }
            if (drops.none { d ->
                    val dx = d.x - x
                    val dy = d.y - y
                    dx * dx + dy * dy < (d.r + r + 5f) * (d.r + r + 5f)
                }
            ) {
                drops += Droplet(
                    x = x, y = y, r = r,
                    wobble = random.nextFloat() * (2f * Math.PI).toFloat(),
                )
            }
        }

        for (d in drops) {
            if (d.evaporating) {
                d.r -= (1.3f + density) * cappedDt
                if (d.r <= 0.4f) d.r = -1f
                continue
            }

            if (!d.falling) {
                // Crecen lentamente en reposo; se evaporan con poca probabilidad.
                if (random.nextFloat() < 2f * density * cappedDt && !d.running) d.r += 0.05f
                if (random.nextFloat() < 0.5f * cappedDt) d.r *= 0.9997f

                // Umbral de tensión superficial: gotas grandes corren antes.
                val threshold = if (d.r > 6f) 2.4f else 3.4f
                if (!d.running && d.r > threshold && random.nextFloat() < 0.10f + density * cappedDt * 8f) {
                    d.running = true
                }

                if (d.running) {
                    // Desliza: más rápido cuanto más grande, con leve jadeo.
                    d.vy = (d.r - 1.6f).coerceAtLeast(0f) * (13f + density * 7f)
                    d.stretch = (d.stretch + cappedDt * 1.4f).coerceAtMost(0.5f + d.r * 0.05f)
                    d.y += d.vy * cappedDt
                    d.x += (random.nextFloat() - 0.5f) * 5f * cappedDt
                    addTrail(d)
                }
            } else {
                d.vy += 2400f * cappedDt
                d.y += d.vy * cappedDt
                d.r *= 0.999f
            }

            if (d.y > height - 1f && !d.falling) {
                d.falling = true
                d.vy = 0f
            }
        }

        trailAcc += cappedDt
        for (t in trails) {
            t.alpha -= 0.20f * cappedDt
            t.length *= (1f - 0.12f * cappedDt)
        }
        trails.removeAll { it.alpha <= 0.02f }

        coalesce()
        drops.removeAll { it.r <= 0f || (it.falling && it.y > height + 400f) }
    }

    private fun addTrail(d: Droplet) {
        trailAcc += 0f
        // Busca la estela más cercana y corta en x para fundirla.
        val existing = trails.firstOrNull {
            (it.x - d.x).let { dx -> dx * dx } < (d.r * 1.4f) * (d.r * 1.4f)
        }
        if (existing != null) {
            existing.length += d.vy * 0.05f
            existing.alpha = min(existing.alpha + 0.04f, 0.22f)
            existing.width = maxOf(existing.width, d.r * 0.5f)
        } else if (trails.size < 90) {
            trails += Trail(
                x = d.x,
                topY = d.y - d.r,
                length = d.r * 3f,
                width = d.r * 0.45f,
                alpha = 0.18f,
            )
        }
    }

    /** Junta gotas cercanas (coalescencia con volumen conservado) solo si no corren. */
    private fun coalesce() {
        val n = drops.size
        for (i in 0 until n) {
            val a = drops[i]
            if (a.falling || a.evaporating || a.running) continue
            for (j in i + 1 until n) {
                val b = drops[j]
                if (b.falling || b.evaporating || b.running) continue
                val dx = a.x - b.x
                val dy = a.y - b.y
                val reach = a.r + b.r
                if (dx * dx + dy * dy > reach * reach * 0.64f) continue
                val keep = if (a.r >= b.r) a else b
                val absorb = if (a.r >= b.r) b else a
                keep.r = cbrt(keep.r * keep.r * keep.r + absorb.r * absorb.r * absorb.r).toFloat()
                absorb.r = -1f
                // Una gota que engorda puede cruzar el umbral y empezar a correr.
                if (!keep.running && keep.r > 3f) keep.running = true
            }
        }
    }

    fun clear() {
        drops.clear()
        trails.clear()
    }
}