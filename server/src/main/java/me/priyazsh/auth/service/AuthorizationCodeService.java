package me.priyazsh.auth.service;

import me.priyazsh.auth.entity.AuthorizationCode;
import me.priyazsh.auth.repository.AuthorizationCodeRepository;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues one-time authorization codes and builds the redirect back to the
 * requesting OAuth client.
 */
@Service
public class AuthorizationCodeService {

    private final AuthorizationCodeRepository repository;
    private final CryptoService cryptoService;

    public AuthorizationCodeService(AuthorizationCodeRepository repository,
                                    CryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    /**
     * Creates a 5-minute, single-use code and returns the client redirect URL
     * with {@code code} and {@code state}.
     */
    public String issueAndBuildRedirect(String clientId,
                                        UUID userId,
                                        String redirectUri,
                                        String codeChallenge,
                                        String clientState) {
        String code = cryptoService.generateState();
        repository.save(new AuthorizationCode(
            code,
            clientId,
            userId,
            redirectUri,
            codeChallenge,
            Instant.now().plusSeconds(300)));

        String separator = redirectUri.contains("?") ? "&" : "?";
        return redirectUri + separator
            + "code=" + encode(code)
            + "&state=" + encode(clientState == null ? "" : clientState);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
