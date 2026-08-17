# Sistema Visual

> Extraído de `PROJECT.md` (secciones 2, 3, 7, 8, 9, 12, 13 y 20).
> Detalle de render en `docs/RENDERING_ENGINE.md`.

## Concepto

El wallpaper del usuario debe permanecer visible. FoxWeather agrega efectos
ambientales encima o detrás de los iconos.

```text
┌──────────────────────────────────┐
│ 🌧️      💧                       │
│         💧          ⚡            │
│                                  │
│       📱      📱       📱        │
│           💧                     │
│                                  │
│      💧                🌧️        │
└──────────────────────────────────┘
```

## Modos

-   **Behind Icons**: `Wallpaper → Weather Effects → Launcher/Icons`.
-   **Above Icons**: `Wallpaper → Launcher/Icons → Weather Overlay`.
-   **Wet Glass**: cristal delante de la pantalla (gotas adheridas, gotas en
    movimiento, condensación, refracción, distorsión, reflejos, blur
    localizado).

## Escenas

| Condición | Características                                            |
| --------- | ---------------------------------------------------------- |
| ☀️ Day    | Cielo luminoso, iluminación ambiental, partículas opcionales |
| ☁️ Nube   | Cielo apagado, nubes dinámicas, iluminación difusa          |
| 🌦️ Llovizna | Pocas gotas, movimiento lento, baja intensidad             |
| 🌧️ Lluvia | Más partículas, gotas grandes, gotas en cristal, salpicaduras |
| 🌧️ Intensa | Alta densidad, mayor velocidad, viento, cristal            |
| ⛈️ Tormenta | Lluvia intensa, nubes oscuras, relámpagos, flash, viento, trueno |
| 🌅 Amanecer | Transición progresiva: Noche→Blue hour→Amanecer→Sunrise→Día |
| 🌇 Atardecer | Día→Golden hour→Atardecer→Noche→Luna                      |
| 🌙 Noche    | Estrellas, cielo oscuro, iluminación nocturna              |
| ☁️🌙 Nube noche | Luna parcialmente oculta                                |

## Sistema de escenas (capas composables)

No crear una escena por combinación. Composar capas:

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

## Motor de partículas

No usar GIFs ni vídeos como mecanismo principal. Sistema de partículas:

```text
RainParticle → position · velocity · length · thickness · opacity ·
               windInfluence
```

Una sola textura base produce cientos de variaciones. Tipos iniciales:
`RainParticle`, `SnowParticle`, `DustParticle`, `MistParticle`,
`StarParticle`.

## Shaders

Solo para efectos que lo necesiten. Caso de uso principal: **Wet Glass**.

```text
Wallpaper/Launcher → Distortion Map → Water Mask → Shader → Wet Glass
```

Efectos posibles: refracción, distorsión, blur localizado, highlights,
reflejos, movimiento de gotas, condensación. Para el MVP basta un **shader
simple de distorsión**.

## Relámpagos

`LightningController` (sin sistema costoso):

```text
WAIT → Random delay → Lightning event → Screen flash → Lightning shape
→ Fade → WAIT
```

Combina textura de relámpago, flash global, variación de iluminación y sonido
opcional. Eventos poco frecuentes y aleatorios.

## Calidad visual

Priorizar: naturalidad, suavidad, bajo consumo, transparencia, coherencia
con el clima real, transiciones suaves.

Evitar: lluvia artificial, partículas repetitivas, efectos intensos, flashes
constantes, consumo elevado, overlays que bloqueen iconos, animaciones tipo
vídeo en loop.