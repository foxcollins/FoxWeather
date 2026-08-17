# FoxWeather --- Documento Maestro del Proyecto

## 1. Visión

**FoxWeather** es una aplicación Android de clima ambiental que
transforma la pantalla de inicio del teléfono en una representación
visual del entorno real.

La aplicación **no reemplaza necesariamente el fondo de pantalla del
usuario**. Su objetivo principal es superponer o renderizar efectos
ambientales sobre el wallpaper y, cuando el usuario lo permita, sobre la
pantalla de inicio y sus iconos.

La idea central:

> **El clima de tu ciudad, vivo en tu pantalla.**

La aplicación debe combinar:

-   Clima actual y pronóstico.
-   Hora local.
-   Amanecer y atardecer.
-   Día, noche y blue hour.
-   Posición/fase aproximada de la luna.
-   Lluvia.
-   Nieve.
-   Niebla.
-   Nubosidad.
-   Viento.
-   Tormentas.
-   Relámpagos.
-   Efectos de gotas y cristal mojado.
-   Cambios de iluminación ambiental.
-   Animaciones suaves y eficientes.

El objetivo no es crear otra aplicación meteorológica tradicional. El
producto debe sentirse como una **capa ambiental dinámica** que hace que
el teléfono reaccione al mundo exterior.

------------------------------------------------------------------------

# 2. Concepto visual

El wallpaper original del usuario debe permanecer visible.

Ejemplo:

``` text
┌──────────────────────────────────┐
│                                  │
│       WALLPAPER DEL USUARIO      │
│                                  │
│       📱      📱       📱        │
│                                  │
│             WIDGET               │
│                                  │
│                                  │
└──────────────────────────────────┘
```

FoxWeather agrega una capa visual:

``` text
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

La capa puede funcionar en diferentes modos:

### Modo 1 --- Behind Icons

Los efectos aparecen detrás de los iconos.

``` text
Wallpaper
    ↓
Weather Effects
    ↓
Launcher / Icons
```

### Modo 2 --- Above Icons

Los efectos aparecen sobre los iconos.

``` text
Wallpaper
    ↓
Launcher / Icons
    ↓
Weather Overlay
```

### Modo 3 --- Wet Glass

Se simula que existe una superficie de cristal delante de la pantalla:

-   gotas adheridas;
-   gotas desplazándose;
-   condensación;
-   refracción;
-   distorsión;
-   reflejos;
-   blur localizado.

El usuario debe poder elegir el modo.

------------------------------------------------------------------------

# 3. Ejemplos de escenas

## Día despejado

``` text
☀️
Cielo luminoso
Iluminación ambiental
Pequeñas partículas opcionales
```

## Día nublado

``` text
☁️ ☁️
Cielo más apagado
Nubes dinámicas
Iluminación difusa
```

## Llovizna

``` text
🌦️
Pocas gotas
Movimiento lento
Baja intensidad
```

## Lluvia

``` text
🌧️
Más partículas
Gotas de mayor tamaño
Gotas adheridas al cristal
Pequeñas salpicaduras
```

## Lluvia intensa

``` text
🌧️🌧️🌧️
Alta densidad de partículas
Mayor velocidad
Viento
Gotas grandes
Efectos de cristal
```

## Tormenta

``` text
⛈️
Lluvia intensa
Nubes oscuras
Relámpagos
Flash de iluminación
Viento
Truenos opcionales
```

## Amanecer

La transición debe ser progresiva:

``` text
🌌 Noche
  ↓
🔵 Blue hour
  ↓
🌄 Amanecer
  ↓
🌅 Sunrise
  ↓
☀️ Día
```

## Atardecer

``` text
☀️ Día
  ↓
🌇 Golden hour
  ↓
🌆 Atardecer
  ↓
🌌 Noche
  ↓
🌙 Luna
```

## Noche despejada

``` text
🌙
✨ estrellas
Cielo oscuro
Iluminación ambiental nocturna
```

## Noche nublada

``` text
☁️
      🌙
☁️
```

La luna puede quedar parcialmente oculta detrás de las nubes.

------------------------------------------------------------------------

# 4. Arquitectura conceptual

``` text
                         ┌─────────────────────┐
                         │   Weather Provider  │
                         │   Open-Meteo        │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Weather Engine    │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
             TIME ENGINE       WEATHER DATA      ASTRONOMY
                 │                  │                  │
                 │                  │                  ├── Sun
                 │                  │                  ├── Moon
                 │                  │                  └── Moon Phase
                 │                  │
                 ├── Day            ├── Rain
                 ├── Night          ├── Snow
                 ├── Sunrise        ├── Clouds
                 ├── Sunset         ├── Wind
                 └── Blue Hour      ├── Humidity
                                    └── Storm
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │ Scene Composer   │
                                  └────────┬─────────┘
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │ Render Engine    │
                                  └────────┬─────────┘
                                           │
                              ┌────────────┴────────────┐
                              ▼                         ▼
                         Canvas / 2D                Shaders
                              │                         │
                              └────────────┬────────────┘
                                           ▼
                                  Screen Presentation
                                           │
                              ┌────────────┴────────────┐
                              ▼                         ▼
                       Behind Icons               Above Icons
```

------------------------------------------------------------------------

# 5. Stack tecnológico

## Android

-   Kotlin.
-   Android nativo.
-   Jetpack Compose para configuración y UI.
-   Coroutines.
-   Flow / StateFlow.
-   DataStore.
-   WorkManager.
-   Fused Location Provider.
-   WallpaperService.
-   Application Overlay cuando corresponda.
-   Android Canvas.
-   Android Graphics.
-   Shaders para efectos avanzados.

## Datos meteorológicos

Primera opción:

**Open-Meteo**

Ventajas para el MVP:

-   API sencilla.
-   No requiere API key para el uso no comercial permitido.
-   Datos meteorológicos suficientes para empezar.
-   No requiere backend propio para el prototipo.

En una fase comercial se debe revisar la licencia y condiciones de uso
y, si es necesario, migrar a un proveedor comercial.

## Datos astronómicos

No es necesario depender completamente de la API meteorológica.

El proyecto puede calcular localmente:

-   día/noche;
-   sunrise;
-   sunset;
-   blue hour;
-   posición aproximada del sol;
-   fase lunar;
-   posición aproximada de la luna.

Esto reduce dependencia de servicios externos.

------------------------------------------------------------------------

# 6. Arquitectura de software recomendada

Separar claramente los datos de la representación visual.

## WeatherState

Objeto central que representa el entorno actual.

Ejemplo conceptual:

``` kotlin
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

``` kotlin
data class EnvironmentState(
    val timeOfDay: TimeOfDay,
    val weather: WeatherState,
    val sunState: SunState,
    val moonState: MoonState
)
```

## Scene

El `SceneComposer` convierte el `EnvironmentState` en una escena visual.

Ejemplo:

``` text
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

------------------------------------------------------------------------

# 7. Motor de partículas

No utilizar GIFs o vídeos como mecanismo principal.

Crear un sistema de partículas.

Cada partícula puede tener:

``` text
x
y
velocityX
velocityY
scale
rotation
opacity
lifetime
type
```

Ejemplo:

``` text
RainParticle
    ↓
position
velocity
length
thickness
opacity
windInfluence
```

Una sola textura base puede producir cientos de variaciones.

Tipos iniciales:

-   RainParticle.
-   SnowParticle.
-   DustParticle.
-   MistParticle.
-   StarParticle.

------------------------------------------------------------------------

# 8. Shaders

Los shaders deben utilizarse para los efectos que realmente lo
necesitan.

Principal caso de uso:

## Wet Glass

Conceptualmente:

``` text
Wallpaper / Launcher
        ↓
Distortion Map
        ↓
Water Mask
        ↓
Shader
        ↓
Wet Glass Result
```

Efectos posibles:

-   refracción;
-   distorsión;
-   blur localizado;
-   highlights;
-   reflejos;
-   movimiento de gotas;
-   condensación.

No es necesario implementar todos estos efectos en el MVP.

Primero crear un shader simple de distorsión.

------------------------------------------------------------------------

# 9. Relámpagos

Los relámpagos no necesitan un sistema costoso.

Implementar un `LightningController`.

``` text
WAIT
  ↓
Random delay
  ↓
Lightning event
  ↓
Screen flash
  ↓
Lightning shape
  ↓
Fade
  ↓
WAIT
```

Puede combinar:

-   textura de relámpago;
-   flash global;
-   variación de iluminación;
-   sonido opcional.

Los eventos deben ser poco frecuentes y aleatorios.

------------------------------------------------------------------------

# 10. Optimización de batería

La batería es una prioridad del proyecto.

No mantener una carga gráfica máxima permanentemente.

Evitar:

``` text
while(true) {
    render()
}
```

Usar rendering adaptativo.

Ejemplo:

``` text
Tormenta fuerte → 30 FPS
Lluvia normal   → 20-30 FPS
Llovizna        → 10-20 FPS
Nieve           → 15-25 FPS
Noche estática  → 0-10 FPS
Sin animación   → 0 FPS
```

Los valores anteriores son objetivos iniciales, no requisitos
definitivos. Deben validarse mediante profiling en dispositivos reales.

La aplicación debe reducir o detener efectos cuando:

-   la pantalla está apagada;
-   la aplicación no está visible cuando corresponda;
-   el usuario desactiva animaciones;
-   la batería está baja;
-   el dispositivo está caliente;
-   el sistema limita la actividad.

------------------------------------------------------------------------

# 11. Optimización de red

No consultar el clima constantemente.

Flujo recomendado:

``` text
Location
   ↓
Weather Request
   ↓
Cache
   ↓
WeatherState
```

Actualizar según necesidad, por ejemplo cada 15--60 minutos en función
de la fase del producto y del proveedor.

La ubicación tampoco debe solicitarse constantemente.

Usar:

-   ubicación aproximada cuando sea suficiente;
-   cached location;
-   actualización cuando exista un cambio significativo;
-   mínima frecuencia necesaria.

Si no hay conexión:

``` text
API unavailable
      ↓
Last known WeatherState
      ↓
Continue visual experience
```

------------------------------------------------------------------------

# 12. Sistema de escenas

No crear una escena independiente para cada combinación posible.

Usar capas composables.

``` text
SCENE
│
├── Time Layer
│   ├── Day
│   ├── Night
│   ├── Sunrise
│   └── Sunset
│
├── Sky Layer
│   ├── Clear
│   ├── Cloudy
│   └── Overcast
│
├── Sun Layer
│
├── Moon Layer
│
├── Cloud Layer
│
├── Precipitation Layer
│   ├── Rain
│   ├── Heavy Rain
│   └── Snow
│
├── Fog Layer
│
├── Wind Layer
│
├── Lightning Layer
│
├── Wet Glass Layer
│
└── Stars Layer
```

Esto permite combinaciones:

``` text
Sunset + Rain
Night + Moon + Clouds
Day + Heavy Rain
Night + Thunderstorm
Sunrise + Fog
```

sin duplicar assets ni código.

------------------------------------------------------------------------

# 13. Assets

Buscar assets en:

-   Unity Asset Store.
-   itch.io.
-   Kenney.
-   OpenGameArt.
-   Envato Elements.
-   Otros marketplaces de VFX y game assets.

Términos de búsqueda:

``` text
rain particles
rain VFX
weather VFX
rain overlay
rain on glass
water droplets
wet glass shader
water droplet shader
lightning VFX
thunderstorm VFX
snow particles
fog overlay
cloud particles
```

Pero el objetivo final no debe ser depender de cientos de assets.

La estrategia debe ser:

``` text
Pocos assets base
        +
Particle System
        +
Shaders
        +
Procedural variation
        =
Muchos efectos visuales
```

Siempre verificar licencia comercial antes de utilizar un asset en una
aplicación publicada.

------------------------------------------------------------------------

# 14. MVP

El primer MVP NO debe tener:

-   cuentas;
-   backend;
-   IA;
-   sistema social;
-   monetización;
-   demasiados proveedores;
-   pronóstico complejo;
-   veinte tipos de clima.

El MVP debe demostrar una sola cosa:

> **La pantalla del teléfono puede reaccionar visualmente al clima real
> de forma atractiva y eficiente.**

## MVP v0.1

Crear una app Android con:

-   Kotlin.
-   Pantalla de configuración simple.
-   Botones de prueba.
-   Motor de partículas.
-   Canvas.
-   Tres escenas:
    -   ☀️ Clear.
    -   🌧️ Rain.
    -   ⛈️ Thunderstorm.
-   Modo Behind Icons.
-   Modo Above Icons experimental.
-   Control de intensidad.
-   FPS configurable.

Sin API todavía.

------------------------------------------------------------------------

# 15. MVP v0.2

Agregar:

-   Open-Meteo.
-   Ubicación.
-   WeatherState.
-   Cache.
-   Actualización periódica.
-   Condiciones reales.

Flujo:

``` text
GPS
 ↓
Latitude / Longitude
 ↓
Open-Meteo
 ↓
WeatherState
 ↓
SceneComposer
 ↓
RenderEngine
```

------------------------------------------------------------------------

# 16. MVP v0.3

Agregar tiempo astronómico:

-   sunrise;
-   sunset;
-   day;
-   night;
-   blue hour;
-   moon;
-   moon phase.

Crear transiciones.

Ejemplo:

``` text
Night → Sunrise → Day
Day → Sunset → Night
```

No realizar cambios abruptos.

------------------------------------------------------------------------

# 17. MVP v0.4

Agregar:

-   Snow.
-   Fog.
-   Clouds.
-   Wind.
-   Stars.
-   Moon.
-   Mejoras de lluvia.
-   Gotas sobre cristal.

------------------------------------------------------------------------

# 18. MVP v0.5

Implementar Wet Glass Shader:

``` text
Base layer
+
Droplet mask
+
Distortion
+
Highlight
+
Motion
```

Este es uno de los principales elementos diferenciadores del producto.

------------------------------------------------------------------------

# 19. Beta

Realizar pruebas en varios dispositivos Android.

Medir:

-   FPS.
-   CPU.
-   GPU.
-   RAM.
-   temperatura;
-   consumo de batería;
-   estabilidad;
-   comportamiento con diferentes launchers;
-   comportamiento con pantalla apagada;
-   comportamiento después de reiniciar el teléfono;
-   permisos de overlay;
-   restricciones de background;
-   diferentes densidades de pantalla.

Dispositivos objetivo iniciales:

-   Android de gama baja.
-   Android de gama media.
-   Android moderno de gama alta.

El producto debe degradar efectos visuales antes que degradar la
experiencia general del teléfono.

------------------------------------------------------------------------

# 20. Sistema de calidad visual

El proyecto debe priorizar:

1.  Naturalidad.
2.  Suavidad.
3.  Bajo consumo.
4.  Transparencia.
5.  Coherencia con el clima real.
6.  Transiciones suaves.

Evitar:

-   lluvia demasiado artificial;
-   partículas repetitivas;
-   efectos excesivamente intensos;
-   flashes constantes;
-   consumo elevado;
-   overlays que dificulten tocar iconos;
-   animaciones que parezcan un vídeo en loop.

------------------------------------------------------------------------

# 21. Configuración del usuario

Opciones iniciales:

``` text
Weather Effects
    ON / OFF

Display Mode
    Behind Icons
    Above Icons

Intensity
    Low
    Medium
    High

Animations
    Full
    Reduced
    Off

Lightning
    ON / OFF

Thunder Sound
    ON / OFF

Moon
    ON / OFF

Stars
    ON / OFF

Battery Saver
    ON / OFF

Update Frequency
    Automatic
    15 min
    30 min
    60 min
```

------------------------------------------------------------------------

# 22. Arquitectura de módulos

Propuesta inicial:

``` text
app/
│
├── core/
│   ├── model/
│   ├── location/
│   ├── network/
│   ├── storage/
│   └── time/
│
├── weather/
│   ├── WeatherRepository
│   ├── WeatherService
│   ├── WeatherMapper
│   └── WeatherState
│
├── astronomy/
│   ├── SunCalculator
│   ├── MoonCalculator
│   └── AstronomyState
│
├── environment/
│   ├── EnvironmentEngine
│   ├── EnvironmentState
│   └── TimeOfDay
│
├── rendering/
│   ├── RenderEngine
│   ├── ParticleSystem
│   ├── ShaderManager
│   └── SceneComposer
│
├── scenes/
│   ├── rain/
│   ├── snow/
│   ├── storm/
│   ├── sunny/
│   ├── night/
│   ├── sunset/
│   └── fog/
│
├── wallpaper/
│   └── WeatherWallpaperService
│
├── overlay/
│   └── WeatherOverlayService
│
└── ui/
    ├── settings/
    ├── preview/
    └── onboarding/
```

La estructura puede evolucionar durante el desarrollo. No es necesario
crear todos estos módulos antes de escribir código.

------------------------------------------------------------------------

# 23. Roadmap general

``` text
FASE 0
│
├── Definir concepto
├── Crear repositorio
├── Crear documentación
├── Investigar Android rendering
└── Probar APIs necesarias
        ↓
FASE 1
│
├── Kotlin
├── Android project
├── Canvas
├── Particle System
└── Rain prototype
        ↓
FASE 2
│
├── Behind Icons
├── Above Icons
├── Settings
└── Performance profiling
        ↓
FASE 3
│
├── Open-Meteo
├── Location
├── WeatherState
└── Cache
        ↓
FASE 4
│
├── Sun
├── Moon
├── Sunrise
├── Sunset
├── Day/Night
└── Transitions
        ↓
FASE 5
│
├── Rain
├── Snow
├── Fog
├── Clouds
├── Lightning
└── Wind
        ↓
FASE 6
│
├── Wet Glass Shader
├── Distortion
├── Droplets
└── Advanced VFX
        ↓
FASE 7
│
├── Battery optimization
├── Device compatibility
├── Launcher compatibility
└── Beta testing
        ↓
FASE 8
│
├── Play Store
├── Analytics
├── Crash reporting
├── Monetization
└── Commercial weather API if necessary
```

------------------------------------------------------------------------

# 24. Qué necesitamos para comenzar

## Software

Instalar:

-   Android Studio.
-   JDK compatible con la versión actual de Android Studio.
-   Android SDK.
-   Git.
-   Un dispositivo Android físico para pruebas.

Un emulador puede utilizarse, pero **los efectos gráficos y el consumo
deben medirse en dispositivos físicos**.

## Cuenta

Para desarrollo local no se necesita publicar inmediatamente en Google
Play.

Para la primera etapa basta con:

``` text
Android Studio
+
Git
+
Android Device
+
Open-Meteo
```

## Assets

Al principio incluso se puede utilizar:

-   formas básicas;
-   partículas generadas;
-   una textura simple de gota;
-   una textura básica de relámpago.

No comprar una biblioteca completa de assets antes de validar el motor.

------------------------------------------------------------------------

# 25. Primera tarea técnica

El primer objetivo del repositorio debe ser:

> **Renderizar lluvia animada sobre una pantalla Android y permitir
> cambiar la intensidad en tiempo real.**

No comenzar todavía con:

-   GPS;
-   API;
-   luna;
-   clima;
-   monetización.

Primero resolver:

``` text
Android
   ↓
Canvas
   ↓
Particle System
   ↓
Rain
   ↓
60/30/20 FPS
   ↓
Performance profiling
```

Después:

``` text
Rain
+
Wallpaper / Overlay
```

Y recién entonces integrar el resto.

------------------------------------------------------------------------

# 26. Criterio de éxito del prototipo

El primer prototipo será considerado exitoso si:

-   La lluvia se ve natural.
-   Las partículas no consumen CPU excesivamente.
-   El FPS es estable.
-   Se puede controlar la intensidad.
-   El efecto puede funcionar detrás de los iconos.
-   El efecto puede funcionar sobre los iconos cuando el sistema lo
    permita.
-   No bloquea la interacción del launcher.
-   El efecto puede pausarse.
-   La pantalla puede volver a su estado normal sin reiniciar el
    dispositivo.

------------------------------------------------------------------------

# 27. Decisiones técnicas iniciales

## Elegir

**Kotlin + Android nativo**

**Canvas + Particle System**

**Shaders para efectos avanzados**

**Compose para UI**

**Open-Meteo para MVP**

**WorkManager para tareas periódicas**

**DataStore para configuración**

**WallpaperService para Behind Icons**

**Overlay para Above Icons**

## Evitar inicialmente

-   Unity.
-   Flutter.
-   React Native.
-   Backend propio.
-   Firebase.
-   IA.
-   Base de datos remota.
-   Sistema de cuentas.
-   Arquitectura distribuida.

El objetivo inicial es que el proyecto pueda funcionar de forma
prácticamente local.

------------------------------------------------------------------------

# 28. Posible evolución comercial

Una vez validado el producto:

### Free

-   Clima básico.
-   Lluvia.
-   Sol.
-   Noche.
-   Luna.
-   Algunas animaciones.

### Premium

-   Wet Glass.
-   Tormentas avanzadas.
-   Relámpagos.
-   Efectos de nieve.
-   Más escenas.
-   Personalización avanzada.
-   Efectos de sonido.
-   Más controles.

También puede explorarse:

-   compra única;
-   suscripción;
-   paquetes de efectos;
-   temas estacionales.

La monetización debe decidirse después de validar la experiencia y la
retención, no antes.

------------------------------------------------------------------------

# 29. Riesgos principales

## Android / permisos

Los overlays y el comportamiento sobre el launcher están sujetos a
restricciones y diferencias entre versiones y fabricantes de Android.

Debe probarse en dispositivos reales.

## Batería

Una implementación ingenua puede generar:

-   alto consumo de CPU;
-   alto consumo de GPU;
-   calentamiento;
-   reducción de batería.

La optimización debe formar parte del desarrollo desde el principio.

## Compatibilidad

Diferentes fabricantes y launchers pueden comportarse de forma
diferente.

## Licencias

Los assets externos deben tener licencia compatible con distribución
comercial.

## Clima

Los proveedores meteorológicos tienen límites, licencias y condiciones
diferentes. No asumir que una API gratuita seguirá siendo gratuita
cuando el producto tenga escala comercial.

------------------------------------------------------------------------

# 30. Visión de largo plazo

La aplicación podría evolucionar desde:

``` text
Weather App
```

hacia:

``` text
Ambient Environment Engine
```

El motor podría representar:

-   clima;
-   hora;
-   astronomía;
-   estación del año;
-   eventos ambientales;
-   efectos especiales.

En el futuro podrían agregarse:

-   auroras;
-   meteoros;
-   eclipses;
-   tormentas solares;
-   lluvia de estrellas;
-   eventos astronómicos;
-   estaciones;
-   Navidad;
-   Halloween;
-   efectos regionales.

La arquitectura debe permitir agregar nuevas capas sin reescribir el
motor.

------------------------------------------------------------------------

# 31. Principio fundamental del proyecto

La aplicación no debe intentar decirle al usuario:

> "Hoy está lloviendo."

Debe hacer que el usuario **sienta visualmente que está lloviendo**.

Ese es el diferencial.

La información meteorológica alimenta el motor, pero el producto real es
la **experiencia ambiental dinámica**.

------------------------------------------------------------------------

# 32. Estado inicial del proyecto

Al crear el repositorio, comenzar con:

``` text
FoxWeather/
│
├── README.md
├── PROJECT.md
├── ROADMAP.md
├── ARCHITECTURE.md
├── DECISIONS.md
├── PERFORMANCE.md
├── LICENSE
│
├── docs/
│   ├── visual-concept.md
│   ├── weather-model.md
│   ├── astronomy.md
│   ├── rendering.md
│   └── android-overlay.md
│
├── assets/
│   ├── textures/
│   ├── particles/
│   ├── lightning/
│   └── references/
│
└── android/
    └── FoxWeather/
```

Para el comienzo real, basta con crear:

``` text
README.md
PROJECT.md
ROADMAP.md
ARCHITECTURE.md
```

y el proyecto Android.

------------------------------------------------------------------------

# 33. Primer sprint

## Sprint 0 --- Preparación

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

## Resultado esperado

Al finalizar el primer sprint debe existir una aplicación que permita:

``` text
┌──────────────────────────────┐
│                              │
│    💧       💧               │
│          💧        💧        │
│  💧                         │
│             💧               │
│       💧              💧     │
│                              │
│   [ LOW ] [ MED ] [ HIGH ]  │
│                              │
└──────────────────────────────┘
```

Ese prototipo será la base sobre la que se construirá todo FoxWeather.

------------------------------------------------------------------------

# 34. Regla de desarrollo

No implementar una característica porque "sería interesante".

Cada feature debe responder:

1.  ¿Mejora la experiencia?
2.  ¿Es visualmente perceptible?
3.  ¿Tiene un coste aceptable de batería?
4.  ¿Es compatible con Android?
5.  ¿Puede mantenerse modular?
6.  ¿Aporta diferenciación?

Si la respuesta es negativa, queda fuera del MVP.

------------------------------------------------------------------------

# 35. Objetivo final

Construir una aplicación Android que convierta el launcher del usuario
en una representación ambiental viva:

``` text
                    🌍 MUNDO REAL
                         │
             ┌───────────┴───────────┐
             │                       │
          WEATHER                  TIME
             │                       │
             └───────────┬───────────┘
                         │
                     ASTRONOMY
                         │
                         ▼
                 ENVIRONMENT ENGINE
                         │
                         ▼
                    SCENE ENGINE
                         │
                  ┌──────┴──────┐
                  ▼             ▼
               PARTICLES      SHADERS
                  │             │
                  └──────┬──────┘
                         ▼
                    ANDROID
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
        Behind Icons           Above Icons
              │                     │
              └──────────┬──────────┘
                         ▼
                EXPERIENCIA AMBIENTAL
```

**Primera meta: lluvia convincente.**

**Segunda meta: clima real.**

**Tercera meta: ciclo día/noche + sol/luna.**

**Cuarta meta: efectos avanzados de agua, tormenta y atmósfera.**

**Quinta meta: optimización y publicación.**
