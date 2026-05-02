package com.teapack.collection.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operator_events", schema = "collection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "line_id", nullable = false)
    private String lineId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    private String reason;

    @Column(name = "scrap_count")
    private Integer scrapCount;

    private String comment;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}