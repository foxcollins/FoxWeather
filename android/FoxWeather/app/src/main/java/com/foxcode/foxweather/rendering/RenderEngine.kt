package com.foxcode.foxweather.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.foxcode.foxweather.environment.TimeOfDay
import com.foxcode.foxweather.scenes.NightScene
import com.foxcode.foxweather.scenes.SkyScene
import com.foxcode.foxweather.ui.theme.DayMountainBack
import com.foxcode.foxweather.ui.theme.DayMountainFront
import com.foxcode.foxweather.ui.theme.DaySkyHorizon
import com.foxcode.foxweather.ui.theme.DaySkyTop
import com.foxcode.foxweather.ui.theme.DuskMountainBack
import com.foxcode.foxweather.ui.theme.DuskMountainFront
import com.foxcode.foxweather.ui.theme.DuskSkyHorizon
import com.foxcode.foxweather.ui.theme.DuskSkyTop
import com.foxcode.foxweather.ui.theme.GlassDrop
import com.foxcode.foxweather.ui.theme.GlassShade
import com.foxcode.foxweather.ui.theme.MoonLight
import com.foxcode.foxweather.ui.theme.MountainBack
import com.foxcode.foxweather.ui.theme.MountainFront
import com.foxcode.foxweather.ui.theme.RainBlue
import com.foxcode.foxweather.ui.theme.SkyHorizon
import com.foxcode.foxweather.ui.theme.SkyTop
import com.foxcode.foxweather.ui.theme.SunLight
import com.foxcode.foxweather.weather.PrecipitationKind
import kotlin.math.max
import kotlin.math.sin

/**
 * RenderEngine del prototipo: dibuja dentro de un [DrawScope] (el receiver
 * del composable Canvas). La escena es una capa de fondo (Time/Sky del
 * sistema de escenas) sobre la que se pinta el clima.
 */
object RenderEngine {

    /** Fondo de escena nocturna: cielo en gradiente, luna, estrellas, montañas. */
    fun DrawScope.drawSkyScene(
        scene: SkyScene,
        tod: TimeOfDay,
        dayProgress: Float,
        moonPhase: Float,
        t: Float,
    ) {
        val w = size.width
        val h = size.height

        val (top, horizon) = when (tod) {
            TimeOfDay.DAY -> DaySkyTop to DaySkyHorizon
            TimeOfDay.SUNRISE, TimeOfDay.SUNSET -> DuskSkyTop to DuskSkyHorizon
            TimeOfDay.NIGHT -> SkyTop to SkyHorizon
        }
        drawRect(
            brush = Brush.verticalGradient(listOf(top, horizon)),
            size = this.size,
        )

        if (tod == TimeOfDay.DAY) {
            drawSun(scene.sun(dayProgress), w, h)
        }
        if (tod == TimeOfDay.NIGHT) {
            drawMoon(scene.moon(moonPhase), w, h)
        }

        if (tod == TimeOfDay.NIGHT || tod == TimeOfDay.SUNRISE) {
            for (s in scene.stars) {
                drawCircle(
                    color = Color.White.copy(alpha = scene.twinkle(s, t)),
                    radius = s.r,
                    center = Offset(s.nx * w, s.ny * h),
                )
            }
        }

        drawCrest(scene.backCrest, backColor(tod), w, h)
        drawCrest(scene.frontCrest, frontColor(tod), w, h)
    }

    private fun backColor(tod: TimeOfDay) = when (tod) {
        TimeOfDay.DAY -> DayMountainBack
        TimeOfDay.NIGHT -> MountainBack
        else -> DuskMountainBack
    }

    private fun frontColor(tod: TimeOfDay) = when (tod) {
        TimeOfDay.DAY -> DayMountainFront
        TimeOfDay.NIGHT -> MountainFront
        else -> DuskMountainFront
    }

    private fun DrawScope.drawSun(p: SkyScene.P, w: Float, h: Float) {
        val sr = w * 0.035f
        val c = Offset(p.nx * w, p.ny * h)
        drawCircle(SunLight.copy(alpha = 0.10f), radius = sr * 2.4f, center = c)
        drawCircle(SunLight.copy(alpha = 0.22f), radius = sr * 1.5f, center = c)
        drawCircle(SunLight, radius = sr, center = c)
    }

    private fun DrawScope.drawMoon(p: SkyScene.P, w: Float, h: Float) {
        val mr = w * 0.02f
        val mc = Offset(p.nx * w, p.ny * h)
        drawCircle(MoonLight.copy(alpha = 0.08f), radius = mr * 1.7f, center = mc)
        drawCircle(MoonLight.copy(alpha = 0.16f), radius = mr * 1.25f, center = mc)
        drawCircle(MoonLight, radius = mr, center = mc)
    }

    private fun DrawScope.drawCrest(crest: List<SkyScene.P>, color: Color, w: Float, h: Float) {
        val path = Path()
        crest.forEachIndexed { i, p ->
            val x = p.nx * w
            val y = p.ny * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        drawPath(path, color)
    }

    /** Precipitación: dibuja según el tipo (líneas = lluvia, puntas = granizo, círculos = nieve). */
    fun DrawScope.drawPrecipitation(system: RainParticleSystem, alpha: Float = 1f) {
        val kind = system.currentKind
        for (p in system.particles) {
            when (kind) {
                PrecipitationKind.SNOW, PrecipitationKind.HAIL -> {
                    val r = p.length * 0.5f
                    drawCircle(
                        color = if (kind == PrecipitationKind.HAIL) Color.Cyan.copy(alpha = p.opacity * alpha) else Color.White.copy(alpha = p.opacity * alpha),
                        radius = r,
                        center = Offset(p.x, p.y),
                    )
                }

                else -> drawLine(
                    color = RainBlue.copy(alpha = p.opacity * alpha),
                    start = Offset(p.x, p.y),
                    end = Offset(p.x + p.vx * 0.02f, p.y + p.length),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    /** Nubes: puffs de color cielo apagado (Cloud Layer). */
    fun DrawScope.drawClouds(system: CloudSystem, cover: Float, alpha: Float = 1f) {
        val w = size.width
        val h = size.height
        for (c in system.clouds) {
            val cx = c.nx * w
            val cy = c.ny * h
            val cw = c.w * w
            val ch = c.h * h
            val col = Color.White.copy(alpha = c.alpha.coerceAtMost(cover * 1.2f) * alpha)
            for (i in c.puffs.indices) {
                val frac = i.toFloat() / (c.puffs.size - 1)
                val px = cx - cw * 0.35f + cw * 0.7f * frac
                val py = cy + sin(frac * 3.14f) * ch * 0.35f
                drawCircle(color = col, radius = c.puffs[i] * cw, center = Offset(px, py))
            }
        }
    }

    /** Niebla: baños anchos translúcidos cerca del horizonte (Fog Layer). */
    fun DrawScope.drawFog(system: FogLayer, alpha: Float = 1f) {
        val w = size.width
        val h = size.height
        for (b in system.bands) {
            val y = b.ny * h
            val bw = w * 1.8f
            val bh = b.h * h
            drawRect(
                color = Color.White.copy(alpha = b.alpha * alpha),
                topLeft = Offset(b.nx * w * 0.5f, y - bh / 2),
                size = androidx.compose.ui.geometry.Size(bw, bh),
            )
        }
    }

    /** Relámpago: flash global + rayo (Lightning Layer). */
    fun DrawScope.drawLightning(system: LightningSystem, time: Float) {
        if (system.flashAlpha > 0.001f) {
            drawRect(
                color = Color.White.copy(alpha = system.flashAlpha),
                size = this.size,
            )
        }
        if (system.active && system.phase == LightningSystem.Phase.STRIKE) {
            val w = size.width
            val h = size.height
            val path = Path()
            system.bolt.forEachIndexed { i, (nx, ny) ->
                val x = nx * w
                val y = ny * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // Añade ramas secundarias cortas para efecto ramificado
            bolatBranch(system.bolt, w, h, path)
            drawPath(path, color = Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = StrokeCap.Round))
            drawPath(path, color = Color(0xFFBBD4FF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
        }
    }

    private fun DrawScope.bolatBranch(bolt: List<Pair<Float, Float>>, w: Float, h: Float, path: Path) {
        if (bolt.size < 2) return
        for (i in 1 until bolt.size step 2) {
            val (nx, ny) = bolt[i]
            val bx = nx * w
            val by = ny * h
            val off = 0.04f + (i % 3) * 0.02f
            path.moveTo(bx, by)
            path.lineTo(bx + off * w * (if (i % 2 == 0) 1f else -1f), by - off * h)
        }
    }

    /**
     * Wet Glass: pantalla mojada con gotas grandes tipo "cuenta de agua".
     * Cada gota:
     *  - interior = lente que refracta el cielo (más claro arriba);
     *  - borde inferior oscuro y grueso (la masa de agua captura la luz);
     *  - arco especular brillante arriba + punto de luz;
     *  - halo de humedad alrededor (refracción suave del contorno);
     *  - corrientes estiradas con estela que lava el cristal.
     */
    fun DrawScope.drawDroplets(
        system: DropletSystem,
        top: Color = SkyTop,
        horizon: Color = SkyHorizon,
    ) {
        // 0) Estelas lavadas (tenue, detrás de todo).
        for (t in system.trails) {
            drawRect(
                color = Color.White.copy(alpha = t.alpha),
                topLeft = Offset(t.x - t.width / 2, t.topY),
                size = Size(t.width, t.length),
            )
        }

        for (d in system.drops) {
            if (d.falling) continue
            val r = max(d.r, 0.5f)
            val x = d.x
            val y = d.y
            val evap = d.evaporating
            val bodyH = r * (if (d.running) 1f + d.stretch else 1f)
            val bodyTop = y - bodyH / 2

            // 1) Halo de humedad alrededor de la gota (refracción suave).
            if (!evap) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = r * 1.35f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = GlassDrop.copy(alpha = 0.12f),
                    radius = r * 1.12f,
                    center = Offset(x, y),
                )
            }

            // 2) Cuerpo-lente: refracta el cielo, con interior luminoso.
            val lens = Path().apply {
                addOval(Rect(x - r, bodyTop, x + r, bodyTop + bodyH))
            }
            clipPath(lens) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to horizon.copy(alpha = 0.55f),
                        0.55f to top.copy(alpha = 0.62f),
                        1f to horizon.copy(alpha = 0.55f),
                    ),
                    size = Size(r * 2f, bodyH),
                    topLeft = Offset(x - r, bodyTop),
                )
                // Borde inferior oscuro (masa de agua).
                drawOval(
                    color = Color.Black.copy(alpha = 0.18f),
                    topLeft = Offset(x - r * 0.95f, y + r * 0.05f),
                    size = Size(r * 1.9f, bodyH * 0.75f),
                )
            }

            // 3) Contorno: borde finísimo más brillante arriba.
            drawOval(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = Offset(x - r, bodyTop),
                size = Size(r * 2f, bodyH),
                style = Stroke(width = max(1f, r * 0.07f)),
            )

            // 4) Borde inferior oscuro grueso: la firma del "agua sobre vidrio".
            if (!evap) {
                drawArc(
                    color = Color.Black.copy(alpha = 0.22f),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(x - r, bodyTop),
                    size = Size(r * 2f, bodyH),
                    style = Stroke(width = max(2f, r * 0.18f), cap = StrokeCap.Round),
                )
            }

            // 5) Highlight principal: arco brillante superior.
            val arcHr = r * 0.62f
            drawArc(
                color = Color.White.copy(alpha = if (evap) 0.25f else 0.9f),
                startAngle = -155f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(x + r * 0.18f - arcHr, y - bodyH * 0.40f - arcHr),
                size = Size(arcHr * 2f, arcHr * 2f),
                style = Stroke(width = max(1.6f, r * 0.16f), cap = StrokeCap.Round),
            )
            // Punto de luz intenso.
            if (r > 3f) {
                drawCircle(
                    color = Color.White.copy(alpha = 1f),
                    radius = r * 0.12f,
                    center = Offset(x + r * 0.34f, y - bodyH * 0.36f),
                )
            }

            // 6) Reflejo inferior de luz (suelo/entorno) en gotas quietas.
            if (!d.running && r > 2.5f && !evap) {
                drawOval(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(x - r * 0.30f, y + r * 0.42f),
                    size = Size(r * 0.6f, r * 0.12f),
                )
            }
        }
    }
}