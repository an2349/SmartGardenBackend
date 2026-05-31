package com.smartgardenmini.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mac;

    @Column(nullable = false)
    private float doam; // Độ ẩm đất (%)

    private Float nhietDo;   // Nhiệt độ (°C)
    private Float doAmKK;    // Độ ẩm không khí (%)
    private Float anhSang;   // Ánh sáng (lux)
    private Float luuLuong;  // Lưu lượng nước (L/phút)

    @Column(nullable = false)
    private LocalDateTime time;

    public SensorData() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public float getDoam() { return doam; }
    public void setDoam(float doam) { this.doam = doam; }
    public Float getNhietDo() { return nhietDo; }
    public void setNhietDo(Float nhietDo) { this.nhietDo = nhietDo; }
    public Float getDoAmKK() { return doAmKK; }
    public void setDoAmKK(Float doAmKK) { this.doAmKK = doAmKK; }
    public Float getAnhSang() { return anhSang; }
    public void setAnhSang(Float anhSang) { this.anhSang = anhSang; }
    public Float getLuuLuong() { return luuLuong; }
    public void setLuuLuong(Float luuLuong) { this.luuLuong = luuLuong; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
}