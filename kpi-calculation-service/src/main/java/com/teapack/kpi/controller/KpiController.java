package com.teapack.kpi.controller;

import com.teapack.kpi.dto.KpiResultDto;
import com.teapack.kpi.entity.ShiftKpi;
import com.teapack.kpi.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @PostMapping("/calculate/{shiftId}")
    public ResponseEntity<KpiResultDto> calculate(@PathVariable Long shiftId) {
        return ResponseEntity.ok(kpiService.calculateAndSave(shiftId));
    }

    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<KpiResultDto> getByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(kpiService.getKpiByShift(shiftId));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<List<ShiftKpi>> getByLine(@PathVariable String lineId) {
        return ResponseEntity.ok(kpiService.getKpiByLine(lineId));
    }
}