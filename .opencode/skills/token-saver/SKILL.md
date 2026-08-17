---
name: token-saver
description: Use when the user asks to save tokens, reduce budget, avoid burning context, work efficiently/cheaply, or keep agents fast. Defines rules that minimize token consumption without losing quality.
---

# Token Saver

Reglas para trabajar con el mínimo consumo de tokens sin perder calidad.
Aplicar SIEMPRE que el usuario mencione ahorrar tokens, presupuesto,
contexto o trabajar de forma barata/eficiente.

## 1. Trabajar en lotes

- Agrupar tool calls **independientes** en un único mensaje (varios `read`,
  `grep`, `glob` a la vez). Cada mensaje con herramientas cuesta overhead;
  los lotes lo amortizan.

## 2. Delegar exploración a subagentes

- Para búsquedas abiertas ("¿dónde está X?", "cómo funciona Y") usar el
  subagente `explore` o `general` y recibir solo el **resumen**.
- La salida completa de sus herramientas NUNCA entra en el contexto
  principal. Es la fuente de ahorro más grande.
- No duplicar luego ese trabajo: confiar en el resumen del subagente.

## 3. Lecturas dirigidas

- Preferir `grep`/`glob` a leer archivos completos.
- Usar `read` con `offset`/`limit` cuando el archivo es grande; nunca volcar
  un archivo enorme entero.
- No releer archivos ya leídos; si la salida fue truncada, buscar con `grep`
  o leer el slice exacto en vez de releer todo.

## 4. Salidas de herramientas

- No reproducir contenido de salidas en respuestas de texto.
- Si una salida se trunca y se escribe a archivo, buscar en ella con `rg`
  (no `Select-String`) o leer secciones precisas.
- No imprimir diffs completos innecesarios.

## 5. Escritura en un solo paso

- Escribir archivos nuevos completos con un único `write`.
- Evitar cadenas de `edit` para lo que se puede escribir de una vez.
- Reusar `replaceAll` cuando haya renombrados/variables.

## 6. Verificación mínima

- Ejecutar el comando correcto a la primera; no probar comandos "a ver".
- Correr lint/typecheck/tests **una vez** al final, no tras cada cambio.
- No ejecutar builds repetitivos sin razón.

## 7. Nada de ruido en la conversación

- Respuestas cortas (el sistema ya lo exige): sin preamble ni postamble.
- No pedir confirmaciones para lo obvio; no enumerar lo que ya se hizo si no
  aporta.

## 8. Web

- Preferir `websearch` (resultados compactos) sobre `webfetch` de páginas
  completas.
- Mantener `contextMaxCharacters` bajo.
- Para docs de librerías usar **Context7** (solo el tema concreto), no
  webfetch de documentación entera.

## 9. MCP

- Cada MCP activo añade tool-schemas a **cada** petición. Mantener solo los
  necesarios y desactivar los que no se usen (`enabled: false`).
- Usar `mcp-find`/`mcp-add` solo cuando se necesite la capacidad.

## 10. Escaneos acotados

- Acotar `glob` con patrones específicos (`src/**/*.ts`) en vez de escanear
  todo el repo.
- No listar recursivamente directorios grandes sin motivo.