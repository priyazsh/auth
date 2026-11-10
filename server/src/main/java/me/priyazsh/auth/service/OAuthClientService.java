package me.priyazsh.auth.service;

import me.priyazsh.auth.entity.OAuthClient;
import me.priyazsh.auth.repository.OAuthClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OAuthClientService {

    private final OAuthClientRepository oAuthClientRepository;

    public OAuthClientService(OAuthClientRepository oAuthClientRepository) {
        this.oAuthClientRepository = oAuthClientRepository;
    }

    public Optional<OAuthClient> findByClientId(String clientId) {
        return oAuthClientRepository.findByClientId(clientId);
    }

    public boolean validateRedirectUri(String clientId, String redirectUri) {
        Optional<OAuthClient> client = oAuthClientRepository.findByClientId(clientId);
        return client.isPresent() && client.get().getRedirectUri().equals(redirectUri);
    }

    public OAuthClient registerClient(String clientId, String clientSecret, String redirectUri, String name) {
        if (oAuthClientRepository.existsByClientId(clientId)) {
            throw new IllegalArgumentException("Client already registered: " + clientId);
        }
        OAuthClient client = new OAuthClient(clientId, clientSecret, redirectUri, name);
        return oAuthClientRepository.save(client);
    }
}
