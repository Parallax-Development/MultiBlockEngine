# Capability: wiring-db-persistence

## Purpose
TBD

## Requirements

### Requirement: Database cable tracking
Cuando el método de persistencia es `db`, el addon MUST guardar las coordenadas de los cables colocados y borrarlos cuando se rompen.

#### Scenario: Cable is placed
- **WHEN** un jugador coloca un bloque de cable
- **THEN** el addon guarda las coordenadas y el `NetworkType` en la base de datos

#### Scenario: Server starts up
- **WHEN** el addon se habilita (`onEnable` u orquestador)
- **THEN** lee todas las entradas de la base de datos e invoca `NetworkService.registerNode` para cada una de ellas
