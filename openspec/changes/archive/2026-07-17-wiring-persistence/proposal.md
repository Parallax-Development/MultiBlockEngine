## Why

Actualmente el addon `mbe-wiring` almacena los grafos lógicos de cables puramente en memoria dentro del `NetworkService`. Esto ocasiona que, al reiniciar el servidor, la lógica del motor olvide qué cables existían (aunque los bloques visuales, `BlockDisplay`, persistan). Esta desincronización impide que los cables nuevos se conecten con los antiguos después de un reinicio. Este cambio añade un sistema dual de persistencia dinámico configurado por el administrador para reconstruir el estado de la red.

## What Changes

- Añade un archivo `config.yml` en el addon `mbe-wiring` con la opción `cable-saving-method: pdc | db`.
- Implementa la persistencia de cables basada en eventos de carga de chunks (`pdc`), leyendo el PersistentDataContainer de los BlockDisplays.
- Implementa la persistencia basada en base de datos (`db`) usando el `StorageService` del motor para rastrear la ubicación global de los cables.
- Delega toda la lógica de persistencia al addon en lugar de modificar el `core`, alineándose con la arquitectura SOA de MBE.

## Capabilities

### New Capabilities
- `wiring-persistence-config`: Capacidad de configurar métodos de persistencia en el addon `mbe-wiring`.
- `wiring-chunk-persistence`: Reconstrucción "on-the-fly" vía eventos de carga de chunks y NBT/PDC.
- `wiring-db-persistence`: Persistencia activa basada en registro de localizaciones usando `StorageService`.

### Modified Capabilities


## Impact

- **mbe-wiring**: Nuevo archivo de configuración `resources/config.yml`, Listeners adicionales, nueva capa de almacenamiento.
- **core (NetworkService)**: Ninguno (solo recibirá llamadas externas de `registerNode`).
