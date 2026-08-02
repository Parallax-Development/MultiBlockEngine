## ADDED Requirements

### Requirement: Particle Network Trail
El modo de debug del Wire Cutter SHALL recorrer el grafo al cual pertenece el nodo inspeccionado y emitir paquetes de partículas para crear un rastro visual a lo largo de todos los cables de esa red.

#### Scenario: Visual Debugging
- **WHEN** el jugador hace clic derecho en un `NetworkNode` usando el modo de debug del wire cutter
- **THEN** el sistema calcula las líneas 3D entre todos los nodos de ese grafo y envía los correspondientes paquetes `WrapperPlayServerParticle` al jugador para visualizar la red.
