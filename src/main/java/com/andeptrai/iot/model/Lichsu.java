/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.model;

/**
 *
 * @author an
 */
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity

public class Lichsu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column( nullable = false)
    private String username;
    @Column( nullable = false)
    private String mac;
    @Column( nullable = false)
    private String action;
    @Column( nullable = false)
    private int auto;
    @Column( nullable = false)
    private LocalDateTime timestamp;

    public void setId(Long id) {
        this.id = id;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setAuto(int auto) {
        this.auto = auto;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getMac() {
        return mac;
    }

    public String getAction() {
        return action;
    }

    public int getAuto() {
        return auto;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    

}
