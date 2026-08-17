# Rendimiento

> Extraído de `PROJECT.md` (secciones 10, 11 y 19). Detalle ampliado en
> `docs/BATTERY_OPTIMIZATION.md`.

## Principio

No mantener una carga gráfica máxima permanentemente.

Evitar:

```text
while(true) {
    render()
}
```

Usar **rendering adaptativo**.

## Objetivos de FPS (iniciales)

```text
Tormenta fuerte → 30 FPS
Lluvia normal   → 20-30 FPS
Llovizna        → 10-20 FPS
Nieve           → 15-25 FPS
Noche estática  → 0-10 FPS
Sin animación   → 0 FPS
```

Objetivos iniciales, no requisitos definitivos. Deben validarse con
profiling en dispositivos reales.

## Reducir/detener efectos cuando

-   la pantalla está apagada;
-   la aplicación no está visible (cuando corresponda);
-   el usuario desactiva animaciones;
-   la batería está baja;
-   el dispositivo está caliente;
-   el sistema limita la actividad.

## Red

No consultar el clima constantemente.

```text
Location → Weather Request → Cache → WeatherState
```

-   Actualizar cada 15–60 min según la fase del producto y el proveedor.
-   Ubicación aproximada cuando sea suficiente; cached location; actualizar
    solo ante un cambio significativo.
-   Si no hay conexión: usar el último `WeatherState` y continuar la
    experiencia visual.

## Medición (Beta)

En varios dispositivos (baja, media y alta gama) medir:

-   FPS, CPU, GPU, RAM.
-   Temperatura y consumo de batería.
-   Estabilidad, comportamiento con pantalla apagada y tras reinicio.

El producto debe **degradar efectos visuales antes que degradar la
experiencia general del teléfono**.