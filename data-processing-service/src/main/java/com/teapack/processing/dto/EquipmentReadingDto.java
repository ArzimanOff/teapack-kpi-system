package com.teapack.processing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EquipmentReadingDto {
    private String lineId;
    private LocalDateTime timestamp;
    private BigDecimal temperature;
    private BigDecimal lineSpeed;
    private String status;
    private Integer outputCount;
    private Long shiftId;
}