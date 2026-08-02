## Context

Actualmente, la clase `MultiblockListener` actúa como una "God class", mezclando el manejo de eventos de infraestructura de Bukkit (`BlockBreakEvent`) con lógica pesada del dominio (ejecución manual de acciones `onBreakActions`, manejo de límites de jugador, destrucción de la instancia en el runtime y envío de mensajes de desensamblado). Además, el listener hace un uso intensivo del patrón Service Locator (`MultiBlockEngine.getInstance().getCoreService(...)`), lo que acopla fuertemente el código, impide la inyección de dependencias (DI) y dificulta el testing aislado.

## Goals / Non-Goals

**Goals:**
- Desacoplar la infraestructura (Bukkit) de la lógica de dominio (ciclo de vida del multiblock).
- Eliminar el uso del patrón Service Locator en `MultiblockListener`.
- Crear un servicio dedicado al ciclo de vida del multiblock (`MultiblockLifecycleService`) que orqueste la destrucción de forma limpia.
- Lograr que el listener de Bukkit se limite a escuchar el evento de rotura, traducirlo a un contexto de dominio y delegar al servicio.

**Non-Goals:**
- Refactorizar otros listeners que no sean `MultiblockListener`.
- Cambiar la forma en la que se definen o configuran las acciones (`onBreakActions`) en el yaml o parser.
- Modificar el sistema de interacción por clicks (eso ya es manejado por `InteractionPipelineService`).

## Decisions

**1. Creación de `MultiblockLifecycleService` (Opción Seleccionada)**
- *Decisión*: Mover toda la lógica de destrucción (acciones, límites, mensajes, runtime) a un nuevo servicio de dominio `MultiblockLifecycleService`.
- *Alternativa considerada*: Extender `InteractionPipelineService` con un `BreakIntent`. Se descartó porque el ciclo de vida (creación y destrucción) es un dominio cohesivo propio que no debería estar mezclado con simples interacciones de uso (clicks, abrir UIs).
- *Rationale*: Separa las responsabilidades. El ciclo de vida del multiblock se vuelve un servicio testeable, agnóstico de Bukkit, y que recibe sus dependencias por constructor vía inyección (`ServiceRegistry`).

**2. Delegación limpia desde `MultiblockListener`**
- *Decisión*: `MultiblockListener.onBlockBreak` invocará a `MultiblockLifecycleService.tryDisassemble(instance, mbePlayer)`. 
- *Rationale*: Si el dominio o algún addon cancela el evento de destrucción (por ejemplo, interceptando el `MultiblockBreakEvent`), `tryDisassemble` devolverá `false` y el listener cancelará el evento de Bukkit, preservando la coherencia del estado.

**3. Inyección de Dependencias Estricta**
- *Decisión*: `DefaultMultiblockLifecycleService` recibirá todas sus dependencias (`MultiblockLimitService`, `MultiblockRuntimeService`, `PlayerMessageService`, `EventDispatcher`/`EventCaller`, etc.) a través de su constructor, siendo registradas y resueltas por el `ServiceRegistry`.

## Risks / Trade-offs

- **[Risk] Rotura de compatibilidad para Addons (BREAKING)**
  - *Contexto*: Addons que dependían del comportamiento implícito del Listener de Bukkit o de manipular el estado del multiblock manualmente en eventos concurrentes.
  - *Mitigación*: Documentar el cambio en las notas de la release. Los Addons ahora deben escuchar el evento de dominio `MultiblockBreakEvent` para interactuar con la fase de destrucción.
- **[Risk] Fallos de sincronización en cancelación**
  - *Contexto*: Si un addon cancela el evento de Bukkit en otro listener, o si el evento de dominio es cancelado, el estado podría quedar inconsistente.
  - *Mitigación*: `MultiblockLifecycleService` dispara el evento de dominio sincrónicamente y respeta la cancelación antes de modificar cualquier estado (límites, runtime).
