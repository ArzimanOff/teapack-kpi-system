package com.teapack.collection.controller;

import com.teapack.collection.dto.EquipmentReadingDto;
import com.teapack.collection.dto.OperatorEventDto;
import com.teapack.collection.entity.EquipmentReading;
import com.teapack.collection.service.DataCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collect")
@RequiredArgsConstructor
public class DataCollectionController {

    private final DataCollectionService dataCollectionService;

    @PostMapping("/equipment")
    public ResponseEntity<EquipmentReading> receiveEquipmentData(
            @Valid @RequestBody EquipmentReadingDto dto) {
        return ResponseEntity.ok(dataCollectionService.saveEquipmentReading(dto));
    }

    @PostMapping("/operator-event")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<?> receiveOperatorEvent(
            @Valid @RequestBody OperatorEventDto dto) {
        return ResponseEntity.ok(dataCollectionService.saveOperatorEvent(dto));
    }

    @GetMapping("/readings/invalid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EquipmentReading>> findInvalidReadings(
            @RequestParam(required = false) String lineId,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(dataCollectionService.findInvalidReadings(lineId, limit));
    }
}
