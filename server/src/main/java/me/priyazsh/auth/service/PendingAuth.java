package me.priyazsh.auth.service;

import java.time.Instant;

/**
 * Short-lived in-memory record of an authorization request that is currently
 * in flight at Google. Keyed by the internal {@code state} value we sent to
 * Google. Never persisted; entries expire after 10 minutes.
 */
public class PendingAuth {

    private final String state;
    private final String clientId;
    private final String redirectUri;
    private final String clientState;
    private final String codeChallenge;
    private final String nonce;
    private final String codeVerifier;
    private final String callbackUrl;
    private final Instant createdAt;

    public PendingAuth(String state,
                       String clientId,
                       String redirectUri,
                       String clientState,
                       String codeChallenge,
                       String nonce,
                       String codeVerifier,
                       String callbackUrl) {
        this.state = state;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.clientState = clientState;
        this.codeChallenge = codeChallenge;
        this.nonce = nonce;
        this.codeVerifier = codeVerifier;
        this.callbackUrl = callbackUrl;
        this.createdAt = Instant.now();
    }

    public String getState() { return state; }
    public String getClientId() { return clientId; }
    public String getRedirectUri() { return redirectUri; }
    public String getClientState() { return clientState; }
    public String getCodeChallenge() { return codeChallenge; }
    public String getNonce() { return nonce; }
    public String getCodeVerifier() { return codeVerifier; }
    public String getCallbackUrl() { return callbackUrl; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return createdAt.plusSeconds(600).isBefore(Instant.now());
    }
}
