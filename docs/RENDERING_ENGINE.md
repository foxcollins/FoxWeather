# Motor de Render

> Extraído de `PROJECT.md` (secciones 6, 7, 8, 9, 12 y 22). Aspectos de
> rendimiento en `docs/BATTERY_OPTIMIZATION.md` y `PERFORMANCE.md`.

## Responsabilidades

Convierte el `EnvironmentState` en escenas visuales sobre el wallpaper.

```text
EnvironmentState → SceneComposer → (capas) → RenderEngine → Pantalla
```

## Módulo

```text
rendering/
├── RenderEngine
├── ParticleSystem
├── ShaderManager
└── SceneComposer
scenes/
├── rain/ · snow/ · storm/ · sunny/ · night/ · sunset/ · fog/
```

## Motor de partículas

No usar GIFs/vídeos como mecanismo principal. Sistema de partículas.

Atributos genéricos:

```text
x · y · velocityX · velocityY · scale · rotation · opacity · lifetime ·
type
```

`RainParticle`:

```text
position · velocity · length · thickness · opacity · windInfluence
```

Una sola textura base produce cientos de variaciones. Tipos iniciales:
`RainParticle`, `SnowParticle`, `DustParticle`, `MistParticle`,
`StarParticle`.

## Sistema de escenas

Capas composables (no una escena por combinación):

```text
SCENE
├── Time Layer        Day / Night / Sunrise / Sunset
├── Sky Layer         Clear / Cloudy / Overcast
├── Sun Layer
├── Moon Layer
├── Cloud Layer
├── Precipitation     Rain / Heavy Rain / Snow
├── Fog Layer
├── Wind Layer
├── Lightning Layer
├── Wet Glass Layer
└── Stars Layer
```

Combinaciones: `Sunset + Rain`, `Night + Moon + Clouds`, `Day + Heavy Rain`,
`Night + Thunderstorm`, `Sunrise + Fog`.

## Iluminación ambiental

Las capas de tiempo (amanecer, atardecer, noche, día) ajustan la iluminación
global de forma **progresiva**, nunca con cambios abruptos:

```text
Night → Blue hour → Sunrise → Day
Day → Golden hour → Sunset → Night
```

## LightningController

```text
WAIT → Random delay → Lightning event → Screen flash → Lightning shape
→ Fade → WAIT
```

Combine textura de relámpago, flash global, variación de iluminación y sonido
opcional. Eventos poco frecuentes y aleatorios.

## Shaders

Solo para efectos que lo necesiten. Caso principal: **Wet Glass**.

```text
Wallpaper/Launcher → Distortion Map → Water Mask → Shader → Wet Glass
```

Efectos: refracción, distorsión, blur localizado, highlights, reflejos,
movimiento de gotas, condensación. Para el MVP: **un shader simple de
distorsión** primero.

## Rendering adaptativo

No `while(true) { render() }`. Objetivos iniciales de FPS:

```text
Tormenta fuerte → 30 FPS
Lluvia normal   → 20-30 FPS
Llovizna        → 10-20 FPS
Nieve           → 15-25 FPS
Noche estática  → 0-10 FPS
Sin animación   → 0 FPS
```

Reducir/detener al apagar pantalla, perder visibilidad, animaciones off,
batería baja, calor o limitación del sistema.