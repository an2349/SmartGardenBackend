package com.smartgardenmini.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "thietbi_iot")
public class Iot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Username không được để trống")
    private String username;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "MAC ID không được để trống")
    private String macId;

    @Column(nullable = false)
    @NotBlank(message = "Tên thiết bị không được để trống")
    private String name;

    @Column(nullable = false)
    private int water = 0; // 0=thủ công, 1=tự động

    @Column(nullable = false)
    private float do_am; // Ngưỡng độ ẩm

    @Column(nullable = false)
    private boolean online = false; // Trạng thái kết nối

    public Iot() {}

    public Iot(String name, String username, String mac, int water) {
        this.username = username;
        this.name = name;
        this.macId = mac;
        this.water = water;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWater() { return water; }
    public void setWater(int water) { this.water = water; }
    public String getMacId() { return macId; }
    public void setMacId(String macId) { this.macId = macId; }
    public float getDo_am() { return do_am; }
    public void setDo_am(float do_am) { this.do_am = do_am; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}