/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.service;

import com.andeptrai.iot.model.dulieu;
import com.andeptrai.iot.repository.Data_repo;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 *
 * @author an
 */
@Service
public class Data_ser {
    @Autowired
    private Data_repo data_repo;
    @Transactional
   public void saveData(dulieu newdata) {
        dulieu data = new dulieu();
        data.setMac(newdata.getMac());
        data.setDoam( newdata.getDoam());
        data.setTime(LocalDateTime.now());
        data_repo.save(data);
    }
    
    
}
