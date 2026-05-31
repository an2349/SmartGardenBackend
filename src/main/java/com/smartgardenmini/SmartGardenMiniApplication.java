package com.smartgardenmini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartGardenMiniApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartGardenMiniApplication.class, args);
    }
}