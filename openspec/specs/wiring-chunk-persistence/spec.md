# Capability: wiring-chunk-persistence

## Purpose
TBD

## Requirements

### Requirement: Rebuilding graphs via ChunkLoadEvent
Cuando el método de persistencia es `pdc`, el addon MUST buscar entidades `BlockDisplay` marcadas como cables al cargar un chunk y registrarlas en `NetworkService`.

#### Scenario: Chunk loads with cables
- **WHEN** Bukkit dispara `ChunkLoadEvent` y el chunk tiene entidades con el tag `mbe_cable`
- **THEN** el addon extrae la red (usualmente del PDC o de sus tags) y llama a `NetworkService.registerNode` para cada cable detectado
