package com.teapack.reporting.client;

import com.teapack.reporting.dto.DowntimeEventDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "data-processing-service", url = "${data-processing.url}")
public interface ProcessingClient {

    @GetMapping("/api/shifts/{shiftId}/downtimes")
    List<DowntimeEventDto> getDowntimes(@PathVariable("shiftId") Long shiftId);
}
