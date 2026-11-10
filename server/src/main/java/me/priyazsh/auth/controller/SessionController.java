package me.priyazsh.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

    private final UserService userService;

    public SessionController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the current cookie-backed session, or {@code {"session": null}}.
     * The frontend calls this to decide whether the user is signed in.
     */
    @GetMapping("/get-session")
    public ResponseEntity<Map<String, Object>> getSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String userId = session == null ? null : (String) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.ok(Map.of("session", java.util.Collections.emptyMap()));
        }

        User user = userService.findById(UUID.fromString(userId)).orElse(null);
        if (user == null) {
            session.invalidate();
            return ResponseEntity.ok(Map.of("session", java.util.Collections.emptyMap()));
        }

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("image", user.getImage());
        userMap.put("emailVerified", user.isEmailVerified());
        userMap.put("role", user.getRole());

        Map<String, Object> sessionMap = new LinkedHashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("userId", user.getId().toString());
        sessionMap.put("user", userMap);

        return ResponseEntity.ok(Map.of("session", sessionMap));
    }
}
