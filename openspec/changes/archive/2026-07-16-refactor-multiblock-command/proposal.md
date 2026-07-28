## Why
El comando principal del plugin (`MultiblockCommand.java`) ha crecido hasta alcanzar casi las 1300 líneas, convirtiéndose en un "God Object". Actualmente, asume responsabilidades que no le corresponden: parseo de argumentos, validación manual de permisos, formateo de mensajes para el usuario, raytracing de bloques e incluso lógica pesada de creación de items o exportación, gestionando *absolutamente todos* los subcomandos (`/mbe export`, `/mbe blueprint`, `/mbe debug`, etc.) en un bloque centralizado. Esto viola el Principio de Responsabilidad Única (SRP) y dificulta la mantenibilidad. Adoptar un framework moderno como **Incendo Cloud** (`cloud-paper`) nos permitirá delegar el parseo, el autocompletado y fragmentar la lógica masiva en clases pequeñas y manejables.

## What Changes
- **BREAKING**: Reemplazar la implementación nativa de Bukkit (`CommandExecutor`, `TabCompleter`) del comando base y los enrutadores internos (`ServicesCommandRouter`, `AddonsCommandRouter`) por **Incendo Cloud** (usando `LegacyPaperCommandManager` o `PaperCommandManager` según convenga por versión).
- Fragmentar `MultiblockCommand` aislando cada subcomando lógico en su propia clase dedicada (ej. `ExportCommand`, `BlueprintCommand`, `DebugCommand`).
- Delegar validaciones repetitivas y pesadas (como buscar jugadores por nombre, extraer el `MultiblockType` por ID, o buscar el bloque al que mira el jugador) en los parsers/resolvers nativos de Cloud, limpiando el cuerpo del comando.
- Configurar las clases de comando para que solo acepten sus dependencias (servicios de dominio) por inyección, actuando puramente como presentadores y controladores.

## Capabilities

### New Capabilities
- `command-framework`: Integración de Incendo Cloud, habilitando soporte avanzado de brigadier, autocompletado inteligente y validación estricta de tipos.
- `modular-commands`: Un sistema estandarizado para registrar comandos. Esto permitirá a los Addons registrar de forma sencilla subcomandos bajo la rama `/mbe` (ej. `/mbe miaddon algo`) usando la misma instancia del Command Manager.

### Modified Capabilities
- `command-dispatcher`: Los sistemas de comandos delegados (como `AssemblyCommandService` y `BlueprintCommandService`) se simplificarán drásticamente al eliminar el boilerplate de Bukkit.

## Impact
- **Código**: Reescritura masiva en el paquete `dev.darkblade.mbe.core.application.command`.
- **Dependencias**: Se añadirá la dependencia de `cloud-paper` (y posiblemente `cloud-annotations`) al módulo `core` y/o `platform-bukkit`.
- **Addons (BREAKING)**: Cualquier API expuesta anteriormente para que los addons inyectaran comandos de forma nativa cambiará para exponer el manejador de Cloud, ofreciendo una integración mucho más robusta.
