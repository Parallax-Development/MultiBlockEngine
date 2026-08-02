# Wiring Debug Particles

## Purpose
TBD: Define la lógica para visualizar las redes de wiring enviando paquetes de partículas a lo largo de las conexiones del grafo cuando se usa el Wire Cutter en modo debug.

## Requirements

### Requirement: Particle Network Trail
El modo de debug del Wire Cutter SHALL recorrer el grafo al cual pertenece el nodo inspeccionado y emitir paquetes de partículas para crear un rastro visual a lo largo de todos los cables de esa red, independientemente del tipo de red (ej. energy, information).

#### Scenario: Visual Debugging on any network
- **WHEN** el jugador hace clic derecho en un `NetworkNode` (sea de type energy o information) usando el modo de debug del wire cutter
- **THEN** el sistema calcula las líneas 3D entre todos los nodos de ese grafo y envía los correspondientes paquetes `WrapperPlayServerParticle` al jugador para visualizar la red.
