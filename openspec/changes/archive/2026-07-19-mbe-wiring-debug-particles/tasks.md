## 1. Setup API & Dependencies

- [x] 1.1 Add `packetevents-api` dependency to the `api` module's `build.gradle`
- [x] 1.2 Define the `PacketService` interface in the `dev.darkblade.mbe.api.packet` package

## 2. Implement Core Services

- [x] 2.1 Create the concrete implementation of `PacketService` en `core` o `platform-bukkit` (adaptando o reemplazando el `PacketEventsAdapter` actual)
- [x] 2.2 Registrar la implementación de `PacketService` en el `ServiceRegistry` de MBE durante el ciclo de inicialización

## 3. Refactor DebugWiringAction

- [x] 3.1 Modificar el constructor de `DebugWiringAction` y `WireCutterTool` en el addon `mbe-wiring` para inyectar `PacketService`
- [x] 3.2 Actualizar `WiringAddon.java` donde se instancia la herramienta para proveer la nueva dependencia
- [x] 3.3 Implementar el recorrido del `NetworkGraph` (BFS iterativo) dentro del método `execute` de `DebugWiringAction`
- [x] 3.4 Implementar lógica matemática para interpolar puntos 3D a lo largo de las conexiones entre nodos (líneas)
- [x] 3.5 Construir y despachar paquetes `WrapperPlayServerParticle` para dibujar las líneas usando el `PacketService`
