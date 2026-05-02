package com.teapack.processing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "kpi-calculation-service", url = "${kpi-calculation.url}")
public interface KpiCalculationClient {

    @PostMapping("/api/kpi/calculate/{shiftId}")
    void calculateKpi(@PathVariable("shiftId") Long shiftId);
}