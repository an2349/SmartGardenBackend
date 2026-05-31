package com.smartgardenmini.controller;

import com.smartgardenmini.jwt.JwtUtil;
import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.User;
import com.smartgardenmini.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginUser) {
        ResponseEntity<?> result = userService.login(loginUser);
        if (result.getStatusCode().is2xxSuccessful()) {
            User user = (User) result.getBody();
            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), String.valueOf(user.getRole()));
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "username", user.getUsername(),
                    "role", user.getRole(),
                    "name", user.getName()
            ));
        }
        return result;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody User newUser) {
        return userService.register(newUser);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            log.warn("Refresh token thất bại", e);
            return ResponseEntity.status(401).body("Token không hợp lệ");
        }
    }

    @PostMapping("/dangkythietbi")
    public ResponseEntity<String> registerDevice(@RequestParam("authCode") int code,
                                                  @RequestBody Iot newIot) {
        return userService.registerDevice(code, newIot);
    }
}