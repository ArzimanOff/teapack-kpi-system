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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectionService {

    private final EquipmentReadingRepository equipmentReadingRepository;
    private final OperatorEventRepository operatorEventRepository;
    private final DataProcessingClient dataProcessingClient;
    private final ValidationService validationService;

    @Transactional
    public EquipmentReading saveEquipmentReading(EquipmentReadingDto dto) {
        ValidationService.ValidationResult validation = validationService.validate(dto);

        EquipmentReading entity = EquipmentReading.builder()
                .lineId(dto.getLineId())
                .timestamp(dto.getTimestamp())
                .temperature(dto.getTemperature())
                .lineSpeed(dto.getLineSpeed())
                .status(dto.getStatus())
                .outputCount(dto.getOutputCount() != null ? dto.getOutputCount() : 0)
                .shiftId(dto.getShiftId())
                .isValid(validation.isValid())
                .validationNote(validation.getNote())
                .build();

        entity = equipmentReadingRepository.save(entity);

        if (!validation.isValid()) {
            log.warn("Outlier reading flagged for line={}: {}", dto.getLineId(), validation.getNote());
            // Невалидные показания не доходят до агрегации в processing-service
            return entity;
        }

        log.debug("Saved valid reading: lineId={}, status={}", dto.getLineId(), dto.getStatus());

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

        try {
            dataProcessingClient.sendOperatorEvent(dto);
        } catch (Exception e) {
            log.error("Failed to send operator event to processing service: {}", e.getMessage());
        }

        return entity;
    }

    public List<EquipmentReading> findInvalidReadings(String lineId, int limit) {
        var pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 500)));
        if (lineId != null && !lineId.isBlank()) {
            return equipmentReadingRepository.findByLineIdAndIsValidFalseOrderByTimestampDesc(lineId, pageable);
        }
        return equipmentReadingRepository.findByIsValidFalseOrderByTimestampDesc(pageable);
    }
}
