## Why

Actualmente, el modo debug del `wire_cutter` en `mbe-wiring` solo emite un mensaje de texto en el chat con los IDs de la red y el grafo, lo cual no es muy intuitivo ni visual para rastrear estructuras complejas. Al proveer un feedback visual post-acción usando un rastro de partículas a lo largo de la red, se facilita enormemente la comprensión y el debugging de las conexiones entre multibloques.

## What Changes

- Añadir la dependencia `packetevents-api` al módulo `api` de MBE para permitir la integración con PacketEvents sin acoplar los addons a implementaciones de Bukkit directamente.
- Crear una interfaz oficial de servicio de envío de paquetes (`PacketService`) en el módulo `api`.
- Implementar e inyectar `PacketService` en el `ServiceRegistry` desde `core` / `platform-bukkit`.
- Actualizar `DebugWiringAction` dentro del addon `mbe-wiring` para inyectar este `PacketService`.
- Implementar la lógica de trazado (recorrido del `NetworkGraph` e interpolación de vectores a lo largo de las conexiones) en la acción de debug para generar líneas de partículas (`WrapperPlayServerParticle`).

## Capabilities

### New Capabilities

- `visual-packet-service`: Creación de un servicio en el API de MBE para interactuar con PacketEvents de forma segura e inyectable por addons.
- `wiring-debug-particles`: Capacidad del `wire_cutter` (modo debug) para dibujar las redes (`NetworkGraph`) visualmente en el mundo usando partículas.

### Modified Capabilities


## Impact

- **API (`api`)**: Nueva dependencia cruzada (`packetevents-api`) y nuevo servicio oficial (`PacketService`).
- **Core (`core` / `platform-bukkit`)**: Modificación en el registro de servicios para proveer el `PacketEventsAdapter` o una nueva implementación de `PacketService`.
- **Addon (`mbe-wiring`)**: Refactor matemático y funcional de `DebugWiringAction` para realizar trazado 3D de nodos de red.
