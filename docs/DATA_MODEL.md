# Modelo de Datos

> Extraído de `PROJECT.md` (secciones 6 y 22). Define los estados centrales
> que separan datos de representación visual.

## Principio

Separar claramente los **datos** de la **representación visual**. El
`SceneComposer` convierte el estado del entorno en una escena; el render
nunca consulta fuentes externas directamente.

## WeatherState

Representa el clima actual.

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
```

## EnvironmentState

Combina clima + hora + astronomía.

```kotlin
data class EnvironmentState(
    val timeOfDay: TimeOfDay,
    val weather: WeatherState,
    val sunState: SunState,
    val moonState: MoonState
)
```

## SunState / MoonState

```kotlin
data class SunState(
    val sunrise: Instant,
    val sunset: Instant,
    // posición aproximada del sol...
)

data class MoonState(
    val phase: MoonPhase,
    // posición aproximada de la luna...
)
```

## De estado a escena

```text
EnvironmentState
       ↓
SceneComposer
       ↓
RainScene
+ CloudLayer
+ LightningController
+ WetGlassLayer
       ↓
RenderEngine
```

## Configuración del usuario

Preferencias (guardadas con **DataStore**):

```text
Weather Effects    ON / OFF
Display Mode       Behind Icons / Above Icons
Intensity          Low / Medium / High
Animations         Full / Reduced / Off
Lightning          ON / OFF
Thunder Sound      ON / OFF
Moon               ON / OFF
Stars              ON / OFF
Battery Saver      ON / OFF
Update Frequency   Automatic / 15 min / 30 min / 60 min
```

## Ubicación en el mapa de módulos

```text
core/             model · location · network · storage · time
weather/          WeatherRepository · WeatherService · WeatherMapper ·
                  WeatherState
astronomy/        SunCalculator · MoonCalculator · AstronomyState
environment/      EnvironmentEngine · EnvironmentState · TimeOfDay
```