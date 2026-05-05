package com.teapack.emulator.controller;

import com.teapack.emulator.service.EmulatorRunState;
import com.teapack.emulator.service.EmulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/emulator")
@RequiredArgsConstructor
public class EmulatorController {

    private final EmulatorService emulatorService;

    @PostMapping("/start")
    public ResponseEntity<EmulatorRunState> start(@RequestParam Long shiftId,
                                                  @RequestParam(required = false) String lineId) {
        return ResponseEntity.ok(emulatorService.start(shiftId, lineId));
    }

    /**
     * Останавливает один конкретный run по shiftId.
     * Если shiftId не указан — останавливает все запущенные (для обратной
     * совместимости со старым фронтом, который дёргал /stop без параметров).
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam(required = false) Long shiftId) {
        if (shiftId != null) {
            EmulatorRunState run = emulatorService.stop(shiftId);
            return ResponseEntity.ok(Map.of(
                    "status", "stopped",
                    "shiftId", shiftId,
                    "found", run != null
            ));
        }
        emulatorService.getRuns().forEach(r -> emulatorService.stop(r.getShiftId()));
        return ResponseEntity.ok(Map.of("status", "stopped-all"));
    }

    @DeleteMapping("/{shiftId}")
    public ResponseEntity<?> remove(@PathVariable Long shiftId) {
        boolean removed = emulatorService.remove(shiftId);
        return ResponseEntity.ok(Map.of("removed", removed));
    }

    @PostMapping("/{shiftId}/scenario")
    public ResponseEntity<EmulatorRunState> applyScenario(
            @PathVariable Long shiftId,
            @RequestParam String scenario,
            @RequestParam(defaultValue = "5") int ticks
    ) {
        return ResponseEntity.ok(emulatorService.applyScenario(shiftId, scenario, ticks));
    }

    @GetMapping("/runs")
    public ResponseEntity<Collection<EmulatorRunState>> runs() {
        return ResponseEntity.ok(emulatorService.getRuns());
    }

    @GetMapping("/state")
    public ResponseEntity<?> state(@RequestParam(required = false) Long shiftId) {
        if (shiftId != null) {
            EmulatorRunState run = emulatorService.getRun(shiftId);
            if (run == null) return ResponseEntity.ok(Map.of("active", false));
            return ResponseEntity.ok(run);
        }
        Collection<EmulatorRunState> all = emulatorService.getRuns();
        return ResponseEntity.ok(Map.of(
                "totalRuns", all.size(),
                "running", all.stream().filter(r -> "RUNNING".equals(r.getStatus())).count(),
                "runs", all
        ));
    }
}
