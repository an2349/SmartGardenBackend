package com.smartgardenmini.controller;

import com.smartgardenmini.dto.LoginRequest;
import com.smartgardenmini.dto.RegisterRequest;
import com.smartgardenmini.jwt.JwtUtil;
import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.User;
import com.smartgardenmini.model.ApiResponse;
import com.smartgardenmini.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController//doan nay danh tag bao hieu file nay la controller
@RequestMapping("/auth") //tag cho url
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")//http post
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User loginUser = new User();
        loginUser.setUsername(loginRequest.getUsername());
        loginUser.setPassword(loginRequest.getPassword());

        ResponseEntity<?> result = userService.login(loginUser);
        if (result.getStatusCode().is2xxSuccessful()) {
            User user = (User) result.getBody();
            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), String.valueOf(user.getRole()));
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), String.valueOf(user.getRole()));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng nhập thành công",
                    "data", Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshToken,
                        "username", user.getUsername(),
                        "role", user.getRole(),
                        "name", user.getName()
                    )
            ));
        }
        return result;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(registerRequest.getPassword());
        newUser.setName(registerRequest.getName());
        newUser.setSdt(registerRequest.getSdt());
        newUser.setRole(1); // Mặc định là user thường
        
        var result = userService.register(newUser);
        if (result.getStatusCode().is4xxClientError()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.getBody()));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("accessToken", newAccessToken)
            ));
        } catch (Exception e) {
            log.warn("Refresh token thất bại", e);
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token không hợp lệ"));
        }
    }

    @PostMapping("/dangkythietbi")
    //@GetMapping("/dangkythietbi")//http get
    public ResponseEntity<String> registerDevice(@RequestParam("authCode") int code,
                                                  @RequestBody Iot newIot) { //@requet param va boby
                                                                                // la du lieu lay tu param va body
        log.info("=== DANG KY THIET BI ===");
        log.info("authCode={}, macId={}, username={}, name={}", code, newIot.getMacId(), newIot.getUsername(), newIot.getName());
        log.info("water={}, do_am={}", newIot.getWater(), newIot.getDo_am());
        ResponseEntity<String> result = userService.registerDevice(code, newIot);
        log.info("Ket qua dang ky: status={}, body={}", result.getStatusCode(), result.getBody());
        return result;
    }
}