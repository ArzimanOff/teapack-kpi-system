package com.teapack.processing.controller;

import com.teapack.processing.dto.EquipmentReadingDto;
import com.teapack.processing.dto.OperatorEventDto;
import com.teapack.processing.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
public class ProcessingController {

    private final ShiftService shiftService;

    @PostMapping("/equipment")
    public ResponseEntity<?> processEquipment(@RequestBody EquipmentReadingDto dto) {
        shiftService.processEquipmentReading(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/operator-event")
    public ResponseEntity<?> processOperatorEvent(@RequestBody OperatorEventDto dto) {
        shiftService.processOperatorEvent(dto);
        return ResponseEntity.ok().build();
    }
}