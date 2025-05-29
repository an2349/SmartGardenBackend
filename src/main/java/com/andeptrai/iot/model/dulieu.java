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
@Table(name = "dulieu")
public class dulieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( nullable = false)
    private String mac;
    @Column( nullable = false)
    private float doam;
    @Column( nullable = false)
    private LocalDateTime time;

    public void setId(Long id) {
        this.id = id;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setDoam(float doam) {
        this.doam = doam;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public String getMac() {
        return mac;
    }

    public float getDoam() {
        return doam;
    }

    public LocalDateTime getTime() {
        return time;
    }

    

}
