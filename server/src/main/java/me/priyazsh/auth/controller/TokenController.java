package me.priyazsh.auth.controller;

import me.priyazsh.auth.dto.TokenResponse;
import me.priyazsh.auth.entity.AuthorizationCode;
import me.priyazsh.auth.entity.OAuthClient;
import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.repository.AuthorizationCodeRepository;
import me.priyazsh.auth.repository.OAuthClientRepository;
import me.priyazsh.auth.service.CryptoService;
import me.priyazsh.auth.service.JwtService;
import me.priyazsh.auth.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class TokenController {

    private final OAuthClientRepository oAuthClientRepository;
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final CryptoService cryptoService;

    public TokenController(OAuthClientRepository oAuthClientRepository,
                           AuthorizationCodeRepository authorizationCodeRepository,
                           UserService userService,
                           JwtService jwtService,
                           CryptoService cryptoService) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.cryptoService = cryptoService;
    }

    /**
     * OIDC token endpoint (authorization_code grant).
     *
     * <p>Server-to-server only: the client authenticates with its
     * {@code client_secret} and proves PKCE possession with {@code code_verifier}.
     * Returns an ES256 ID token and access token.</p>
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier) {

        if (!"authorization_code".equals(grantType)) {
            return oauthError("unsupported_grant_type", "Only authorization_code is supported");
        }

        OAuthClient client = oAuthClientRepository.findByClientId(clientId).orElse(null);
        if (client == null || !cryptoService.constantTimeEquals(client.getClientSecret(), clientSecret)) {
            return ResponseEntity.status(401).body(errorBody("invalid_client", "Client authentication failed"));
        }
        if (!client.allowsRedirectUri(redirectUri)) {
            return oauthError("invalid_grant", "redirect_uri does not match");
        }

        AuthorizationCode authCode = authorizationCodeRepository.findByCode(code).orElse(null);
        if (authCode == null || authCode.isUsed()) {
            return oauthError("invalid_grant", "Authorization code is invalid or already used");
        }
        if (authCode.getExpiresAt().isBefore(Instant.now())) {
            return oauthError("invalid_grant", "Authorization code has expired");
        }
        if (!authCode.getClientId().equals(clientId)) {
            return oauthError("invalid_grant", "Authorization code was issued to another client");
        }
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            return oauthError("invalid_grant", "redirect_uri does not match the authorization request");
        }

        // PKCE S256 verification.
        if (authCode.getCodeChallenge() != null) {
            if (codeVerifier == null || codeVerifier.isBlank()) {
                return oauthError("invalid_grant", "code_verifier is required");
            }
            String computed = cryptoService.generateCodeChallenge(codeVerifier);
            if (!cryptoService.constantTimeEquals(computed, authCode.getCodeChallenge())) {
                return oauthError("invalid_grant", "PKCE verification failed");
            }
        }

        User user = userService.findById(authCode.getUserId())
            .orElseThrow(() -> new IllegalStateException("User for authorization code no longer exists"));

        // Authorization codes are single-use.
        authCode.setUsed(true);
        authorizationCodeRepository.save(authCode);

        String scope = "openid profile email";
        try {
            String idToken = jwtService.signIdToken(user, clientId, null);
            String accessToken = jwtService.signAccessToken(user, clientId, scope);
            TokenResponse response = new TokenResponse(
                accessToken, "Bearer", (int) jwtService.getTtlSeconds(), idToken, scope);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign tokens", e);
        }
    }

    private static ResponseEntity<Map<String, Object>> oauthError(String error, String description) {
        return ResponseEntity.badRequest().body(errorBody(error, description));
    }

    private static Map<String, Object> errorBody(String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return body;
    }
}
