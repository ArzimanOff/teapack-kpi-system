package com.teapack.kpi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_kpi", schema = "kpi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false, unique = true)
    private Long shiftId;

    @Column(name = "line_id", nullable = false)
    private String lineId;

    // Время (в минутах)
    @Column(name = "planned_time")
    private BigDecimal plannedTime;

    @Column(name = "operating_time")
    private BigDecimal operatingTime;

    @Column(name = "downtime")
    private BigDecimal downtime;

    @Column(name = "downtime_rate")
    private BigDecimal downtimeRate;

    @Column(name = "number_of_stops")
    private Integer numberOfStops;

    @Column(name = "avg_downtime")
    private BigDecimal avgDowntime;

    // Выпуск
    @Column(name = "total_output")
    private Integer totalOutput;

    @Column(name = "good_output")
    private Integer goodOutput;

    @Column(name = "scrap_count")
    private Integer scrapCount;

    @Column(name = "planned_output")
    private Integer plannedOutput;

    // Скорость
    @Column(name = "nominal_speed")
    private BigDecimal nominalSpeed;

    @Column(name = "avg_speed")
    private BigDecimal avgSpeed;

    @Column(name = "output_rate")
    private BigDecimal outputRate;

    @Column(name = "speed_loss")
    private BigDecimal speedLoss;

    // Базовые KPI
    @Column(name = "availability")
    private BigDecimal availability;

    @Column(name = "performance")
    private BigDecimal performance;

    @Column(name = "quality")
    private BigDecimal quality;

    @Column(name = "oee")
    private BigDecimal oee;

    // Производные
    @Column(name = "performance_loss")
    private BigDecimal performanceLoss;

    @Column(name = "plan_fulfillment")
    private BigDecimal planFulfillment;

    @Column(name = "scrap_rate")
    private BigDecimal scrapRate;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @PrePersist
    public void prePersist() {
        this.calculatedAt = LocalDateTime.now();
    }
}