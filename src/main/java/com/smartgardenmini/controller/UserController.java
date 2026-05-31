package com.smartgardenmini.controller;

import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.User;
import com.smartgardenmini.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return users.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices")
    public ResponseEntity<List<Iot>> getMyDevices() {
        List<Iot> devices = userService.getMyDevices();
        return devices.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(devices);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User newUser) {
        return userService.updateUser(id, newUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }

    @GetMapping("/root/{key}")
    public ResponseEntity<List<User>> searchUser(@PathVariable String key) {
        List<User> users = userService.searchUser(key);
        return users.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/code")
    public ResponseEntity<Integer> generateAuthCode() {
        int code = userService.generateAuthCode();
        return ResponseEntity.ok(code);
    }
}