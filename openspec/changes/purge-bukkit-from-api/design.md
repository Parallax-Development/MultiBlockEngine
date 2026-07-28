## Context

El paquete `api` de MultiBlockEngine (MBE) ha sido contaminado históricamente con la dependencia directa de `org.bukkit.*`. Esto acopla la lógica de negocio puramente teórica (Multiblocks, conexiones, energía) a la plataforma de ejecución específica (Spigot/Paper). Para garantizar la mantenibilidad y testabilidad del motor (como dicta la regla `mbe-design-rules`), el dominio debe ser puro.

## Goals / Non-Goals

**Goals:**
- Eliminar por completo la dependencia de `org.bukkit.*` en el paquete `dev.darkblade.mbe.api.*`.
- Introducir el `EventBusService` (o similar) para eventos internos del motor, sin depender de Bukkit's `Event` o `HandlerList`.
- Proveer interfaces Wrapper en el API (ej: `MBEPlayer`, `MBEBlock`, `MBELocation`) y mover sus implementaciones Bukkit al módulo `platform-bukkit`.

**Non-Goals:**
- Reescribir la lógica interna de los multiblocks (solo se cambian los tipos que reciben/emiten).
- Crear soporte multi-plataforma para (Folia, Sponge, Fabric) ahora mismo, el objetivo es el *desacoplamiento*, no la multi-plataforma per se.

## Decisions

- **Patrón Wrapper**: Crearemos `MBEPlayer`, `MBELocation`, `MBEBlock` e `MBEItemStack` en `api.common` (o similar). La implementación concreta será inyectada o parseada desde `platform-bukkit`.
  - *Alternativa*: Devolver UUIDs y que cada servicio resuelva el jugador. *Rechazado* porque obliga a los servicios de dominio a depender de Bukkit para todo.
- **Event Bus Personalizado**: Reemplazaremos los eventos Bukkit (los que heredan de `Event`) por eventos MBE. Usaremos el patrón Pub-Sub mediante un `MBEEventBus` en el `core`.
  - *Alternativa*: Mantener los eventos como Bukkit pero en `platform-bukkit`. *Rechazado* porque los addons y el dominio necesitan suscribirse a eventos de ensamblaje.
- **Service Locator para Wrappers**: No se debe abusar, pero se proveerá una fábrica/adaptador en el `core` (inyectado) para convertir `Player` de Bukkit a `MBEPlayer`.

## Risks / Trade-offs

- [Risk] Romper la API pública para addons existentes -> Mitigación: Marcar los cambios como MAJOR (Breaking Change), proveer documentación clara en las Release Notes.
- [Risk] Degradación de rendimiento al wrappear objetos pesados en cada tick -> Mitigación: Usar *flyweights* o no wrappear objetos temporalmente en operaciones O(n^2), sino usar primitivas (x,y,z en lugar de Location).
