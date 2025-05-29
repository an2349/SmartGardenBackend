/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.andeptrai.iot.repository;

import com.andeptrai.iot.model.Lichsu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author an
 */
@Repository
public interface Lichsu_repo extends JpaRepository<Lichsu, Long> {
    java.util.List<Lichsu> findByMac(String deviceId);
}
