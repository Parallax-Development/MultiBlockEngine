## Context

El addon `mbe-wiring` provee un modo de debug (`DebugWiringAction`) en su herramienta `wire_cutter` que actualmente solo imprime el ID del grafo en texto al hacer clic en un nodo. Se busca mejorar esta experiencia agregando un trazado visual con partículas. 
Dado que MBE minimiza las dependencias directas de Bukkit en el módulo `api` y en los addons, se utilizará PacketEvents para emitir las partículas. 

## Goals / Non-Goals

**Goals:**
- Exponer la dependencia `packetevents-api` en el módulo `api` de MBE.
- Crear un servicio `PacketService` (inyectable vía `@InjectService`) en el API para despachar paquetes.
- Refactorizar `DebugWiringAction` para hacer un recorrido del `NetworkGraph` (BFS) y trazar líneas de partículas entre nodos conectados.
- Interpolar puntos 3D para crear rastros continuos y claros.

**Non-Goals:**
- Crear un framework visual o holográfico gigantesco. El scope es limitado al envío crudo de paquetes y la emisión puntual de partículas.
- Re-escribir otros componentes del wire cutter que no estén relacionados a debug.

## Decisions

- **Añadir PacketEvents al API**: Se decidió usar el "camino flexible". Expondremos `packetevents-api` para que los addons puedan construir cualquier `PacketWrapper`. Esto empodera fuertemente al ecosistema sin atarnos al NMS de una versión concreta de Spigot.
- **`PacketService` estandarizado**: A pesar de que PacketEvents posee acceso estático (`PacketEvents.getAPI()`), se definirá un `PacketService` en el API (implementado en core) para respetar el Service-Oriented Architecture (SOA) de MBE.
- **Algoritmo BFS para el Trazado**: El `DebugWiringAction` arrancará desde el nodo clickeado e iterará sobre todas las conexiones del grafo, manteniendo un `Set` de "conexiones dibujadas" para evitar dibujar la ida y la vuelta de un mismo cable, optimizando los paquetes.

## Risks / Trade-offs

- **[Risk] Acoplamiento de API a terceros**: El contrato de MBE quedará acoplado a PacketEvents. 
  → *Mitigación*: Usaremos exclusivamente el artefacto `packetevents-api` (no la implementación spigot), cuya interfaz es muy estable.
- **[Risk] Rendimiento en redes inmensas**: Generar miles de partículas puede colapsar el cliente o el hilo del servidor. 
  → *Mitigación*: Usaremos interpolaciones moderadas (ej. 1 partícula cada 0.5 bloques) y se podría limitar a trazar como máximo N nodos para prevenir abusos.
