# Arquitectura

> Extraído de `PROJECT.md` (secciones 4, 5, 6 y 22). Detalle ampliado en
> `docs/DATA_MODEL.md` y `docs/RENDERING_ENGINE.md`.

## Visión conceptual

```text
Weather Provider (Open-Meteo)
        ↓
Weather Engine ──┬── TIME ENGINE   (day / night / sunrise / sunset / blue hour)
                 ├── WEATHER DATA  (rain / snow / clouds / wind / humidity / storm)
                 └── ASTRONOMY     (sun / moon / moon phase)
                        ↓
                 Scene Composer
                        ↓
                 Render Engine ──┬── Canvas / 2D
                                 └── Shaders
                        ↓
                 Screen Presentation ──┬── Behind Icons
                                       └── Above Icons
```

## Principio de diseño

Separar claramente los **datos** de la **representación visual**. El motor
trabaja sobre estados inmutables y el render decide cómo pintarlos.

## Stack tecnológico

-   **Android**: Kotlin, nativo, Jetpack Compose (configuración/UI),
    Coroutines, Flow/StateFlow.
-   **Persistencia**: DataStore (configuración), cache de clima.
-   **Tareas periódicas**: WorkManager.
-   **Ubicación**: Fused Location Provider.
-   **Render**: Android Canvas, Android Graphics, Shaders para efectos
    avanzados.
-   **Dashboard/servicios**: `WallpaperService` (Behind Icons), Application
    Overlay (Above Icons).

## Modelo de estados

```kotlin
data class WeatherState(
    val condition: WeatherCondition,
    val temperature: Double,
    val precipitation: Double,
    val windSpeed: Double,
    val humidity: Double,
    val cloudCover: Double,
    val timestamp: Instant
)

data class EnvironmentState(
    val timeOfDay: TimeOfDay,
    val weather: WeatherState,
    val sunState: SunState,
    val moonState: MoonState
)
```

El `SceneComposer` convierte `EnvironmentState` en una escena visual:

```text
EnvironmentState → SceneComposer → RainScene
                                  + CloudLayer
                                  + LightningController
                                  + WetGlassLayer
                                          ↓
                                  RenderEngine
```

## Estructura de módulos (Android)

Propuesta inicial (`android/FoxWeather`):

```text
app/
├── core/            model · location · network · storage · time
├── weather/         WeatherRepository · WeatherService · WeatherMapper ·
│                    WeatherState
├── astronomy/       SunCalculator · MoonCalculator · AstronomyState
├── environment/     EnvironmentEngine · EnvironmentState · TimeOfDay
├── rendering/       RenderEngine · ParticleSystem · ShaderManager ·
│                    SceneComposer
├── scenes/          rain · snow · storm · sunny · night · sunset · fog
├── wallpaper/       WeatherWallpaperService
├── overlay/         WeatherOverlayService
└── ui/              settings · preview · onboarding
```

La estructura puede evolucionar. No es necesario crear todos los módulos
antes de escribir código.

## Sistema de escenas

No crear una escena por cada combinación posible. Usar capas composables:

```text
SCENE
├── Time Layer       Day / Night / Sunrise / Sunset
├── Sky Layer        Clear / Cloudy / Overcast
├── Sun Layer
├── Moon Layer
├── Cloud Layer
├── Precipitation    Rain / Heavy Rain / Snow
├── Fog Layer
├── Wind Layer
├── Lightning Layer
├── Wet Glass Layer
└── Stars Layer
```

Combinaciones sin duplicar assets ni código: `Sunset + Rain`,
`Night + Moon + Clouds`, `Day + Heavy Rain`, `Night + Thunderstorm`,
`Sunrise + Fog`.