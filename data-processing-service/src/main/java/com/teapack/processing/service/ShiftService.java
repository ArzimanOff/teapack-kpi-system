package com.teapack.processing.service;

import com.teapack.processing.client.KpiCalculationClient;
import com.teapack.processing.dto.*;
import com.teapack.processing.entity.*;
import com.teapack.processing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftAggregateRepository shiftAggregateRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final KpiCalculationClient kpiCalculationClient;

    @Transactional
    public Shift createShift(CreateShiftRequest request) {
        // Проверяем нет ли уже активной смены на этой линии
        shiftRepository.findByLineIdAndStatus(request.getLineId(), "ACTIVE")
                .ifPresent(s -> { throw new RuntimeException("Active shift already exists for line: " + request.getLineId()); });

        Shift shift = Shift.builder()
                .lineId(request.getLineId())
                .operatorId(request.getOperatorId())
                .plannedStart(request.getPlannedStart())
                .plannedEnd(request.getPlannedEnd())
                .plannedOutput(request.getPlannedOutput())
                .nominalSpeed(request.getNominalSpeed())
                .status("PLANNED")
                .build();

        shift = shiftRepository.save(shift);

        // Создаём пустой агрегат для смены
        ShiftAggregate aggregate = ShiftAggregate.builder()
                .shiftId(shift.getId())
                .totalOutput(0)
                .goodOutput(0)
                .scrapCount(0)
                .downtimeMinutes(BigDecimal.ZERO)
                .build();
        shiftAggregateRepository.save(aggregate);

        log.info("Created shift: id={}, lineId={}", shift.getId(), shift.getLineId());
        return shift;
    }

    @Transactional
    public Shift startShift(Long shiftId) {
        Shift shift = getShiftOrThrow(shiftId);
        shift.setStatus("ACTIVE");
        shift.setActualStart(LocalDateTime.now());
        shift = shiftRepository.save(shift);
        log.info("Started shift: id={}", shiftId);
        return shift;
    }

    @Transactional
    public Shift closeShift(Long shiftId) {
        Shift shift = getShiftOrThrow(shiftId);
        if (!"ACTIVE".equals(shift.getStatus())) {
            throw new RuntimeException("Shift is not active: " + shiftId);
        }

        // Закрываем открытый простой если есть
        downtimeEventRepository.findOpenDowntimeByShiftId(shiftId)
                .ifPresent(d -> {
                    d.setEndTime(LocalDateTime.now());
                    d.setDurationMinutes(calculateDuration(d.getStartTime(), d.getEndTime()));
                    downtimeEventRepository.save(d);
                });

        shift.setStatus("CLOSED");
        shift.setActualEnd(LocalDateTime.now());
        shift = shiftRepository.save(shift);

        log.info("Closed shift: id={}", shiftId);

        // Запускаем расчёт KPI
        try {
            kpiCalculationClient.calculateKpi(shiftId);
        } catch (Exception e) {
            log.error("Failed to trigger KPI calculation for shift {}: {}", shiftId, e.getMessage());
        }

        return shift;
    }

    @Transactional
    public void processEquipmentReading(EquipmentReadingDto dto) {
        if (dto.getShiftId() == null) return;

        shiftAggregateRepository.findByShiftId(dto.getShiftId()).ifPresent(aggregate -> {
            if (dto.getOutputCount() != null && dto.getOutputCount() > 0) {
                aggregate.setTotalOutput(aggregate.getTotalOutput() + dto.getOutputCount());
                aggregate.setGoodOutput(aggregate.getTotalOutput() - aggregate.getScrapCount());
            }
            shiftAggregateRepository.save(aggregate);
        });
    }

    @Transactional
    public void processOperatorEvent(OperatorEventDto dto) {
        switch (dto.getEventType()) {
            case "STOP", "DOWNTIME" -> handleDowntimeStart(dto);
            case "START" -> handleDowntimeEnd(dto);
            case "SCRAP" -> handleScrap(dto);
            default -> log.warn("Unknown event type: {}", dto.getEventType());
        }
    }

    private void handleDowntimeStart(OperatorEventDto dto) {
        // Закрываем предыдущий простой если есть
        downtimeEventRepository.findOpenDowntimeByShiftId(dto.getShiftId())
                .ifPresent(d -> {
                    d.setEndTime(dto.getTimestamp());
                    d.setDurationMinutes(calculateDuration(d.getStartTime(), d.getEndTime()));
                    downtimeEventRepository.save(d);
                });

        DowntimeEvent downtime = DowntimeEvent.builder()
                .shiftId(dto.getShiftId())
                .startTime(dto.getTimestamp())
                .reason(dto.getReason())
                .build();
        downtimeEventRepository.save(downtime);

        log.debug("Downtime started for shift: {}, reason: {}", dto.getShiftId(), dto.getReason());
    }

    private void handleDowntimeEnd(OperatorEventDto dto) {
        downtimeEventRepository.findOpenDowntimeByShiftId(dto.getShiftId())
                .ifPresent(d -> {
                    d.setEndTime(dto.getTimestamp());
                    d.setDurationMinutes(calculateDuration(d.getStartTime(), d.getEndTime()));
                    downtimeEventRepository.save(d);

                    // Обновляем суммарный простой в агрегате
                    BigDecimal totalDowntime = downtimeEventRepository.findTotalDowntimeByShiftId(dto.getShiftId());
                    shiftAggregateRepository.findByShiftId(dto.getShiftId()).ifPresent(a -> {
                        a.setDowntimeMinutes(totalDowntime);
                        shiftAggregateRepository.save(a);
                    });
                });

        log.debug("Downtime ended for shift: {}", dto.getShiftId());
    }

    private void handleScrap(OperatorEventDto dto) {
        if (dto.getScrapCount() == null || dto.getScrapCount() <= 0) return;

        shiftAggregateRepository.findByShiftId(dto.getShiftId()).ifPresent(aggregate -> {
            aggregate.setScrapCount(aggregate.getScrapCount() + dto.getScrapCount());
            aggregate.setGoodOutput(aggregate.getTotalOutput() - aggregate.getScrapCount());
            shiftAggregateRepository.save(aggregate);
        });

        log.debug("Scrap recorded for shift: {}, count: {}", dto.getShiftId(), dto.getScrapCount());
    }

    public Shift getShiftOrThrow(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));
    }

    public ShiftAggregate getShiftAggregate(Long shiftId) {
        return shiftAggregateRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new RuntimeException("Aggregate not found for shift: " + shiftId));
    }

    public List<Shift> getShiftsByLine(String lineId) {
        return shiftRepository.findByLineIdOrderByCreatedAtDesc(lineId);
    }

    private BigDecimal calculateDuration(LocalDateTime start, LocalDateTime end) {
        long seconds = Duration.between(start, end).getSeconds();
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public List<DowntimeEvent> getDowntimeEvents(Long shiftId) {
        return downtimeEventRepository.findByShiftIdOrderByStartTimeAsc(shiftId);
    }
}