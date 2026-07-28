## 1. Configuración del Addon

- [x] 1.1 Crear archivo `src/main/resources/config.yml` con la clave `cable-saving-method: pdc` por defecto.
- [x] 1.2 Implementar carga de configuración en `WiringAddon.java` (o crear un servicio de configuración).

## 2. Sistema de Persistencia PDC

- [x] 2.1 Crear un Listener `CableChunkLoadListener` que escuche `ChunkLoadEvent`.
- [x] 2.2 Implementar lógica en el Listener para escanear entidades del chunk buscando `mbe_cable`.
- [x] 2.3 Implementar lógica para leer la red de la entidad y llamar a `NetworkService.registerNode`.
- [x] 2.4 Registrar condicionalmente el Listener en `WiringAddon` solo si `config.yml` dice `pdc`.

## 3. Sistema de Persistencia DB

- [x] 3.1 Crear un servicio `CableDatabaseService` para guardar localizaciones de cables.
- [x] 3.2 Modificar `CableManager` para que en `BlockPlaceEvent` guarde la localización en `CableDatabaseService` (solo si el método es `db`).
- [x] 3.3 Modificar `CableManager` para que en `BlockBreakEvent` elimine la localización (solo si el método es `db`).
- [x] 3.4 Añadir lógica de inicialización en `WiringAddon` (o un nuevo servicio de ciclo de vida) para cargar todas las entradas de `CableDatabaseService` y registrar los nodos al arrancar.
