package com.teapack.kpi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShiftSummaryDto {
    private Long id;
    private String lineId;
    private Long operatorId;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private Integer plannedOutput;
    private String status;
}
