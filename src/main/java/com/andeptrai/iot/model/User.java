/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.andeptrai.iot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 *
 * @author an
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private int role; 
    @Column(nullable = false)
    private String sdt;
    @Column(nullable = false)
    private String name;

    
    // Getter, Setter...
    public User(){}
    public User(String Uname,String pass,String name ,String sdt,int role){
        this.username = Uname;
        this.password = pass;
        this.name = name;
        this.sdt = sdt;
        this.role = role;
        }
    
    
    //toan get
    public String getUsername(){//lay username
        return this.username;
    }
    public String getPassword(){//lay pass
        return this.password;
    }
    
    public int getRole(){//lay quyen
        return this.role;
    }
    public String getName(){//layten
        return this.name;
    }
    public String getSdt(){//lay sdt
        return this.sdt;
    }
    public Long getId(){
        return this.id;
    }
    
    
    //toan set
    public void setUsername(String name){this.username = name;}
    public void setPassword(String pass){this.password = pass;}
    public void setId(Long id) {
    this.id = id;
}
    public void setRole(int quyen){this.role = quyen;}
    public void setName(String name){this.name = name;}
    public void setSdt(String sdt){this.sdt = sdt;}
    
    
    
}
