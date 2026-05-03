package com.teapack.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports", schema = "reporting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "line_id", nullable = false)
    private String lineId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    private BigDecimal oee;
    private BigDecimal availability;
    private BigDecimal performance;
    private BigDecimal quality;

    @Column(name = "total_output")
    private Integer totalOutput;

    @Column(name = "scrap_count")
    private Integer scrapCount;

    private BigDecimal downtime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "GENERATED";
        if (this.type == null) this.type = "SHIFT";
    }
}