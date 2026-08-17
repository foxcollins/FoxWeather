# Capas sobre Android

> Extraído de `PROJECT.md` (secciones 2, 5, 27 y 29). Mecanismos para
> proyectar efectos sobre el wallpaper y el launcher.

## Concepto

FoxWeather superpone una capa visual al wallpaper del usuario.

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

## Modo 1 — Behind Icons

Efectos por detrás de los iconos del launcher.

```text
Wallpaper → Weather Effects → Launcher / Icons
```

Implementación: **`WallpaperService`**.

## Modo 2 — Above Icons

Efectos por delante de los iconos.

```text
Wallpaper → Launcher / Icons → Weather Overlay
```

Implementación: **Application Overlay** (experimental).

## Modo 3 — Wet Glass

Superficie de cristal simulada delante de la pantalla (gotas, movimiento,
condensación, refracción, distorsión, reflejos, blur). Ver
`docs/RENDERING_ENGINE.md`.

## Notas de plataforma

-   Los overlays y el comportamiento sobre el launcher están sujetos a
    **restricciones y diferencias entre versiones y fabricantes** de
    Android.
-   Debe probarse en **dispositivos físicos** y con diferentes launchers.
-   No debe bloquear la interacción con los iconos.
-   El estado debe poder restaurarse sin reiniciar el dispositivo.

## Módulos

```text
wallpaper/
└── WeatherWallpaperService        (Behind Icons)

overlay/
└── WeatherOverlayService          (Above Icons)
```

Permisos y restricciones: ver `docs/PERMISSIONS.md`.