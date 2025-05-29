/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.service;





import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.model.User;
import com.andeptrai.iot.repository.iot_repo;
import com.andeptrai.iot.repository.user_repo;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author an
 */
@Service
public class user_service {
    @Autowired
    private user_repo userRepo;

    @Autowired
    private iot_repo iotRepo;
    
    @Autowired
    private iot_service iotSer;
    
    private final Map<Integer, codein4> CodeMap = new HashMap<>();
    private final long CODE_TIMEOUT = 3 * 60 * 1000;

    public List<User> getall_users() {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        if (user.isPresent() && user.get().getRole()==0/*0 la admin*/) {
            return userRepo.findAll();
        }
        return List.of();
    }

    public Optional<User> get_user_byID(Long id) {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findById(id);
        Optional<User> current = userRepo.findByUsername(username);
        if (user.isPresent() && current.isPresent()) {
            if (current.get().getRole()==0|| user.get().getUsername().equals(username)) {
                return user;
            }
        }
        return Optional.empty();
    }

    public List<Iot> getIotByUsername(String Username) {
        String currentUsername = getCurrentUsername();
        Optional<User> currentUser = userRepo.findByUsername(currentUsername);
        if (currentUser.isPresent()) {
            if (currentUser.get().getRole()==0 || currentUsername.equals(Username)) {
                return iotRepo.findIotByUsername(Username);
            }
        }
        return List.of();
    }

    public ResponseEntity<User> updateUser(Long id, User newUser) {
        String username = getCurrentUsername();
        Optional<User> current = userRepo.findByUsername(username);
        Optional<User> u = userRepo.findById(id);
        if (u.isPresent() && current.isPresent()) {
            if (current.get().getRole()==0 || u.get().getUsername().equals(username)&& u.get().getId()== id) {
                User updated = u.get();
                updated.setUsername(newUser.getUsername());
                updated.setPassword(newUser.getPassword());
                updated.setName(newUser.getName());
                updated.setRole(newUser.getRole());
                updated.setSdt(newUser.getSdt());
                this.taomoi(updated);
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> deleteUser(Long id) {
        String username = getCurrentUsername();
        Optional<User> current = userRepo.findByUsername(username);
        Optional<User> u = userRepo.findById(id);
        if (u.isPresent() && current.isPresent()) {
            if (current.get().getRole()==0 || u.get().getUsername().equals(username)&& u.get().getId()== id){
                userRepo.deleteById(id);
                return ResponseEntity.ok("Đã xoá user");
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy user");
    }


    public User taomoi(User u) {
        return userRepo.save(u);
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
 
    public List<User> timUserTheoTen(String keyword) {
        String currentUsername = getCurrentUsername();
        Optional<User> current = userRepo.findByUsername(currentUsername);
        if (current.isPresent() && current.get().getRole()==0) {
            return userRepo.findByNameContainingIgnoreCase(keyword);
        }
        return List.of();
    }
    
    public Optional<User> get_user_byUName(String username) {
        return userRepo.findByUsername(username);
    }
    
    public int layCode(String username){
        try{
        String currentUsername = getCurrentUsername();
        Optional<User> current = userRepo.findByUsername(currentUsername);
        if(current.isPresent()){
            int code = new Random().nextInt(1000);
            long timehethan = System.currentTimeMillis() + CODE_TIMEOUT;
            CodeMap.put(code,new codein4(username,timehethan));
                    return code;
        }else return 0;
        }catch(Exception e){e.printStackTrace();return 0;}
        
    }
    public boolean xacthucCode(String Mac,int code,String username){
        Optional<Iot> device = iotRepo.findBymacId(Mac);
        if(device.isEmpty()){
            codein4 in4 = CodeMap.get(code);
            if( in4 != null ){
                if (System.currentTimeMillis() <= in4.hethan) {
                return in4.username.equals(username);}
                else CodeMap.remove(code);
            }
        }
        return false;
    }
    public ResponseEntity<String> themthietbi(int code ,Iot newiot){
        try{
        if(xacthucCode(newiot.getMacId(), code, newiot.getUsername())){
            return iotSer.addDevice(newiot);
        }}catch(Exception e){System.out.println("loi o user service");;e.printStackTrace();}
     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();}
    
    /*public String themthietbi(int code,Iot newiot){
        if(xacthucCode(newiot.getMac(), code, newiot.getUsername())){
             try{iotRepo.save(newiot); return "ok";
        }catch(Exception e){e.printStackTrace();return " ";}
    }
        return "try ko chay";
    
}*/
}  
class codein4{
    String username;
    long hethan;
    public codein4(String username, long hethan) {
        this.username = username;
        this.hethan = hethan;
    }
    
}