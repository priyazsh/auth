package me.priyazsh.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.priyazsh.auth.entity.OAuthClient;
import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.repository.OAuthClientRepository;
import me.priyazsh.auth.service.AuthorizationCodeService;
import me.priyazsh.auth.service.CryptoService;
import me.priyazsh.auth.service.OAuth2ClientService;
import me.priyazsh.auth.service.PendingAuthorization;
import me.priyazsh.auth.service.PendingAuthorizationStore;
import me.priyazsh.auth.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/oauth2")
public class AuthorizeController {

    private final OAuthClientRepository oAuthClientRepository;
    private final CryptoService cryptoService;
    private final OAuth2ClientService oAuth2ClientService;
    private final PendingAuthorizationStore pendingAuthorizationStore;
    private final UserService userService;
    private final AuthorizationCodeService authorizationCodeService;

    @Value("${auth.frontend-url}")
    private String frontendUrl;

    public AuthorizeController(OAuthClientRepository oAuthClientRepository,
                               CryptoService cryptoService,
                               OAuth2ClientService oAuth2ClientService,
                               PendingAuthorizationStore pendingAuthorizationStore,
                               UserService userService,
                               AuthorizationCodeService authorizationCodeService) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.cryptoService = cryptoService;
        this.oAuth2ClientService = oAuth2ClientService;
        this.pendingAuthorizationStore = pendingAuthorizationStore;
        this.userService = userService;
        this.authorizationCodeService = authorizationCodeService;
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
     * <p>If the browser already has an Auth Center session, a code is issued
     * immediately and the user is sent straight back to the client (no Google).
     * Otherwise the request is parked and the browser is sent to the Auth Center
     * login page. Google is only started after the user clicks the login button
     * (see {@link AuthorizeInteractionController}).</p>
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
            @RequestParam(value = "code_challenge_method", defaultValue = "S256") String codeChallengeMethod,
            HttpServletRequest request) {

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

        // Existing Auth Center session -> silent SSO, no Google round-trip.
        HttpSession session = request.getSession(false);
        String userId = session == null ? null : (String) session.getAttribute("userId");
        if (userId != null) {
            User user = findUser(userId);
            if (user != null) {
                String redirect = authorizationCodeService.issueAndBuildRedirect(
                    clientId, user.getId(), redirectUri, codeChallenge, state);
                return ResponseEntity.status(302).location(URI.create(redirect)).build();
            }
            session.invalidate();
        }

        if (!oAuth2ClientService.isConfigured()) {
            throw new IllegalStateException("Google OAuth2 client is not configured");
        }

        // No session: park the validated request and show the login page.
        String requestId = cryptoService.generateState();
        pendingAuthorizationStore.put(new PendingAuthorization(
            requestId,
            clientId,
            client.getName(),
            redirectUri,
            state,
            codeChallenge,
            scope,
            nonce));

        String loginUrl = frontendUrl + "/login?request_id=" + encode(requestId);
        return ResponseEntity.status(302).location(URI.create(loginUrl)).build();
    }

    private User findUser(String userId) {
        try {
            return userService.findById(UUID.fromString(userId)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
