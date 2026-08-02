## 1. Domain Wrappers

- [x] 1.1 Crear las interfaces `MBEPlayer`, `MBEBlock`, `MBELocation` y `MBEItemStack` en el paquete `dev.darkblade.mbe.api`.
- [x] 1.2 Mover las implementaciones concretas que wrappean objetos de Bukkit hacia el módulo `platform-bukkit`.
- [x] 1.3 Proveer una fábrica/adaptador en el `core` para convertir de tipos nativos a Wrappers cuando la infraestructura interactúe con el dominio.

## 2. Agnostic Event Bus

- [x] 2.1 Crear la interfaz base `MBEEvent` y la interfaz `EventBusService` (pub/sub) en el API.
- [x] 2.2 Crear la implementación concreta `MBEEventBus` en el `core` y registrarla en el `ServiceRegistry`.

## 3. Refactor Domain Events

- [x] 3.1 Modificar `MultiblockFormEvent` y demás eventos de dominio para que hereden de `MBEEvent` y no de `org.bukkit.event.Event`.
- [x] 3.2 Actualizar los componentes del motor (`AssemblyCoordinator`, etc.) para publicar eventos usando el `EventBusService` inyectado en lugar de usar `Bukkit.getPluginManager().callEvent()`.

## 4. Purga Final de Bukkit

- [x] 4.1 Eliminar definitivamente todos los imports a `org.bukkit.*` dentro del módulo `api`.
- [x] 4.2 Corregir errores de compilación cascada en `core` y `platform-bukkit` originados por el reemplazo de tipos.
