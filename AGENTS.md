# AGENTS.md — FoxWeather

Proyecto: app Android de clima ambiental (Kotlin, Compose, Canvas/Particle
System). Documentación completa en `PROJECT.md` y `docs/`.

## Carga de contexto (barato siempre)

1. Empezar SIEMPRE leyendo `README.md` y los `docs/` relevantes antes de
   tocar código.
2. Delegar búsquedas abiertas al subagente `explore` y usar solo su resumen.
3. Agrupar tool calls independientes en un único mensaje.
4. Preferir `grep`/`glob` acotado a lecturas completas; usar `offset`/`limit`.
5. Escribir archivos en un solo `write`; no encadenar edits triviales.
6. Respuestas cortas; no reimprimir salidas ni diffs.

## Convenciones

- Documentación y código en **español** cuando aplique (los campos/API en
  inglés).
- Datos separados de la representación: `WeatherState`, `EnvironmentState`.
- Capas composables; nunca una escena por combinación de clima.
- Rendering adaptativo y ahorro de batería desde el inicio.
- No implementar features que no superen la regla de desarrollo
  (PROJECT.md §34).

## Verificación

- No hay build/autoridad de lint configurada aún (Android pendiente de
  Sprint 0). Si se agrega, correr una sola vez al final.