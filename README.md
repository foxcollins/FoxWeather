# FoxWeather

**El clima de tu ciudad, vivo en tu pantalla.**

FoxWeather es una aplicación Android de clima ambiental que transforma la
pantalla de inicio del teléfono en una representación visual del entorno
real: lluvia, nieve, niebla, nubes, viento, tormentas, relámpagos, día,
noche, sol y luna.

No es una app meteorológica tradicional. Es una **capa ambiental dinámica**
sobre el wallpaper que hace que el teléfono reaccione al mundo exterior.

## Principio

La aplicación no debe intentar decirle al usuario *"hoy está lloviendo"*.
Debe hacer que el usuario **sienta visualmente que está lloviendo**. La
información meteorológica alimenta el motor; el producto real es la
experiencia ambiental dinámica.

## Metas

1.  Lluvia convincente.
2.  Clima real (Open-Meteo + ubicación).
3.  Ciclo día/noche + sol/luna.
4.  Efectos avanzados de agua, tormenta y atmósfera.
5.  Optimización y publicación.

## Modos de visualización

-   **Behind Icons** — efectos detrás de los iconos (via `WallpaperService`).
-   **Above Icons** — efectos sobre los iconos (via overlay, experimental).
-   **Wet Glass** — simula un cristal mojado delante de la pantalla (futuro).

## Stack

Kotlin · Android nativo · Jetpack Compose · Canvas/Particle System ·
Shaders · Coroutines/Flow · DataStore · WorkManager ·
Fused Location Provider · Open-Meteo (MVP).

## Documentación

| Archivo      | Contenido                                     |
| ------------ | --------------------------------------------- |
| `PROJECT.md` | Documento maestro completo del proyecto       |
| `ROADMAP.md` | Fases y sprints                               |
| `ARCHITECTURE.md` | Arquitectura de software y módulos        |
| `DECISIONS.md`    | Decisiones técnicas iniciales            |
| `PERFORMANCE.md`  | Rendimiento y optimización de batería    |

Detalle técnico en `docs/`:

-   `docs/PRODUCT.md` — visión, concepto y configuración del usuario.
-   `docs/VISUAL_SYSTEM.md` — sistema visual y escenas.
-   `docs/WEATHER_ENGINE.md` — proveedor meteorológico y estado del clima.
-   `docs/ASTRONOMY_ENGINE.md` — sol, luna, fase lunar, día/noche.
-   `docs/RENDERING_ENGINE.md` — motor de render, partículas y shaders.
-   `docs/ANDROID_LAYERS.md` — caps sobre el launcher (wallpaper/overlay).
-   `docs/BATTERY_OPTIMIZATION.md` — optimización de batería y red.
-   `docs/API.md` — Open-Meteo y flujo de red.
-   `docs/DATA_MODEL.md` — modelos de datos (`WeatherState`, etc.).
-   `docs/PERMISSIONS.md` — permisos y restricciones de Android.
-   `docs/ASSETS.md` — estrategia de assets y licencias.
-   `docs/TESTING.md` — pruebas, dispositivos y criterios de éxito.

## Estado

Fase 0 (preparación). Primera meta técnica:

> Renderizar lluvia animada sobre una pantalla Android y permitir cambiar
> la intensidad en tiempo real.