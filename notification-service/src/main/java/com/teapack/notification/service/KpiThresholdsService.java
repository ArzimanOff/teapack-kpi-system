package com.teapack.notification.service;

import com.teapack.notification.dto.KpiThresholdsDto;
import com.teapack.notification.entity.KpiThresholds;
import com.teapack.notification.repository.KpiThresholdsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KpiThresholdsService {

    private static final long ROW_ID = 1L;

    private final KpiThresholdsRepository repository;

    public KpiThresholds get() {
        return repository.findById(ROW_ID).orElseGet(() -> {
            KpiThresholds defaults = KpiThresholds.builder()
                    .id(ROW_ID)
                    .oeeMin(new BigDecimal("0.65"))
                    .availabilityMin(new BigDecimal("0.80"))
                    .performanceMin(new BigDecimal("0.75"))
                    .qualityMin(new BigDecimal("0.95"))
                    .updatedAt(LocalDateTime.now())
                    .build();
            return repository.save(defaults);
        });
    }

    @Transactional
    public KpiThresholds update(KpiThresholdsDto dto) {
        KpiThresholds t = get();
        t.setOeeMin(dto.getOeeMin());
        t.setAvailabilityMin(dto.getAvailabilityMin());
        t.setPerformanceMin(dto.getPerformanceMin());
        t.setQualityMin(dto.getQualityMin());
        t.setUpdatedAt(LocalDateTime.now());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        t.setUpdatedBy(auth != null ? auth.getName() : "system");
        return repository.save(t);
    }
}
