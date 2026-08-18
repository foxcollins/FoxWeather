package com.foxcode.foxweather.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.foxcode.foxweather.R
import com.foxcode.foxweather.astronomy.MoonCalculator
import com.foxcode.foxweather.core.storage.SettingsStore
import com.foxcode.foxweather.environment.DayCycle
import com.foxcode.foxweather.environment.TimeOfDay
import com.foxcode.foxweather.rendering.DropletSystem
import com.foxcode.foxweather.rendering.RainIntensity
import com.foxcode.foxweather.rendering.RainParticleSystem
import com.foxcode.foxweather.rendering.RenderEngine
import com.foxcode.foxweather.scenes.SkyScene
import com.foxcode.foxweather.ui.theme.Night
import com.foxcode.foxweather.wallpaper.WeatherWallpaperService
import com.foxcode.foxweather.weather.WeatherCondition
import com.foxcode.foxweather.weather.WeatherEffects
import com.foxcode.foxweather.weather.WeatherState
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.ZonedDateTime

private enum class SceneMode(val labelRes: Int) {
    RAIN(R.string.label_rain),
    CRYSTAL(R.string.label_crystal),
}

/**
 * Pantalla de prueba del prototipo (Sprint 0):
 * - LLUVIA: partículas de lluvia en Canvas.
 * - CRISTAL: gotas de agua que condensan, se deslizan, coalescen y caen.
 * Control de intensidad (LOW/MED/HIGH) y FPS objetivo (15/30/60).
 */
@Composable
fun RainPrototypeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(SceneMode.RAIN) }
    var intensity by remember { mutableStateOf(RainIntensity.MEDIUM) }
    var condition by remember { mutableStateOf(WeatherCondition.RAIN) }
    var sceneOverride by remember { mutableStateOf<TimeOfDay?>(null) }
    var targetFps by remember { mutableIntStateOf(30) }
    var frame by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var animT by remember { mutableFloatStateOf(0f) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var showWeather by remember { mutableStateOf(false) }

    val rain = remember { RainParticleSystem() }
    val droplets = remember { DropletSystem() }
    val scene = remember { SkyScene() }
    val cycle = remember { DayCycle(SettingsStore.DEFAULT_LAT, SettingsStore.DEFAULT_LON) }

    LaunchedEffect(mode, intensity, condition, targetFps) {
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
            val w = screenSize.width.toFloat()
            val h = screenSize.height.toFloat()
            val ws = WeatherState(
                condition = condition,
                temperature = 20f,
                precipitation = 5f,
                windSpeed = 8f,
                humidity = 60f,
                cloudCover = 0.7f,
                timestamp = System.currentTimeMillis(),
            )
            val effect = WeatherEffects.resolve(ws)
            when (mode) {
                SceneMode.RAIN -> rain.update(dt, w, h, intensity, effect.kind)
                SceneMode.CRYSTAL -> droplets.update(dt, w, h, intensity)
            }
            if (now >= nextLabelAt) {
                count = if (mode == SceneMode.RAIN) rain.count else droplets.count
                nextLabelAt = now + 1_000_000_000L
            }
            animT += dt
            frame++
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val file = File(context.cacheDir, SettingsStore.CUSTOM_WALLPAPER_FILE)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { out -> input.copyTo(out) }
            }
            SettingsStore.prefs(context).edit()
                .putString(SettingsStore.KEY_BG_MODE, SettingsStore.BG_IMAGE)
                .putBoolean(SettingsStore.KEY_CUSTOM_WALLPAPER, true)
                .apply()
        }
    }

    Box(modifier.fillMaxSize().background(Night)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenSize = it }
        ) {
            frame // fuerza el redibujado al cambiar el frame
            val now = ZonedDateTime.now()
            val tod = sceneOverride ?: cycle.timeOfDay(now)
            with(RenderEngine) {
                drawSkyScene(scene, tod, cycle.dayProgress(now), MoonCalculator.ageFraction(now).toFloat(), animT)
                when (mode) {
                    SceneMode.RAIN -> drawPrecipitation(rain)
                    SceneMode.CRYSTAL -> drawDroplets(droplets)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.screen_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SceneMode.entries.forEach { m ->
                    if (mode == m) {
                        Button(onClick = { mode = m }) {
                            Text(stringResource(m.labelRes))
                        }
                    } else {
                        OutlinedButton(onClick = { mode = m }) {
                            Text(stringResource(m.labelRes))
                        }
                    }
                }
            }
            Text(
                text = stringResource(
                    if (mode == SceneMode.RAIN) R.string.label_particles
                    else R.string.label_drops
                ) + " · " + stringResource(R.string.label_fps) + " $targetFps · " + count,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
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
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showWeather = !showWeather }) {
                Text(stringResource(R.string.label_weather))
            }
            if (showWeather) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    WeatherCondition.entries.forEach { c ->
                        OutlinedButton(onClick = {
                            condition = c
                            SettingsStore.prefs(context).edit()
                                .putString(SettingsStore.KEY_WEATHER, c.name)
                                .apply()
                        }) {
                            Text(c.name)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(onClick = { sceneOverride = null }) {
                        Text("AUTO")
                    }
                    listOf(TimeOfDay.DAY, TimeOfDay.SUNSET, TimeOfDay.NIGHT).forEach { t ->
                        if (sceneOverride == t) {
                            Button(onClick = { sceneOverride = t }) {
                                Text(t.name)
                            }
                        } else {
                            OutlinedButton(onClick = { sceneOverride = t }) {
                                Text(t.name)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(onClick = {
                        SettingsStore.prefs(context).edit()
                            .putString(SettingsStore.KEY_BG_MODE, SettingsStore.BG_SCENE)
                            .apply()
                    }) {
                        Text(stringResource(R.string.label_bg_scene))
                    }
                    OutlinedButton(onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text(stringResource(R.string.label_bg_image))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, WeatherWallpaperService::class.java),
                    )
                }
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.label_activate_wallpaper))
            }
        }
    }
}