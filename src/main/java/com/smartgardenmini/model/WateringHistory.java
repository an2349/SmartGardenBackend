package com.smartgardenmini.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watering_history")
public class WateringHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String mac;

    @Column(nullable = false)
    private String action; // ON / OFF

    @Column(nullable = false)
    private int auto; // 0=thủ công, 1=tự động

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Integer durationSec; // Thời gian tưới (giây), null nếu tưới vô hạn

    public WateringHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getAuto() { return auto; }
    public void setAuto(int auto) { this.auto = auto; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
}