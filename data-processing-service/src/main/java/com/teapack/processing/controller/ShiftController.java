package com.teapack.processing.controller;

import com.teapack.processing.dto.CreateShiftRequest;
import com.teapack.processing.entity.Shift;
import com.teapack.processing.entity.ShiftAggregate;
import com.teapack.processing.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<Shift> createShift(@Valid @RequestBody CreateShiftRequest request) {
        return ResponseEntity.ok(shiftService.createShift(request));
    }

    @PostMapping("/{shiftId}/start")
    public ResponseEntity<Shift> startShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.startShift(shiftId));
    }

    @PostMapping("/{shiftId}/close")
    public ResponseEntity<Shift> closeShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.closeShift(shiftId));
    }

    @GetMapping("/{shiftId}")
    public ResponseEntity<Shift> getShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.getShiftOrThrow(shiftId));
    }

    @GetMapping("/{shiftId}/aggregate")
    public ResponseEntity<ShiftAggregate> getAggregate(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.getShiftAggregate(shiftId));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<List<Shift>> getShiftsByLine(@PathVariable String lineId) {
        return ResponseEntity.ok(shiftService.getShiftsByLine(lineId));
    }
}