package com.teapack.kpi.client;

import com.teapack.kpi.dto.ShiftDataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "data-processing-service", url = "${data-processing.url}")
public interface DataProcessingClient {

    @GetMapping("/api/shifts/{shiftId}/data")
    ShiftDataDto getShiftData(@PathVariable("shiftId") Long shiftId);
}