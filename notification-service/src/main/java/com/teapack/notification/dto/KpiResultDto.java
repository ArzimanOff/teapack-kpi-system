package com.teapack.notification.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class KpiResultDto {
    private Long shiftId;
    private String lineId;
    private BigDecimal oee;
    private BigDecimal availability;
    private BigDecimal performance;
    private BigDecimal quality;
    private BigDecimal scrapRate;
    private BigDecimal downtimeRate;
    private Integer numberOfStops;
}