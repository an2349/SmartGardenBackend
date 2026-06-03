package com.smartgardenmini.dto;

public class UserDTO {
    private String username;
    private String name;
    private String sdt;
    private int role;

    public UserDTO() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }
}