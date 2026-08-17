# Permisos

> Extraído de `PROJECT.md` (secciones 27, 29 y 19). Permisos y
> restricciones para overlays/launcher.

## Permisos identificados

-   **Ubicación** — para lat/long del clima real (Fused Location Provider).
    Usar ubicación aproximada cuando baste, cached location y mínima
    frecuencia.
-   **Overlay** (SYSTEM_ALERT_WINDOW) — para el modo *Above Icons*.
-   **Wallpaper** (WallpaperService envía al proveedor de wallpapers) — para
    el modo *Behind Icons*.
-   **Batería optimización / ignorar optimizaciones** — evaluar solo si es
    imprescindible; nunca forzar al usuario.

## Restricciones de la plataforma

-   Los overlays y el comportamiento sobre el launcher están sujetos a
    **restricciones por versión y fabricante** de Android.
-   Debe probarse en **dispositivos físicos** y con diferentes launchers.
-   Revisar: permisos de overlay, restricciones de background, comportamiento
    con pantalla apagada y después de reiniciar el teléfono.

## Reglas

-   No bloquear la interacción con los iconos.
-   Reducir/detener efectos cuando el sistema lo limite o la pantalla esté
    apagada.
-   El estado debe poder restaurarse sin reiniciar el dispositivo.

## Ciclo de ubicación

```text
GPS → lat/lng → Open-Meteo → WeatherState → SceneComposer → RenderEngine
```

Ubicación aproximada si es suficiente; actualizar solo ante un cambio
significativo.