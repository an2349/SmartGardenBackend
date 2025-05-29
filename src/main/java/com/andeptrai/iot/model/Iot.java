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
import java.sql.Time;

/**
 *
 * @author an
 */
@Entity
@Table(name = "thietbi_iot")
public class Iot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column( nullable = false)
    private String username;//ten chu so huu
    @Column(unique = true, nullable = false)
    private String macId;
    @Column( nullable = false)
    private String name;//ten thiet bi
    @Column( nullable = false)
    private int water =0;//0 la khong tu dong tuoi
    @Column( nullable = false)
    private float do_am;
    
    //private user user;
    
    public Iot(){}
    
    public Iot(String name,String username,String mac,int water){
        this.username = username;
        this.name = name;
        this.macId = mac;
        this.water = water;//de tuoi
        
    }
    
    //toan getter
    public String getUsername(){
        return this.username;
    }
    public String getName(){
        return this.name;
    }
    
    public int getWater(){
        return this.water;
    }

    public String getMacId() {
        return macId;
    }

    public float getDo_am() {
        return do_am;
    }
    
    public long getId(){
        return this.id;
    }
    
    
    //toan setter
    public void setUsername(String username){
        this.username = username;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setWater(int i){
       this.water = i; 
    }

    public void setMacId(String macId) {
        this.macId = macId;
    }

    public void setDo_am(float do_am) {
        this.do_am = do_am;
    }
    

    
}
