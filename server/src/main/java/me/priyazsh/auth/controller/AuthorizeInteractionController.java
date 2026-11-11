package me.priyazsh.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.service.AuthorizationCodeService;
import me.priyazsh.auth.service.CryptoService;
import me.priyazsh.auth.service.OAuth2ClientService;
import me.priyazsh.auth.service.PendingAuth;
import me.priyazsh.auth.service.PendingAuthStore;
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
import java.util.Map;
import java.util.UUID;

/**
 * The interactive part of the authorization flow, used by the Auth Center
 * frontend login page.
 *
 * <ul>
 *   <li>{@code GET /oauth2/authorize/request} — show which app is asking.</li>
 *   <li>{@code GET /oauth2/authorize/start} — user clicked “Continue with
 *       Google”; start the Google hop.</li>
 *   <li>{@code GET /oauth2/authorize/complete} — the browser already has a
 *       session; issue the code without Google.</li>
 * </ul>
 */
@RestController
@RequestMapping("/oauth2/authorize")
public class AuthorizeInteractionController {

    private final PendingAuthorizationStore pendingAuthorizationStore;
    private final PendingAuthStore pendingAuthStore;
    private final OAuth2ClientService oAuth2ClientService;
    private final CryptoService cryptoService;
    private final UserService userService;
    private final AuthorizationCodeService authorizationCodeService;

    @Value("${auth.frontend-url}")
    private String frontendUrl;

    public AuthorizeInteractionController(PendingAuthorizationStore pendingAuthorizationStore,
                                          PendingAuthStore pendingAuthStore,
                                          OAuth2ClientService oAuth2ClientService,
                                          CryptoService cryptoService,
                                          UserService userService,
                                          AuthorizationCodeService authorizationCodeService) {
        this.pendingAuthorizationStore = pendingAuthorizationStore;
        this.pendingAuthStore = pendingAuthStore;
        this.oAuth2ClientService = oAuth2ClientService;
        this.cryptoService = cryptoService;
        this.userService = userService;
        this.authorizationCodeService = authorizationCodeService;
    }

    /** Public metadata for the login page: which client is asking. */
    @GetMapping("/request")
    public ResponseEntity<?> request(@RequestParam("request_id") String requestId) {
        PendingAuthorization pending = pendingAuthorizationStore.get(requestId);
        if (pending == null) {
            return ResponseEntity.status(404).body(Map.of("error", "invalid_request"));
        }
        return ResponseEntity.ok(Map.of(
            "clientId", pending.getClientId(),
            "clientName", pending.getClientName() == null
                ? pending.getClientId()
                : pending.getClientName()));
    }

    /** User clicked the login button: start Google. */
    @GetMapping("/start")
    public ResponseEntity<Void> start(@RequestParam("request_id") String requestId) {
        PendingAuthorization pending = pendingAuthorizationStore.get(requestId);
        if (pending == null) {
            return redirect(frontendUrl + "/login?error=expired");
        }
        if (!oAuth2ClientService.isConfigured()) {
            return redirect(frontendUrl + "/login?error=google_not_configured");
        }

        PendingAuthorization consumed = pendingAuthorizationStore.consume(requestId);
        if (consumed == null) {
            return redirect(frontendUrl + "/login?error=expired");
        }

        String state = cryptoService.generateState();
        String nonce = (consumed.getNonce() != null && !consumed.getNonce().isBlank())
            ? consumed.getNonce()
            : cryptoService.generateNonce();
        String verifier = cryptoService.generateCodeVerifier();
        String challenge = cryptoService.generateCodeChallenge(verifier);

        pendingAuthStore.put(new PendingAuth(
            state,
            consumed.getClientId(),
            consumed.getRedirectUri(),
            consumed.getClientState(),
            consumed.getCodeChallenge(),
            nonce,
            verifier,
            null));

        String googleUrl = oAuth2ClientService.buildGoogleAuthorizeUrl(state, nonce, challenge);
        return redirect(googleUrl);
    }

    /** The browser already has a session: finish without Google. */
    @GetMapping("/complete")
    public ResponseEntity<Void> complete(@RequestParam("request_id") String requestId,
                                         HttpServletRequest request) {
        PendingAuthorization pending = pendingAuthorizationStore.get(requestId);
        if (pending == null) {
            return redirect(frontendUrl + "/login?error=expired");
        }

        HttpSession session = request.getSession(false);
        String userId = session == null ? null : (String) session.getAttribute("userId");
        User user = userId == null ? null : findUser(userId);
        if (user == null) {
            if (session != null) {
                session.invalidate();
            }
            return redirect(frontendUrl + "/login?request_id=" + encode(requestId));
        }

        PendingAuthorization consumed = pendingAuthorizationStore.consume(requestId);
        if (consumed == null) {
            return redirect(frontendUrl + "/login?error=expired");
        }

        String clientRedirect = authorizationCodeService.issueAndBuildRedirect(
            consumed.getClientId(),
            user.getId(),
            consumed.getRedirectUri(),
            consumed.getCodeChallenge(),
            consumed.getClientState());
        return redirect(clientRedirect);
    }

    private User findUser(String userId) {
        try {
            return userService.findById(UUID.fromString(userId)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
