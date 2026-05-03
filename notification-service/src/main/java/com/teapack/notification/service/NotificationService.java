package com.teapack.notification.service;

import com.teapack.notification.dto.KpiResultDto;
import com.teapack.notification.entity.Notification;
import com.teapack.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Value("${kpi.thresholds.oee-min}")
    private BigDecimal oeeMin;

    @Value("${kpi.thresholds.availability-min}")
    private BigDecimal availabilityMin;

    @Value("${kpi.thresholds.performance-min}")
    private BigDecimal performanceMin;

    @Value("${kpi.thresholds.quality-min}")
    private BigDecimal qualityMin;

    @Transactional
    public List<Notification> checkAndNotify(KpiResultDto kpi) {
        List<Notification> notifications = new ArrayList<>();

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
            notificationRepository.saveAll(notifications);
            log.info("Created {} notifications for shift: {}", notifications.size(), kpi.getShiftId());
        }

        return notifications;
    }

    public List<Notification> getUnread() {
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    public List<Notification> getByShift(Long shiftId) {
        return notificationRepository.findByShiftIdOrderByCreatedAtDesc(shiftId);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
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