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
import java.util.Random;

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

    private boolean active = false;
    private Long currentShiftId = null;
    private String currentStatus = "STOPPED";

    private String currentLineId;
    private double nominalSpeed;
    private double minSpeed;
    private double maxSpeed;
    private double minTemp;
    private double maxTemp;

    @Scheduled(fixedDelayString = "${emulator.interval-ms}")
    public void emulate() {
        if (!active || currentShiftId == null) return;

        EquipmentReadingDto dto = generateReading();
        try {
            dataCollectionClient.sendReading(dto);
            log.debug("Sent reading: line={}, status={}, speed={}, output={}",
                    currentLineId, dto.getStatus(), dto.getLineSpeed(), dto.getOutputCount());
        } catch (Exception e) {
            log.error("Failed to send reading: {}", e.getMessage());
        }
    }

    public void start(Long shiftId, String lineId) {
        this.currentShiftId = shiftId;
        this.currentLineId = (lineId != null && !lineId.isBlank()) ? lineId : defaultLineId;
        loadLineParams(this.currentLineId);
        this.active = true;
        this.currentStatus = "RUNNING";
        log.info("Emulator started for shift={}, line={}, nominalSpeed={}, speedRange=[{}..{}], tempRange=[{}..{}]",
                shiftId, currentLineId, nominalSpeed, minSpeed, maxSpeed, minTemp, maxTemp);
    }

    public void stop() {
        this.active = false;
        this.currentStatus = "STOPPED";
        log.info("Emulator stopped");
    }

    public void setStatus(String status) {
        this.currentStatus = status;
    }

    public String getStatus() {
        return currentStatus;
    }

    public boolean isActive() {
        return active;
    }

    public Long getCurrentShiftId() {
        return currentShiftId;
    }

    public String getCurrentLineId() {
        return currentLineId;
    }

    private void loadLineParams(String lineId) {
        try {
            ProductionLineDto line = linesClient.findByCode(lineId);
            this.nominalSpeed = line.getNominalSpeed() != null
                    ? line.getNominalSpeed().doubleValue() : fallbackNominalSpeed;
            // Сохраняем 10%-разброс относительно номинала, но не выходим за пороги outlier-detection
            double speedJitter = nominalSpeed * 0.1;
            this.minSpeed = line.getMinSpeed() != null
                    ? Math.max(line.getMinSpeed().doubleValue(), nominalSpeed - speedJitter)
                    : nominalSpeed - speedJitter;
            this.maxSpeed = line.getMaxSpeed() != null
                    ? Math.min(line.getMaxSpeed().doubleValue(), nominalSpeed + speedJitter)
                    : nominalSpeed + speedJitter;
            // Температура — используем пороги справочника, либо fallback ±5
            this.minTemp = line.getMinTemperature() != null
                    ? line.getMinTemperature().doubleValue()
                    : fallbackTemperatureBase - 5;
            this.maxTemp = line.getMaxTemperature() != null
                    ? line.getMaxTemperature().doubleValue()
                    : fallbackTemperatureBase + 5;
        } catch (Exception e) {
            log.warn("Failed to load line '{}' from reference, using fallbacks: {}", lineId, e.getMessage());
            this.nominalSpeed = fallbackNominalSpeed;
            this.minSpeed = nominalSpeed * 0.9;
            this.maxSpeed = nominalSpeed * 1.1;
            this.minTemp = fallbackTemperatureBase - 5;
            this.maxTemp = fallbackTemperatureBase + 5;
        }
    }

    private EquipmentReadingDto generateReading() {
        EquipmentReadingDto dto = new EquipmentReadingDto();
        dto.setLineId(currentLineId);
        dto.setTimestamp(LocalDateTime.now());
        dto.setShiftId(currentShiftId);
        dto.setStatus(currentStatus);

        if ("RUNNING".equals(currentStatus)) {
            double speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
            dto.setLineSpeed(BigDecimal.valueOf(speed).setScale(2, RoundingMode.HALF_UP));

            double temp = minTemp + random.nextDouble() * (maxTemp - minTemp);
            dto.setTemperature(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP));

            // Выпуск пропорционален средней скорости (~speed/60 шт/с * 5с интервал)
            int output = (int) Math.max(1, Math.round(speed / 60.0 * 5.0));
            dto.setOutputCount(output);
        } else {
            dto.setLineSpeed(BigDecimal.ZERO);
            dto.setTemperature(BigDecimal.valueOf((minTemp + maxTemp) / 2)
                    .setScale(2, RoundingMode.HALF_UP));
            dto.setOutputCount(0);
        }

        return dto;
    }
}
