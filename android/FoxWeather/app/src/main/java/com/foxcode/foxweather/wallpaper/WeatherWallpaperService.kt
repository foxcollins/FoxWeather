package com.foxcode.foxweather.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.toArgb
import com.foxcode.foxweather.environment.DayCycle
import com.foxcode.foxweather.core.storage.SettingsStore
import com.foxcode.foxweather.astronomy.MoonCalculator
import com.foxcode.foxweather.environment.TimeOfDay
import com.foxcode.foxweather.rendering.CloudSystem
import com.foxcode.foxweather.rendering.DropletSystem
import com.foxcode.foxweather.rendering.FogLayer
import com.foxcode.foxweather.rendering.LightningSystem
import com.foxcode.foxweather.rendering.RainIntensity
import com.foxcode.foxweather.rendering.RainParticleSystem
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
import com.foxcode.foxweather.ui.theme.HailColor
import com.foxcode.foxweather.ui.theme.MoonLight
import com.foxcode.foxweather.ui.theme.MountainBack
import com.foxcode.foxweather.ui.theme.MountainFront
import com.foxcode.foxweather.ui.theme.RainBlue
import com.foxcode.foxweather.ui.theme.SkyHorizon
import com.foxcode.foxweather.ui.theme.SkyTop
import com.foxcode.foxweather.ui.theme.SnowColor
import com.foxcode.foxweather.ui.theme.SunLight
import com.foxcode.foxweather.weather.PrecipitationKind
import com.foxcode.foxweather.weather.WeatherCondition
import com.foxcode.foxweather.weather.WeatherEffects
import com.foxcode.foxweather.weather.WeatherState
import com.foxcode.foxweather.weather.OpenMeteoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max

/**
 * Live wallpaper "detrás de los iconos" (FASE 2, Modo 1 de PROJECT.md).
 * Reutiliza el mismo [RainParticleSystem] del prototipo y lo renderiza en
 * el surface del WallpaperService con un bucle a FPS objetivo.
 *
 * Interacción: tocar cicla la intensidad (LOW -> MED -> HIGH -> LOW).
 * Cuando el wallpaper no es visible, se detiene el bucle (ahorro de batería).
 */
class WeatherWallpaperService : WallpaperService() {

    enum class BatteryState { NORMAL, HOT, LOW, CRITICAL }

    override fun onCreateEngine(): Engine = WeatherEngine()

    private inner class WeatherEngine : Engine() {

        private val rain = RainParticleSystem()
        private val handler = Handler(Looper.getMainLooper())
        private val weatherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val scene = SkyScene()
        private var cycle = dayCycle()
        private val clouds = CloudSystem()
        private val fogLayer = FogLayer()
        private val lightning = LightningSystem()
        private val droplets = DropletSystem()
        private var background = BitmapFactory.decodeFile(
            File(cacheDir, SettingsStore.CUSTOM_WALLPAPER_FILE).absolutePath
        )
        private val starPaint = Paint().apply { isAntiAlias = true }
        private val moonPaint = Paint().apply { isAntiAlias = true; color = MoonLight.toArgb() }
        private val sunPaint = Paint().apply { isAntiAlias = true; color = SunLight.toArgb() }
        private val backPaint = Paint().apply { isAntiAlias = true }
        private val frontPaint = Paint().apply { isAntiAlias = true }
        private val imagePaint = Paint().apply { isAntiAlias = true }
        private val rainPaint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            color = RainBlue.toArgb()
        }
        private val precipPaint = Paint().apply { isAntiAlias = true }
        private val cloudPaint = Paint().apply { isAntiAlias = true }
        private val flashPaint = Paint().apply { isAntiAlias = true }
        private val boltPaint = Paint().apply { isAntiAlias = true }

        private var visible = false
        private var running = false
        private var lastNanos = 0L
        private var targetFps = 30
        private var baseFps = 30
        private var intensity = RainIntensity.MEDIUM
        private var battery = BatteryState.NORMAL
        private var frameCounter = 0

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                refreshBattery()
            }
        }

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == SettingsStore.KEY_LATITUDE || key == SettingsStore.KEY_LONGITUDE) {
                cycle = dayCycle()
            }
        }

        private fun dayCycle(): DayCycle {
            val (lat, lon) = SettingsStore.location(applicationContext)
            return DayCycle(lat, lon)
        }

        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) schedule()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            val prefs = prefs()
            intensity = prefs.getString(KEY_INTENSITY, null)
                ?.let { runCatching { RainIntensity.valueOf(it) }.getOrNull() }
                ?: RainIntensity.MEDIUM
            baseFps = prefs.getInt(KEY_FPS, 20).coerceIn(1, 30)
            targetFps = baseFps
            recreateBackground()
            refreshBattery()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            registerReceiver(batteryReceiver, filter)
            prefs().registerOnSharedPreferenceChangeListener(prefsListener)
            refreshWeatherLoop()
        }

        /** Delegado por el ciclo de visibilidad para no chocar con onCreate. */
        private fun refreshWeatherLoop() {
            weatherScope.launch {
                while (isActive) {
                    refreshLiveWeather()
                    delay(30 * 60 * 1000L)
                }
            }
        }

        /** Consulta Open-Meteo para la ubicación guardada y cachea el estado. */
        private suspend fun refreshLiveWeather() {
            val (lat, lon) = SettingsStore.location(applicationContext)
            val ws = runCatching { OpenMeteoClient.currentWeather(lat, lon) }.getOrNull()
                ?: return
            SettingsStore.saveLiveWeather(applicationContext, ws)
        }

        override fun onDestroy() {
            runCatching { unregisterReceiver(batteryReceiver) }
            prefs().unregisterOnSharedPreferenceChangeListener(prefsListener)
            weatherScope.cancel()
            stopLoop()
            super.onDestroy()
        }

        /** Nivel + temperatura de batería. Solo se invoca ante eventos de batería. */
        private fun refreshBattery() {
            val bIntent = registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ) ?: return
            val level = bIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
            val scale = bIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val temp = (bIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250) / 10f).coerceAtLeast(0f)
            val charging = bIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            val pct = if (scale > 0) level * 100 / scale else 100
            battery = when {
                !charging && (pct <= 10 || temp >= 50f) -> BatteryState.CRITICAL
                !charging && (pct <= 25 || temp >= 42f) -> BatteryState.LOW
                temp >= 38f -> BatteryState.HOT
                else -> BatteryState.NORMAL
            }
            applyBatteryPolicy()
        }

        /** Degrada FPS suavemente según el estado de batería (FASE 7). */
        private fun applyBatteryPolicy() {
            when (battery) {
                BatteryState.NORMAL -> targetFps = baseFps
                BatteryState.HOT -> targetFps = max(10, baseFps * 2 / 3)
                BatteryState.LOW -> targetFps = max(8, baseFps / 2)
                BatteryState.CRITICAL -> {
                    targetFps = 6
                    rain.particles.clear()
                    droplets.clear()
                }
            }
        }

        /** Recarga la imagen custom (si cambió en la app) sin reiniciar el servicio. */
        fun recreateBackground() {
            val file = File(applicationContext.cacheDir, SettingsStore.CUSTOM_WALLPAPER_FILE)
            background = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }

        /** Condición activa: predefinida en ajustes o RAIN por defecto (antes de red/FASE 3). */
        private fun currentCondition(): WeatherCondition =
            prefs().getString(KEY_WEATHER, null)
                ?.let { runCatching { WeatherCondition.valueOf(it) }.getOrNull() }
                ?: WeatherCondition.RAIN

        /** Efecto de partículas para la condición activa (clima real si existe). */
        private fun currentEffect() =
            WeatherEffects.resolve(currentState())

        /** Estado real cacheado, o el manual/predefinido si no hay red aún. */
        private fun currentState(): WeatherState =
            SettingsStore.liveWeather(applicationContext) ?: WeatherState(
                condition = currentCondition(),
                temperature = 20f,
                precipitation = 5f,
                windSpeed = 8f,
                humidity = 60f,
                cloudCover = 0.7f,
                timestamp = System.currentTimeMillis(),
            )

        private fun drawBackground(c: Canvas, w: Float, h: Float) {
            val bmp = background ?: return
            val scale = maxOf(w / bmp.width, h / bmp.height)
            val bw = bmp.width * scale
            val bh = bmp.height * scale
            val src = Rect(0, 0, bmp.width, bmp.height)
            val dst = Rect(
                ((w - bw) / 2).toInt(), ((h - bh) / 2).toInt(),
                ((w + bw) / 2).toInt(), ((h + bh) / 2).toInt(),
            )
            c.drawBitmap(bmp, src, dst, imagePaint)
        }

        override fun onVisibilityChanged(v: Boolean) {
            super.onVisibilityChanged(v)
            visible = v
            if (v) startLoop() else stopLoop()
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_UP) {
                intensity = when (intensity) {
                    RainIntensity.LOW -> RainIntensity.MEDIUM
                    RainIntensity.MEDIUM -> RainIntensity.HIGH
                    RainIntensity.HIGH -> RainIntensity.LOW
                }
                prefs().edit().putString(KEY_INTENSITY, intensity.name).apply()
            }
        }

        private fun startLoop() {
            if (running) return
            running = true
            lastNanos = System.nanoTime()
            schedule()
        }

        private fun stopLoop() {
            running = false
            handler.removeCallbacks(frame)
        }

        private fun schedule() {
            handler.postDelayed(frame, 1000L / targetFps)
        }

        private fun drawFrame() {
            var c: Canvas? = null
            try {
                c = surfaceHolder.lockCanvas()
                    ?: return
                val now = System.nanoTime()
                val dt = (now - lastNanos) / 1_000_000_000f
                lastNanos = now
                val w = c.width.toFloat()
                val h = c.height.toFloat()

                if (SettingsStore.backgroundMode(applicationContext) == SettingsStore.BG_IMAGE) {
                    drawBackground(c, w, h)
                } else {
                    drawSceneAndroid(c, w, h, now / 1_000_000_000f)
                }

                val effect = currentEffect()
                rain.update(dt, w, h, effect.intensity, effect.kind)
                clouds.setCover(effect.cloudCover)
                clouds.update(dt, effect.wind)
                lightning.update(dt)
                drawCloudsAndroid(c, w, h)
                if (effect.fog) drawFogAndroid(c, w, h)
                droplets.update(dt, w, h, effect.intensity, maxDrops = 60)
                when (effect.kind) {
                    PrecipitationKind.SNOW, PrecipitationKind.HAIL -> {
                        val isHail = effect.kind == PrecipitationKind.HAIL
                        precipPaint.color = if (isHail) HailColor.toArgb() else SnowColor.toArgb()
                        for (p in rain.particles) {
                            precipPaint.alpha = (p.opacity * 255).toInt()
                            c.drawCircle(p.x, p.y, p.length * 0.5f, precipPaint)
                        }
                    }

                    else -> {
                        for (p in rain.particles) {
                            rainPaint.alpha = (p.opacity * 255).toInt()
                            rainPaint.strokeWidth = 2f
                            c.drawLine(p.x, p.y, p.x + p.vx * 0.02f, p.y + p.length, rainPaint)
                        }
                    }
                }
                if (effect.lightning) drawLightningAndroid(c, w, h)
                // Capa Wet Glass: gotas sobre el cristal en condiciones de lluvia,
                // dibujadas cada 2º frame para reducir coste (FASE 7).
                if (frameCounter % 2 == 0 &&
                    (effect.kind == PrecipitationKind.RAIN || effect.kind == PrecipitationKind.DRIZZLE)
                ) {
                    drawDropletsAndroid(c, w, h)
                }
                frameCounter++
            } finally {
                if (c != null) surfaceHolder.unlockCanvasAndPost(c)
            }
        }

        /** Fondo de escena (cielo + sol/luna + estrellas + montañas) según TimeOfDay. */
        private fun drawSceneAndroid(c: Canvas, w: Float, h: Float, t: Float) {
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val tod = SettingsStore.sceneOverride(applicationContext) ?: cycle.timeOfDay(now)
            val dayProgress = cycle.dayProgress(now)
            val moonPhase = MoonCalculator.ageFraction(now).toFloat()

            val (top, horizon) = when (tod) {
                TimeOfDay.DAY -> DaySkyTop to DaySkyHorizon
                TimeOfDay.SUNRISE, TimeOfDay.SUNSET -> DuskSkyTop to DuskSkyHorizon
                TimeOfDay.NIGHT -> SkyTop to SkyHorizon
            }
            val sky = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h, top.toArgb(), horizon.toArgb(), Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, w, h, sky)

            if (tod == TimeOfDay.DAY) {
                drawSunAndroid(c, scene.sun(dayProgress), w, h)
            } else if (tod == TimeOfDay.NIGHT) {
                drawMoonAndroid(c, scene.moon(moonPhase), w, h)
            }

            if (tod == TimeOfDay.NIGHT || tod == TimeOfDay.SUNRISE) {
                for (s in scene.stars) {
                    starPaint.alpha = (scene.twinkle(s, t) * 255).toInt()
                    c.drawCircle(s.nx * w, s.ny * h, s.r, starPaint)
                }
            }

            backPaint.color = when (tod) {
                TimeOfDay.DAY -> DayMountainBack.toArgb()
                TimeOfDay.NIGHT -> MountainBack.toArgb()
                else -> DuskMountainBack.toArgb()
            }
            frontPaint.color = when (tod) {
                TimeOfDay.DAY -> DayMountainFront.toArgb()
                TimeOfDay.NIGHT -> MountainFront.toArgb()
                else -> DuskMountainFront.toArgb()
            }
            drawCrestAndroid(c, scene.backCrest, backPaint, w, h)
            drawCrestAndroid(c, scene.frontCrest, frontPaint, w, h)
        }

        private fun drawSunAndroid(c: Canvas, p: SkyScene.P, w: Float, h: Float) {
            val sr = w * 0.035f
            val sx = p.nx * w
            val sy = p.ny * h
            sunPaint.alpha = 26
            c.drawCircle(sx, sy, sr * 2.4f, sunPaint)
            sunPaint.alpha = 56
            c.drawCircle(sx, sy, sr * 1.5f, sunPaint)
            sunPaint.alpha = 255
            c.drawCircle(sx, sy, sr, sunPaint)
        }

        private fun drawMoonAndroid(c: Canvas, p: SkyScene.P, w: Float, h: Float) {
            val mr = w * 0.02f
            val mx = p.nx * w
            val my = p.ny * h
            moonPaint.alpha = 20
            c.drawCircle(mx, my, mr * 1.7f, moonPaint)
            moonPaint.alpha = 42
            c.drawCircle(mx, my, mr * 1.25f, moonPaint)
            moonPaint.alpha = 242
            c.drawCircle(mx, my, mr, moonPaint)
        }

        private fun drawCloudsAndroid(c: Canvas, w: Float, h: Float) {
            val cover = currentEffect().cloudCover
            for (cloud in clouds.clouds) {
                val cx = cloud.nx * w
                val cy = cloud.ny * h
                val cw = cloud.w * w
                val ch = cloud.h * h
                cloudPaint.color = Color.WHITE
                val baseAlpha = (cloud.alpha.coerceAtMost(cover * 1.2f) * 255).toInt()
                for (i in cloud.puffs.indices) {
                    val frac = i.toFloat() / (cloud.puffs.size - 1)
                    val px = cx - cw * 0.35f + cw * 0.7f * frac
                    val py = cy + kotlin.math.sin(frac * 3.14f) * ch * 0.35f
                    cloudPaint.alpha = baseAlpha
                    c.drawCircle(px, py, cloud.puffs[i] * cw, cloudPaint)
                }
            }
        }

        private fun drawFogAndroid(c: Canvas, w: Float, h: Float) {
            for (b in fogLayer.bands) {
                val y = b.ny * h
                val bw = w * 1.8f
                val bh = b.h * h
                cloudPaint.color = Color.WHITE
                cloudPaint.alpha = (b.alpha * 255).toInt()
                c.drawRect(b.nx * w * 0.5f, y - bh / 2, b.nx * w * 0.5f + bw, y + bh / 2, cloudPaint)
            }
        }

        private fun drawLightningAndroid(c: Canvas, w: Float, h: Float) {
            if (lightning.flashAlpha > 0.001f) {
                flashPaint.color = Color.WHITE
                flashPaint.alpha = (lightning.flashAlpha * 255).toInt()
                c.drawRect(0f, 0f, w, h, flashPaint)
            }
            if (lightning.active && lightning.phase == LightningSystem.Phase.STRIKE) {
                val path = Path()
                lightning.bolt.forEachIndexed { i, (nx, ny) ->
                    if (i == 0) path.moveTo(nx * w, ny * h) else path.lineTo(nx * w, ny * h)
                }
                boltPaint.color = Color.WHITE
                boltPaint.strokeWidth = 3f
                c.drawPath(path, boltPaint)
                boltPaint.color = Color.rgb(0xBB, 0xD4, 0xFF)
                boltPaint.strokeWidth = 1.5f
                c.drawPath(path, boltPaint)
            }
        }

        /** Wet Glass (android.graphics): pantalla mojada con gotas grandes tipo "cuenta de agua". */
        private fun drawDropletsAndroid(c: Canvas, w: Float, h: Float) {
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val tod = SettingsStore.sceneOverride(applicationContext) ?: cycle.timeOfDay(now)
            val (top, horizon) = when (tod) {
                TimeOfDay.DAY -> DaySkyTop to DaySkyHorizon
                TimeOfDay.SUNRISE, TimeOfDay.SUNSET -> DuskSkyTop to DuskSkyHorizon
                TimeOfDay.NIGHT -> SkyTop to SkyHorizon
            }
            val lensPaint = Paint().apply { isAntiAlias = true }
            val borderPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
            val trailPaint = Paint().apply { isAntiAlias = true }
            val hiPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
            val fillPaint = Paint().apply { isAntiAlias = true }
            val haloPaint = Paint().apply { isAntiAlias = true }

            // 0) Estelas lavadas.
            for (t in droplets.trails) {
                trailPaint.color = android.graphics.Color.WHITE
                trailPaint.alpha = (t.alpha * 255).toInt()
                c.drawRect(t.x - t.width / 2, t.topY, t.x + t.width / 2, t.topY + t.length, trailPaint)
            }

            for (d in droplets.drops) {
                if (d.falling) continue
                val r = max(d.r, 0.5f)
                val x = d.x
                val y = d.y
                val evap = d.evaporating
                val bodyH = r * (if (d.running) 1f + d.stretch else 1f)
                val bodyTop = y - bodyH / 2

                // 1) Halo de humedad.
                if (!evap) {
                    haloPaint.color = android.graphics.Color.WHITE
                    haloPaint.alpha = 26
                    c.drawCircle(x, y, r * 1.35f, haloPaint)
                    haloPaint.color = GlassDrop.toArgb()
                    haloPaint.alpha = 32
                    c.drawCircle(x, y, r * 1.12f, haloPaint)
                }

                // 2) Cuerpo-lente con interior luminoso.
                val save = c.save()
                val lens = Path().apply { addOval(android.graphics.RectF(x - r, bodyTop, x + r, bodyTop + bodyH), Path.Direction.CW) }
                c.clipPath(lens)
                lensPaint.shader = LinearGradient(
                    x - r, bodyTop, x - r, bodyTop + bodyH,
                    intArrayOf(horizon.toArgb(), top.toArgb(), horizon.toArgb()),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
                )
                lensPaint.alpha = 150
                c.drawRect(x - r, bodyTop, x + r, bodyTop + bodyH, lensPaint)
                // Masa de agua: borde inferior oscuro.
                fillPaint.color = android.graphics.Color.argb(45, 0, 0, 0)
                c.drawOval(x - r * 0.95f, y + r * 0.05f, x + r * 0.95f, y + r * 0.05f + bodyH * 0.75f, fillPaint)
                c.restoreToCount(save)

                // 3) Contorno brillante fino.
                borderPaint.color = android.graphics.Color.WHITE
                borderPaint.alpha = 80
                borderPaint.strokeWidth = max(1f, r * 0.07f)
                c.drawOval(x - r, bodyTop, x + r, bodyTop + bodyH, borderPaint)

                // 4) Borde inferior oscuro grueso.
                if (!evap) {
                    borderPaint.color = android.graphics.Color.BLACK
                    borderPaint.alpha = 60
                    borderPaint.strokeWidth = max(2f, r * 0.18f)
                    val bottomArc = Path()
                    bottomArc.addArc(android.graphics.RectF(x - r, bodyTop, x + r, bodyTop + bodyH), 20f, 140f)
                    c.drawPath(bottomArc, borderPaint)
                }

                // 5) Highlight: arco brillante superior.
                hiPaint.color = android.graphics.Color.WHITE
                hiPaint.alpha = if (evap) 60 else 230
                hiPaint.strokeWidth = max(1.6f, r * 0.16f)
                val arcHr = r * 0.62f
                val topArc = Path()
                topArc.addArc(android.graphics.RectF(x + r * 0.18f - arcHr, y - bodyH * 0.40f - arcHr, x + r * 0.18f + arcHr, y - bodyH * 0.40f + arcHr), -155f, 80f)
                c.drawPath(topArc, hiPaint)
                if (r > 3f) {
                    fillPaint.color = android.graphics.Color.WHITE
                    fillPaint.alpha = 255
                    c.drawCircle(x + r * 0.34f, y - bodyH * 0.36f, r * 0.12f, fillPaint)
                }

                // 6) Reflejo inferior en gotas quietas.
                if (!d.running && r > 2.5f && !evap) {
                    fillPaint.color = android.graphics.Color.WHITE
                    fillPaint.alpha = 42
                    c.drawOval(x - r * 0.30f, y + r * 0.42f, x - r * 0.30f + r * 0.6f, y + r * 0.42f + r * 0.12f, fillPaint)
                }
            }
        }

        private fun drawCrestAndroid(
            c: Canvas,
            crest: List<SkyScene.P>,
            paint: Paint,
            w: Float,
            h: Float,
        ) {
            val path = Path()
            crest.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.nx * w, p.ny * h)
                else path.lineTo(p.nx * w, p.ny * h)
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            c.drawPath(path, paint)
        }
    }

    private fun WeatherEngine.prefs() =
        applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS = "foxweather_settings"
        const val KEY_INTENSITY = "intensity"
        const val KEY_FPS = "fps"
        const val KEY_WEATHER = "weather"
    }
}