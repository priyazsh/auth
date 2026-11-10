package me.priyazsh.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import me.priyazsh.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.ttl-seconds:3600}")
    private long ttlSeconds;

    private ECKey ecKey;
    private ECPublicKey ecPublicKey;

    @PostConstruct
    public void init() throws Exception {
        Path privatePath = toPath(privateKeyPath);
        Path publicPath = toPath(publicKeyPath);

        if (!Files.exists(privatePath) || !Files.exists(publicPath)) {
            throw new IllegalStateException(
                "ES256 key files not found. Private: " + privatePath + ", Public: " + publicPath);
        }

        String privatePem = Files.readString(privatePath, StandardCharsets.US_ASCII);
        String publicPem = Files.readString(publicPath, StandardCharsets.US_ASCII);

        PrivateKey privateKey = parsePrivateKey(privatePem);
        PublicKey publicKey = parsePublicKey(publicPem);

        BigInteger x = ((ECPublicKey) publicKey).getW().getAffineX();
        BigInteger y = ((ECPublicKey) publicKey).getW().getAffineY();

        ECKey base = new ECKey.Builder(Curve.P_256,
                Base64URL.encode(toFixedLength(x)),
                Base64URL.encode(toFixedLength(y)))
            .privateKey(privateKey)
            .build();

        // Derive a stable key id (RFC 7638 JWK thumbprint).
        this.ecKey = new ECKey.Builder(base)
            .keyID(base.computeThumbprint().toString())
            .build();
        this.ecPublicKey = (ECPublicKey) publicKey;
    }

    private static Path toPath(String location) {
        String value = location;
        if (value.startsWith("file:")) {
            value = value.substring("file:".length());
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String body = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static PublicKey parsePublicKey(String pem) throws Exception {
        String body = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
    }

    /** P-256 coordinates must be exactly 32 bytes; strip sign padding. */
    private static byte[] toFixedLength(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == 32) {
            return bytes;
        }
        byte[] fixed = new byte[32];
        if (bytes.length > 32) {
            System.arraycopy(bytes, bytes.length - 32, fixed, 0, 32);
        } else {
            System.arraycopy(bytes, 0, fixed, 32 - bytes.length, bytes.length);
        }
        return fixed;
    }

    /** OIDC ID token for an OAuth client (audience = client_id). */
    public String signIdToken(User user, String clientId, String nonce) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(user.getId().toString())
            .audience(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
            .claim("email", user.getEmail())
            .claim("email_verified", user.isEmailVerified())
            .claim("name", user.getName())
            .claim("picture", user.getImage())
            .claim("preferred_username", user.getEmail())
            .claim("role", user.getRole());
        if (nonce != null) {
            builder.claim("nonce", nonce);
        }
        return sign(builder.build());
    }

    /** OAuth2 access token (audience = client_id, carries scope). */
    public String signAccessToken(User user, String clientId, String scope) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(user.getId().toString())
            .audience(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
            .claim("token_use", "access")
            .claim("scope", scope)
            .claim("email", user.getEmail())
            .build();
        return sign(claims);
    }

    private String sign(JWTClaimsSet claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(ecKey.getKeyID())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .build(),
            claims);
        jwt.sign(new ECDSASigner(ecKey));
        return jwt.serialize();
    }

    /**
     * Verify signature, issuer and expiry. Returns the claims on success,
     * or {@code null} when the token is invalid.
     */
    public JWTClaimsSet verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new ECDSAVerifier(ecPublicKey))) {
                return null;
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!issuer.equals(claims.getIssuer())) {
                return null;
            }
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    public ECKey getEcKey() { return ecKey; }
    public ECPublicKey getEcPublicKey() { return ecPublicKey; }
    public long getTtlSeconds() { return ttlSeconds; }
    public String getIssuer() { return issuer; }
}
