package com.darkbladedev.engine.persistence;

import com.darkbladedev.engine.api.persistence.PersistentStorageService;
import com.darkbladedev.engine.api.persistence.StorageDomain;
import com.darkbladedev.engine.api.persistence.StorageFlushReport;
import com.darkbladedev.engine.api.persistence.StorageMetrics;
import com.darkbladedev.engine.api.persistence.StorageNamespace;
import com.darkbladedev.engine.api.persistence.StorageRecordMeta;
import com.darkbladedev.engine.api.persistence.StorageRecoveryReport;
import com.darkbladedev.engine.api.persistence.StorageSchema;
import com.darkbladedev.engine.api.persistence.StorageStore;
import com.darkbladedev.engine.api.persistence.StorageStoreMetrics;
import com.darkbladedev.engine.api.persistence.StorageWriteResult;
import com.darkbladedev.engine.api.persistence.StoredRecord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

public final class FilePersistentStorageService implements PersistentStorageService {

    private static final int SNAPSHOT_MAGIC = 0x4D_42_45_53;
    private static final int WAL_MAGIC = 0x4D_42_45_57;

    private final Path root;
    private final ExecutorService io;
    private final Map<StoreKey, FileStore> openStores = new ConcurrentHashMap<>();

    private final AtomicLong totalWrites = new AtomicLong(0);
    private final AtomicLong totalBytesWritten = new AtomicLong(0);
    private final AtomicLong lastFlushTimestamp = new AtomicLong(0);
    private final AtomicLong recoveryActions = new AtomicLong(0);

    public FilePersistentStorageService(Path root) {
        this.root = Objects.requireNonNull(root, "root");
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "MBE-Persistence-IO");
            t.setDaemon(true);
            return t;
        };
        this.io = Executors.newSingleThreadExecutor(tf);
    }

    @Override
    public void initialize() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create persistence root: " + root, e);
        }
    }

    @Override
    public StorageRecoveryReport recover() {
        List<StorageRecoveryReport.StorageRecoveryAction> actions = new ArrayList<>();

        for (FileStore store : openStores.values()) {
            actions.addAll(store.recoverOnOpen());
        }

        if (!actions.isEmpty()) {
            recoveryActions.addAndGet(actions.size());
        }

        return actions.isEmpty()
            ? StorageRecoveryReport.empty()
            : new StorageRecoveryReport(true, actions);
    }

    @Override
    public StorageFlushReport flush() {
        long started = System.currentTimeMillis();
        List<StorageFlushReport.StorageFlushResult> results = new ArrayList<>();

        for (Map.Entry<StoreKey, FileStore> e : openStores.entrySet()) {
            StoreKey key = e.getKey();
            FileStore store = e.getValue();

            long t0 = System.currentTimeMillis();
            long bytes = 0;
            String err = null;
            boolean ok = true;
            try {
                bytes = store.snapshotAndResetWal();
            } catch (Exception ex) {
                ok = false;
                err = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            }

            results.add(new StorageFlushReport.StorageFlushResult(
                key.namespace,
                key.domain,
                key.store,
                ok,
                bytes,
                Math.max(0, System.currentTimeMillis() - t0),
                err
            ));
        }

        long ts = System.currentTimeMillis();
        lastFlushTimestamp.set(ts);

        StorageFlushReport report = new StorageFlushReport(ts, results);
        long duration = Math.max(0, System.currentTimeMillis() - started);
        totalBytesWritten.addAndGet(results.stream().mapToLong(StorageFlushReport.StorageFlushResult::bytesWritten).sum());

        return report;
    }

    @Override
    public void shutdown(boolean graceful) {
        if (graceful) {
            try {
                flush();
            } catch (Exception ignored) {
            }
        }

        io.shutdown();
        try {
            io.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            io.shutdownNow();
        }
    }

    @Override
    public StorageNamespace namespace(String namespace) {
        String ns = normalizeSegment(namespace);
        return new FileNamespace(ns);
    }

    @Override
    public StorageMetrics metrics() {
        long pending = openStores.values().stream().mapToLong(s -> s.metrics().pendingWrites()).sum();
        return new StorageMetrics(
            pending,
            totalWrites.get(),
            totalBytesWritten.get(),
            lastFlushTimestamp.get(),
            recoveryActions.get()
        );
    }

    private FileStore open(String namespace, String domain, String storeId, StorageSchema schema) {
        StoreKey key = new StoreKey(namespace, domain, storeId);
        return openStores.computeIfAbsent(key, k -> new FileStore(root.resolve(k.namespace).resolve(k.domain).resolve(k.store), schema));
    }

    private static String normalizeSegment(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return "unknown";
        }
        String lower = v.toLowerCase(java.util.Locale.ROOT);
        String safe = lower.replace('\\', '_').replace('/', '_');
        safe = safe.replace("..", "_");
        return safe;
    }

    private record StoreKey(String namespace, String domain, String store) {}

    private final class FileNamespace implements StorageNamespace {
        private final String id;

        private FileNamespace(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public StorageDomain domain(String domain) {
            return new FileDomain(id, normalizeSegment(domain));
        }
    }

    private final class FileDomain implements StorageDomain {
        private final String namespace;
        private final String id;

        private FileDomain(String namespace, String id) {
            this.namespace = namespace;
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public StorageStore store(String storeId, StorageSchema schema) {
            Objects.requireNonNull(schema, "schema");
            return open(namespace, id, normalizeSegment(storeId), schema);
        }
    }

    private final class FileStore implements StorageStore {
        private final Path dir;
        private final Path wal;
        private final Path snapshot;
        private final Path snapshotTmp;
        private final StorageSchema schema;

        private final AtomicReference<Map<String, StoredRecord>> state = new AtomicReference<>(Map.of());
        private final AtomicLong pendingWrites = new AtomicLong(0);
        private final AtomicLong storeWrites = new AtomicLong(0);
        private final AtomicLong storeBytesWritten = new AtomicLong(0);
        private final AtomicLong lastWriteTimestamp = new AtomicLong(0);

        private final List<StorageRecoveryReport.StorageRecoveryAction> pendingRecovery = new ArrayList<>();
        private volatile boolean opened = false;

        private FileStore(Path dir, StorageSchema schema) {
            this.dir = dir;
            this.wal = dir.resolve("wal.log");
            this.snapshot = dir.resolve("snapshot.bin");
            this.snapshotTmp = dir.resolve("snapshot.bin.tmp");
            this.schema = schema;
        }

        @Override
        public String id() {
            return dir.getFileName().toString();
        }

        @Override
        public StorageSchema schema() {
            return schema;
        }

        @Override
        public Optional<StoredRecord> read(String key) {
            if (key == null) {
                return Optional.empty();
            }
            ensureOpened();
            return Optional.ofNullable(state.get().get(key));
        }

        @Override
        public Map<String, StoredRecord> readAll() {
            ensureOpened();
            return state.get();
        }

        @Override
        public StorageWriteResult write(String key, byte[] payload, StorageRecordMeta meta) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(payload, "payload");
            Objects.requireNonNull(meta, "meta");
            return runDurable(() -> writeInternal(Op.PUT, key, payload, meta));
        }

        @Override
        public StorageWriteResult delete(String key, StorageRecordMeta meta) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(meta, "meta");
            return runDurable(() -> writeInternal(Op.DEL, key, new byte[0], meta));
        }

        @Override
        public CompletableFuture<StorageWriteResult> writeAsync(String key, byte[] payload, StorageRecordMeta meta) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(payload, "payload");
            Objects.requireNonNull(meta, "meta");

            CompletableFuture<StorageWriteResult> f = new CompletableFuture<>();
            pendingWrites.incrementAndGet();
            io.execute(() -> {
                try {
                    f.complete(writeInternal(Op.PUT, key, payload, meta));
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                } finally {
                    pendingWrites.decrementAndGet();
                }
            });
            return f;
        }

        @Override
        public CompletableFuture<StorageWriteResult> deleteAsync(String key, StorageRecordMeta meta) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(meta, "meta");

            CompletableFuture<StorageWriteResult> f = new CompletableFuture<>();
            pendingWrites.incrementAndGet();
            io.execute(() -> {
                try {
                    f.complete(writeInternal(Op.DEL, key, new byte[0], meta));
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                } finally {
                    pendingWrites.decrementAndGet();
                }
            });
            return f;
        }

        @Override
        public StorageStoreMetrics metrics() {
            return new StorageStoreMetrics(
                pendingWrites.get(),
                storeWrites.get(),
                storeBytesWritten.get(),
                lastWriteTimestamp.get()
            );
        }

        private List<StorageRecoveryReport.StorageRecoveryAction> recoverOnOpen() {
            ensureOpened();
            if (pendingRecovery.isEmpty()) {
                return List.of();
            }
            return List.copyOf(pendingRecovery);
        }

        private long snapshotAndResetWal() {
            ensureOpened();
            return runDurable(this::snapshotAndResetWalInternal);
        }

        private void ensureOpened() {
            if (opened) {
                return;
            }
            runDurable(() -> {
                if (opened) {
                    return null;
                }
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to create store dir: " + dir, e);
                }

                recoverTempSnapshot();
                Map<String, StoredRecord> loaded = loadSnapshot();
                Map<String, StoredRecord> replayed = replayWal(loaded);
                state.set(Map.copyOf(replayed));
                opened = true;
                return null;
            });
        }

        private void recoverTempSnapshot() {
            try {
                boolean hasTmp = Files.exists(snapshotTmp);
                if (!hasTmp) {
                    return;
                }

                if (Files.exists(snapshot)) {
                    Files.deleteIfExists(snapshotTmp);
                    pendingRecovery.add(action("delete_tmp_snapshot", "tmp_over_snapshot"));
                    return;
                }

                Files.move(snapshotTmp, snapshot);
                pendingRecovery.add(action("promote_tmp_snapshot", "tmp_only"));
            } catch (Exception e) {
                pendingRecovery.add(action("tmp_snapshot_error", e.getClass().getSimpleName()));
            }
        }

        private Map<String, StoredRecord> loadSnapshot() {
            if (!Files.exists(snapshot)) {
                return new HashMap<>();
            }

            Map<String, StoredRecord> out = new HashMap<>();
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(snapshot, StandardOpenOption.READ)))) {
                int magic = in.readInt();
                if (magic != SNAPSHOT_MAGIC) {
                    pendingRecovery.add(action("snapshot_bad_magic", Integer.toHexString(magic)));
                    return out;
                }

                int schemaVersion = in.readInt();
                int entries = in.readInt();
                if (entries < 0) {
                    pendingRecovery.add(action("snapshot_bad_count", String.valueOf(entries)));
                    return out;
                }

                for (int i = 0; i < entries; i++) {
                    String key = readString(in);
                    int payloadLen = in.readInt();
                    if (payloadLen < 0) {
                        break;
                    }
                    byte[] payload = in.readNBytes(payloadLen);
                    int recordSchema = in.readInt();
                    long ts = in.readLong();
                    String producer = readString(in);
                    int crc32 = in.readInt();
                    if (crc32(payload) != crc32) {
                        pendingRecovery.add(action("snapshot_record_crc_mismatch", key));
                        continue;
                    }

                    int target = schema.schemaVersion();
                    byte[] migrated = payload;
                    int migratedSchema = recordSchema;
                    if (recordSchema != target) {
                        StorageSchema.StorageSchemaMigrator migrator = schema.migrator();
                        if (migrator == null) {
                            pendingRecovery.add(action("snapshot_no_migrator", key));
                            continue;
                        }
                        migrated = migrator.migrate(recordSchema, target, payload);
                        migratedSchema = target;
                    }

                    out.put(key, new StoredRecord(migrated, migratedSchema, producer, ts, crc32(migrated)));
                }

                if (schemaVersion != schema.schemaVersion()) {
                    pendingRecovery.add(action("snapshot_schema_mismatch", schemaVersion + "->" + schema.schemaVersion()));
                }

            } catch (EOFException ignored) {
                pendingRecovery.add(action("snapshot_truncated", "eof"));
            } catch (Exception e) {
                pendingRecovery.add(action("snapshot_read_error", e.getClass().getSimpleName()));
            }

            return out;
        }

        private Map<String, StoredRecord> replayWal(Map<String, StoredRecord> base) {
            if (!Files.exists(wal)) {
                return base;
            }

            Map<String, StoredRecord> out = new HashMap<>(base);
            long safeSize = 0;
            try (FileChannel ch = FileChannel.open(wal, StandardOpenOption.READ)) {
                long size = ch.size();
                if (size <= 0) {
                    return out;
                }

                try (DataInputStream in = new DataInputStream(new BufferedInputStream(Channels.newInputStream(ch)))) {
                    while (true) {
                        long recordStart = ch.position();
                        int recordLen;
                        try {
                            recordLen = in.readInt();
                        } catch (EOFException eof) {
                            safeSize = recordStart;
                            break;
                        }
                        if (recordLen <= 0 || recordLen > 64 * 1024 * 1024) {
                            safeSize = recordStart;
                            pendingRecovery.add(action("wal_bad_record_len", String.valueOf(recordLen)));
                            break;
                        }
                        byte[] record = in.readNBytes(recordLen);
                        if (record.length != recordLen) {
                            safeSize = recordStart;
                            pendingRecovery.add(action("wal_truncated_record", "len"));
                            break;
                        }
                        int recordCrc;
                        try {
                            recordCrc = in.readInt();
                        } catch (EOFException eof) {
                            safeSize = recordStart;
                            pendingRecovery.add(action("wal_truncated_crc", "eof"));
                            break;
                        }
                        if (crc32(record) != recordCrc) {
                            safeSize = recordStart;
                            pendingRecovery.add(action("wal_record_crc_mismatch", "pos" + recordStart));
                            break;
                        }

                        try (DataInputStream rec = new DataInputStream(new ByteArrayInputStream(record))) {
                            int magic = rec.readInt();
                            if (magic != WAL_MAGIC) {
                                safeSize = recordStart;
                                pendingRecovery.add(action("wal_bad_magic", Integer.toHexString(magic)));
                                break;
                            }
                            int op = rec.readUnsignedByte();
                            int recordSchema = rec.readInt();
                            long ts = rec.readLong();
                            String producer = readString(rec);
                            String key = readString(rec);
                            byte[] payload = readBytes(rec);

                            int target = schema.schemaVersion();
                            byte[] migrated = payload;
                            int migratedSchema = recordSchema;
                            if (recordSchema != target) {
                                StorageSchema.StorageSchemaMigrator migrator = schema.migrator();
                                if (migrator == null) {
                                    pendingRecovery.add(action("wal_no_migrator", key));
                                    continue;
                                }
                                migrated = migrator.migrate(recordSchema, target, payload);
                                migratedSchema = target;
                            }

                            if (op == Op.PUT.code) {
                                out.put(key, new StoredRecord(migrated, migratedSchema, producer, ts, crc32(migrated)));
                            } else if (op == Op.DEL.code) {
                                out.remove(key);
                            } else {
                                pendingRecovery.add(action("wal_bad_op", String.valueOf(op)));
                            }
                        }

                        safeSize = ch.position();
                    }
                }
            } catch (Exception e) {
                pendingRecovery.add(action("wal_read_error", e.getClass().getSimpleName()));
            }

            try {
                if (Files.exists(wal)) {
                    long size = Files.size(wal);
                    if (safeSize < size) {
                        try (FileChannel ch = FileChannel.open(wal, StandardOpenOption.WRITE)) {
                            ch.truncate(safeSize);
                            ch.force(true);
                        }
                        pendingRecovery.add(action("wal_truncated", safeSize + "/" + size));
                    }
                }
            } catch (Exception e) {
                pendingRecovery.add(action("wal_truncate_error", e.getClass().getSimpleName()));
            }

            return out;
        }

        private StorageWriteResult writeInternal(Op op, String key, byte[] payload, StorageRecordMeta meta) {
            ensureOpened();

            int targetSchema = schema.schemaVersion();

            byte[] recordBytes = buildWalRecord(op, targetSchema, meta.timestamp(), meta.producerId(), key, payload);
            int recordCrc = crc32(recordBytes);
            byte[] frame = buildFrame(recordBytes, recordCrc);

            try {
                Files.createDirectories(dir);
                try (FileChannel ch = FileChannel.open(wal, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND))) {
                    ch.write(java.nio.ByteBuffer.wrap(frame));
                    ch.force(true);
                }
            } catch (IOException e) {
                return StorageWriteResult.ERROR;
            }

            storeWrites.incrementAndGet();
            totalWrites.incrementAndGet();
            storeBytesWritten.addAndGet(frame.length);
            totalBytesWritten.addAndGet(frame.length);
            lastWriteTimestamp.set(System.currentTimeMillis());

            Map<String, StoredRecord> base = state.get();
            Map<String, StoredRecord> next = new HashMap<>(base);
            if (op == Op.PUT) {
                next.put(key, new StoredRecord(payload, targetSchema, meta.producerId(), meta.timestamp(), crc32(payload)));
            } else {
                next.remove(key);
            }
            state.set(Map.copyOf(next));

            return StorageWriteResult.OK;
        }

        private long snapshotAndResetWalInternal() {
            Map<String, StoredRecord> cur = state.get();
            byte[] snapshotBytes = buildSnapshotBytes(cur);

            try {
                Files.createDirectories(dir);
                try (FileChannel ch = FileChannel.open(snapshotTmp, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    ch.write(java.nio.ByteBuffer.wrap(snapshotBytes));
                    ch.force(true);
                }

                try {
                    Files.move(snapshotTmp, snapshot, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(snapshotTmp, snapshot, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                if (Files.exists(wal)) {
                    try (FileChannel ch = FileChannel.open(wal, Set.of(StandardOpenOption.WRITE))) {
                        ch.truncate(0);
                        ch.force(true);
                    }
                }

                return snapshotBytes.length;
            } catch (IOException e) {
                throw new IllegalStateException("Snapshot failed for store dir=" + dir, e);
            }
        }

        private StorageRecoveryReport.StorageRecoveryAction action(String type, String detail) {
            Path rel;
            try {
                rel = root.relativize(dir);
            } catch (Exception e) {
                rel = dir;
            }
            String[] seg = rel.toString().replace('\\', '/').split("/");
            String ns = seg.length > 0 ? seg[0] : "unknown";
            String dom = seg.length > 1 ? seg[1] : "unknown";
            String st = seg.length > 2 ? seg[2] : "unknown";
            return new StorageRecoveryReport.StorageRecoveryAction(type, ns, dom, st, detail);
        }

        private <T> T runDurable(java.util.concurrent.Callable<T> c) {
            try {
                pendingWrites.incrementAndGet();
                CompletableFuture<T> f = new CompletableFuture<>();
                io.execute(() -> {
                    try {
                        f.complete(c.call());
                    } catch (Throwable t) {
                        f.completeExceptionally(t);
                    } finally {
                        pendingWrites.decrementAndGet();
                    }
                });
                return f.join();
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }

    private enum Op {
        PUT(1),
        DEL(2);

        private final int code;

        Op(int code) {
            this.code = code;
        }
    }

    private static byte[] buildFrame(byte[] recordBytes, int recordCrc) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(recordBytes.length + 8);
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(recordBytes.length);
            out.write(recordBytes);
            out.writeInt(recordCrc);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] buildWalRecord(Op op, int schemaVersion, long timestamp, String producerId, String key, byte[] payload) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(WAL_MAGIC);
            out.writeByte(op.code);
            out.writeInt(schemaVersion);
            out.writeLong(timestamp);
            writeString(out, producerId);
            writeString(out, key);
            writeBytes(out, payload);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] buildSnapshotBytes(Map<String, StoredRecord> entries) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(baos));

            out.writeInt(SNAPSHOT_MAGIC);
            int schemaVersion = entries.values().stream().findFirst().map(StoredRecord::schemaVersion).orElse(0);
            out.writeInt(schemaVersion);
            out.writeInt(entries.size());

            for (Map.Entry<String, StoredRecord> e : entries.entrySet()) {
                writeString(out, e.getKey());
                StoredRecord r = e.getValue();
                out.writeInt(r.payload().length);
                out.write(r.payload());
                out.writeInt(r.schemaVersion());
                out.writeLong(r.timestamp());
                writeString(out, r.producerId());
                out.writeInt(crc32(r.payload()));
            }

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 16 * 1024 * 1024) {
            return "";
        }
        byte[] bytes = in.readNBytes(len);
        if (bytes.length != len) {
            throw new EOFException();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeBytes(DataOutputStream out, byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static byte[] readBytes(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 64 * 1024 * 1024) {
            return new byte[0];
        }
        byte[] bytes = in.readNBytes(len);
        if (bytes.length != len) {
            throw new EOFException();
        }
        return bytes;
    }

    private static final class Channels {
        private static java.io.InputStream newInputStream(FileChannel ch) {
            return java.nio.channels.Channels.newInputStream(ch);
        }
    }
}
