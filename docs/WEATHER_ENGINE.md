# Motor Meteorológico

> Extraído de `PROJECT.md` (secciones 5, 6, 11 y 15). Detalle de API en
> `docs/API.md` y modelos en `docs/DATA_MODEL.md`.

## Proveedor

**Open-Meteo** para el MVP.

-   API sencilla.
-   No requiere API key para uso no comercial permitido.
-   Datos suficientes para empezar.
-   No requiere backend propio para el prototipo.

En una fase comercial, revisar licencia y condiciones; migrar a un proveedor
comercial si es necesario.

## Flujo

```text
GPS → Latitude / Longitude → Open-Meteo → WeatherState
→ SceneComposer → RenderEngine
```

## Estado

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

## Datos que consumen los efectos

-   condition / tipo de precipitación.
-   precipitation (intensidad).
-   cloudCover (nubosidad).
-   windSpeed (viento, influye en partículas).
-   humidity (niebla/condensación).
-   condition + hora → tormentas/relámpagos.

## Cache y actualización

No consultar el clima constantemente.

```text
Location → Weather Request → Cache → WeatherState
```

-   Actualizar cada 15–60 min según fase del producto y proveedor.
-   Sin conexión: usar el último `WeatherState` y continuar la experiencia
    visual.

## Módulo

```text
weather/
├── WeatherRepository   (coordina fuente + cache)
├── WeatherService      (llamada a Open-Meteo)
├── WeatherMapper       (respuesta → Estado)
└── WeatherState
```