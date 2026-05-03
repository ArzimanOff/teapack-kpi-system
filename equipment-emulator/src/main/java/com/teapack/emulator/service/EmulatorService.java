package com.teapack.emulator.service;

import com.teapack.emulator.client.DataCollectionClient;
import com.teapack.emulator.dto.EquipmentReadingDto;
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

    @Value("${emulator.line-id}")
    private String lineId;

    @Value("${emulator.nominal-speed}")
    private double nominalSpeed;

    @Value("${emulator.temperature-base}")
    private double temperatureBase;

    private final Random random = new Random();

    // Текущее состояние эмулятора
    private boolean active = false;
    private Long currentShiftId = null;
    private String currentStatus = "STOPPED";

    // Запускается каждые 5 секунд, но отправляет только если active = true
    @Scheduled(fixedDelayString = "${emulator.interval-ms}")
    public void emulate() {
        if (!active || currentShiftId == null) return;

        EquipmentReadingDto dto = generateReading();
        try {
            dataCollectionClient.sendReading(dto);
            log.debug("Sent reading: status={}, speed={}, output={}",
                    dto.getStatus(), dto.getLineSpeed(), dto.getOutputCount());
        } catch (Exception e) {
            log.error("Failed to send reading: {}", e.getMessage());
        }
    }

    public void start(Long shiftId) {
        this.currentShiftId = shiftId;
        this.active = true;
        this.currentStatus = "RUNNING";
        log.info("Emulator started for shift: {}", shiftId);
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

    private EquipmentReadingDto generateReading() {
        EquipmentReadingDto dto = new EquipmentReadingDto();
        dto.setLineId(lineId);
        dto.setTimestamp(LocalDateTime.now());
        dto.setShiftId(currentShiftId);
        dto.setStatus(currentStatus);

        if ("RUNNING".equals(currentStatus)) {
            // Скорость: номинал ± 10%
            double speedVariation = nominalSpeed * 0.1;
            double speed = nominalSpeed - speedVariation + random.nextDouble() * speedVariation * 2;
            dto.setLineSpeed(BigDecimal.valueOf(speed).setScale(2, RoundingMode.HALF_UP));

            // Температура: база ± 5 градусов
            double temp = temperatureBase + (random.nextDouble() * 10 - 5);
            dto.setTemperature(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP));

            // Выпуск: ~2-3 единицы за 5 секунд
            dto.setOutputCount(2 + random.nextInt(2));
        } else {
            dto.setLineSpeed(BigDecimal.ZERO);
            dto.setTemperature(BigDecimal.valueOf(temperatureBase)
                    .setScale(2, RoundingMode.HALF_UP));
            dto.setOutputCount(0);
        }

        return dto;
    }
}