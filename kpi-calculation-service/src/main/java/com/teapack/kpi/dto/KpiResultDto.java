package com.teapack.kpi.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class KpiResultDto {
    private Long shiftId;
    private String lineId;

    private BigDecimal plannedTime;
    private BigDecimal operatingTime;
    private BigDecimal downtime;
    private BigDecimal downtimeRate;
    private Integer numberOfStops;
    private BigDecimal avgDowntime;

    private Integer totalOutput;
    private Integer goodOutput;
    private Integer scrapCount;
    private Integer plannedOutput;

    private BigDecimal nominalSpeed;
    private BigDecimal avgSpeed;
    private BigDecimal outputRate;
    private BigDecimal speedLoss;

    private BigDecimal availability;
    private BigDecimal performance;
    private BigDecimal quality;
    private BigDecimal oee;

    private BigDecimal performanceLoss;
    private BigDecimal planFulfillment;
    private BigDecimal scrapRate;
}