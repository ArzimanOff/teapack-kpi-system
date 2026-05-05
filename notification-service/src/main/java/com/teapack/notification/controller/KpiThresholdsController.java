package com.teapack.notification.controller;

import com.teapack.notification.dto.KpiThresholdsDto;
import com.teapack.notification.entity.KpiThresholds;
import com.teapack.notification.service.KpiThresholdsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thresholds")
@RequiredArgsConstructor
public class KpiThresholdsController {

    private final KpiThresholdsService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<KpiThresholds> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TECHNOLOGIST','ADMIN')")
    public ResponseEntity<KpiThresholds> update(@Valid @RequestBody KpiThresholdsDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
