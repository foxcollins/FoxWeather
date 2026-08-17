package com.foxcode.foxweather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.foxcode.foxweather.R
import com.foxcode.foxweather.rendering.RainIntensity
import com.foxcode.foxweather.rendering.RainParticleSystem
import com.foxcode.foxweather.rendering.RenderEngine
import com.foxcode.foxweather.ui.theme.Night
import kotlinx.coroutines.isActive

/**
 * Pantalla de prueba del prototipo (Sprint 0):
 * lluvia en Canvas + control de intensidad + FPS objetivo.
 */
@Composable
fun RainPrototypeScreen(modifier: Modifier = Modifier) {
    var intensity by remember { mutableStateOf(RainIntensity.MEDIUM) }
    var targetFps by remember { mutableIntStateOf(30) }
    var frame by remember { mutableIntStateOf(0) }
    var particleCount by remember { mutableIntStateOf(0) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    val system = remember { RainParticleSystem() }

    LaunchedEffect(intensity, targetFps) {
        var lastRender = 0L
        var nextFrameAt = 0L
        var nextLabelAt = 0L
        val frameNanos = 1_000_000_000L / targetFps
        while (isActive) {
            val now = withFrameNanos { it }
            if (now < nextFrameAt) continue
            if (lastRender == 0L) lastRender = now
            val dt = (now - lastRender) / 1_000_000_000f
            lastRender = now
            nextFrameAt = now + frameNanos
            system.update(
                dt = dt,
                width = screenSize.width.toFloat(),
                height = screenSize.height.toFloat(),
                intensity = intensity,
            )
            if (now >= nextLabelAt) {
                particleCount = system.count
                nextLabelAt = now + 1_000_000_000L
            }
            frame++
        }
    }

    Box(modifier.fillMaxSize().background(Night)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenSize = it }
        ) {
            frame // fuerza el redibujado al cambiar el frame
            with(RenderEngine) {
                drawRain(system)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.screen_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.label_intensity) + " · " + stringResource(R.string.label_fps) +
                    " $targetFps · partículas: $particleCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                RainIntensity.entries.forEach { level ->
                    if (intensity == level) {
                        Button(onClick = { intensity = level }) {
                            Text(level.label)
                        }
                    } else {
                        OutlinedButton(onClick = { intensity = level }) {
                            Text(level.label)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                listOf(15, 30, 60).forEach { fps ->
                    if (targetFps == fps) {
                        Button(onClick = { targetFps = fps }) {
                            Text("${fps} FPS")
                        }
                    } else {
                        OutlinedButton(onClick = { targetFps = fps }) {
                            Text("${fps} FPS")
                        }
                    }
                }
            }
        }
    }
}