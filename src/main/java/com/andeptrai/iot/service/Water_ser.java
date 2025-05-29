/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.service;

import com.andeptrai.iot.model.Lichsu;
import com.andeptrai.iot.repository.Lichsu_repo;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 *
 * @author an
 */
@Service
public class Water_ser {
    @Autowired
    private Lichsu_repo water_ser;
    
    public List<Lichsu> getHistory(String id){
    
    return water_ser.findByMac(id);
    }
}
