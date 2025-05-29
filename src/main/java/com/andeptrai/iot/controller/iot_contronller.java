/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.controller;

import com.andeptrai.iot.model.dulieu;
import com.andeptrai.iot.model.Lichsu;
import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.service.iot_service;
import com.andeptrai.iot.websocket.WebHander;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 *
 * @author an
 */
@RestController
@RequestMapping("/iot")
public class iot_contronller {
    
   @Autowired
    private iot_service iotService;

    @GetMapping("/devices")
    public ResponseEntity<List<Iot>> getAllDevices() {
        return iotService.getAllDevices();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Iot> getIotById(@PathVariable Long id) {
        return iotService.getIotById(id);
    }

    @PostMapping("/fix")
    public ResponseEntity<Iot> updateIot(@RequestBody Iot newIot) {
        return iotService.updateIot(newIot);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<String> deleteIot(@PathVariable String deviceId) {
        return iotService.deleteIot(deviceId);
    }

    @GetMapping("/data/{deviceId}")
    public ResponseEntity<List<dulieu>> getDataByDeviceId(@PathVariable String deviceId) {
        return iotService.getDataId(deviceId);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Lichsu>> getHistoryByDeviceId(@PathVariable String deviceId) {
        return iotService.Lichsutuoi(deviceId);
    }

    @PostMapping("/control/bom/{deviceId}")
    public ResponseEntity<String> Command(@PathVariable String deviceId, @RequestParam String command) {
        return iotService.Command(deviceId, command);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addDevice(@RequestBody Iot newDevice) {
        return iotService.addDevice(newDevice);
    }
    
    @GetMapping("/doam/{deviceId}")
    public float xemDoam(@RequestBody String deviceID) throws Exception{
    
        return iotService.xemDoam(deviceID);
    }
    
}  
