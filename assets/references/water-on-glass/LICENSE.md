# Referencias de gotas de agua sobre cristal

Videos de referencia de **ActionVFX (pago)** → sustituidos por alternativas
**gratuitas** para estudiar el comportamiento real de las gotas sobre
cristal. NO se usan como mecanismo de render (PROJECT.md §7): sirven para
control visual y guiar partículas/shader de Wet Glass (`docs/ROADMAP.md`
v0.4/v0.5).

## Archivos

| Archivo | Qué ilustra |
| --- | --- |
| `mixkit-rain-water-droplets-moving-on-the-glass-720.mp4` | Gotas deslizándose y acumulándose en cristal |
| `mixkit-window-on-a-rainy-day-720.mp4` | Lluvia en ventana, condensación y corrientes de gotas |
| `mixkit-rain-hitting-a-window-720.mp4` | Impacto directo de gotas con salpicaduras |
| `mixkit-rain-falling-on-a-car-window-720.mp4` | Gotas que chocan y resbalan (efecto viento/gravedad) |
| `mixkit-looking-out-through-a-wet-car-window-720.mp4` | Punto de vista "a través del cristal mojado" (Wet Glass) |

## Fuente y licencia

- Todos descargados de **Mixkit** (`mixkit.co`), licencia Mixkit Standard
  (uso comercial y personal gratuito, sin atribución), ver
  `https://mixkit.co/license/`.
- Descargados en 720p para ocultar peso de almacenamiento.
- Son material de **referencia interna**, no se distribuyen con la app.

## Pautas visuales para tomar de estos clips

1. Las gotas "nacen" pequeñas y **coalescen** al juntarse.
2. Deslizan más rápido al acumular peso; dejan una **estela** estrecha.
3. En superficie vertical, la condensación rellena micro-gota a micro-gota.
4. La cara trasera de la gota refracta/distorsiona lo que está detrás.
5. Golpes directos generan una **salpicadura** radial breve seguida de una
   gota que resbala.

Esto se traducirá en propiedades del `WetGlassLayer` (assets acidos: máscaras
de gota, normal/distortion maps, highlights) y en la física de los
`DropletParticle`s.