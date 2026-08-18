package com.foxcode.foxweather.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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

    /**
     * Gotas de cristal: esfera de agua con sombra interior y brillo
     * especular en el cuadrante superior-izquierdo (luz virtual).
     */
    fun DrawScope.drawDroplets(system: DropletSystem) {
        for (d in system.drops) {
            val r = max(d.r, 0.5f)
            val c = Offset(d.x, d.y)
            drawCircle(
                color = GlassDrop.copy(alpha = 0.6f),
                radius = r,
                center = c,
            )
            drawCircle(
                color = GlassShade.copy(alpha = 0.32f),
                radius = r * 0.68f,
                center = Offset(d.x, d.y + r * 0.06f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = r * 0.22f,
                center = Offset(d.x + r * 0.32f, d.y - r * 0.32f),
            )
        }
    }
}