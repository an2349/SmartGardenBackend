package com.smartgardenmini.dto;

import java.time.LocalDateTime;

public class SensorDataDTO {
    private String mac;
    private float doam;
    private Float nhietDo;
    private Float doAmKK;
    private Float anhSang;
    private Float luuLuong;
    private LocalDateTime time;

    public SensorDataDTO() {}

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