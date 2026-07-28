## Context

El addon `mbe-wiring` maneja los cables pero no posee memoria persistente, perdiendo las conexiones tras reinicios. El `NetworkService` del `core` no debe modificarse porque su única función es el cálculo lógico del grafo en RAM. Se creará una configuración en el addon para permitir a los administradores de servidores seleccionar la forma en que los cables sobreviven a los reinicios.

## Goals / Non-Goals

**Goals:**
- Implementar configuración flexible (`cable-saving-method: pdc | db`).
- Asegurar que `mbe-wiring` se encargue de reinyectar nodos en el `NetworkService` cuando arranca el servidor o se cargan chunks.
- Mantener la separación de responsabilidades (SOA) sin modificar el módulo `core`.

**Non-Goals:**
- Modificar el sistema de `NetworkService` en el `core` para forzar persistencia universal de topologías.

## Decisions

- **Localización de config**: `mbe-wiring/src/main/resources/config.yml`. El addon cargará este archivo en su `onEnable` usando los métodos estándar de Bukkit u orquestador de Addons de MBE.
- **Implementación PDC**: Se crea un listener para `ChunkLoadEvent`. Al cargar, busca entidades dentro del chunk. Si es un `BlockDisplay` con tags `mbe_cable` y `mbe_cable_node_*`, lee su data, determina su red y llama a `networkService.registerNode(...)`.
- **Implementación DB**: Se crea un componente de almacenamiento local al addon (o integrándose con `StorageService`). En `BlockPlaceEvent`, se guarda la coordenada. En `BlockBreakEvent`, se elimina. Durante el encendido del addon, se leen todas las coordenadas de la DB y se llama a `registerNode`.

## Risks / Trade-offs

- **Riesgo `ChunkLoadEvent`**: Podría generar un pico de lag al cargar chunks con muchos cables.
  - *Mitigación*: Mantener la operación lo más ligera posible. Filtrar entidades por sus scoreboards de forma eficiente.
- **Trade-off**: `db` es más escalable en CPU para redes gigantes (no depende de qué chunks están cargados), pero `pdc` requiere menos almacenamiento externo y previene cables fantasmas si la BD se desincroniza del mundo. Ofrecer ambos cubre todos los casos.
