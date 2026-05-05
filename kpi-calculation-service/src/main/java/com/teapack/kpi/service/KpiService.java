package com.teapack.kpi.service;

import com.teapack.kpi.client.DataProcessingClient;
import com.teapack.kpi.client.NotificationClient;
import com.teapack.kpi.client.ReportingClient;
import com.teapack.kpi.dto.KpiHistoryFilterRequest;
import com.teapack.kpi.dto.KpiResultDto;
import com.teapack.kpi.dto.LineSummaryDto;
import com.teapack.kpi.dto.ShiftDataDto;
import com.teapack.kpi.entity.ShiftKpi;
import com.teapack.kpi.repository.ShiftKpiRepository;
import com.teapack.kpi.repository.ShiftKpiSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiService {

    private final ShiftKpiRepository shiftKpiRepository;
    private final KpiCalculator kpiCalculator;
    private final DataProcessingClient dataProcessingClient;
    private final ReportingClient reportingClient;
    private final NotificationClient notificationClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public KpiResultDto calculateAndSave(Long shiftId) {
        log.info("Calculating KPI for shift: {}", shiftId);

        // Получаем данные смены из data-processing-service
        ShiftDataDto shiftData = dataProcessingClient.getShiftData(shiftId);

        // Считаем KPI
        KpiResultDto result = kpiCalculator.calculate(shiftData);

        // Сохраняем в БД
        ShiftKpi entity = mapToEntity(result);
        shiftKpiRepository.findByShiftId(shiftId)
                .ifPresent(existing -> entity.setId(existing.getId()));
        shiftKpiRepository.save(entity);

        log.info("KPI calculated: shiftId={}, OEE={}, Availability={}, Performance={}, Quality={}",
                shiftId, result.getOee(), result.getAvailability(),
                result.getPerformance(), result.getQuality());

        // Пушим результат по WebSocket
        messagingTemplate.convertAndSend("/topic/kpi/" + shiftData.getLineId(), result);

        // Уведомляем notification-service
        try {
            notificationClient.checkKpiThresholds(result);
        } catch (Exception e) {
            log.error("Failed to send to notification service: {}", e.getMessage());
        }

        // Генерируем отчёт
        try {
            reportingClient.generateReport(shiftId);
        } catch (Exception e) {
            log.error("Failed to trigger report generation: {}", e.getMessage());
        }

        return result;
    }

    public KpiResultDto getKpiByShift(Long shiftId) {
        ShiftKpi entity = shiftKpiRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new RuntimeException("KPI not found for shift: " + shiftId));
        return mapToDto(entity);
    }

    public List<ShiftKpi> getKpiByLine(String lineId) {
        return shiftKpiRepository.findByLineIdOrderByCalculatedAtDesc(lineId);
    }

    public List<ShiftKpi> findKpiHistory(KpiHistoryFilterRequest filter) {
        return shiftKpiRepository.findAll(
                ShiftKpiSpecifications.fromFilter(filter),
                Sort.by(Sort.Direction.DESC, "calculatedAt")
        );
    }

    public Page<ShiftKpi> findKpiHistoryPaged(KpiHistoryFilterRequest filter, Pageable pageable) {
        return shiftKpiRepository.findAll(
                ShiftKpiSpecifications.fromFilter(filter),
                pageable
        );
    }

    public LineSummaryDto getLineSummary(String lineId, LocalDateTime from,
                                         LocalDateTime to, int recentLimit) {
        Object[] row = shiftKpiRepository.aggregateByLine(lineId, from, to);
        // JPA возвращает Object[] с одной агрегатной строкой
        Object[] r = row != null && row.length == 1 && row[0] instanceof Object[]
                ? (Object[]) row[0] : row;

        LineSummaryDto dto = new LineSummaryDto();
        dto.setLineId(lineId);
        dto.setShifts(asLong(r[0]));
        dto.setAvgOee(asScaled(r[1], 4));
        dto.setAvgAvailability(asScaled(r[2], 4));
        dto.setAvgPerformance(asScaled(r[3], 4));
        dto.setAvgQuality(asScaled(r[4], 4));
        dto.setTotalOutput(asLong(r[5]));
        dto.setGoodOutput(asLong(r[6]));
        dto.setScrapCount(asLong(r[7]));
        dto.setTotalDowntime(asScaled(r[8], 2));
        dto.setTotalStops(asLong(r[9]));
        dto.setFirstCalculatedAt(asDate(r[10]));
        dto.setLastCalculatedAt(asDate(r[11]));

        Pageable top = PageRequest.of(0, Math.max(1, recentLimit));
        List<ShiftKpi> recent = (from != null || to != null)
                ? shiftKpiRepository.findByLineIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(
                        lineId,
                        from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0),
                        to != null ? to : LocalDateTime.now().plusYears(100),
                        top)
                : shiftKpiRepository.findByLineIdOrderByCalculatedAtDesc(lineId, top);
        dto.setRecent(recent);
        return dto;
    }

    private long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private BigDecimal asScaled(Object v, int scale) {
        if (v == null) return null;
        BigDecimal bd = (v instanceof BigDecimal bdv) ? bdv : new BigDecimal(v.toString());
        return bd.setScale(scale, RoundingMode.HALF_UP);
    }

    private LocalDateTime asDate(Object v) {
        return v instanceof LocalDateTime ld ? ld : null;
    }

    private ShiftKpi mapToEntity(KpiResultDto dto) {
        return ShiftKpi.builder()
                .shiftId(dto.getShiftId())
                .lineId(dto.getLineId())
                .plannedTime(dto.getPlannedTime())
                .operatingTime(dto.getOperatingTime())
                .downtime(dto.getDowntime())
                .downtimeRate(dto.getDowntimeRate())
                .numberOfStops(dto.getNumberOfStops())
                .avgDowntime(dto.getAvgDowntime())
                .totalOutput(dto.getTotalOutput())
                .goodOutput(dto.getGoodOutput())
                .scrapCount(dto.getScrapCount())
                .plannedOutput(dto.getPlannedOutput())
                .nominalSpeed(dto.getNominalSpeed())
                .avgSpeed(dto.getAvgSpeed())
                .outputRate(dto.getOutputRate())
                .speedLoss(dto.getSpeedLoss())
                .availability(dto.getAvailability())
                .performance(dto.getPerformance())
                .quality(dto.getQuality())
                .oee(dto.getOee())
                .performanceLoss(dto.getPerformanceLoss())
                .planFulfillment(dto.getPlanFulfillment())
                .scrapRate(dto.getScrapRate())
                .build();
    }

    private KpiResultDto mapToDto(ShiftKpi e) {
        KpiResultDto dto = new KpiResultDto();
        dto.setShiftId(e.getShiftId());
        dto.setLineId(e.getLineId());
        dto.setPlannedTime(e.getPlannedTime());
        dto.setOperatingTime(e.getOperatingTime());
        dto.setDowntime(e.getDowntime());
        dto.setDowntimeRate(e.getDowntimeRate());
        dto.setNumberOfStops(e.getNumberOfStops());
        dto.setAvgDowntime(e.getAvgDowntime());
        dto.setTotalOutput(e.getTotalOutput());
        dto.setGoodOutput(e.getGoodOutput());
        dto.setScrapCount(e.getScrapCount());
        dto.setPlannedOutput(e.getPlannedOutput());
        dto.setNominalSpeed(e.getNominalSpeed());
        dto.setAvgSpeed(e.getAvgSpeed());
        dto.setOutputRate(e.getOutputRate());
        dto.setSpeedLoss(e.getSpeedLoss());
        dto.setAvailability(e.getAvailability());
        dto.setPerformance(e.getPerformance());
        dto.setQuality(e.getQuality());
        dto.setOee(e.getOee());
        dto.setPerformanceLoss(e.getPerformanceLoss());
        dto.setPlanFulfillment(e.getPlanFulfillment());
        dto.setScrapRate(e.getScrapRate());
        return dto;
    }
}