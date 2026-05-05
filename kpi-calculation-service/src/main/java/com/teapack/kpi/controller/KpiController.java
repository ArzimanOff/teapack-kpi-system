package com.teapack.kpi.controller;

import com.teapack.kpi.dto.KpiHistoryFilterRequest;
import com.teapack.kpi.dto.KpiResultDto;
import com.teapack.kpi.dto.LineSummaryDto;
import com.teapack.kpi.entity.ShiftKpi;
import com.teapack.kpi.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<KpiResultDto> getByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(kpiService.getKpiByShift(shiftId));
    }

    @GetMapping("/line/{lineId}")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<List<ShiftKpi>> getByLine(@PathVariable String lineId) {
        return ResponseEntity.ok(kpiService.getKpiByLine(lineId));
    }

    @GetMapping("/line/{lineId}/summary")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<LineSummaryDto> getLineSummary(
            @PathVariable String lineId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(kpiService.getLineSummary(lineId, dateFrom, dateTo, limit));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('TECHNOLOGIST','ADMIN')")
    public ResponseEntity<Page<ShiftKpi>> getHistory(
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) BigDecimal oeeMin,
            @RequestParam(required = false) BigDecimal availabilityMin,
            @RequestParam(required = false) BigDecimal performanceMin,
            @RequestParam(required = false) BigDecimal qualityMin,
            @PageableDefault(size = 20, sort = "calculatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        KpiHistoryFilterRequest filter = new KpiHistoryFilterRequest();
        filter.setLineId(lineId);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setOeeMin(oeeMin);
        filter.setAvailabilityMin(availabilityMin);
        filter.setPerformanceMin(performanceMin);
        filter.setQualityMin(qualityMin);
        return ResponseEntity.ok(kpiService.findKpiHistoryPaged(filter, pageable));
    }
}
