/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.controller;

import com.andeptrai.iot.jwtS.JwtUtil;
import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.model.User;
import com.andeptrai.iot.service.user_service;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *
 * @author an
 */
@RestController
@RequestMapping("/auth")
public class Auth {
    @Autowired
    private user_service user_ser;

    @Autowired
    private JwtUtil jwtUtil;
    
    //Dang nhap
    @PostMapping("/login")
    public ResponseEntity<String> DangNhap(@RequestBody User loginUser) {
        Optional<User> userOptional = user_ser.get_user_byUName(loginUser.getUsername());
        try{
        System.out.println(userOptional.get().getUsername());}
        catch(Exception e){e.printStackTrace();}
        if (userOptional.isPresent()) {
            User dbUser = userOptional.get();
            if (dbUser.getPassword().equals(loginUser.getPassword())) {
                try{
                String token = jwtUtil.generateToken(dbUser.getUsername(), Integer.toString(dbUser.getRole()));
                return ResponseEntity.ok(token);}
                catch(Exception e){ResponseEntity.status(401).body(e);}
                
            }
        }
        return ResponseEntity.status(401).body("Lỗi");
    }

    //dang ky
    @PostMapping("/register")
    public ResponseEntity<String> DangKy(@RequestBody User newUser) {
        System.out.println("newUser = " + newUser);
        try{
        if (user_ser.get_user_byUName(newUser.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Tên người dùng đã tồn tại");
        }}
        catch(Exception e){System.out.println(e);}
        try{
        user_ser.taomoi(newUser);}
        catch(Exception e){System.out.println(e);}
        return ResponseEntity.ok("Đăng ký thành công");
    }
    @PostMapping("/dangkythietbi")
    public ResponseEntity<String> themthietbi(@RequestParam("authCode") int code,@RequestBody Iot newiot){
        return user_ser.themthietbi(code,newiot);
        }
}

