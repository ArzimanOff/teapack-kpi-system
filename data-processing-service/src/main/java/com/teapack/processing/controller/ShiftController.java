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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<Shift> createShift(@Valid @RequestBody CreateShiftRequest request) {
        return ResponseEntity.ok(shiftService.createShift(request));
    }

    @PostMapping("/{shiftId}/start")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<Shift> startShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.startShift(shiftId));
    }

    @PostMapping("/{shiftId}/close")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<Shift> closeShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.closeShift(shiftId));
    }

    @DeleteMapping("/{shiftId}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<Shift> cancelShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.cancelPlannedShift(shiftId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<Page<Shift>> findShifts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ShiftFilterRequest filter = new ShiftFilterRequest();
        filter.setStatus(status);
        filter.setLineId(lineId);
        filter.setOperatorId(operatorId);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        return ResponseEntity.ok(shiftService.findShiftsPaged(filter, pageable));
    }

    @GetMapping("/{shiftId}/data")
    public ResponseEntity<ShiftDataDto> getShiftData(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.buildShiftData(shiftId));
    }

    @GetMapping("/{shiftId}")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<Shift> getShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.getShiftOrThrow(shiftId));
    }

    @GetMapping("/{shiftId}/aggregate")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<ShiftAggregate> getAggregate(@PathVariable Long shiftId) {
        return ResponseEntity.ok(shiftService.getShiftAggregate(shiftId));
    }

    @GetMapping("/line/{lineId}")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<List<Shift>> getShiftsByLine(@PathVariable String lineId) {
        return ResponseEntity.ok(shiftService.getShiftsByLine(lineId));
    }
}
