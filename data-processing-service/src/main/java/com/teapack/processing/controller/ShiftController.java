package com.teapack.processing.controller;

import com.teapack.processing.dto.CreateShiftRequest;
import com.teapack.processing.dto.ShiftDataDto;
import com.teapack.processing.entity.DowntimeEvent;
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

    @GetMapping("/{shiftId}/data")
    public ResponseEntity<ShiftDataDto> getShiftData(@PathVariable Long shiftId) {
        Shift shift = shiftService.getShiftOrThrow(shiftId);
        ShiftAggregate aggregate = shiftService.getShiftAggregate(shiftId);
        List<DowntimeEvent> downtimes = shiftService.getDowntimeEvents(shiftId);

        ShiftDataDto dto = new ShiftDataDto();
        dto.setShiftId(shift.getId());
        dto.setLineId(shift.getLineId());
        dto.setPlannedStart(shift.getPlannedStart());
        dto.setPlannedEnd(shift.getPlannedEnd());
        dto.setActualStart(shift.getActualStart());
        dto.setActualEnd(shift.getActualEnd());
        dto.setPlannedOutput(shift.getPlannedOutput());
        dto.setNominalSpeed(shift.getNominalSpeed());
        dto.setTotalOutput(aggregate.getTotalOutput());
        dto.setGoodOutput(aggregate.getGoodOutput());
        dto.setScrapCount(aggregate.getScrapCount());
        dto.setDowntimeMinutes(aggregate.getDowntimeMinutes());
        dto.setAvgSpeed(aggregate.getAvgSpeed());
        dto.setNumberOfStops(downtimes.size());
        return ResponseEntity.ok(dto);
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