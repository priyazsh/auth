package me.priyazsh.auth.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import me.priyazsh.auth.dto.UserInfo;
import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.service.JwtService;
import me.priyazsh.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/oauth2")
public class UserInfoController {

    private final JwtService jwtService;
    private final UserService userService;

    public UserInfoController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * OIDC UserInfo endpoint. Requires {@code Authorization: Bearer <access token>}.
     */
    @GetMapping("/userinfo")
    public ResponseEntity<?> userinfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                .header("WWW-Authenticate", "Bearer")
                .body(java.util.Map.of("error", "invalid_token"));
        }

        String token = authorization.substring("Bearer ".length()).trim();
        JWTClaimsSet claims = jwtService.verify(token);
        if (claims == null || !"access".equals(claims.getClaim("token_use"))) {
            return ResponseEntity.status(401)
                .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                .body(java.util.Map.of("error", "invalid_token"));
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                .body(java.util.Map.of("error", "invalid_token"));
        }

        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401)
                .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                .body(java.util.Map.of("error", "invalid_token"));
        }

        UserInfo info = new UserInfo(
            user.getId().toString(),
            user.getEmail(),
            user.isEmailVerified(),
            user.getName(),
            user.getImage(),
            user.getEmail(),
            user.getRole());

        return ResponseEntity.ok(info);
    }
}
