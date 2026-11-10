package me.priyazsh.auth.controller;

import me.priyazsh.auth.entity.OAuthClient;
import me.priyazsh.auth.repository.OAuthClientRepository;
import me.priyazsh.auth.service.CryptoService;
import me.priyazsh.auth.service.OAuth2ClientService;
import me.priyazsh.auth.service.PendingAuth;
import me.priyazsh.auth.service.PendingAuthStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/oauth2")
public class AuthorizeController {

    private final OAuthClientRepository oAuthClientRepository;
    private final CryptoService cryptoService;
    private final OAuth2ClientService oAuth2ClientService;
    private final PendingAuthStore pendingAuthStore;

    public AuthorizeController(OAuthClientRepository oAuthClientRepository,
                               CryptoService cryptoService,
                               OAuth2ClientService oAuth2ClientService,
                               PendingAuthStore pendingAuthStore) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.cryptoService = cryptoService;
        this.oAuth2ClientService = oAuth2ClientService;
        this.pendingAuthStore = pendingAuthStore;
    }

    /**
     * OAuth2/OIDC authorization endpoint.
     *
     * <pre>
     * GET /oauth2/authorize?client_id=...&redirect_uri=...&response_type=code
     *     &scope=openid+profile+email&state=...&nonce=...
     *     &code_challenge=...&code_challenge_method=S256
     * </pre>
     *
     * Validates the client, then redirects the browser to Google.
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "response_type", defaultValue = "code") String responseType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam("state") String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "code_challenge_method", defaultValue = "S256") String codeChallengeMethod) {

        OAuthClient client = oAuthClientRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown client_id: " + clientId));

        if (!client.allowsRedirectUri(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri is not registered for client: " + clientId);
        }
        if (!"code".equals(responseType)) {
            throw new IllegalArgumentException("Only response_type=code is supported");
        }
        if (!"S256".equalsIgnoreCase(codeChallengeMethod)) {
            throw new IllegalArgumentException("Only code_challenge_method=S256 is supported");
        }
        if (!oAuth2ClientService.isConfigured()) {
            throw new IllegalStateException("Google OAuth2 client is not configured");
        }

        // Internal state protects our Google hop; the client's state is echoed back later.
        String internalState = cryptoService.generateState();
        String googleNonce = (nonce != null && !nonce.isBlank()) ? nonce : cryptoService.generateNonce();
        String googleVerifier = cryptoService.generateCodeVerifier();
        String googleChallenge = cryptoService.generateCodeChallenge(googleVerifier);

        pendingAuthStore.put(new PendingAuth(
            internalState, clientId, redirectUri, state, codeChallenge,
            googleNonce, googleVerifier, null));

        String googleUrl = oAuth2ClientService.buildGoogleAuthorizeUrl(
            internalState, googleNonce, googleChallenge);

        return ResponseEntity.status(302).location(URI.create(googleUrl)).build();
    }
}
