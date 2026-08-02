## 1. Wire Cutter Persistence

- [x] 1.1 Identify where wire cutter disconnection events occur and add logic to save disconnected face data to chunk/block metadata via `StorageService`.
- [x] 1.2 Modify assembly/network chunk load logic to read the persistent data and skip automatic connection for faces explicitly marked as disconnected.

## 2. Debug Particles Fix

- [x] 2.1 Locate the debug visualization logic (where `WrapperPlayServerParticle` packets are dispatched).
- [x] 2.2 Remove any filters restricting traversal to only `energy` networks, allowing traversal across all graph implementations.

## 3. Visual Mappings Configuration

- [x] 3.1 Create configuration parser to load `mappings/*.yml` into memory (e.g. at addon startup).
- [x] 3.2 Cache the mappings in an efficient in-memory lookup table.
- [x] 3.3 Modify the client-side `BlockDisplay` or rendering logic of cables to query this lookup table and stretch visual connections to adjacent valid blocks.

## 4. Verification

- [ ] 4.1 Test wire cutter persistence across server restarts.
- [ ] 4.2 Test debug particles on both `energy` and `information` networks.
- [ ] 4.3 Test visual mappings by creating a sample `mappings/test_cable.yml` and placing valid and invalid blocks next to it.
