# Roadmap

> Extraído de `PROJECT.md` (secciones 23 y 33). Es la hoja de ruta general
> del producto, de la preparación a la publicación.

## Fases generales

```text
FASE 0  Definir concepto · repositorio · documentación · investigación ·
        APIs
FASE 1  Kotlin · Android project · Canvas · Particle System · Rain
        prototype
FASE 2  Behind Icons · Above Icons · Settings · Performance profiling
FASE 3  Open-Meteo · Location · WeatherState · Cache
FASE 4  Sun · Moon · Sunrise · Sunset · Day/Night · Transitions
FASE 5  Rain · Snow · Fog · Clouds · Lightning · Wind
FASE 6  Wet Glass Shader · Distortion · Droplets · Advanced VFX
FASE 7  Battery optimization · Device compatibility · Launcher
        compatibility · Beta testing
FASE 8  Play Store · Analytics · Crash reporting · Monetization · API
        comercial si es necesario
```

## MVP

### v0.1 — Prototipo de lluvia (sin API)

-   Kotlin + Compose.
-   Pantalla de configuración simple + botones de prueba.
-   Motor de partículas + Canvas.
-   Tres escenas: Clear, Rain, Thunderstorm.
-   Modo Behind Icons y Above Icons (experimental).
-   Control de intensidad y FPS configurable.

### v0.2 — Clima real

-   Open-Meteo + ubicación.
-   `WeatherState`, cache y actualización periódica.

```text
GPS → lat/lng → Open-Meteo → WeatherState → SceneComposer → RenderEngine
```

### v0.3 — Tiempo astronómico

-   Sunrise, sunset, day, night, blue hour, moon, moon phase.
-   Transiciones sin cambios abruptos: `Night → Sunrise → Day`,
    `Day → Sunset → Night`.

### v0.4 — Más efectos

-   Snow, Fog, Clouds, Wind, Stars, Moon, mejoras de lluvia y gotas sobre
    cristal.

### v0.5 — Wet Glass Shader

```text
Base layer + Droplet mask + Distortion + Highlight + Motion
```

Uno de los principales elementos diferenciadores del producto.

### Beta

-   Pruebas multi-dispositivo (baja, media y alta gama).
-   Medición de FPS, CPU, GPU, RAM, temperatura y batería.
-   Compatibilidad con launchers, pantalla apagada, reinicio y
    restricciones de background; permisos de overlay; densidades.

## Sprint 0 — Preparación

1.  Crear repositorio Git.
2.  Crear proyecto Android nativo.
3.  Configurar Kotlin.
4.  Configurar Compose.
5.  Crear pantalla de prueba.
6.  Crear `RenderEngine`.
7.  Crear `ParticleSystem`.
8.  Crear `RainParticle`.
9.  Dibujar lluvia con Canvas.
10. Añadir control de intensidad.
11. Añadir FPS configurable.
12. Medir CPU/GPU.
13. Probar en dispositivo físico.

### Resultado esperado del Sprint 0

```text
┌──────────────────────────────┐
│    💧       💧               │
│          💧        💧        │
│  💧                         │
│             💧               │
│       💧              💧     │
│                              │
│   [ LOW ] [ MED ] [ HIGH ]  │
└──────────────────────────────┘
```

Ese prototipo será la base sobre la que se construirá todo FoxWeather.

## Criterio de éxito del prototipo

-   La lluvia se ve natural.
-   Las partículas no consumen CPU excesivamente.
-   El FPS es estable.
-   Se puede controlar la intensidad.
-   Funciona detrás y (cuando el sistema lo permita) sobre los iconos.
-   No bloquea la interacción del launcher.
-   Puede pausarse y la pantalla vuelve a su estado normal sin reiniciar.