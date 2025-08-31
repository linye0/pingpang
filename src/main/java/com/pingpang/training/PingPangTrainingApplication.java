package com.pingpang.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PingPangTrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PingPangTrainingApplication.class, args);
    }
} 