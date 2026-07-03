package com.smartgardenmini.service;

import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.User;
import com.smartgardenmini.repository.IotRepository;
import com.smartgardenmini.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepo;
    private final IotRepository iotRepo;
    private final DeviceService deviceService;
    private final PasswordEncoder passwordEncoder;

    private final Map<Integer, CodeInfo> codeMap = new ConcurrentHashMap<>();
    private static final long CODE_TIMEOUT = 3 * 60 * 1000; // 3 phút

    public UserService(UserRepository userRepo, IotRepository iotRepo,
                       DeviceService deviceService, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.iotRepo = iotRepo;
        this.deviceService = deviceService;
        this.passwordEncoder = passwordEncoder;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Optional<User> getCurrentUser() {
        return userRepo.findByUsername(getCurrentUsername());
    }

    private boolean isAdmin() {
        return getCurrentUser().map(u -> u.getRole() == 0).orElse(false);
    }

    // ==================== Auth ====================

    public ResponseEntity<String> register(User newUser) {
        if (newUser.getUsername() == null || newUser.getPassword() == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin username/password");
        }
        if (userRepo.existsByUsername(newUser.getUsername())) {
            return ResponseEntity.badRequest().body("Tên người dùng đã tồn tại");
        }
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        userRepo.save(newUser);
        log.info("User mới đăng ký: {}", newUser.getUsername());
        return ResponseEntity.ok("Đăng ký thành công");
    }

    public ResponseEntity<?> login(User loginUser) {
        Optional<User> userOpt = userRepo.findByUsername(loginUser.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Sai tên đăng nhập hoặc mật khẩu");
        }
        User dbUser = userOpt.get();
        if (!passwordEncoder.matches(loginUser.getPassword(), dbUser.getPassword())) {
            return ResponseEntity.status(401).body("Sai tên đăng nhập hoặc mật khẩu");
        }
        return ResponseEntity.ok(dbUser);
    }

    public String getRole(String username) {
        return userRepo.findByUsername(username)
                .map(u -> String.valueOf(u.getRole()))
                .orElse("1");
    }

    // ==================== User CRUD ====================

    public List<User> getAllUsers() {
        if (isAdmin()) {
            return userRepo.findAll();
        }
        return List.of();
    }

    public Optional<User> getUserById(Long id) {
        Optional<User> user = userRepo.findById(id);
        if (user.isPresent() && (isAdmin() || user.get().getUsername().equals(getCurrentUsername()))) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> getMyProfile() {
        return getCurrentUser();
    }

    public ResponseEntity<User> updateMyProfile(User newUser) {
        Optional<User> current = getCurrentUser();
        if (current.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User updated = current.get();
        if (newUser.getName() != null) updated.setName(newUser.getName());
        if (newUser.getSdt() != null) updated.setSdt(newUser.getSdt());
        if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
            updated.setPassword(passwordEncoder.encode(newUser.getPassword()));
        }
        return ResponseEntity.ok(userRepo.save(updated));
    }

    public ResponseEntity<String> deleteMyAccount() {
        Optional<User> current = getCurrentUser();
        if (current.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userRepo.deleteById(current.get().getId());
        return ResponseEntity.ok("Đã xoá tài khoản");
    }

    public List<Iot> getMyDevices() {
        return iotRepo.findIotByUsername(getCurrentUsername());
    }

    public ResponseEntity<User> updateUser(Long id, User newUser) {
        Optional<User> current = getCurrentUser();
        Optional<User> target = userRepo.findById(id);
        if (target.isEmpty() || current.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin() && !target.get().getUsername().equals(getCurrentUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User updated = target.get();
        if (newUser.getName() != null) updated.setName(newUser.getName());
        if (newUser.getSdt() != null) updated.setSdt(newUser.getSdt());
        if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
            updated.setPassword(passwordEncoder.encode(newUser.getPassword()));
        }
        if (isAdmin()) {
            if (newUser.getRole() != 0) updated.setRole(newUser.getRole());
            if (newUser.getUsername() != null) updated.setUsername(newUser.getUsername());
        }
        return ResponseEntity.ok(userRepo.save(updated));
    }

    public ResponseEntity<String> deleteUser(Long id) {
        Optional<User> current = getCurrentUser();
        Optional<User> target = userRepo.findById(id);
        if (target.isEmpty() || current.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin() && !target.get().getUsername().equals(getCurrentUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userRepo.deleteById(id);
        return ResponseEntity.ok("Đã xoá user");
    }

    public List<User> searchUser(String keyword) {
        if (isAdmin()) {
            return userRepo.findByNameContainingIgnoreCase(keyword);
        }
        return List.of();
    }

    // ==================== Auth Code ====================

    public int generateAuthCode() {
        String username = getCurrentUsername();
        int code = new Random().nextInt(999999); // 6 so
        long expiry = System.currentTimeMillis() + CODE_TIMEOUT;
        codeMap.put(code, new CodeInfo(username, expiry));
        log.info("Sinh authCode: code={}, username={}, expiry={}", code, username, expiry);
        return code;
    }

    public boolean verifyAuthCode(String macId, int code, String username) {
        log.info("=== verifyAuthCode ===");
        log.info("Input: macId={}, code={}, username={}", macId, code, username);

        if (iotRepo.existsBymacId(macId)) {
            log.warn("Thiet bi da ton tai trong DB: macId={}", macId);
            return false;
        }
        CodeInfo info = codeMap.get(code);
        if (info == null) {
            log.warn("Khong tim thay authCode={} trong codeMap. codeMap size={}", code, codeMap.size());
            // Log cac ma dang co
            for (Map.Entry<Integer, CodeInfo> e : codeMap.entrySet()) {
                log.debug("  code={}, username={}, expiry={}", e.getKey(), e.getValue().username, e.getValue().expiry);
            }
            return false;
        }
        if (System.currentTimeMillis() > info.expiry) {
            log.warn("AuthCode da het han: code={}, expiry={}, now={}", code, info.expiry, System.currentTimeMillis());
            codeMap.remove(code);
            return false;
        }
        boolean match = info.username.equals(username);
        if (!match) {
            log.warn("Username khong khop: expected={}, actual={}", info.username, username);
        } else {
            log.info("Xac thuc authCode thanh cong: macId={}, code={}, username={}", macId, code, username);
        }
        return match;
    }

    public ResponseEntity<String> registerDevice(int code, Iot newIot) {
        log.info("=== registerDevice ===");
        log.info("code={}, macId={}, username={}, name={}, water={}, do_am={}",
                code, newIot.getMacId(), newIot.getUsername(), newIot.getName(),
                newIot.getWater(), newIot.getDo_am());

        if (verifyAuthCode(newIot.getMacId(), code, newIot.getUsername())) {
            codeMap.remove(code);
            log.info("AuthCode valid, removing from map");
            // Luu truc tiep thiet bi voi username da duoc xac thuc qua authCode,
            // khong qua deviceService.addDevice() de tranh bi ghi de username boi anonymousUser
            if (iotRepo.existsBymacId(newIot.getMacId())) {
                log.warn("Thiet bi da ton tai truoc khi luu: macId={}", newIot.getMacId());
                return ResponseEntity.badRequest().body("Thiet bi da ton tai!");
            }
            iotRepo.save(newIot);
            log.info("Da luu thiet bi thanh cong: macId={}", newIot.getMacId());
            return ResponseEntity.ok("Da them thiet bi thanh cong");
        }
        log.warn("Dang ky that bai: authCode khong hop le");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ma xac thuc khong hop le hoac da het han");
    }

    private static class CodeInfo {
        String username;
        long expiry;
        CodeInfo(String username, long expiry) {
            this.username = username;
            this.expiry = expiry;
        }
    }
}