package com.foxcode.foxweather.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.foxcode.foxweather.R
import com.foxcode.foxweather.astronomy.MoonCalculator
import com.foxcode.foxweather.core.storage.SettingsStore
import com.foxcode.foxweather.environment.DayCycle
import com.foxcode.foxweather.environment.TimeOfDay
import com.foxcode.foxweather.rendering.DropletSystem
import com.foxcode.foxweather.rendering.CloudSystem
import com.foxcode.foxweather.rendering.FogLayer
import com.foxcode.foxweather.rendering.LightningSystem
import com.foxcode.foxweather.rendering.RainIntensity
import com.foxcode.foxweather.rendering.RainParticleSystem
import com.foxcode.foxweather.rendering.RenderEngine
import com.foxcode.foxweather.scenes.SkyScene
import com.foxcode.foxweather.ui.theme.Night
import com.foxcode.foxweather.wallpaper.WeatherWallpaperService
import com.foxcode.foxweather.weather.WeatherCondition
import com.foxcode.foxweather.weather.WeatherEffects
import com.foxcode.foxweather.weather.WeatherState
import com.foxcode.foxweather.weather.OpenMeteoClient
import com.foxcode.foxweather.weather.GeoPlace
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime

private enum class SceneMode(val labelRes: Int) {
    RAIN(R.string.label_rain),
    CRYSTAL(R.string.label_crystal),
}

private fun defaultWs() = WeatherState(
    condition = WeatherCondition.RAIN,
    temperature = 20f,
    precipitation = 5f,
    windSpeed = 8f,
    humidity = 60f,
    cloudCover = 0.7f,
    timestamp = System.currentTimeMillis(),
)

private fun conditionName(condition: WeatherCondition): Int = when (condition) {
    WeatherCondition.CLEAR -> R.string.condition_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.condition_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.condition_cloudy
    WeatherCondition.OVERCAST -> R.string.condition_overcast
    WeatherCondition.FOG -> R.string.condition_fog
    WeatherCondition.DRIZZLE -> R.string.condition_drizzle
    WeatherCondition.RAIN -> R.string.condition_rain
    WeatherCondition.HEAVY_RAIN -> R.string.condition_heavy_rain
    WeatherCondition.THUNDERSTORM -> R.string.condition_thunderstorm
    WeatherCondition.SNOW -> R.string.condition_snow
    WeatherCondition.SLEET -> R.string.condition_sleet
    WeatherCondition.HAIL -> R.string.condition_hail
}

private fun phaseName(tod: TimeOfDay): Int = when (tod) {
    TimeOfDay.DAY -> R.string.phase_day
    TimeOfDay.NIGHT -> R.string.phase_night
    TimeOfDay.SUNRISE -> R.string.phase_sunrise
    TimeOfDay.SUNSET -> R.string.phase_sunset
}

private fun currentLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return runCatching {
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        ).mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time ?: 0L }
    }.getOrNull()
}

/**
 * Pantalla del prototipo con aspecto de app real:
 * - Búsqueda de ciudad en vivo (autocompletar sin botón).
 * - Botón de geolocalización (GPS) para usar la ubicación real.
 * - Temperatura, condición, fase del día (día/tarde/noche según hora real) y detalles.
 * - Panel DEV colapsado para calibrar render (modo, clima manual, FPS, escena).
 */
@OptIn(ExperimentalLayoutApi::class)
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
    var showDev by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var activeEffect by remember { mutableStateOf(WeatherEffects.resolve(defaultWs())) }

    val rain = remember { RainParticleSystem() }
    val droplets = remember { DropletSystem() }
    val scene = remember { SkyScene() }
    var cycle by remember { mutableStateOf(run { val (la, lo) = SettingsStore.location(context); DayCycle(la, lo) }) }
    val clouds = remember { CloudSystem() }
    val fog = remember { FogLayer() }
    val lightning = remember { LightningSystem() }

    var cityName by remember { mutableStateOf(SettingsStore.cityName(context) ?: "") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeoPlace>>(emptyList()) }
    var liveWeather by remember { mutableStateOf<WeatherState?>(SettingsStore.liveWeather(context)) }
    val scope = rememberCoroutineScope()

    // Al abrir: refresca el clima de la ubicación ya guardada para tener datos vivos.
    LaunchedEffect(Unit) {
        val (lat, lon) = SettingsStore.location(context)
        val ws = runCatching { OpenMeteoClient.currentWeather(lat, lon) }.getOrNull()
        if (ws != null) {
            liveWeather = ws
            SettingsStore.saveLiveWeather(context, ws)
            condition = ws.condition
        }
    }

    fun applyPlace(place: GeoPlace) {
        showSearch = false
        cycle = DayCycle(place.lat, place.lon)
        SettingsStore.saveLocation(context, place.lat, place.lon, place.toString())
        cityName = place.toString()
        scope.launch {
            val ws = OpenMeteoClient.currentWeather(place.lat, place.lon)
            liveWeather = ws
            SettingsStore.saveLiveWeather(context, ws)
            condition = ws.condition
        }
    }

    // Búsqueda en vivo: autocompletar mientras se escribe (debounce 350 ms).
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        results = OpenMeteoClient.searchCity(q)
    }

    // GPS: geolocalización con ubicación real y reverse geocoding.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val loc = currentLocation(context)
            if (loc != null) {
                scope.launch {
                    OpenMeteoClient.reverseGeocode(loc.latitude, loc.longitude)?.let { applyPlace(it) }
                }
            }
        }
    }

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
            val ws = liveWeather ?: WeatherState(
                condition = condition,
                temperature = 20f,
                precipitation = 5f,
                windSpeed = 8f,
                humidity = 60f,
                cloudCover = 0.7f,
                timestamp = System.currentTimeMillis(),
            )
            val effect = WeatherEffects.resolve(ws)
            activeEffect = effect
            clouds.setCover(effect.cloudCover)
            clouds.update(dt, effect.wind)
            lightning.update(dt)
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

    val now = ZonedDateTime.now()
    val tod = sceneOverride ?: cycle.timeOfDay(now)
    val ws = liveWeather
    val temp = ws?.temperature?.toInt() ?: 20

    Box(modifier.fillMaxSize().background(Night)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenSize = it }
        ) {
            frame // fuerza el redibujado al cambiar el frame
            val n = ZonedDateTime.now()
            val timeOfDay = sceneOverride ?: cycle.timeOfDay(n)
            val effect = activeEffect
            with(RenderEngine) {
                drawSkyScene(scene, timeOfDay, cycle.dayProgress(n), MoonCalculator.ageFraction(n).toFloat(), animT)
                drawClouds(clouds, effect.cloudCover)
                if (effect.fog) drawFog(fog)
                when (mode) {
                    SceneMode.RAIN -> {
                        drawPrecipitation(rain)
                        if (effect.lightning) drawLightning(lightning, animT)
                    }
                    SceneMode.CRYSTAL -> {
                        val (top, horizon) = when (timeOfDay) {
                            TimeOfDay.DAY -> com.foxcode.foxweather.ui.theme.DaySkyTop to com.foxcode.foxweather.ui.theme.DaySkyHorizon
                            TimeOfDay.SUNRISE, TimeOfDay.SUNSET -> com.foxcode.foxweather.ui.theme.DuskSkyTop to com.foxcode.foxweather.ui.theme.DuskSkyHorizon
                            TimeOfDay.NIGHT -> com.foxcode.foxweather.ui.theme.SkyTop to com.foxcode.foxweather.ui.theme.SkyHorizon
                        }
                        drawDroplets(droplets, top, horizon)
                    }
                }
            }
        }

        // Cabecera: ciudad + fase del día (según hora real).
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(onClick = { showSearch = true }) {
                Text(
                    text = if (cityName.isBlank()) stringResource(R.string.label_my_location) else cityName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(phaseName(tod)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        // Contenido principal: temperatura grande + condición + detalles.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$temp°",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 88.sp,
                color = Color.White,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = stringResource(conditionName(ws?.condition ?: condition)),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val wind = ws?.windSpeed?.toInt()?.toString() ?: "--"
                val humidity = ws?.humidity?.toInt()?.toString() ?: "--"
                val precip = ws?.precipitation?.toString() ?: "--"
                Detail(stringResource(R.string.label_wind), wind, if (wind == "--") "" else "km/h")
                Detail(stringResource(R.string.label_humidity), humidity, if (humidity == "--") "" else "%")
                Detail(stringResource(R.string.label_precip), precip, if (precip == "--") "" else "mm")
            }
        }

        // Selector de ubicación (in-vivo + GPS).
        if (showSearch) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xE6000000), MaterialTheme.shapes.medium)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_placeholder), color = Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                    )
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val loc = currentLocation(context)
                            if (loc != null) {
                                scope.launch {
                                    OpenMeteoClient.reverseGeocode(loc.latitude, loc.longitude)?.let { applyPlace(it) }
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    }) {
                        Text("GPS")
                    }
                    TextButton(onClick = { showSearch = false }) {
                        Text(stringResource(R.string.label_close), color = Color.White)
                    }
                }
                if (query.trim().length >= 2) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        results.forEach { place ->
                            OutlinedButton(onClick = {
                                applyPlace(place)
                                query = ""
                                results = emptyList()
                            }) {
                                Text(place.toString(), color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Barra inferior: corazón + acceso a panel DEV.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(onClick = { showDev = !showDev }) {
                Text(
                    text = stringResource(R.string.label_dev),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            if (showDev) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xE6000000), MaterialTheme.shapes.medium)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            if (mode == SceneMode.RAIN) R.string.label_particles
                            else R.string.label_drops
                        ) + " · " + stringResource(R.string.label_fps) + " $targetFps · " + count,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(8.dp))
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        WeatherCondition.entries.forEach { c ->
                            val selected = condition == c
                            if (selected) {
                                Button(onClick = {
                                    condition = c
                                    liveWeather = null
                                    SettingsStore.prefs(context).edit()
                                        .putString(SettingsStore.KEY_WEATHER, c.name)
                                        .apply()
                                }) {
                                    Text(if (c.name.length > 8) c.name.take(6) + "." else c.name)
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    condition = c
                                    liveWeather = null
                                    SettingsStore.prefs(context).edit()
                                        .putString(SettingsStore.KEY_WEATHER, c.name)
                                        .apply()
                                }) {
                                    Text(if (c.name.length > 8) c.name.take(6) + "." else c.name)
                                }
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
                    Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Detail(labels: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text = labels,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}