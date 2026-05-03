package com.teapack.emulator.controller;

import com.teapack.emulator.service.EmulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emulator")
@RequiredArgsConstructor
public class EmulatorController {

    private final EmulatorService emulatorService;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam Long shiftId) {
        emulatorService.start(shiftId);
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "shiftId", shiftId
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop() {
        emulatorService.stop();
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    @PostMapping("/status")
    public ResponseEntity<?> setStatus(@RequestParam String status) {
        emulatorService.setStatus(status);
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/state")
    public ResponseEntity<?> getState() {
        return ResponseEntity.ok(Map.of(
                "active", emulatorService.isActive(),
                "status", emulatorService.getStatus(),
                "shiftId", emulatorService.getCurrentShiftId() != null
                        ? emulatorService.getCurrentShiftId() : "none"
        ));
    }
}