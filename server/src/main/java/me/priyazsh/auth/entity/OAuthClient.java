package me.priyazsh.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "oauth_clients")
public class OAuthClient {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "client_id", unique = true, nullable = false)
    @NotBlank
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    @NotBlank
    private String clientSecret;

    /**
     * One or more exact redirect URIs, comma-separated. Exact matching only:
     * no wildcards and no trailing-slash normalisation.
     */
    @Column(name = "redirect_uri", nullable = false, length = 1024)
    @NotBlank
    private String redirectUri;

    @Column(name = "name")
    @NotBlank
    private String name;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public OAuthClient() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public OAuthClient(String clientId, String clientSecret, String redirectUri, String name) {
        this();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.name = name;
    }

    public List<String> getRedirectUriList() {
        if (redirectUri == null || redirectUri.isBlank()) {
            return List.of();
        }
        return Arrays.stream(redirectUri.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    public boolean allowsRedirectUri(String candidate) {
        return candidate != null && getRedirectUriList().contains(candidate);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
