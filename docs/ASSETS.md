# Assets

> Extraído de `PROJECT.md` (secciones 13 y 24).

## Estrategia

El objetivo no es depender de cientos de assets.

```text
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

No comprar una biblioteca completa de assets antes de validar el motor.

## Dónde buscar

-   Unity Asset Store.
-   itch.io.
-   Kenney.
-   OpenGameArt.
-   Envato Elements.
-   Otros marketplaces de VFX y game assets.

## Términos de búsqueda

```text
rain particles · rain VFX · weather VFX · rain overlay · rain on glass ·
water droplets · wet glass shader · water droplet shader · lightning VFX ·
thunderstorm VFX · snow particles · fog overlay · cloud particles
```

## Licencias

Siempre verificar **licencia comercial** antes de utilizar un asset en una
aplicación publicada.

## Inicio

Al principio se puede usar:

-   formas básicas;
-   partículas generadas;
-   una textura simple de gota;
-   una textura básica de relámpago.

## Carpeta local

```text
assets/
├── references/    — imágenes de referencia / moodboard
├── textures/      — texturas base (gotas, nubes, partículas, relámpagos)
├── particles/     — sprites de partículas
├── shaders/       — glsl/shaders (wet glass, distorsión)
└── audio/         — sonidos opcionales (truenos, lluvia ambiente)
```

Recuerda anotar la licencia de cada asset en un `LICENSE` dentro de cada
subcarpeta.