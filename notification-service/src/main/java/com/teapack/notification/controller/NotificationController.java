package com.teapack.notification.controller;

import com.teapack.notification.dto.KpiResultDto;
import com.teapack.notification.entity.Notification;
import com.teapack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/check")
    public ResponseEntity<List<Notification>> check(@RequestBody KpiResultDto kpi) {
        return ResponseEntity.ok(notificationService.checkAndNotify(kpi));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread() {
        return ResponseEntity.ok(notificationService.getUnread());
    }

    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<List<Notification>> getByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(notificationService.getByShift(shiftId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}