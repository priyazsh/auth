package me.priyazsh.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for in-flight authorization requests.
 *
 * <p>This is sufficient for a single local instance. For multi-instance or
 * production deployments this should be backed by the database or Redis so
 * that state survives restarts and is shared across nodes.</p>
 */
@Service
public class PendingAuthStore {

    private final Map<String, PendingAuth> pending = new ConcurrentHashMap<>();

    public void put(PendingAuth auth) {
        pending.put(auth.getState(), auth);
    }

    /** Atomically fetch and remove an entry (single-use). */
    public PendingAuth consume(String state) {
        if (state == null) {
            return null;
        }
        PendingAuth auth = pending.remove(state);
        if (auth == null || auth.isExpired()) {
            return null;
        }
        return auth;
    }

    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        pending.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
