package dev.darkblade.mbe.api.persistence;

import java.util.Map;
import java.util.UUID;

public interface StorageDescriptor {

    UUID id();

    long capacity();

    Map<String, Object> properties();
}
