package com.teapack.collection.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_readings", schema = "collection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_id", nullable = false)
    private String lineId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private BigDecimal temperature;

    @Column(name = "line_speed")
    private BigDecimal lineSpeed;

    @Column(nullable = false)
    private String status;

    @Column(name = "output_count")
    private Integer outputCount;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid;

    @Column(name = "validation_note")
    private String validationNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.isValid == null) this.isValid = true;
    }
}
