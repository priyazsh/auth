package me.priyazsh.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for validated authorization requests waiting on the login
 * page.
 *
 * <p>This is sufficient for a single instance. For multiple instances, back it
 * with the database or Redis so the login page and the callback can land on
 * different nodes.</p>
 */
@Service
public class PendingAuthorizationStore {

    private final Map<String, PendingAuthorization> pending = new ConcurrentHashMap<>();

    public void put(PendingAuthorization authorization) {
        pending.put(authorization.getRequestId(), authorization);
    }

    /** Read without consuming (the login page needs the client name). */
    public PendingAuthorization get(String requestId) {
        if (requestId == null) {
            return null;
        }
        PendingAuthorization authorization = pending.get(requestId);
        if (authorization == null || authorization.isExpired()) {
            pending.remove(requestId);
            return null;
        }
        return authorization;
    }

    /** Atomically fetch and remove (single-use). */
    public PendingAuthorization consume(String requestId) {
        if (requestId == null) {
            return null;
        }
        PendingAuthorization authorization = pending.remove(requestId);
        if (authorization == null || authorization.isExpired()) {
            return null;
        }
        return authorization;
    }

    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        pending.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
