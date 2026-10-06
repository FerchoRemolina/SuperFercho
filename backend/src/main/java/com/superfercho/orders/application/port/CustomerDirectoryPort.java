package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Batch commercial-identity lookup for order customer ids. Consumer-owned
 * port (orders side); the adapter lives in orders infrastructure and bridges
 * to Identity without joins or FKs across schemas.
 */
public interface CustomerDirectoryPort {

    /**
     * Resolves as many ids as possible in bulk. Unknown or deleted-record ids
     * are simply absent from the result; the map never contains null values.
     */
    Map<UUID, CustomerDirectoryEntry> findByUserIds(Collection<UUID> userIds);
}
