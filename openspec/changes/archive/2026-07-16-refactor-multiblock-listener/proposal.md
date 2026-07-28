## Why

`MultiblockListener` actúa actualmente como una "God class" para los eventos, estando fuertemente acoplado a la implementación del core (vía llamadas estáticas a `MultiBlockEngine.getInstance()`) y mezclando la recepción de eventos de infraestructura (Bukkit) con la lógica pesada del dominio (límites de jugadores, parseo de acciones de rotura, I18n, etc.). 
Esto viola los principios fundamentales de Service-Oriented Architecture (SOA) del motor, rompe la inyección de dependencias e impide el testing aislado. Este cambio resolverá ese acoplamiento, aislando a Bukkit en la capa de infraestructura y delegando la lógica de negocio a servicios puramente de dominio.

## What Changes

- **BREAKING**: Eliminación del patrón Service Locator (`MultiBlockEngine.getInstance()`) dentro del `MultiblockListener`. Todas las dependencias requeridas usarán inyección propia del motor.
- **Extracción de lógica de dominio**: Toda la lógica de "desmontaje" (ejecución de acciones, limpieza de límites, destrucción de memoria y envío de mensajes) será movida de `onBlockBreak` hacia un servicio de dominio especializado (por ejemplo, `MultiblockLifecycleService` o ampliando el `InteractionPipelineService`).
- **Listener delgado**: `MultiblockListener` se limitará únicamente a escuchar el evento crudo de Bukkit, transformarlo en un intento de dominio y pasarlo a la capa de servicios.

## Capabilities

### New Capabilities
- `multiblock-lifecycle`: Un servicio encargado explícitamente de gobernar el ciclo de vida del multiblock (destrucción limpia, evaluación de límites y acciones), abstrayendo esta lógica fuera de los listeners de Bukkit.

### Modified Capabilities
- `interaction-pipeline`: Se ajustará para contemplar las interacciones de rotura, logrando que el flujo sea el mismo sin importar si el multiblock se rompió por un jugador, un comando o una API externa.

## Impact

- `platform-bukkit/.../listener/MultiblockListener.java` (Limpieza radical).
- `core/.../InteractionPipelineService.java` o creación de `MultiblockLifecycleService`.
- **Addons**: Impacto mínimo si estaban escuchando `MultiblockBreakEvent` (API), pero **BREAKING** si dependían del comportamiento implícito del Listener de Bukkit o de manipular el estado del multiblock manualmente al romperse.
