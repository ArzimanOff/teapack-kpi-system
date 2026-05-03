package com.teapack.reporting.client;

import com.teapack.reporting.dto.KpiResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kpi-calculation-service", url = "${kpi-calculation.url}")
public interface KpiClient {

    @GetMapping("/api/kpi/shift/{shiftId}")
    KpiResultDto getKpiByShift(@PathVariable("shiftId") Long shiftId);
}