package me.priyazsh.auth.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import me.priyazsh.auth.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class OAuth2ClientService {

    private static final String GOOGLE_AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${google.redirect-uri}")
    private String googleRedirectUri;

    public OAuth2ClientService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return googleClientId != null && !googleClientId.isBlank()
            && googleClientSecret != null && !googleClientSecret.isBlank();
    }

    public String buildGoogleAuthorizeUrl(String state, String nonce, String codeChallenge) {
        return GOOGLE_AUTHORIZE_ENDPOINT
            + "?response_type=code"
            + "&client_id=" + encode(googleClientId)
            + "&redirect_uri=" + encode(googleRedirectUri)
            + "&scope=" + encode("openid profile email")
            + "&state=" + encode(state)
            + "&nonce=" + encode(nonce)
            + "&prompt=select_account"
            + "&code_challenge_method=S256"
            + "&code_challenge=" + encode(codeChallenge);
    }

    /** Exchange a Google authorization code (with PKCE verifier) for tokens. */
    public Map<String, Object> exchangeCodeForTokens(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", googleRedirectUri);
        form.add("grant_type", "authorization_code");
        form.add("code_verifier", codeVerifier);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> response =
            restTemplate.postForEntity(GOOGLE_TOKEN_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Google token exchange failed: " + response.getStatusCode());
        }
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            Map<String, Object> result = new HashMap<>();
            node.properties().forEach(e -> result.put(e.getKey(), e.getValue().asText()));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Google token response", e);
        }
    }

    /** Fetch the Google profile for an access token. */
    public GoogleUserInfo fetchGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            GOOGLE_USERINFO_ENDPOINT, HttpMethod.GET, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Google userinfo request failed: " + response.getStatusCode());
        }
        return parseGoogleUserInfo(response.getBody());
    }

    private GoogleUserInfo parseGoogleUserInfo(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            GoogleUserInfo info = new GoogleUserInfo();
            info.setId(node.path("id").asText());
            info.setEmail(node.path("email").asText());
            info.setVerifiedEmail(node.path("verified_email").asBoolean(false));
            info.setName(node.path("name").asText());
            info.setPicture(node.path("picture").asText());
            info.setGivenName(node.path("given_name").asText());
            info.setFamilyName(node.path("family_name").asText());
            return info;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Google user info", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String getGoogleClientId() { return googleClientId; }
    public String getGoogleRedirectUri() { return googleRedirectUri; }
}
