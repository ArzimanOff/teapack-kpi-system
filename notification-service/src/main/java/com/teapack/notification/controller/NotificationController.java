package com.teapack.notification.controller;

import com.teapack.notification.dto.KpiResultDto;
import com.teapack.notification.entity.Notification;
import com.teapack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Без @PreAuthorize: эндпоинт зовётся внутренне через Feign из kpi-calculation-service.
    @PostMapping("/check")
    public ResponseEntity<List<Notification>> check(@RequestBody KpiResultDto kpi) {
        return ResponseEntity.ok(notificationService.checkAndNotify(kpi));
    }

    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<List<Notification>> getUnread(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUnread(auth.getName()));
    }

    @GetMapping("/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<List<Notification>> getByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(notificationService.getByShift(shiftId));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication auth) {
        notificationService.markAsRead(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        int updated = notificationService.markAllRead(auth.getName());
        return ResponseEntity.ok(java.util.Map.of("updated", updated));
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<?> countUnread(Authentication auth) {
        return ResponseEntity.ok(java.util.Map.of("count", notificationService.countUnread(auth.getName())));
    }
}
