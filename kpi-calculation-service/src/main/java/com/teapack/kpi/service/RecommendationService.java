package com.teapack.kpi.service;

import com.teapack.kpi.client.DataProcessingClient;
import com.teapack.kpi.dto.DowntimeEventDto;
import com.teapack.kpi.dto.RecommendationDto;
import com.teapack.kpi.dto.ShiftDataDto;
import com.teapack.kpi.dto.ShiftSummaryDto;
import com.teapack.kpi.entity.ShiftKpi;
import com.teapack.kpi.repository.ShiftKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based движок рекомендаций.
 *
 * Состояние не хранится: рекомендации вычисляются по запросу из живых данных
 * (текущие активные смены, история KPI). Это упрощает архитектуру — нет
 * фоновой задачи и таблицы рекомендаций; всё прозрачно для защиты ВКР.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ShiftKpiRepository kpiRepository;
    private final DataProcessingClient processingClient;

    // ============ Пороговые значения ============
    // Оператор:
    private static final double OP_SPEED_DROP_RATIO = 0.6;        // avgSpeed < 60% от номинала → WARN
    private static final double OP_SPEED_DROP_RATIO_CRIT = 0.4;   // < 40% → CRITICAL
    private static final double OP_SCRAP_RATE_WARN = 0.05;
    private static final double OP_SCRAP_RATE_CRIT = 0.10;
    private static final double OP_OPEN_DOWNTIME_WARN_MIN = 15.0;
    private static final double OP_OPEN_DOWNTIME_CRIT_MIN = 30.0;
    private static final int OP_STOPS_WARN = 5;
    private static final int OP_STOPS_CRIT = 10;
    private static final double OP_PLAN_LAG_WARN = 0.15; // отставание ≥ 15 п.п. от ожидаемого прогресса

    // Технолог (за период):
    private static final double TECH_AVG_PERF_WARN = 0.75;
    private static final double TECH_AVG_PERF_CRIT = 0.60;
    private static final double TECH_AVG_QUAL_WARN = 0.95;
    private static final double TECH_AVG_QUAL_CRIT = 0.90;
    private static final double TECH_AVG_OEE_CRIT = 0.50;
    private static final double TECH_AVG_AVDOWNTIME_WARN_MIN = 10.0;
    private static final double TECH_AVG_PLAN_FULFILL_WARN = 0.85;

    // Админ (за период, кросс-линия):
    private static final double ADM_LINE_DOWNTIME_RATE_WARN = 0.30;
    private static final double ADM_LINE_OEE_CRIT = 0.50;
    private static final int    ADM_MIN_SHIFTS_FOR_TREND = 3;

    // ============ Публичные методы ============

    public List<RecommendationDto> recommend(String role, String lineId, int days) {
        if (role == null || role.isBlank()) {
            // Без роли — отдаём всё
            List<RecommendationDto> all = new ArrayList<>();
            all.addAll(safe(() -> forOperator(lineId)));
            all.addAll(safe(() -> forTechnologist(lineId, days)));
            all.addAll(safe(() -> forAdmin(days)));
            return sort(all);
        }
        return switch (role.toUpperCase()) {
            case "OPERATOR", "ROLE_OPERATOR" -> sort(safe(() -> forOperator(lineId)));
            case "TECHNOLOGIST", "ROLE_TECHNOLOGIST" -> sort(safe(() -> forTechnologist(lineId, days)));
            case "ADMIN", "ROLE_ADMIN" -> {
                List<RecommendationDto> a = new ArrayList<>();
                a.addAll(safe(() -> forAdmin(days)));
                a.addAll(safe(() -> forTechnologist(lineId, days)));
                yield sort(a);
            }
            default -> List.of();
        };
    }

    // ============ Оператор: «прямо сейчас» ============
    private List<RecommendationDto> forOperator(String lineId) {
        List<RecommendationDto> out = new ArrayList<>();
        List<ShiftSummaryDto> active = fetchActiveShifts(lineId);
        for (ShiftSummaryDto s : active) {
            ShiftDataDto d = safeGet(() -> processingClient.getShiftData(s.getId()), null);
            if (d == null) continue;
            applyOperatorRulesForShift(s, d, out);
        }
        return out;
    }

    private void applyOperatorRulesForShift(ShiftSummaryDto s, ShiftDataDto d, List<RecommendationDto> out) {
        double avg = dbl(d.getAvgSpeed());
        double nom = dbl(d.getNominalSpeed());
        int total = nullToZero(d.getTotalOutput());
        int scrap = nullToZero(d.getScrapCount());
        int stops = nullToZero(d.getNumberOfStops());

        Map<String, Object> scope = new HashMap<>();
        scope.put("shiftId", s.getId());
        scope.put("lineId", s.getLineId());

        // R1: Скорость ниже номинала
        if (nom > 0 && avg > 0) {
            double ratio = avg / nom;
            if (ratio < OP_SPEED_DROP_RATIO_CRIT) {
                out.add(rec("OP_SPEED_" + s.getId(), "OPERATOR", "CRITICAL", "PERFORMANCE",
                        "Критическое падение скорости",
                        "Текущая скорость линии " + fmt1(avg) + " шт/мин против номинала " + fmt1(nom)
                                + " шт/мин (" + pct(ratio) + ").",
                        "Остановите линию и проверьте дозатор/конвейер/настройки скорости. Возможна заклинка.",
                        "avgSpeed", avg, nom * OP_SPEED_DROP_RATIO_CRIT, scope));
            } else if (ratio < OP_SPEED_DROP_RATIO) {
                out.add(rec("OP_SPEED_" + s.getId(), "OPERATOR", "WARN", "PERFORMANCE",
                        "Скорость ниже нормы",
                        "Средняя скорость " + fmt1(avg) + " шт/мин — " + pct(ratio) + " от номинала " + fmt1(nom) + ".",
                        "Проверьте натяжение конвейера, настройку дозатора, отсутствие пробуксовок.",
                        "avgSpeed", avg, nom * OP_SPEED_DROP_RATIO, scope));
            }
        }

        // R2: Брак
        if (total > 0) {
            double scrapRate = (double) scrap / total;
            if (scrapRate >= OP_SCRAP_RATE_CRIT) {
                out.add(rec("OP_SCRAP_" + s.getId(), "OPERATOR", "CRITICAL", "QUALITY",
                        "Критический уровень брака",
                        "Брак составил " + pct(scrapRate) + " (" + scrap + " из " + total + " шт).",
                        "Немедленно приостановите линию, проведите калибровку оборудования, проверьте сырьё.",
                        "scrapRate", scrapRate, OP_SCRAP_RATE_CRIT, scope));
            } else if (scrapRate >= OP_SCRAP_RATE_WARN) {
                out.add(rec("OP_SCRAP_" + s.getId(), "OPERATOR", "WARN", "QUALITY",
                        "Повышенный брак",
                        "Брак " + pct(scrapRate) + " (" + scrap + " из " + total + " шт) — выше нормы " + pct(OP_SCRAP_RATE_WARN) + ".",
                        "Проверьте качество входящего сырья и настройку упаковочного автомата.",
                        "scrapRate", scrapRate, OP_SCRAP_RATE_WARN, scope));
            }
        }

        // R3: Открытый простой
        DowntimeEventDto open = findOpenDowntime(s.getId());
        if (open != null && open.getStartTime() != null) {
            double mins = Duration.between(open.getStartTime().plusMinutes(180), LocalDateTime.now()).toSeconds() / 60.0;
            if (mins >= OP_OPEN_DOWNTIME_CRIT_MIN) {
                out.add(rec("OP_OPEN_DT_" + s.getId(), "OPERATOR", "CRITICAL", "DOWNTIME",
                        "Длительный простой не устранён",
                        "Текущий простой длится " + fmt1(mins) + " мин (с " + open.getStartTime() + ").",
                        "Срочно вызовите механика и зафиксируйте причину в журнале.",
                        "openDowntimeMinutes", mins, OP_OPEN_DOWNTIME_CRIT_MIN, scope));
            } else if (mins >= OP_OPEN_DOWNTIME_WARN_MIN) {
                out.add(rec("OP_OPEN_DT_" + s.getId(), "OPERATOR", "WARN", "DOWNTIME",
                        "Затяжной простой",
                        "Текущий простой длится " + fmt1(mins) + " мин.",
                        "Сообщите механику, если устранение займёт больше 15 минут.",
                        "openDowntimeMinutes", mins, OP_OPEN_DOWNTIME_WARN_MIN, scope));
            }
        }

        // R4: Частые остановки
        if (stops >= OP_STOPS_CRIT) {
            out.add(rec("OP_STOPS_" + s.getId(), "OPERATOR", "CRITICAL", "AVAILABILITY",
                    "Очень частые остановки",
                    "За смену зафиксировано " + stops + " остановок.",
                    "Линия нестабильна — приостановите выпуск и проведите диагностику.",
                    "numberOfStops", (double) stops, (double) OP_STOPS_CRIT, scope));
        } else if (stops >= OP_STOPS_WARN) {
            out.add(rec("OP_STOPS_" + s.getId(), "OPERATOR", "WARN", "AVAILABILITY",
                    "Частые остановки",
                    "За смену уже " + stops + " остановок — выше нормы " + OP_STOPS_WARN + ".",
                    "Проверьте загрузку питателя и состояние датчиков.",
                    "numberOfStops", (double) stops, (double) OP_STOPS_WARN, scope));
        }

        // R5: Отставание от плана
        Double lag = computePlanLag(s, d);
        if (lag != null && lag >= OP_PLAN_LAG_WARN) {
            out.add(rec("OP_PLAN_" + s.getId(), "OPERATOR", "WARN", "PERFORMANCE",
                    "Отставание от плана",
                    "Прошло " + pct(computeElapsedShare(s)) + " смены, выпущено " + pct(computeOutputShare(s, d))
                            + " от плана. Отставание " + pct(lag) + ".",
                    "Увеличьте темп либо сообщите технологу о невыполнимости плана.",
                    "planLag", lag, OP_PLAN_LAG_WARN, scope));
        }
    }

    // ============ Технолог: тренды за период ============
    private List<RecommendationDto> forTechnologist(String lineId, int days) {
        if (days <= 0) days = 7;
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<RecommendationDto> out = new ArrayList<>();

        if (lineId != null && !lineId.isBlank()) {
            applyTechRulesForLine(lineId, from, days, out);
        } else {
            // Без выбранной линии — пройдём по всем уникальным линиям из истории
            for (String ln : distinctLinesFromHistory(from)) {
                applyTechRulesForLine(ln, from, days, out);
            }
        }
        return out;
    }

    private void applyTechRulesForLine(String lineId, LocalDateTime from, int days, List<RecommendationDto> out) {
        List<ShiftKpi> history = kpiRepository.findByLineIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(
                lineId, from, LocalDateTime.now(),
                org.springframework.data.domain.PageRequest.of(0, 200));
        if (history.size() < ADM_MIN_SHIFTS_FOR_TREND) return;

        double avgPerf = avg(history, ShiftKpi::getPerformance);
        double avgQual = avg(history, ShiftKpi::getQuality);
        double avgOee  = avg(history, ShiftKpi::getOee);
        double avgPlanFul = avg(history, ShiftKpi::getPlanFulfillment);
        double avgDowntime = avg(history, ShiftKpi::getAvgDowntime);

        Map<String, Object> scope = new HashMap<>();
        scope.put("lineId", lineId);
        scope.put("days", days);
        scope.put("shiftsAnalyzed", history.size());

        // R6: Performance
        if (avgPerf > 0 && avgPerf < TECH_AVG_PERF_CRIT) {
            out.add(rec("TECH_PERF_" + lineId, "TECHNOLOGIST", "CRITICAL", "PERFORMANCE",
                    "Хроническая низкая производительность — линия " + lineId,
                    "Средний Performance " + pct(avgPerf) + " за " + days + " дней (по " + history.size() + " смен).",
                    "Проанализируйте узкие места: скорость, простои, время разогрева. Возможно требуется ТО.",
                    "performance", avgPerf, TECH_AVG_PERF_CRIT, scope));
        } else if (avgPerf > 0 && avgPerf < TECH_AVG_PERF_WARN) {
            out.add(rec("TECH_PERF_" + lineId, "TECHNOLOGIST", "WARN", "PERFORMANCE",
                    "Низкий Performance — линия " + lineId,
                    "Средний Performance " + pct(avgPerf) + " за " + days + " дней.",
                    "Проверьте, не завышен ли nominal_speed в справочнике. Возможно требуется регулировка оборудования.",
                    "performance", avgPerf, TECH_AVG_PERF_WARN, scope));
        }

        // R7: Quality
        if (avgQual > 0 && avgQual < TECH_AVG_QUAL_CRIT) {
            out.add(rec("TECH_QUAL_" + lineId, "TECHNOLOGIST", "CRITICAL", "QUALITY",
                    "Снижение качества — линия " + lineId,
                    "Среднее качество " + pct(avgQual) + " за " + days + " дней (< " + pct(TECH_AVG_QUAL_CRIT) + ").",
                    "Проверьте: партии сырья, режимы упаковки, износ резальных кромок.",
                    "quality", avgQual, TECH_AVG_QUAL_CRIT, scope));
        } else if (avgQual > 0 && avgQual < TECH_AVG_QUAL_WARN) {
            out.add(rec("TECH_QUAL_" + lineId, "TECHNOLOGIST", "WARN", "QUALITY",
                    "Качество ниже целевого — линия " + lineId,
                    "Среднее качество " + pct(avgQual) + ".",
                    "Усильте выборочный контроль; проследите корреляцию с конкретными операторами/партиями.",
                    "quality", avgQual, TECH_AVG_QUAL_WARN, scope));
        }

        // R8: OEE
        if (avgOee > 0 && avgOee < TECH_AVG_OEE_CRIT) {
            out.add(rec("TECH_OEE_" + lineId, "TECHNOLOGIST", "CRITICAL", "PERFORMANCE",
                    "Низкий OEE — линия " + lineId,
                    "Средний OEE " + pct(avgOee) + " за " + days + " дней. Линия неэффективна.",
                    "Разложите потери по Availability/Performance/Quality и работайте с самой слабой компонентой.",
                    "oee", avgOee, TECH_AVG_OEE_CRIT, scope));
        }

        // R9: PlanFulfillment стабильно низкий
        if (avgPlanFul > 0 && avgPlanFul < TECH_AVG_PLAN_FULFILL_WARN) {
            out.add(rec("TECH_PLAN_" + lineId, "TECHNOLOGIST", "WARN", "PROCESS",
                    "Систематическое невыполнение плана — линия " + lineId,
                    "Средний Plan Fulfillment " + pct(avgPlanFul) + " за " + days + " дней.",
                    "План может быть завышен относительно реальных возможностей линии. Пересмотрите planned_output_per_hour.",
                    "planFulfillment", avgPlanFul, TECH_AVG_PLAN_FULFILL_WARN, scope));
        }

        // R10: Длинные простои
        if (avgDowntime > TECH_AVG_AVDOWNTIME_WARN_MIN) {
            out.add(rec("TECH_AVGDT_" + lineId, "TECHNOLOGIST", "WARN", "DOWNTIME",
                    "Долгие остановки — линия " + lineId,
                    "Средняя длительность простоя " + fmt1(avgDowntime) + " мин — выше " + fmt1(TECH_AVG_AVDOWNTIME_WARN_MIN) + " мин.",
                    "Пересмотрите SOP по устранению типовых простоев; рассмотрите быстрозаменяемые узлы.",
                    "avgDowntime", avgDowntime, TECH_AVG_AVDOWNTIME_WARN_MIN, scope));
        }
    }

    // ============ Админ: организационное / кросс-линия ============
    private List<RecommendationDto> forAdmin(int days) {
        if (days <= 0) days = 30;
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<RecommendationDto> out = new ArrayList<>();

        for (String lineId : distinctLinesFromHistory(from)) {
            List<ShiftKpi> hist = kpiRepository.findByLineIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(
                    lineId, from, LocalDateTime.now(),
                    org.springframework.data.domain.PageRequest.of(0, 500));
            if (hist.size() < ADM_MIN_SHIFTS_FOR_TREND) continue;

            double avgDtRate = avg(hist, ShiftKpi::getDowntimeRate);
            double avgOee = avg(hist, ShiftKpi::getOee);

            Map<String, Object> scope = new HashMap<>();
            scope.put("lineId", lineId);
            scope.put("days", days);
            scope.put("shiftsAnalyzed", hist.size());

            // R11: высокий downtime
            if (avgDtRate > ADM_LINE_DOWNTIME_RATE_WARN) {
                out.add(rec("ADM_DTRATE_" + lineId, "ADMIN", "WARN", "STAFFING",
                        "Высокий простой — линия " + lineId,
                        "Средняя доля простоев " + pct(avgDtRate) + " за " + days + " дней. "
                                + "Линия теряет значительную часть времени.",
                        "Рассмотрите дополнительную смену для ТО или пересмотрите график обслуживания.",
                        "downtimeRate", avgDtRate, ADM_LINE_DOWNTIME_RATE_WARN, scope));
            }

            // R12: критически низкий OEE
            if (avgOee > 0 && avgOee < ADM_LINE_OEE_CRIT) {
                out.add(rec("ADM_OEE_" + lineId, "ADMIN", "CRITICAL", "EQUIPMENT",
                        "Критически низкий OEE — линия " + lineId,
                        "Средний OEE " + pct(avgOee) + " за " + days + " дней. Эффективность ниже 50%.",
                        "Рассмотрите модернизацию или замену оборудования; "
                                + "перераспределите нагрузку на другие линии.",
                        "oee", avgOee, ADM_LINE_OEE_CRIT, scope));
            }
        }

        // R13: Data quality — смены без operatorId среди последних
        List<ShiftSummaryDto> recent = fetchRecentClosedShifts(50);
        long noOperator = recent.stream().filter(s -> s.getOperatorId() == null).count();
        if (noOperator >= 3) {
            Map<String, Object> scope = new HashMap<>();
            scope.put("days", days);
            scope.put("shiftsWithoutOperator", noOperator);
            out.add(rec("ADM_NO_OPERATOR", "ADMIN", "INFO", "DATA_QUALITY",
                    "Смены без указания оператора",
                    "В последних 50 закрытых сменах " + noOperator + " без operatorId.",
                    "Сделайте поле «оператор» обязательным при создании смены или ужесточите валидацию.",
                    "shiftsWithoutOperator", (double) noOperator, 3.0, scope));
        }

        return out;
    }

    // ============ helpers ============

    private List<ShiftSummaryDto> fetchActiveShifts(String lineId) {
        try {
            var page = processingClient.findShifts("ACTIVE", lineId, 0, 50);
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.debug("findShifts failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ShiftSummaryDto> fetchRecentClosedShifts(int size) {
        try {
            var page = processingClient.findShifts("CLOSED", null, 0, size);
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private DowntimeEventDto findOpenDowntime(Long shiftId) {
        try {
            return processingClient.getDowntimes(shiftId).stream()
                    .filter(d -> d.getEndTime() == null)
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> distinctLinesFromHistory(LocalDateTime from) {
        // Берём из недавно посчитанных KPI уникальные линии
        return kpiRepository.findAll().stream()
                .filter(k -> k.getCalculatedAt() != null && k.getCalculatedAt().isAfter(from))
                .map(ShiftKpi::getLineId)
                .distinct()
                .toList();
    }

    /** Сколько от смены уже прошло (0..1) — по плановым границам. */
    private Double computeElapsedShare(ShiftSummaryDto s) {
        if (s.getPlannedStart() == null || s.getPlannedEnd() == null) return null;
        long total = Duration.between(s.getPlannedStart(), s.getPlannedEnd()).toSeconds();
        if (total <= 0) return null;
        long passed = Duration.between(s.getPlannedStart(), LocalDateTime.now()).toSeconds();
        if (passed < 0) return 0.0;
        return Math.min(1.0, (double) passed / total);
    }

    /** Доля выпуска от плана (0..1+). */
    private Double computeOutputShare(ShiftSummaryDto s, ShiftDataDto d) {
        Integer planned = d.getPlannedOutput() != null ? d.getPlannedOutput() : s.getPlannedOutput();
        if (planned == null || planned == 0) return null;
        int total = nullToZero(d.getTotalOutput());
        return (double) total / planned;
    }

    /** Отставание = elapsedShare - outputShare (положительное = отстаём). */
    private Double computePlanLag(ShiftSummaryDto s, ShiftDataDto d) {
        Double elapsed = computeElapsedShare(s);
        Double output = computeOutputShare(s, d);
        if (elapsed == null || output == null) return null;
        if (elapsed < 0.1) return null; // ещё рано судить
        return elapsed - output;
    }

    private double avg(List<ShiftKpi> list, java.util.function.Function<ShiftKpi, BigDecimal> f) {
        return list.stream()
                .map(f).filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0.0);
    }

    private RecommendationDto rec(String id, String role, String severity, String category,
                                  String title, String description, String action,
                                  String metric, double value, double threshold,
                                  Map<String, Object> scope) {
        return RecommendationDto.builder()
                .id(id)
                .role(role)
                .severity(severity)
                .category(category)
                .title(title)
                .description(description)
                .suggestedAction(action)
                .metric(metric)
                .value(value)
                .threshold(threshold)
                .scope(scope)
                .triggeredAt(LocalDateTime.now())
                .build();
    }

    private double dbl(BigDecimal v) { return v == null ? 0.0 : v.doubleValue(); }
    private int nullToZero(Integer v) { return v == null ? 0 : v; }
    private String fmt1(double v) { return String.format(java.util.Locale.US, "%.1f", v); }
    private String pct(double v) { return String.format(java.util.Locale.US, "%.1f%%", v * 100.0); }

    private static int sevWeight(String s) {
        return switch (s) {
            case "CRITICAL" -> 0;
            case "WARN" -> 1;
            case "INFO" -> 2;
            default -> 3;
        };
    }

    private List<RecommendationDto> sort(List<RecommendationDto> in) {
        in.sort(Comparator
                .comparingInt((RecommendationDto r) -> sevWeight(r.getSeverity()))
                .thenComparing(RecommendationDto::getCategory, Comparator.nullsLast(String::compareTo))
                .thenComparing(RecommendationDto::getTitle, Comparator.nullsLast(String::compareTo)));
        return in;
    }

    private <T> List<T> safe(java.util.function.Supplier<List<T>> sup) {
        try { return sup.get(); } catch (Exception e) {
            log.warn("Recommendation block failed: {}", e.getMessage());
            return List.of();
        }
    }
    private <T> T safeGet(java.util.function.Supplier<T> sup, T fallback) {
        try { return sup.get(); } catch (Exception e) { return fallback; }
    }
}
