# API

> Extraído de `PROJECT.md` (secciones 5, 11 y 15). Proveedor meteorológico
> y flujo de red del MVP.

## Proveedor: Open-Meteo

Elegido para el MVP por:

-   API sencilla.
-   No requiere API key para el uso no comercial permitido.
-   Datos meteorológicos suficientes para empezar.
-   No requiere backend propio para el prototipo.

> Antes de una fase comercial: revisar licencia y condiciones de uso y, si es
> necesario, migrar a un proveedor comercial.

## Flujo

```text
GPS → Latitude / Longitude
   ↓
Open-Meteo (WeatherService)
   ↓
WeatherMapper
   ↓
WeatherState → Cache
   ↓
SceneComposer → RenderEngine
```

## Uso responsable

-   No consultar el clima constantemente.
-   Actualizar cada **15–60 min** según la fase del producto y el proveedor.
-   **Ubicación**: usar aproximada cuando baste, cached location, actualizar
    solo ante un cambio significativo, mínima frecuencia necesaria.

## Sin conexión

```text
API unavailable
      ↓
Last known WeatherState
      ↓
Continue visual experience
```

## Módulo

```text
weather/
├── WeatherRepository   (coordina fuente + cache)
├── WeatherService      (llamada a Open-Meteo)
├── WeatherMapper       (respuesta → Estado)
└── WeatherState
```