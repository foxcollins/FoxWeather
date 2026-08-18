package com.foxcode.foxweather.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
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
import com.foxcode.foxweather.ui.theme.MoonLight
import com.foxcode.foxweather.ui.theme.MountainBack
import com.foxcode.foxweather.ui.theme.MountainFront
import com.foxcode.foxweather.ui.theme.RainBlue
import com.foxcode.foxweather.ui.theme.SkyHorizon
import com.foxcode.foxweather.ui.theme.SkyTop
import com.foxcode.foxweather.ui.theme.SunLight
import com.foxcode.foxweather.ui.theme.HailColor
import com.foxcode.foxweather.ui.theme.SnowColor
import com.foxcode.foxweather.weather.PrecipitationKind
import com.foxcode.foxweather.weather.WeatherCondition
import com.foxcode.foxweather.weather.WeatherEffects
import com.foxcode.foxweather.weather.WeatherState
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Live wallpaper "detrás de los iconos" (FASE 2, Modo 1 de PROJECT.md).
 * Reutiliza el mismo [RainParticleSystem] del prototipo y lo renderiza en
 * el surface del WallpaperService con un bucle a FPS objetivo.
 *
 * Interacción: tocar cicla la intensidad (LOW -> MED -> HIGH -> LOW).
 * Cuando el wallpaper no es visible, se detiene el bucle (ahorro de batería).
 */
class WeatherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WeatherEngine()

    private inner class WeatherEngine : Engine() {

        private val rain = RainParticleSystem()
        private val handler = Handler(Looper.getMainLooper())
        private val scene = SkyScene()
        private val cycle = DayCycle(SettingsStore.DEFAULT_LAT, SettingsStore.DEFAULT_LON)
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

        private var visible = false
        private var running = false
        private var lastNanos = 0L
        private var targetFps = 30
        private var intensity = RainIntensity.MEDIUM

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
            targetFps = prefs.getInt(KEY_FPS, 30).coerceIn(1, 60)
            recreateBackground()
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

        /** Efecto de partículas para la condición activa. */
        private fun currentEffect() =
            WeatherEffects.resolve(
                WeatherState(
                    condition = currentCondition(),
                    temperature = 20f,
                    precipitation = 5f,
                    windSpeed = 8f,
                    humidity = 60f,
                    cloudCover = 0.7f,
                    timestamp = System.currentTimeMillis(),
                )
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

        override fun onDestroy() {
            stopLoop()
            super.onDestroy()
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