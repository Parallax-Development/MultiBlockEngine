## ADDED Requirements

### Requirement: Configurability of persistence method
El addon `mbe-wiring` MUST cargar el archivo `config.yml` y leer la propiedad `cable-saving-method`.

#### Scenario: Valid PDC configuration
- **WHEN** config.yml tiene `cable-saving-method: pdc`
- **THEN** el addon habilita el listener de ChunkLoadEvent y deshabilita la persistencia por base de datos

#### Scenario: Valid DB configuration
- **WHEN** config.yml tiene `cable-saving-method: db`
- **THEN** el addon deshabilita el listener de chunks, inicializa el almacenamiento de BD y lee el estado global al arrancar
