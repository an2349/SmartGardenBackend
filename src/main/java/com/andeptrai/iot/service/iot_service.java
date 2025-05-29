/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.service;



import com.andeptrai.iot.model.Lichsu;
import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.model.User;
import com.andeptrai.iot.model.dulieu;
import com.andeptrai.iot.repository.Data_repo;
import com.andeptrai.iot.repository.Lichsu_repo;
import com.andeptrai.iot.repository.iot_repo;
import com.andeptrai.iot.repository.user_repo;
import com.andeptrai.iot.websocket.WebHander;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


/**
 *
 * @author an
 */

@Service
public class iot_service {

    @Autowired
    private iot_repo iotRepo;

    @Autowired
    private user_repo userRepo;

    @Autowired
    private Data_repo dataRepo;

    @Autowired
    private Lichsu_repo lichsuRepo;

    @Autowired
    private WebHander webHander;

    public ResponseEntity<List<Iot>> getAllDevices() {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        return user.map(u -> u.getRole()==0
                ? ResponseEntity.ok(iotRepo.findAll())
                : ResponseEntity.ok(iotRepo.findIotByUsername(username)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public ResponseEntity<Iot> getIotById(Long id) {
        String username = getCurrentUsername();
        Optional<User> u = userRepo.findByUsername(username);
        Optional<Iot> i = iotRepo.findById(id);
        if (u.isPresent() && i.isPresent()) {
            if (u.get().getRole()==0 || i.get().getUsername().equals(username)) {
                return ResponseEntity.ok(i.get());
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Iot> updateIot(Iot newIot) {
        String username = getCurrentUsername();
        Optional<User> u = userRepo.findByUsername(username);
        return (ResponseEntity<Iot>) iotRepo.findBymacId(newIot.getMacId()).map(iot -> {
            if (u.isPresent() && (u.get().getRole()==0 || iot.getUsername().equals(username))) {
                iot.setName(newIot.getName());
                iot.setMacId(newIot.getMacId());
                iot.setWater(newIot.getWater());
                iot.setUsername(newIot.getUsername());
                return ResponseEntity.ok(iotRepo.save(iot));
            } else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<String> deleteIot(String id) {
        String username = getCurrentUsername();
        Optional<User> u = userRepo.findByUsername(username);
        Optional<Iot> i = iotRepo.findBymacId(id);
        if (i.isPresent() && u.isPresent()) {
            if (u.get().getRole()==0 || i.get().getUsername().equals(username)) {
                iotRepo.deleteBymacId(id);
                return ResponseEntity.ok("Đã xoá thiết bị");
            } else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<dulieu>> getDataId(String macId) {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device != null && user.isPresent()) {
            if (user.get().getRole()==0 || device.get().getUsername().equals(username)) {
                return ResponseEntity.ok(dataRepo.findByMac(macId));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<Lichsu>> Lichsutuoi(String macId) {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device != null && user.isPresent()) {
            if (user.get().getRole()==0 || device.get().getUsername().equals(username)) {
                return ResponseEntity.ok(lichsuRepo.findByMac(macId));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> Command(String macId, String command) {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device != null && user.isPresent()) {
            if (user.get().getRole()==0/*0 la admin*/ || device.get().getUsername().equals(username)) {
                if(webHander.sendCommandToDevice(macId, command)){
                return ResponseEntity.ok(macId);}
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> addDevice(Iot newDevice) {
        //String username = getCurrentUsername();
        if(iotRepo.existsBymacId(newDevice.getMacId())){
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("nó đã tồn tại");}
        else
            try{
        //newDevice.setUsername(username);
        iotRepo.save(newDevice);}
        catch(Exception e){System.out.println("loi o iot servie");;e.printStackTrace();}
        return ResponseEntity.ok("Đã thêm thiết bị thành công");
    }
    
    public float xemDoam(String macId) throws Exception{
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        if(user.isEmpty()){throw new Exception("Thiết bị vô chủ");}
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if(device.get().getUsername().equals(user.get().getUsername()) || user.get().getRole() == 0 /*0 la admin*/){
            return webHander.xemDoam(macId);
        }
        else  throw new Exception("Không đủ quền");
        
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    
    
}

