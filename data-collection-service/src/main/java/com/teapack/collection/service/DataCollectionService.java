package com.teapack.collection.service;

import com.teapack.collection.client.DataProcessingClient;
import com.teapack.collection.dto.EquipmentReadingDto;
import com.teapack.collection.dto.OperatorEventDto;
import com.teapack.collection.entity.EquipmentReading;
import com.teapack.collection.entity.OperatorEvent;
import com.teapack.collection.repository.EquipmentReadingRepository;
import com.teapack.collection.repository.OperatorEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectionService {

    private final EquipmentReadingRepository equipmentReadingRepository;
    private final OperatorEventRepository operatorEventRepository;
    private final DataProcessingClient dataProcessingClient;

    // Допустимые диапазоны для валидации
    private static final BigDecimal TEMP_MIN = BigDecimal.valueOf(0);
    private static final BigDecimal TEMP_MAX = BigDecimal.valueOf(150);
    private static final BigDecimal SPEED_MIN = BigDecimal.valueOf(0);
    private static final BigDecimal SPEED_MAX = BigDecimal.valueOf(500);

    @Transactional
    public EquipmentReading saveEquipmentReading(EquipmentReadingDto dto) {
        // Валидация и фильтрация выбросов
        if (!validateEquipmentReading(dto)) {
            log.warn("Invalid equipment reading rejected: {}", dto);
            return null;
        }

        EquipmentReading entity = EquipmentReading.builder()
                .lineId(dto.getLineId())
                .timestamp(dto.getTimestamp())
                .temperature(dto.getTemperature())
                .lineSpeed(dto.getLineSpeed())
                .status(dto.getStatus())
                .outputCount(dto.getOutputCount() != null ? dto.getOutputCount() : 0)
                .shiftId(dto.getShiftId())
                .build();

        entity = equipmentReadingRepository.save(entity);
        log.debug("Saved equipment reading: lineId={}, status={}", dto.getLineId(), dto.getStatus());

        // Отправляем в data-processing-service асинхронно
        try {
            dataProcessingClient.sendEquipmentReading(dto);
        } catch (Exception e) {
            log.error("Failed to send equipment reading to processing service: {}", e.getMessage());
        }

        return entity;
    }

    @Transactional
    public OperatorEvent saveOperatorEvent(OperatorEventDto dto) {
        OperatorEvent entity = OperatorEvent.builder()
                .shiftId(dto.getShiftId())
                .lineId(dto.getLineId())
                .timestamp(dto.getTimestamp())
                .eventType(dto.getEventType())
                .reason(dto.getReason())
                .scrapCount(dto.getScrapCount() != null ? dto.getScrapCount() : 0)
                .comment(dto.getComment())
                .operatorId(dto.getOperatorId())
                .build();

        entity = operatorEventRepository.save(entity);
        log.debug("Saved operator event: shiftId={}, type={}", dto.getShiftId(), dto.getEventType());

        // Отправляем в data-processing-service
        try {
            dataProcessingClient.sendOperatorEvent(dto);
        } catch (Exception e) {
            log.error("Failed to send operator event to processing service: {}", e.getMessage());
        }

        return entity;
    }

    private boolean validateEquipmentReading(EquipmentReadingDto dto) {
        if (dto.getTemperature() != null) {
            if (dto.getTemperature().compareTo(TEMP_MIN) < 0 ||
                dto.getTemperature().compareTo(TEMP_MAX) > 0) {
                log.warn("Temperature out of range: {}", dto.getTemperature());
                return false;
            }
        }
        if (dto.getLineSpeed() != null) {
            if (dto.getLineSpeed().compareTo(SPEED_MIN) < 0 ||
                dto.getLineSpeed().compareTo(SPEED_MAX) > 0) {
                log.warn("Line speed out of range: {}", dto.getLineSpeed());
                return false;
            }
        }
        return true;
    }
}