# Producto

> Extraído de `PROJECT.md` (secciones 1, 2, 14-18, 21, 28, 30, 31 y 35).

## Visión

FoxWeather es una aplicación Android de clima ambiental que transforma la
pantalla de inicio del teléfono en una representación visual del entorno
real. **No reemplaza necesariamente el wallpaper del usuario**: superpone o
renderiza efectos ambientales sobre él y, cuando el usuario lo permite,
sobre la pantalla de inicio y sus iconos.

> **El clima de tu ciudad, vivo en tu pantalla.**

Combina: clima actual y pronóstico, hora local, amanecer/atardecer,
día/noche y blue hour, posición/fase aproximada de la luna, lluvia, nieve,
niebla, nubosidad, viento, tormentas, relámpagos, gotas y cristal mojado,
cambios de iluminación y animaciones suaves y eficientes.

El producto debe sentirse como una **capa ambiental dinámica**, no como otra
app meteorológica.

## Concepto

El wallpaper original permanece visible; FoxWeather agrega una capa visual:

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

### Modos

-   **Behind Icons**: `Wallpaper → Effects → Launcher/Icons`.
-   **Above Icons**: `Wallpaper → Launcher/Icons → Overlay`.
-   **Wet Glass**: superficie de cristal delante de la pantalla (gotas,
    movimiento, condensación, refracción, distorsión, reflejos, blur).

El usuario elige el modo.

## Escenas de referencia

-   Día despejado (☀️), día nublado (☁️), llovizna (🌦️), lluvia (🌧️),
    lluvia intensa (🌧️🌧️🌧️), tormenta (⛈️).
-   Amanecer: `Noche → Blue hour → Amanecer → Sunrise → Día`.
-   Atardecer: `Día → Golden hour → Atardecer → Noche → Luna`.
-   Noche despejada (🌙 + estrellas), noche nublada (luna parcialmente
    oculta).

## MVP

El primer MVP **no** tiene: cuentas, backend, IA, sistema social,
monetización, demasiados proveedores, pronóstico complejo ni veinte tipos de
clima.

> **La pantalla del teléfono puede reaccionar visualmente al clima real de
> forma atractiva y eficiente.**

-   **v0.1** — Prototipo de lluvia (sin API): Kotlin, pantalla de
    configuración simple, botones de prueba, motor de partículas, Canvas,
    tres escenas (Clear/Rain/Thunderstorm), Behind Icons, Above Icons
    experimental, control de intensidad y FPS.
-   **v0.2** — Open-Meteo, ubicación, `WeatherState`, cache y actualización
    periódica.
-   **v0.3** — Tiempo astronómico (sunrise/sunset/day/night/blue hour/moon/
    fase) y transiciones sin cambios abruptos.
-   **v0.4** — Snow, Fog, Clouds, Wind, Stars, Moon, mejoras de lluvia y
    gotas sobre cristal.
-   **v0.5** — Wet Glass Shader (base + droplet mask + distorsión +
    highlight + motion).

## Configuración del usuario

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

## Sistema de calidad visual

Priorizar: naturalidad, suavidad, bajo consumo, transparencia, coherencia
con el clima real, transiciones suaves.

Evitar: lluvia demasiado artificial, partículas repetitivas, efectos
excesivamente intensos, flashes constantes, consumo elevado, overlays que
dificulten tocar iconos, animaciones que parezcan un vídeo en loop.

## Moneda / evolución comercial

-   **Free**: clima básico, lluvia, sol, noche, luna, algunas animaciones.
-   **Premium**: Wet Glass, tormentas avanzadas, relámpagos, nieve, más
    escenas, personalización, sonido, más controles.

Opciones futuras: compra única, suscripción, paquetes de efectos, temas
estacionales. La monetización se decide después de validar experiencia y
retención.

## Largo plazo

Evolucionar de *Weather App* a *Ambient Environment Engine*: estaciones, auroras,
meteoros, eclipses, lluvia de estrellas, eventos regionales y festivos. La
arquitectura debe permitir agregar capas sin reescribir el motor.

## Objetivo final

```text
MUNDO REAL → (WEATHER + TIME) → ASTRONOMY → ENVIRONMENT ENGINE
→ SCENE ENGINE → (PARTICLES + SHADERS) → ANDROID
→ (Behind Icons | Above Icons) → EXPERIENCIA AMBIENTAL
```

Metas: lluvia convincente → clima real → ciclo día/noche + sol/luna →
efectos avanzados → optimización y publicación.