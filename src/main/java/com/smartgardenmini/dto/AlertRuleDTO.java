package com.smartgardenmini.dto;

public class AlertRuleDTO {
    private String macId;
    private float minHumidity;
    private float maxHumidity;
    private boolean notifyOnDisconnect;
    private boolean notifyOnAutoWater;
    private boolean enabled;

    public AlertRuleDTO() {}

    public String getMacId() { return macId; }
    public void setMacId(String macId) { this.macId = macId; }
    public float getMinHumidity() { return minHumidity; }
    public void setMinHumidity(float minHumidity) { this.minHumidity = minHumidity; }
    public float getMaxHumidity() { return maxHumidity; }
    public void setMaxHumidity(float maxHumidity) { this.maxHumidity = maxHumidity; }
    public boolean isNotifyOnDisconnect() { return notifyOnDisconnect; }
    public void setNotifyOnDisconnect(boolean notifyOnDisconnect) { this.notifyOnDisconnect = notifyOnDisconnect; }
    public boolean isNotifyOnAutoWater() { return notifyOnAutoWater; }
    public void setNotifyOnAutoWater(boolean notifyOnAutoWater) { this.notifyOnAutoWater = notifyOnAutoWater; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}