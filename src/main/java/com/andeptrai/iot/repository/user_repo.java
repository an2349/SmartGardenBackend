/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.repository;
import com.andeptrai.iot.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 *
 * @author an
 */
@Repository
public interface user_repo extends JpaRepository<User, Long> {
    List<User> findByName(String name);
    Optional<User> findByUsername(String Username);
    boolean existsByUsername(String username);
    List<User> findByNameContainingIgnoreCase(String key);
   
    
}
