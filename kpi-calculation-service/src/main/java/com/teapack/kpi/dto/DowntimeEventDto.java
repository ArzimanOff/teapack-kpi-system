package com.teapack.kpi.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DowntimeEventDto {
    private Long id;
    private Long shiftId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private BigDecimal durationMinutes;
}
