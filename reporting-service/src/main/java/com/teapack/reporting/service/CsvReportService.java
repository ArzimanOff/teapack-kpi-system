package com.teapack.reporting.service;

import com.teapack.reporting.client.KpiClient;
import com.teapack.reporting.dto.KpiResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class CsvReportService {

    private final KpiClient kpiClient;

    public byte[] renderShiftReport(Long shiftId) {
        KpiResultDto k = kpiClient.getKpiByShift(shiftId);
        StringBuilder sb = new StringBuilder();
        // BOM для корректной кириллицы в Excel
        sb.append('﻿');
        sb.append("Метрика;Значение\n");

        row(sb, "Смена ID", k.getShiftId());
        row(sb, "Линия", k.getLineId());

        section(sb, "Время");
        row(sb, "Плановое время, мин", k.getPlannedTime());
        row(sb, "Рабочее время, мин", k.getOperatingTime());
        row(sb, "Простой, мин", k.getDowntime());
        row(sb, "Доля простоя", percent(k.getDowntimeRate()));
        row(sb, "Кол-во остановок", k.getNumberOfStops());
        row(sb, "Средн. длительность остановки, мин", k.getAvgDowntime());

        section(sb, "Выпуск");
        row(sb, "Всего выпущено, шт", k.getTotalOutput());
        row(sb, "Годных, шт", k.getGoodOutput());
        row(sb, "Брак, шт", k.getScrapCount());
        row(sb, "План выпуска, шт", k.getPlannedOutput());

        section(sb, "Скорость");
        row(sb, "Номинальная скорость, шт/мин", k.getNominalSpeed());
        row(sb, "Средняя скорость, шт/мин", k.getAvgSpeed());
        row(sb, "Output Rate, шт/мин", k.getOutputRate());
        row(sb, "Speed Loss, шт/мин", k.getSpeedLoss());

        section(sb, "KPI");
        row(sb, "Availability", percent(k.getAvailability()));
        row(sb, "Performance", percent(k.getPerformance()));
        row(sb, "Quality", percent(k.getQuality()));
        row(sb, "OEE", percent(k.getOee()));

        section(sb, "Производные");
        row(sb, "Performance Loss", percent(k.getPerformanceLoss()));
        row(sb, "Plan Fulfillment", percent(k.getPlanFulfillment()));
        row(sb, "Scrap Rate", percent(k.getScrapRate()));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void section(StringBuilder sb, String title) {
        sb.append("\n=== ").append(escape(title)).append(" ===;\n");
    }

    private void row(StringBuilder sb, String name, Object value) {
        sb.append(escape(name)).append(';')
                .append(value == null ? "" : escape(String.valueOf(value)))
                .append('\n');
    }

    private String percent(BigDecimal v) {
        if (v == null) return "";
        return v.movePointRight(2).setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
