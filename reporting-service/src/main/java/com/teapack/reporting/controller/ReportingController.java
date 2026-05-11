package com.teapack.reporting.controller;

import com.teapack.reporting.entity.Report;
import com.teapack.reporting.service.CsvReportService;
import com.teapack.reporting.service.PdfReportService;
import com.teapack.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
    private final CsvReportService csvReportService;
    private final PdfReportService pdfReportService;

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

    @GetMapping(value = "/{shiftId}/csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('TECHNOLOGIST','ADMIN')")
    public ResponseEntity<byte[]> exportShiftCsv(@PathVariable Long shiftId) {
        byte[] body = csvReportService.renderShiftReport(shiftId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment",
                "shift-" + shiftId + "-report.csv");
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping(value = "/{shiftId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('TECHNOLOGIST','ADMIN')")
    public ResponseEntity<byte[]> exportShiftPdf(@PathVariable Long shiftId) {
        byte[] body = pdfReportService.renderShiftReport(shiftId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "shift-" + shiftId + "-report.pdf");
        return ResponseEntity.ok().headers(headers).body(body);
    }
}