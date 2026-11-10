package me.priyazsh.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.priyazsh.auth.dto.GoogleUserInfo;
import me.priyazsh.auth.entity.AuthorizationCode;
import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.repository.AuthorizationCodeRepository;
import me.priyazsh.auth.service.CryptoService;
import me.priyazsh.auth.service.OAuth2ClientService;
import me.priyazsh.auth.service.PendingAuth;
import me.priyazsh.auth.service.PendingAuthStore;
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
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class CallbackController {

    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final UserService userService;
    private final OAuth2ClientService oAuth2ClientService;
    private final PendingAuthStore pendingAuthStore;
    private final CryptoService cryptoService;

    @Value("${auth.frontend-url}")
    private String frontendUrl;

    public CallbackController(AuthorizationCodeRepository authorizationCodeRepository,
                              UserService userService,
                              OAuth2ClientService oAuth2ClientService,
                              PendingAuthStore pendingAuthStore,
                              CryptoService cryptoService) {
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.userService = userService;
        this.oAuth2ClientService = oAuth2ClientService;
        this.pendingAuthStore = pendingAuthStore;
        this.cryptoService = cryptoService;
    }

    /**
     * Google OAuth2 callback.
     *
     * <p>Validates {@code state}, exchanges the Google code (with PKCE), resolves
     * the local user, establishes an Auth Center Spring Session, and then either
     * redirects back to the requesting OAuth client with an authorization code,
     * or to the frontend dashboard for a direct login.</p>
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code,
                                         @RequestParam("state") String state,
                                         HttpServletRequest request) {

        PendingAuth pending = pendingAuthStore.consume(state);
        if (pending == null) {
            throw new IllegalArgumentException("Invalid or expired state");
        }

        Map<String, Object> tokenResponse =
            oAuth2ClientService.exchangeCodeForTokens(code, pending.getCodeVerifier());
        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Google did not return an access token");
        }

        GoogleUserInfo googleUser = oAuth2ClientService.fetchGoogleUserInfo(accessToken);
        if (googleUser.getEmail() == null || googleUser.getEmail().isBlank()) {
            throw new IllegalStateException("Google profile did not include an email");
        }

        User user = userService.findOrCreateByGoogleSub(
            googleUser.getId(),
            googleUser.getEmail(),
            googleUser.getName(),
            googleUser.getPicture());

        // Establish the Auth Center browser session (HttpOnly SESSION cookie).
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId().toString());

        // OAuth client flow: issue a one-time authorization code and bounce back.
        if (pending.getClientId() != null) {
            String authCode = cryptoService.generateState();
            AuthorizationCode stored = new AuthorizationCode(
                authCode,
                pending.getClientId(),
                user.getId(),
                pending.getRedirectUri(),
                pending.getCodeChallenge(),
                Instant.now().plusSeconds(300));
            authorizationCodeRepository.save(stored);

            String redirect = pending.getRedirectUri()
                + "?code=" + encode(authCode)
                + "&state=" + encode(pending.getClientState() == null ? "" : pending.getClientState());
            return ResponseEntity.status(302).location(URI.create(redirect)).build();
        }

        // Direct login flow: return to the frontend.
        String callback = pending.getCallbackUrl();
        String redirect;
        if (callback == null || callback.isBlank()) {
            redirect = frontendUrl + "/dashboard";
        } else if (callback.startsWith("http://") || callback.startsWith("https://")) {
            redirect = callback;
        } else {
            redirect = frontendUrl + (callback.startsWith("/") ? callback : "/" + callback);
        }
        return ResponseEntity.status(302).location(URI.create(redirect)).build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
