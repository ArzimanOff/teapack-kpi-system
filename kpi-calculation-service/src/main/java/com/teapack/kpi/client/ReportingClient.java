package com.teapack.kpi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "reporting-service", url = "${reporting.url}")
public interface ReportingClient {

    @PostMapping("/api/reports/generate/{shiftId}")
    void generateReport(@PathVariable("shiftId") Long shiftId);
}