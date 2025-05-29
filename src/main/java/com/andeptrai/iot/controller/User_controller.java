/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.controller;

import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.model.User;
import com.andeptrai.iot.service.iot_service;
import com.andeptrai.iot.service.user_service;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author an
 */
@RestController
@RequestMapping("/users")
public class User_controller {
    
    @Autowired
    private user_service userService;

    @Autowired
    private iot_service iotService;

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userService.getall_users();
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> u = userService.get_user_byID(id);
        return u.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices")
    public ResponseEntity<List<Iot>> getMyDevices() {
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    List<Iot> list = userService.getIotByUsername(currentUsername);
    return list.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(list);
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
    public List<User> timUser(@PathVariable String key){
        return userService.timUserTheoTen(key);
    }
   @GetMapping("/code")
   public long layCode() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userService.layCode(username);
}
    
    }
  
