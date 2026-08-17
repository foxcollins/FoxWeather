# Testing

> Extraído de `PROJECT.md` (secciones 19, 24 y 26).

## Contexto

-   Emulador permitido, pero **los efectos gráficos y el consumo deben
    medirse en dispositivos físicos**.
-   Dispositivos objetivo iniciales: gama baja, media y alta.

## Medición en Beta

En varios dispositivos medir:

-   FPS.
-   CPU.
-   GPU.
-   RAM.
-   temperatura.
-   consumo de batería.
-   estabilidad.
-   comportamiento con diferentes launchers.
-   comportamiento con pantalla apagada.
-   comportamiento después de reiniciar el teléfono.
-   permisos de overlay.
-   restricciones de background.
-   diferentes densidades de pantalla.

## A validar según los modos

-   **Wallpaper/Live wallpaper (Behind Icons)** — frente a diferentes
    launchers, bloqueos, restauración del estado.
-   **Overlay (Above Icons)** — que no bloquee la interacción con iconos y
    respete restricciones del sistema.

## Criterio de éxito del prototipo (Sprint 0)

-   La lluvia se ve natural.
-   Las partículas no consumen CPU excesivamente.
-   El FPS es estable.
-   Se puede controlar la intensidad.
-   El efecto funciona detrás de los iconos.
-   El efecto funciona sobre los iconos cuando el sistema lo permita.
-   No bloquea la interacción del launcher.
-   El efecto puede pausarse.
-   La pantalla puede volver a su estado normal sin reiniciar el dispositivo.

## Prioridad

El producto debe **degradar efectos visuales antes que degradar la
experiencia general del teléfono**.