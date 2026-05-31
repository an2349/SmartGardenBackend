package com.smartgardenmini.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "Password không được để trống")
    private String password; // Lưu BCrypt hash

    @Column(nullable = false)
    private int role; // 0=Admin, 1=User

    @Column(nullable = false)
    @NotBlank(message = "Số điện thoại không được để trống")
    private String sdt;

    @Column(nullable = false)
    @NotBlank(message = "Tên không được để trống")
    private String name;

    public User() {}

    public User(String username, String password, String name, String sdt, int role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.sdt = sdt;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }
}