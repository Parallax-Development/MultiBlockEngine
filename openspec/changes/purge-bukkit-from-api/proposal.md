## Why
El paquete `api` de MultiBlockEngine tiene el mandato arquitectónico de definir contratos completamente agnósticos de la plataforma. Sin embargo, un análisis revela decenas de dependencias directas a `org.bukkit.*` (incluyendo `org.bukkit.event.Event`, `HandlerList`, `Player`, `Location`, `Block`, etc.) a lo largo de las interfaces y eventos del dominio. Esto ata irremediablemente el motor al ecosistema de Bukkit, impidiendo ejecutar el dominio en entornos de testing puros (sin mocks pesados) y rompiendo el propósito del módulo `platform`. Limpiar esto es crítico para recuperar la portabilidad y la verdadera separación de responsabilidades.

## What Changes
- **BREAKING**: Desacoplar todos los eventos de dominio (`MultiblockFormEvent`, `EnergyProducedEvent`, etc.) de `org.bukkit.event.Event`, `HandlerList` y `Cancellable`. Pasarán a implementar un contrato puro (ej. `MBEEvent`).
- **BREAKING**: Reemplazar masivamente en las firmas de la API todos los tipos espaciales y entidades crudas de Bukkit (`Player`, `Block`, `Location`, `Vector`) por sus envoltorios agnósticos ya existentes (`MBEPlayer`, `MBEBlock`, `MBELocation`, etc.).
- **BREAKING**: Abstraer los usos de `ItemStack` e `Inventory` en el `api` introduciendo representaciones puras propias (ej. `MBEItemStack`).
- Creación de un servicio de bus de eventos agnóstico en el `core` (`EventBusService`), y un puente opcional en `platform-bukkit` para emitir eventos del dominio hacia Bukkit si fuese necesario para compatibilidad externa.

## Capabilities

### New Capabilities
- `agnostic-event-bus`: Un sistema interno de despacho y suscripción a eventos de dominio (sin dependencia de `PluginManager` de Bukkit) para que los addons y servicios se comuniquen internamente.
- `item-abstractions`: Interfaces en `api/platform` para manipular objetos e inventarios de forma abstracta, aislando a `org.bukkit.inventory.*`.

### Modified Capabilities
- `platform-wrappers`: Adopción estricta (enforce) de los wrappers espaciales y de entidades existentes en todas las firmas de la API.

## Impact
- **Módulos**: Afectará transversalmente al módulo `api`, requiriendo un refactor profundo y sincronizado en `core` y `platform-bukkit`.
- **Addons (BREAKING)**: Cualquier addon que esté escuchando eventos propios de MBE usando `@EventHandler` de Bukkit, o llamando a métodos de la API pasando `Player` o `Location`, dejará de compilar. Deberán migrar al nuevo bus de eventos de MBE y usar los tipos `MBE*`.
