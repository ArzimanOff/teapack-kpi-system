package com.teapack.kpi.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShiftDataDto {
    private Long shiftId;
    private String lineId;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private Integer plannedOutput;
    private BigDecimal nominalSpeed;
    private Integer totalOutput;
    private Integer goodOutput;
    private Integer scrapCount;
    private BigDecimal downtimeMinutes;
    private BigDecimal avgSpeed;
    private Integer numberOfStops;
}