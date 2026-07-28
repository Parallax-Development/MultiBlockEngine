## ADDED Requirements

### Requirement: PacketService Interface
El módulo `api` SHALL proveer una interfaz `PacketService` inyectable por los addons, que exponga métodos seguros para despachar paquetes a jugadores basados en PacketEvents.

#### Scenario: Addon packet execution
- **WHEN** un addon inyecta el `PacketService` y llama a `sendPacket(jugador, wrapper)`
- **THEN** el wrapper se despacha al cliente a través de PacketEvents, sin que el addon necesite referenciar directamente a Bukkit.
