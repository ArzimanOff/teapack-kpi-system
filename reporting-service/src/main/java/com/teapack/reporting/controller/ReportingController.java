package com.teapack.reporting.controller;

import com.teapack.reporting.entity.Report;
import com.teapack.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    // Без @PreAuthorize: эндпоинт зовётся из kpi-calculation Feign'ом при close смены.
    // Внешний доступ через gateway всё равно требует валидный JWT.
    @PostMapping("/generate/{shiftId}")
    public ResponseEntity<Report> generate(@PathVariable Long shiftId) {
        return ResponseEntity.ok(reportingService.generateReport(shiftId));
    }

    @GetMapping("/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('TECHNOLOGIST','ADMIN')")
    public ResponseEntity<Report> getByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(reportingService.getReportByShift(shiftId));
    }

    @GetMapping("/line/{lineId}")
    @PreAuthorize("hasAnyRole('TECHNOLOGIST','ADMIN')")
    public ResponseEntity<List<Report>> getByLine(@PathVariable String lineId) {
        return ResponseEntity.ok(reportingService.getReportsByLine(lineId));
    }
}