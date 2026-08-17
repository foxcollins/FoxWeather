# Optimización de Batería

> Extraído de `PROJECT.md` (secciones 10 y 11). Resumen también en
> `PERFORMANCE.md`.

## Principio

La batería es una prioridad del proyecto. **No mantener una carga gráfica
máxima permanentemente.**

Evitar:

```text
while(true) {
    render()
}
```

Usar **rendering adaptativo**.

## FPS por escena (objetivos iniciales)

```text
Tormenta fuerte → 30 FPS
Lluvia normal   → 20-30 FPS
Llovizna        → 10-20 FPS
Nieve           → 15-25 FPS
Noche estática  → 0-10 FPS
Sin animación   → 0 FPS
```

Objetivos iniciales, no requisitos definitivos. Validar con profiling en
dispositivos reales.

## Reducir/detener efectos cuando

-   la pantalla está apagada;
-   la aplicación no está visible (cuando corresponda);
-   el usuario desactiva animaciones;
-   la batería está baja;
-   el dispositivo está caliente;
-   el sistema limita la actividad.

## Optimización de red

No consultar el clima constantemente.

```text
Location → Weather Request → Cache → WeatherState
```

-   Actualizar cada 15–60 min según la fase del producto y el proveedor.
-   Ubicación aproximada cuando sea suficiente; cached location; actualizar
    solo ante un cambio significativo; mínima frecuencia necesaria.
-   Sin conexión: usar el último `WeatherState` y continuar la experiencia
    visual.

## Prioridad del producto

El producto debe **degradar efectos visuales antes que degradar la
experiencia general del teléfono**.