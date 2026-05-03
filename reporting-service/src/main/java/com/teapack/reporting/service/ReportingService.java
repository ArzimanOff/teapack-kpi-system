package com.teapack.reporting.service;

import com.teapack.reporting.client.KpiClient;
import com.teapack.reporting.dto.KpiResultDto;
import com.teapack.reporting.entity.Report;
import com.teapack.reporting.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final ReportRepository reportRepository;
    private final KpiClient kpiClient;

    @Transactional
    public Report generateReport(Long shiftId) {
        log.info("Generating report for shift: {}", shiftId);

        KpiResultDto kpi = kpiClient.getKpiByShift(shiftId);

        Report report = reportRepository.findByShiftId(shiftId)
                .orElse(Report.builder().shiftId(shiftId).build());

        report.setLineId(kpi.getLineId());
        report.setOee(kpi.getOee());
        report.setAvailability(kpi.getAvailability());
        report.setPerformance(kpi.getPerformance());
        report.setQuality(kpi.getQuality());
        report.setTotalOutput(kpi.getTotalOutput());
        report.setScrapCount(kpi.getScrapCount());
        report.setDowntime(kpi.getDowntime());
        report.setStatus("GENERATED");
        report.setType("SHIFT");

        report = reportRepository.save(report);
        log.info("Report generated: id={}, shiftId={}, OEE={}", report.getId(), shiftId, kpi.getOee());
        return report;
    }

    public Report getReportByShift(Long shiftId) {
        return reportRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new RuntimeException("Report not found for shift: " + shiftId));
    }

    public List<Report> getReportsByLine(String lineId) {
        return reportRepository.findByLineIdOrderByCreatedAtDesc(lineId);
    }
}