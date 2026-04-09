package dev.darkblade.mbe.core.application.service.io;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOChannel;
import dev.darkblade.mbe.api.io.IOPayload;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.io.IOType;
import dev.darkblade.mbe.api.io.TransferResult;
import dev.darkblade.mbe.api.io.event.IOTransferFailEvent;
import dev.darkblade.mbe.api.io.event.PortRegisteredEvent;
import dev.darkblade.mbe.api.io.event.PortUnregisteredEvent;
import dev.darkblade.mbe.api.io.event.PostIOTransferEvent;
import dev.darkblade.mbe.api.io.event.PreIOTransferEvent;
import dev.darkblade.mbe.api.persistence.PersistentStorageService;
import dev.darkblade.mbe.api.persistence.StorageRecordMeta;
import dev.darkblade.mbe.api.persistence.StorageSchema;
import dev.darkblade.mbe.api.persistence.StorageStore;
import dev.darkblade.mbe.api.persistence.StoredRecord;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DefaultIOService implements IOService {
    private static final String NAMESPACE = "mbe:io_ports";
    private static final String DOMAIN = "ports";
    private static final String STORE = "registry";
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final PersistentStorageService persistence;
    private final StorageStore store;
    private final Consumer<Event> eventCaller;
    private final Gson gson = new Gson();
    private final InternalNetwork internalNetwork = new InternalNetwork();
    private final Map<ChannelType, IOChannel> channels = new EnumMap<>(ChannelType.class);
    private final ConcurrentHashMap<MultiblockInstance, ConcurrentHashMap<BlockPos, IOPort>> portsByInstance = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, IOPort> portsByPosition = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> networkLocks = new ConcurrentHashMap<>();

    public DefaultIOService(PersistentStorageService persistence) {
        this(persistence, Bukkit.getPluginManager()::callEvent);
    }

    public DefaultIOService(PersistentStorageService persistence, Consumer<Event> eventCaller) {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.eventCaller = Objects.requireNonNull(eventCaller, "eventCaller");
        this.store = this.persistence.namespace(NAMESPACE).domain(DOMAIN).store(STORE, schema());
        for (ChannelType value : ChannelType.values()) {
            channels.put(value, new BasicIOChannel(value));
        }
        restorePersistedPorts();
    }

    @Override
    public String getServiceId() {
        return "mbe:io.service";
    }

    @Override
    public Collection<IOPort> getPorts(MultiblockInstance instance) {
        if (instance == null) {
            return List.of();
        }
        Map<BlockPos, IOPort> ports = portsByInstance.get(instance);
        if (ports == null || ports.isEmpty()) {
            return List.of();
        }
        return List.copyOf(ports.values());
    }

    public Optional<IOPort> findPort(BlockPos position) {
        if (position == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(portsByPosition.get(position));
    }

    @Override
    public void registerPort(IOPort port) {
        Objects.requireNonNull(port, "port");
        portsByPosition.put(port.getPosition(), port);
        portsByInstance
                .computeIfAbsent(port.getOwner(), unused -> new ConcurrentHashMap<>())
                .put(port.getPosition(), port);
        internalNetwork.register(port);
        persist(port);
        eventCaller.accept(new PortRegisteredEvent(port));
    }

    @Override
    public void unregisterPort(IOPort port) {
        if (port == null) {
            return;
        }
        portsByPosition.remove(port.getPosition());
        ConcurrentHashMap<BlockPos, IOPort> byPos = portsByInstance.get(port.getOwner());
        if (byPos != null) {
            byPos.remove(port.getPosition());
            if (byPos.isEmpty()) {
                portsByInstance.remove(port.getOwner(), byPos);
            }
        }
        internalNetwork.unregister(port);
        store.delete(keyOf(port.getPosition()), StorageRecordMeta.now("core"));
        eventCaller.accept(new PortUnregisteredEvent(port));
    }

    @Override
    public TransferResult transfer(IOPort from, IOPort to, IOPayload payload) {
        if (from == null || to == null || payload == null) {
            fail(from, to, payload, "invalid_arguments");
            return TransferResult.FAIL;
        }
        Object lock = networkLocks.computeIfAbsent(from.getNetworkId(), unused -> new Object());
        synchronized (lock) {
        if (!validateSameChannel(from, to, payload)) {
            fail(from, to, payload, "channel_mismatch");
            return TransferResult.FAIL;
        }
        if (!validateDirection(from, to)) {
            fail(from, to, payload, "direction_blocked");
            return TransferResult.BLOCKED;
        }
        if (!validateOwnership(from, to)) {
            fail(from, to, payload, "network_blocked");
            return TransferResult.BLOCKED;
        }

        PreIOTransferEvent pre = new PreIOTransferEvent(from, to, payload);
        eventCaller.accept(pre);
        if (pre.isCancelled()) {
            TransferResult cancelledResult = pre.getOverrideResult() == null ? TransferResult.BLOCKED : pre.getOverrideResult();
            if (cancelledResult != TransferResult.SUCCESS && cancelledResult != TransferResult.PARTIAL) {
                fail(from, to, pre.getPayload(), "cancelled");
            }
            return cancelledResult;
        }
        if (pre.getOverrideResult() != null) {
            TransferResult forced = pre.getOverrideResult();
            eventCaller.accept(new PostIOTransferEvent(from, to, pre.getPayload(), forced));
            if (forced != TransferResult.SUCCESS && forced != TransferResult.PARTIAL) {
                fail(from, to, pre.getPayload(), "forced_fail");
            }
            return forced;
        }

        IOChannel channel = resolveChannel(from.getChannel());
        if (channel == null || !channel.canTransfer(pre.getPayload())) {
            fail(from, to, pre.getPayload(), "channel_not_supported");
            return TransferResult.FAIL;
        }

        TransferResult result = channel.transfer(from, to, pre.getPayload());
        eventCaller.accept(new PostIOTransferEvent(from, to, pre.getPayload(), result));
        if (result != TransferResult.SUCCESS && result != TransferResult.PARTIAL) {
            fail(from, to, pre.getPayload(), "transfer_failed");
        }
        return result;
        }
    }

    private boolean validateSameChannel(IOPort from, IOPort to, IOPayload payload) {
        return from.getChannel() == to.getChannel() && from.getChannel() == payload.getType();
    }

    private boolean validateDirection(IOPort from, IOPort to) {
        return supportsOutput(from.getType()) && supportsInput(to.getType());
    }

    private boolean validateOwnership(IOPort from, IOPort to) {
        return Objects.equals(from.getNetworkId(), to.getNetworkId());
    }

    private IOChannel resolveChannel(ChannelType channelType) {
        if (channelType == null) {
            return null;
        }
        return channels.get(channelType);
    }

    private void fail(IOPort from, IOPort to, IOPayload payload, String reason) {
        if (from == null || to == null || payload == null || reason == null) {
            return;
        }
        eventCaller.accept(new IOTransferFailEvent(from, to, payload, reason));
    }

    private void persist(IOPort port) {
        Map<String, Object> data = Map.of(
                "position", serializePosition(port.getPosition()),
                "face", port.getFace().name(),
                "type", port.getType().name(),
                "channel", port.getChannel().name(),
                "networkId", port.getNetworkId().toString()
        );
        byte[] payload = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        store.write(keyOf(port.getPosition()), payload, StorageRecordMeta.now("core"));
    }

    private void restorePersistedPorts() {
        for (Map.Entry<String, StoredRecord> entry : store.readAll().entrySet()) {
            StoredRecord record = entry.getValue();
            if (record == null || record.payload() == null || record.payload().length == 0) {
                continue;
            }
            try {
                String json = new String(record.payload(), StandardCharsets.UTF_8);
                Map<String, Object> data = gson.fromJson(json, MAP_TYPE);
                BlockPos position = deserializePosition(String.valueOf(data.get("position")));
                if (position == null) {
                    continue;
                }
                Direction face = Direction.valueOf(String.valueOf(data.get("face")));
                IOType type = IOType.valueOf(String.valueOf(data.get("type")));
                ChannelType channel = ChannelType.valueOf(String.valueOf(data.get("channel")));
                UUID networkId = UUID.fromString(String.valueOf(data.get("networkId")));
                portsByPosition.put(position, new DetachedIOPort(position, face, type, channel, networkId));
            } catch (Throwable ignored) {
            }
        }
    }

    private static String serializePosition(BlockPos position) {
        return position.worldId() + ":" + position.x() + ":" + position.y() + ":" + position.z();
    }

    private static BlockPos deserializePosition(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(":");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new BlockPos(
                    UUID.fromString(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean supportsInput(IOType type) {
        return type == IOType.INPUT || type == IOType.BOTH;
    }

    private static boolean supportsOutput(IOType type) {
        return type == IOType.OUTPUT || type == IOType.BOTH;
    }

    private static String keyOf(BlockPos position) {
        return position.worldId() + "_" + position.x() + "_" + position.y() + "_" + position.z();
    }

    private static StorageSchema schema() {
        return new StorageSchema() {
            @Override
            public int schemaVersion() {
                return 1;
            }

            @Override
            public StorageSchemaMigrator migrator() {
                return (fromVersion, toVersion, payload) -> {
                    if (fromVersion == toVersion && toVersion == 1) {
                        return payload;
                    }
                    throw new IllegalStateException("Unsupported migration: " + fromVersion + "->" + toVersion);
                };
            }
        };
    }

    private static final class DetachedIOPort implements IOPort {
        private final BlockPos position;
        private final Direction face;
        private final IOType type;
        private final ChannelType channel;
        private final UUID networkId;

        private DetachedIOPort(BlockPos position, Direction face, IOType type, ChannelType channel, UUID networkId) {
            this.position = position;
            this.face = face;
            this.type = type;
            this.channel = channel;
            this.networkId = networkId;
        }

        @Override
        public BlockPos getPosition() {
            return position;
        }

        @Override
        public Direction getFace() {
            return face;
        }

        @Override
        public IOType getType() {
            return type;
        }

        @Override
        public ChannelType getChannel() {
            return channel;
        }

        @Override
        public UUID getNetworkId() {
            return networkId;
        }

        @Override
        public MultiblockInstance getOwner() {
            return null;
        }
    }
}
