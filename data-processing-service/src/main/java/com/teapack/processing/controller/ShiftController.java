package com.teapack.processing.controller;

import com.teapack.processing.dto.CreateShiftRequest;
import com.teapack.processing.dto.ShiftDataDto;
import com.teapack.processing.dto.ShiftFilterRequest;
import com.teapack.processing.entity.DowntimeEvent;
import com.teapack.processing.entity.Shift;
import com.teapack.processing.entity.ShiftAggregate;
import com.teapack.processing.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Shift> cancelShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.cancelPlannedShift(shiftId));
    }

    @GetMapping
    public ResponseEntity<List<Shift>> findShifts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        ShiftFilterRequest filter = new ShiftFilterRequest();
        filter.setStatus(status);
        filter.setLineId(lineId);
        filter.setOperatorId(operatorId);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        return ResponseEntity.ok(shiftService.findShifts(filter));
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
