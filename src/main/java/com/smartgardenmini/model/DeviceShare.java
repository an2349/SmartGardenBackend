package com.smartgardenmini.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_shares")
public class DeviceShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String macId;

    @Column(nullable = false)
    private String ownerUsername;

    @Column(nullable = false)
    private String sharedUsername;

    @Column(nullable = false)
    private String permission = "CONTROL"; // VIEW, CONTROL, ADMIN

    private LocalDateTime expiresAt; // null = vĩnh viễn

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DeviceShare() {}

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

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}