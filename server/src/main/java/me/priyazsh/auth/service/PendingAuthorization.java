package me.priyazsh.auth.service;

import java.time.Instant;

/**
 * A validated OAuth authorization request that is waiting for the user to
 * authenticate at the Auth Center login page.
 *
 * <p>Created by {@code GET /oauth2/authorize} when the browser has no existing
 * Auth Center session. The login page uses the opaque {@code requestId} to show
 * the requesting app and to start Google only after the user clicks.</p>
 *
 * <p>In-memory only; entries expire after 10 minutes.</p>
 */
public class PendingAuthorization {

    private final String requestId;
    private final String clientId;
    private final String clientName;
    private final String redirectUri;
    private final String clientState;
    private final String codeChallenge;
    private final String scope;
    private final String nonce;
    private final Instant createdAt;

    public PendingAuthorization(String requestId,
                                String clientId,
                                String clientName,
                                String redirectUri,
                                String clientState,
                                String codeChallenge,
                                String scope,
                                String nonce) {
        this.requestId = requestId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.redirectUri = redirectUri;
        this.clientState = clientState;
        this.codeChallenge = codeChallenge;
        this.scope = scope;
        this.nonce = nonce;
        this.createdAt = Instant.now();
    }

    public String getRequestId() { return requestId; }
    public String getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getRedirectUri() { return redirectUri; }
    public String getClientState() { return clientState; }
    public String getCodeChallenge() { return codeChallenge; }
    public String getScope() { return scope; }
    public String getNonce() { return nonce; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return createdAt.plusSeconds(600).isBefore(Instant.now());
    }
}
