package me.priyazsh.auth.controller;

import jakarta.validation.Valid;
import me.priyazsh.auth.dto.SocialSignInRequest;
import me.priyazsh.auth.dto.SocialSignInResponse;
import me.priyazsh.auth.service.CryptoService;
import me.priyazsh.auth.service.OAuth2ClientService;
import me.priyazsh.auth.service.PendingAuth;
import me.priyazsh.auth.service.PendingAuthStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SocialSignInController {

    private final OAuth2ClientService oAuth2ClientService;
    private final CryptoService cryptoService;
    private final PendingAuthStore pendingAuthStore;

    public SocialSignInController(OAuth2ClientService oAuth2ClientService,
                                  CryptoService cryptoService,
                                  PendingAuthStore pendingAuthStore) {
        this.oAuth2ClientService = oAuth2ClientService;
        this.cryptoService = cryptoService;
        this.pendingAuthStore = pendingAuthStore;
    }

    /**
     * Begin a direct Google sign-in.
     *
     * <p>Body: {@code {"provider":"google","callbackURL":"/dashboard"}}. Returns
     * the Google authorization URL for the browser to follow.</p>
     */
    @PostMapping("/sign-in/social")
    public ResponseEntity<?> signInSocial(@Valid @RequestBody SocialSignInRequest request) {
        if (!"google".equalsIgnoreCase(request.getProvider())) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "unsupported_provider"));
        }
        if (!oAuth2ClientService.isConfigured()) {
            return ResponseEntity.status(503)
                .body(java.util.Map.of("error", "google_not_configured"));
        }

        String state = cryptoService.generateState();
        String nonce = cryptoService.generateNonce();
        String codeVerifier = cryptoService.generateCodeVerifier();
        String codeChallenge = cryptoService.generateCodeChallenge(codeVerifier);

        pendingAuthStore.put(new PendingAuth(
            state, null, null, null, null, nonce, codeVerifier, request.getCallbackURL()));

        String googleUrl = oAuth2ClientService.buildGoogleAuthorizeUrl(state, nonce, codeChallenge);
        return ResponseEntity.ok(new SocialSignInResponse(googleUrl, true));
    }
}
