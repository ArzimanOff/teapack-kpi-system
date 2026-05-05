package com.teapack.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kpi_thresholds", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiThresholds {

    @Id
    private Long id;

    @Column(name = "oee_min", nullable = false)
    private BigDecimal oeeMin;

    @Column(name = "availability_min", nullable = false)
    private BigDecimal availabilityMin;

    @Column(name = "performance_min", nullable = false)
    private BigDecimal performanceMin;

    @Column(name = "quality_min", nullable = false)
    private BigDecimal qualityMin;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
