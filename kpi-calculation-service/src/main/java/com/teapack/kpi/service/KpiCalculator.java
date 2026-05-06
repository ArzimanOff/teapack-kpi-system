package com.teapack.kpi.service;

import com.teapack.kpi.dto.KpiResultDto;
import com.teapack.kpi.dto.ShiftDataDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Component
public class KpiCalculator {

    private static final int SCALE = 4;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    public KpiResultDto calculate(ShiftDataDto data) {
        KpiResultDto result = new KpiResultDto();
        result.setShiftId(data.getShiftId());
        result.setLineId(data.getLineId());

        // --- Исходные данные ---
        BigDecimal plannedTime = calcPlannedTime(data);
        BigDecimal downtime = safe(data.getDowntimeMinutes());
        BigDecimal operatingTime = plannedTime.subtract(downtime).max(BigDecimal.ZERO);
        Integer totalOutput = safe(data.getTotalOutput());
        Integer goodOutput = safe(data.getGoodOutput());
        Integer scrapCount = safe(data.getScrapCount());
        Integer plannedOutput = safe(data.getPlannedOutput());
        BigDecimal nominalSpeed = safe(data.getNominalSpeed());
        BigDecimal avgSpeed = safe(data.getAvgSpeed());
        Integer numberOfStops = safe(data.getNumberOfStops());

        // --- Метрики времени ---
        result.setPlannedTime(plannedTime);
        result.setOperatingTime(operatingTime);
        result.setDowntime(downtime);
        result.setNumberOfStops(numberOfStops);

        // DowntimeRate = Downtime / PlannedTime
        result.setDowntimeRate(divideOrZero(downtime, plannedTime));

        // AvgDowntime = Downtime / NumberOfStops
        result.setAvgDowntime(numberOfStops > 0
                ? downtime.divide(BigDecimal.valueOf(numberOfStops), SCALE, RM)
                : BigDecimal.ZERO);

        // --- Метрики производительности ---
        result.setTotalOutput(totalOutput);
        result.setGoodOutput(goodOutput);
        result.setScrapCount(scrapCount);
        result.setPlannedOutput(plannedOutput);
        result.setNominalSpeed(nominalSpeed);
        result.setAvgSpeed(avgSpeed);

        // OutputRate = TotalOutput / OperatingTime  (шт/мин фактически)
        BigDecimal outputRate = divideOrZero(
                BigDecimal.valueOf(totalOutput), operatingTime);
        result.setOutputRate(outputRate);

        // SpeedLoss = NominalSpeed - AvgSpeed  (шт/мин потерь скорости)
        result.setSpeedLoss(nominalSpeed.subtract(avgSpeed).max(BigDecimal.ZERO));

        // --- Метрики качества ---
        // ScrapRate = Scrap / TotalOutput
        result.setScrapRate(totalOutput > 0
                ? divideOrZero(BigDecimal.valueOf(scrapCount), BigDecimal.valueOf(totalOutput))
                : BigDecimal.ZERO);

        // --- Базовые KPI ---
        // Availability = OperatingTime / PlannedTime  ∈ [0,1]
        BigDecimal availability = divideOrZero(operatingTime, plannedTime).min(BigDecimal.ONE);
        result.setAvailability(availability);

        // Performance = TotalOutput / (NominalSpeed × OperatingTime)
        // — отношение фактического выпуска к теоретическому максимуму
        // на ВРЕМЕНИ РАБОТЫ (без простоев). Соответствует классической ОЕЕ-методике:
        // Availability покрывает простои, Performance — потери скорости при работе.
        // Без max(1) — наоборот, capped to 1 (если из-за округления пакетов > 100%).
        BigDecimal theoreticalMax = nominalSpeed.multiply(operatingTime);
        BigDecimal performance = theoreticalMax.compareTo(BigDecimal.ZERO) > 0
                ? divideOrZero(BigDecimal.valueOf(totalOutput), theoreticalMax).min(BigDecimal.ONE)
                : BigDecimal.ZERO;
        result.setPerformance(performance);

        // Quality = GoodOutput / TotalOutput
        BigDecimal quality = totalOutput > 0
                ? divideOrZero(BigDecimal.valueOf(goodOutput), BigDecimal.valueOf(totalOutput))
                : BigDecimal.ZERO;
        result.setQuality(quality);

        // OEE = A × P × Q
        BigDecimal oee = availability.multiply(performance).multiply(quality)
                .setScale(SCALE, RM);
        result.setOee(oee);

        // --- Производные ---
        // PerformanceLoss = 1 - Performance (доля потерянного выпуска от теор. максимума)
        result.setPerformanceLoss(BigDecimal.ONE.subtract(performance).max(BigDecimal.ZERO));

        // PlanFulfillment = TotalOutput / PlannedOutput
        // (отдельная метрика «выполнение плана» — НЕ Performance)
        result.setPlanFulfillment(plannedOutput > 0
                ? divideOrZero(BigDecimal.valueOf(totalOutput), BigDecimal.valueOf(plannedOutput))
                : BigDecimal.ZERO);

        return result;
    }

    private BigDecimal calcPlannedTime(ShiftDataDto data) {
        if (data.getPlannedStart() != null && data.getPlannedEnd() != null) {
            long seconds = Duration.between(data.getPlannedStart(), data.getPlannedEnd()).getSeconds();
            return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(60), SCALE, RM);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal divideOrZero(BigDecimal a, BigDecimal b) {
        if (b == null || b.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return a.divide(b, SCALE, RM);
    }

    private BigDecimal safe(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private Integer safe(Integer v) { return v != null ? v : 0; }
}