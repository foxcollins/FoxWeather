package com.foxcode.foxweather.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.foxcode.foxweather.ui.theme.RainBlue

/**
 * RenderEngine del prototipo: dibuja las gotas dentro de un [DrawScope]
 * (el receiver del composable Canvas). En el futuro se sustituirá el bucle
 * por rendering adaptativo (ver docs/RENDERING_ENGINE.md).
 */
object RenderEngine {

    fun DrawScope.drawRain(
        system: RainParticleSystem,
        alpha: Float = 1f,
    ) {
        for (p in system.particles) {
            drawLine(
                color = RainBlue.copy(alpha = p.opacity * alpha),
                start = Offset(p.x, p.y),
                end = Offset(p.x + p.vx * 0.02f, p.y + p.length),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}