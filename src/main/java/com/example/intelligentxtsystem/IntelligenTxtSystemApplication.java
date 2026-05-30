package com.example.intelligentxtsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntelligenTxtSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelligenTxtSystemApplication.class, args);
    }

}
