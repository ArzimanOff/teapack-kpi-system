package com.teapack.processing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_aggregates", schema = "processing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false, unique = true)
    private Long shiftId;

    @Column(name = "total_output")
    private Integer totalOutput;

    @Column(name = "good_output")
    private Integer goodOutput;

    @Column(name = "scrap_count")
    private Integer scrapCount;

    @Column(name = "downtime_minutes")
    private BigDecimal downtimeMinutes;

    @Column(name = "avg_speed")
    private BigDecimal avgSpeed;

    @Column(name = "avg_temperature")
    private BigDecimal avgTemperature;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}