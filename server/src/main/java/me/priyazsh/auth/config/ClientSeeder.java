package me.priyazsh.auth.config;

import me.priyazsh.auth.entity.OAuthClient;
import me.priyazsh.auth.repository.OAuthClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures the GoLink OAuth client exists on startup. Idempotent: existing
 * clients are left untouched. Secrets come from configuration, never code.
 */
@Component
public class ClientSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientSeeder.class);

    private final OAuthClientRepository oAuthClientRepository;

    @Value("${auth.seed.client-id}")
    private String clientId;

    @Value("${auth.seed.client-secret}")
    private String clientSecret;

    @Value("${auth.seed.redirect-uris}")
    private String redirectUris;

    @Value("${auth.seed.name:OAuth Client}")
    private String name;

    public ClientSeeder(OAuthClientRepository oAuthClientRepository) {
        this.oAuthClientRepository = oAuthClientRepository;
    }

    @Override
    public void run(String... args) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }
        if (oAuthClientRepository.existsByClientId(clientId)) {
            log.info("OAuth client '{}' already registered", clientId);
            return;
        }
        if (clientSecret == null || clientSecret.isBlank() || "change_me_client_secret".equals(clientSecret)) {
            log.warn("Seeding OAuth client '{}' with a default/dev secret. Set GOLINK_CLIENT_SECRET in production.",
                clientId);
        }
        OAuthClient client = new OAuthClient(clientId, clientSecret, redirectUris, name);
        oAuthClientRepository.save(client);
        log.info("Seeded OAuth client '{}' with redirect URIs: {}", clientId, redirectUris);
    }
}
