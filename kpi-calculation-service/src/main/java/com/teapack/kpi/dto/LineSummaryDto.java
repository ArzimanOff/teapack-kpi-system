package com.teapack.kpi.dto;

import com.teapack.kpi.entity.ShiftKpi;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LineSummaryDto {
    private String lineId;
    private long shifts;
    private BigDecimal avgOee;
    private BigDecimal avgAvailability;
    private BigDecimal avgPerformance;
    private BigDecimal avgQuality;
    private long totalOutput;
    private long goodOutput;
    private long scrapCount;
    private BigDecimal totalDowntime;
    private long totalStops;
    private LocalDateTime firstCalculatedAt;
    private LocalDateTime lastCalculatedAt;

    private List<ShiftKpi> recent;
}
