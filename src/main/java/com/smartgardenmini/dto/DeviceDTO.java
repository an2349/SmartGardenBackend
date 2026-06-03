package com.smartgardenmini.dto;

public class DeviceDTO {
    private String macId;
    private String name;
    private boolean online;
    private int autoMode;       // field water trong Iot
    private float threshold;    // field do_am trong Iot

    public DeviceDTO() {}

    public String getMacId() { return macId; }
    public void setMacId(String macId) { this.macId = macId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public int getAutoMode() { return autoMode; }
    public void setAutoMode(int autoMode) { this.autoMode = autoMode; }
    public float getThreshold() { return threshold; }
    public void setThreshold(float threshold) { this.threshold = threshold; }
}