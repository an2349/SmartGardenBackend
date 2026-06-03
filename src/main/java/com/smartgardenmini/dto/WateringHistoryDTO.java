package com.smartgardenmini.dto;

import java.time.LocalDateTime;

public class WateringHistoryDTO {
    private String mac;
    private String action;
    private int auto;
    private LocalDateTime timestamp;
    private Integer durationSec;

    public WateringHistoryDTO() {}

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