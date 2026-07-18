package com.example.datnhathub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DatnHatHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatnHatHubApplication.class, args);
    }

}
