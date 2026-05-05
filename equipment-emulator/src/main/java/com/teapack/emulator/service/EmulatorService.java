package com.teapack.emulator.service;

import com.teapack.emulator.client.DataCollectionClient;
import com.teapack.emulator.client.LinesClient;
import com.teapack.emulator.dto.EquipmentReadingDto;
import com.teapack.emulator.dto.ProductionLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmulatorService {

    private final DataCollectionClient dataCollectionClient;
    private final LinesClient linesClient;

    @Value("${emulator.line-id}")
    private String defaultLineId;

    @Value("${emulator.nominal-speed}")
    private double fallbackNominalSpeed;

    @Value("${emulator.temperature-base}")
    private double fallbackTemperatureBase;

    private final Random random = new Random();

    /** key = shiftId; ConcurrentHashMap для безопасного итерирования из @Scheduled и REST */
    private final Map<Long, EmulatorRunState> runs = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${emulator.interval-ms}")
    public void emulate() {
        for (EmulatorRunState run : runs.values()) {
            if (!"RUNNING".equals(run.getStatus())) continue;
            try {
                EquipmentReadingDto dto = generateReading(run);
                dataCollectionClient.sendReading(dto);
                run.setTicks(run.getTicks() + 1);
                if (run.getScenarioTicksLeft() > 0) {
                    int left = run.getScenarioTicksLeft() - 1;
                    run.setScenarioTicksLeft(left);
                    if (left == 0) {
                        log.info("Scenario {} finished for shift {}", run.getScenario(), run.getShiftId());
                        run.setScenario("NORMAL");
                    }
                }
                log.debug("Tick: shift={}, line={}, speed={}, output={}, scenario={}",
                        run.getShiftId(), run.getLineId(),
                        dto.getLineSpeed(), dto.getOutputCount(), run.getScenario());
            } catch (Exception e) {
                log.error("Failed tick for shift {}: {}", run.getShiftId(), e.getMessage());
            }
        }
    }

    public EmulatorRunState start(Long shiftId, String lineId) {
        if (shiftId == null) throw new IllegalArgumentException("shiftId is required");
        EmulatorRunState run = runs.computeIfAbsent(shiftId, id -> new EmulatorRunState());
        run.setShiftId(shiftId);
        run.setLineId((lineId != null && !lineId.isBlank()) ? lineId : defaultLineId);
        run.setStartedAt(LocalDateTime.now());
        run.setStatus("RUNNING");
        run.setScenario("NORMAL");
        run.setScenarioTicksLeft(0);
        loadLineParams(run);
        log.info("Emulator START: shift={}, line={}, nominalSpeed={}, speedRange=[{}..{}], tempRange=[{}..{}]",
                shiftId, run.getLineId(), run.getNominalSpeed(),
                run.getMinSpeed(), run.getMaxSpeed(), run.getMinTemp(), run.getMaxTemp());
        return run;
    }

    public EmulatorRunState stop(Long shiftId) {
        EmulatorRunState run = runs.get(shiftId);
        if (run == null) return null;
        run.setStatus("STOPPED");
        log.info("Emulator STOP: shift={}", shiftId);
        return run;
    }

    /** Полное удаление записи о смене из эмулятора */
    public boolean remove(Long shiftId) {
        EmulatorRunState removed = runs.remove(shiftId);
        if (removed != null) log.info("Emulator REMOVED: shift={}", shiftId);
        return removed != null;
    }

    /** Применение сценария — действует на ближайшие N тиков. */
    public EmulatorRunState applyScenario(Long shiftId, String scenario, int ticks) {
        EmulatorRunState run = runs.get(shiftId);
        if (run == null) throw new IllegalStateException("No emulator run for shift " + shiftId);
        String norm = scenario == null ? "NORMAL" : scenario.toUpperCase();
        if (!List.of("NORMAL", "SPEED_DROP", "SCRAP_BURST", "OUTLIER").contains(norm)) {
            throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        run.setScenario(norm);
        run.setScenarioTicksLeft(Math.max(0, ticks));
        log.info("Scenario {} applied to shift {} for {} ticks", norm, shiftId, ticks);
        return run;
    }

    public Collection<EmulatorRunState> getRuns() {
        return new ArrayList<>(runs.values());
    }

    public EmulatorRunState getRun(Long shiftId) {
        return runs.get(shiftId);
    }

    private void loadLineParams(EmulatorRunState run) {
        try {
            ProductionLineDto line = linesClient.findByCode(run.getLineId());
            double nominal = line.getNominalSpeed() != null
                    ? line.getNominalSpeed().doubleValue() : fallbackNominalSpeed;
            double jitter = nominal * 0.1;
            double minS = line.getMinSpeed() != null
                    ? Math.max(line.getMinSpeed().doubleValue(), nominal - jitter)
                    : nominal - jitter;
            double maxS = line.getMaxSpeed() != null
                    ? Math.min(line.getMaxSpeed().doubleValue(), nominal + jitter)
                    : nominal + jitter;
            run.setNominalSpeed(nominal);
            run.setMinSpeed(minS);
            run.setMaxSpeed(maxS);
            run.setMinTemp(line.getMinTemperature() != null
                    ? line.getMinTemperature().doubleValue()
                    : fallbackTemperatureBase - 5);
            run.setMaxTemp(line.getMaxTemperature() != null
                    ? line.getMaxTemperature().doubleValue()
                    : fallbackTemperatureBase + 5);
        } catch (Exception e) {
            log.warn("Failed to load line '{}' from reference, using fallbacks: {}",
                    run.getLineId(), e.getMessage());
            run.setNominalSpeed(fallbackNominalSpeed);
            run.setMinSpeed(fallbackNominalSpeed * 0.9);
            run.setMaxSpeed(fallbackNominalSpeed * 1.1);
            run.setMinTemp(fallbackTemperatureBase - 5);
            run.setMaxTemp(fallbackTemperatureBase + 5);
        }
    }

    private EquipmentReadingDto generateReading(EmulatorRunState run) {
        EquipmentReadingDto dto = new EquipmentReadingDto();
        dto.setLineId(run.getLineId());
        dto.setTimestamp(LocalDateTime.now());
        dto.setShiftId(run.getShiftId());
        dto.setStatus("RUNNING");

        double speed = run.getMinSpeed() + random.nextDouble() * (run.getMaxSpeed() - run.getMinSpeed());
        double temp = run.getMinTemp() + random.nextDouble() * (run.getMaxTemp() - run.getMinTemp());
        int output = (int) Math.max(1, Math.round(speed / 60.0 * 5.0));

        switch (run.getScenario()) {
            case "SPEED_DROP" -> {
                speed = run.getNominalSpeed() * (0.3 + random.nextDouble() * 0.2);
                output = (int) Math.max(0, Math.round(speed / 60.0 * 5.0));
            }
            case "SCRAP_BURST" -> {
                dto.setStatus("SCRAP");
            }
            case "OUTLIER" -> {
                speed = run.getMaxSpeed() * 2.5;
                temp = run.getMaxTemp() + 30;
            }
            default -> { /* NORMAL */ }
        }

        dto.setLineSpeed(BigDecimal.valueOf(speed).setScale(2, RoundingMode.HALF_UP));
        dto.setTemperature(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP));
        dto.setOutputCount(output);
        return dto;
    }
}
