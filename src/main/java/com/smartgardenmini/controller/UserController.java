package com.smartgardenmini.controller;

import com.smartgardenmini.dto.DeviceDTO;
import com.smartgardenmini.dto.UserDTO;
import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.User;
import com.smartgardenmini.model.ApiResponse;
import com.smartgardenmini.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private UserDTO toUserDTO(User entity) {
        UserDTO dto = new UserDTO();
        dto.setUsername(entity.getUsername());
        dto.setName(entity.getName());
        dto.setSdt(entity.getSdt());
        dto.setRole(entity.getRole());
        return dto;
    }

    private DeviceDTO toDeviceDTO(Iot entity) {
        DeviceDTO dto = new DeviceDTO();
        dto.setMacId(entity.getMacId());
        dto.setName(entity.getName());
        dto.setOnline(entity.isOnline());
        dto.setAutoMode(entity.getWater());
        dto.setThreshold(entity.getDo_am());
        return dto;
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Không có quyền"));
        }
        List<UserDTO> dtos = users.stream().map(this::toUserDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.ok("Thành công", toUserDTO(user))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getMyDevices() {
        List<Iot> devices = userService.getMyDevices();
        List<DeviceDTO> dtos = devices.stream().map(this::toDeviceDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id, @RequestBody User newUser) {
        var result = userService.updateUser(id, newUser);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND || result.getStatusCode() == HttpStatus.FORBIDDEN) {
            return ResponseEntity.status(result.getStatusCode()).build();
        }
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật", toUserDTO(result.getBody())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        var result = userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    @GetMapping("/root/{key}")
    public ResponseEntity<ApiResponse<List<UserDTO>>> searchUser(@PathVariable String key) {
        List<User> users = userService.searchUser(key);
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Không có quyền"));
        }
        List<UserDTO> dtos = users.stream().map(this::toUserDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @GetMapping("/code")
    public ResponseEntity<ApiResponse<Integer>> generateAuthCode() {
        int code = userService.generateAuthCode();
        return ResponseEntity.ok(ApiResponse.ok("Thành công", code));
    }
}