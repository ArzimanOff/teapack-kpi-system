package com.teapack.notification.service;

import com.teapack.notification.dto.KpiResultDto;
import com.teapack.notification.entity.KpiThresholds;
import com.teapack.notification.entity.Notification;
import com.teapack.notification.entity.NotificationRead;
import com.teapack.notification.repository.NotificationReadRepository;
import com.teapack.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository readRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KpiThresholdsService thresholdsService;

    @Transactional
    public List<Notification> checkAndNotify(KpiResultDto kpi) {
        List<Notification> notifications = new ArrayList<>();
        KpiThresholds t = thresholdsService.get();
        BigDecimal oeeMin = t.getOeeMin();
        BigDecimal availabilityMin = t.getAvailabilityMin();
        BigDecimal performanceMin = t.getPerformanceMin();
        BigDecimal qualityMin = t.getQualityMin();

        if (kpi.getOee() != null && kpi.getOee().compareTo(oeeMin) < 0) {
            notifications.add(createNotification(kpi, "OEE_LOW", "OEE",
                    kpi.getOee(), oeeMin,
                    String.format("OEE %.1f%% ниже порогового значения %.1f%%",
                            kpi.getOee().multiply(BigDecimal.valueOf(100)),
                            oeeMin.multiply(BigDecimal.valueOf(100)))));
        }

        if (kpi.getAvailability() != null && kpi.getAvailability().compareTo(availabilityMin) < 0) {
            notifications.add(createNotification(kpi, "AVAILABILITY_LOW", "Availability",
                    kpi.getAvailability(), availabilityMin,
                    String.format("Доступность %.1f%% ниже порогового значения %.1f%%",
                            kpi.getAvailability().multiply(BigDecimal.valueOf(100)),
                            availabilityMin.multiply(BigDecimal.valueOf(100)))));
        }

        if (kpi.getPerformance() != null && kpi.getPerformance().compareTo(performanceMin) < 0) {
            notifications.add(createNotification(kpi, "PERFORMANCE_LOW", "Performance",
                    kpi.getPerformance(), performanceMin,
                    String.format("Производительность %.1f%% ниже порогового значения %.1f%%",
                            kpi.getPerformance().multiply(BigDecimal.valueOf(100)),
                            performanceMin.multiply(BigDecimal.valueOf(100)))));
        }

        if (kpi.getQuality() != null && kpi.getQuality().compareTo(qualityMin) < 0) {
            notifications.add(createNotification(kpi, "QUALITY_LOW", "Quality",
                    kpi.getQuality(), qualityMin,
                    String.format("Качество %.1f%% ниже порогового значения %.1f%%",
                            kpi.getQuality().multiply(BigDecimal.valueOf(100)),
                            qualityMin.multiply(BigDecimal.valueOf(100)))));
        }

        if (!notifications.isEmpty()) {
            List<Notification> saved = notificationRepository.saveAll(notifications);
            log.info("Created {} notifications for shift: {}", saved.size(), kpi.getShiftId());
            try {
                messagingTemplate.convertAndSend("/topic/notifications", saved);
            } catch (Exception e) {
                log.warn("Failed to push notifications via WS: {}", e.getMessage());
            }
            return saved;
        }

        return notifications;
    }

    @Transactional
    public int markAllRead(String username) {
        int updated = readRepository.markAllReadForUser(username);
        try {
            messagingTemplate.convertAndSend(
                    "/topic/notifications/read-all/" + username, updated);
        } catch (Exception ignored) {}
        return updated;
    }

    public long countUnread(String username) {
        return notificationRepository.countUnreadForUser(username);
    }

    public List<Notification> getUnread(String username) {
        return notificationRepository.findUnreadForUser(username);
    }

    public List<Notification> getByShift(Long shiftId) {
        return notificationRepository.findByShiftIdOrderByCreatedAtDesc(shiftId);
    }

    @Transactional
    public void markAsRead(Long id, String username) {
        if (!notificationRepository.existsById(id)) return;
        if (readRepository.existsByNotificationIdAndUsername(id, username)) return;
        readRepository.save(NotificationRead.builder()
                .notificationId(id)
                .username(username)
                .readAt(LocalDateTime.now())
                .build());
    }

    private Notification createNotification(KpiResultDto kpi, String type,
                                            String metricName, BigDecimal value, BigDecimal threshold, String message) {
        return Notification.builder()
                .shiftId(kpi.getShiftId())
                .lineId(kpi.getLineId())
                .type(type)
                .severity("WARNING")
                .message(message)
                .metricName(metricName)
                .metricValue(value)
                .threshold(threshold)
                .build();
    }
}
