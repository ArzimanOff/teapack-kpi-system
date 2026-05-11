package com.teapack.kpi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class RecommendationDto {
    /** Стабильный синтетический id рекомендации (для дедупликации/«пометить как выполнено» на клиенте). */
    private String id;

    /** Целевая роль: OPERATOR / TECHNOLOGIST / ADMIN */
    private String role;

    /** INFO / WARN / CRITICAL */
    private String severity;

    /** PERFORMANCE / AVAILABILITY / QUALITY / DOWNTIME / PROCESS / STAFFING / EQUIPMENT / DATA_QUALITY */
    private String category;

    private String title;
    private String description;
    private String suggestedAction;

    /** Какая метрика триггернула правило (например "performance", "scrapRate"). */
    private String metric;
    private Double value;
    private Double threshold;

    /** shiftId / lineId / operatorId / period — что применимо. */
    @Builder.Default
    private Map<String, Object> scope = new HashMap<>();

    private LocalDateTime triggeredAt;
}
