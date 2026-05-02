package com.teapack.processing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShiftDto {
    private Long id;
    private String lineId;
    private Long operatorId;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private Integer plannedOutput;
    private BigDecimal nominalSpeed;
    private String status;
}