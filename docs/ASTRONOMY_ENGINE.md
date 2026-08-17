# Motor Astronómico

> Extraído de `PROJECT.md` (secciones 5, 6, 16, 17 y 18).

## Principio

No depender de la API meteorológica para astronomía. El proyecto calcula
**localmente**:

-   día/noche;
-   sunrise / sunset;
-   blue hour;
-   posición aproximada del sol;
-   fase lunar;
-   posición aproximada de la luna.

Reduce dependencia de servicios externos.

## Módulo

```text
astronomy/
├── SunCalculator
├── MoonCalculator
└── AstronomyState
```

## Día/noche

`TimeOfDay` alimenta la capa de tiempo de las escenas:

```text
Night → Sunrise → Day
Day → Sunset → Night
```

Las transiciones deben ser progresivas, **sin cambios abruptos**:

```text
Noche → Blue hour → Amanecer → Sunrise → Día
Día → Golden hour → Atardecer → Noche → Luna
```

## Sol y luna

-   **SunState** — sunrise, sunset, azimut/elevación aproximada; alimenta la
    capa de sol y la iluminación ambiental.
-   **MoonState** — fase lunar y posición aproximada; alimenta la capa de
    luna y las estrellas. La luna puede quedar parcialmente oculta tras las
    nubes.

## Estados

```kotlin
data class SunState(
    val sunrise: Instant,
    val sunset: Instant,
    // posición aproximada...
)

data class MoonState(
    val phase: MoonPhase,
    // posición aproximada...
)

data class EnvironmentState(
    val timeOfDay: TimeOfDay,
    val weather: WeatherState,
    val sunState: SunState,
    val moonState: MoonState
)
```

## Integración

`EnvironmentEngine` combina tiempo + clima + astronomía en un
`EnvironmentState` que consume el `SceneComposer` (ver
`docs/ARCHITECTURE.md`, `docs/DATA_MODEL.md`).