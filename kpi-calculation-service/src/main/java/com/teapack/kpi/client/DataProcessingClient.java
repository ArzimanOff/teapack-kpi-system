package com.teapack.kpi.client;

import com.teapack.kpi.dto.DowntimeEventDto;
import com.teapack.kpi.dto.PageResponseDto;
import com.teapack.kpi.dto.ShiftDataDto;
import com.teapack.kpi.dto.ShiftSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "data-processing-service", url = "${data-processing.url}")
public interface DataProcessingClient {

    @GetMapping("/api/shifts/{shiftId}/data")
    ShiftDataDto getShiftData(@PathVariable("shiftId") Long shiftId);

    @GetMapping("/api/shifts")
    PageResponseDto<ShiftSummaryDto> findShifts(@RequestParam("status") String status,
                                                @RequestParam(value = "lineId", required = false) String lineId,
                                                @RequestParam(value = "page", defaultValue = "0") int page,
                                                @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/api/shifts/{shiftId}/downtimes")
    List<DowntimeEventDto> getDowntimes(@PathVariable("shiftId") Long shiftId);
}