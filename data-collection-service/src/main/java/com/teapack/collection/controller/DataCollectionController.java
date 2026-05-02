package com.teapack.collection.controller;

import com.teapack.collection.dto.EquipmentReadingDto;
import com.teapack.collection.dto.OperatorEventDto;
import com.teapack.collection.service.DataCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collect")
@RequiredArgsConstructor
public class DataCollectionController {

    private final DataCollectionService dataCollectionService;

    @PostMapping("/equipment")
    public ResponseEntity<?> receiveEquipmentData(
            @Valid @RequestBody EquipmentReadingDto dto) {
        var result = dataCollectionService.saveEquipmentReading(dto);
        if (result == null) {
            return ResponseEntity.badRequest().body("Invalid data rejected");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/operator-event")
    public ResponseEntity<?> receiveOperatorEvent(
            @Valid @RequestBody OperatorEventDto dto) {
        var result = dataCollectionService.saveOperatorEvent(dto);
        return ResponseEntity.ok(result);
    }
}