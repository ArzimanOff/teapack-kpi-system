package com.teapack.emulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class EquipmentEmulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EquipmentEmulatorApplication.class, args);
    }
}