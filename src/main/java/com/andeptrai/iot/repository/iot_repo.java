/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.repository;

import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.model.User;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author an
 */
@Repository
public interface iot_repo extends JpaRepository<Iot, Long> {
    List<Iot> findIotByUsername(String username);
    Optional<Iot> findById(Long id);
    Optional<Iot> findBymacId(String macId);
    public boolean existsBymacId(String mac);
    public void deleteBymacId(String macId);
    
}
