package com.smartgardenmini.dto;

import java.time.LocalDateTime;

public class DeviceShareDTO {
    private Long id;
    private String macId;
    private String ownerUsername;
    private String sharedUsername;
    private String permission;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public DeviceShareDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMacId() { return macId; }
    public void setMacId(String macId) { this.macId = macId; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public String getSharedUsername() { return sharedUsername; }
    public void setSharedUsername(String sharedUsername) { this.sharedUsername = sharedUsername; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}