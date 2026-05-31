package com.smartgardenmini.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alert_rules")
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String macId;

    @Column(nullable = false)
    private String username;

    private float minHumidity = 20; // Cảnh báo nếu độ ẩm dưới ngưỡng
    private float maxHumidity = 90; // Cảnh báo nếu độ ẩm trên ngưỡng
    private boolean notifyOnDisconnect = true;
    private boolean notifyOnAutoWater = true;
    private boolean enabled = true;

    public AlertRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMacId() { return macId; }
    public void setMacId(String macId) { this.macId = macId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
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