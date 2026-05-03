package com.teapack.kpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KpiCalculationApplication {
    public static void main(String[] args) {
        SpringApplication.run(KpiCalculationApplication.class, args);
    }
}