# Decisiones

> Extraído de `PROJECT.md` (secciones 5, 27, 29 y 34). Decisiones técnicas
> iniciales y reglas de desarrollo.

## Elegido

| Área      | Decisión                                   |
| --------- | ------------------------------------------ |
| Lenguaje  | **Kotlin** + Android nativo                |
| Render    | **Canvas + Particle System**               |
| FX        | **Shaders** para efectos avanzados         |
| UI        | **Jetpack Compose**                        |
| Clima     | **Open-Meteo** para el MVP                 |
| Period.   | **WorkManager** para tareas periódicas     |
| Config.   | **DataStore**                              |
| Behind    | **WallpaperService**                       |
| Above     | **Overlay**                                |

## Astronomía local

No depender de la API meteorológica para astronomía. Calcular localmente:

-   día/noche, sunrise, sunset, blue hour;
-   posición aproximada del sol;
-   fase lunar y posición aproximada de la luna.

Reduce dependencia de servicios externos.

## Evitar inicialmente

-   Unity, Flutter, React Native.
-   Backend propio, Firebase, base de datos remota.
-   IA, sistema de cuentas, arquitectura distribuida.

El objetivo inicial es que el proyecto funcione de forma prácticamente local.

## No hacer en el MVP

-   Cuentas · backend · IA · sistema social · monetización.
-   Demasiados proveedores · pronóstico complejo · veinte tipos de clima.

El MVP demuestra una sola cosa: **la pantalla del teléfono puede reaccionar
visualmente al clima real de forma atractiva y eficiente.**

## Regla de desarrollo

No implementar una característica porque "sería interesante". Cada feature
debe responder:

1.  ¿Mejora la experiencia?
2.  ¿Es visualmente perceptible?
3.  ¿Tiene un coste aceptable de batería?
4.  ¿Es compatible con Android?
5.  ¿Puede mantenerse modular?
6.  ¿Aporta diferenciación?

Si la respuesta es negativa, queda fuera del MVP.

## Riesgos principales

-   **Android / permisos**: overlays y comportamiento sobre el launcher
    sujetos a restricciones por versión y fabricante. Probar en dispositivos
    reales.
-   **Batería**: una implementación ingenua genera alto consumo de
    CPU/GPU, calentamiento y reducción de batería. La optimización forma
    parte del desarrollo desde el principio.
-   **Compatibilidad**: distintos fabricantes y launchers se comportan
    diferente.
-   **Licencias**: los assets externos deben tener licencia compatible con
    distribución comercial.
-   **Clima**: no asumir que una API gratuita seguirá siendo gratuita a
    escala comercial.

## Evolución comercial (posterior a validación)

-   **Free**: clima básico, lluvia, sol, noche, luna, algunas animaciones.
-   **Premium**: Wet Glass, tormentas avanzadas, relámpagos, nieve, más
    escenas, personalización, sonido, más controles.

La monetización se decide después de validar experiencia y retención, no
antes.