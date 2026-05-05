-- Глобальные пороги KPI для notification-service.
-- Single-row pattern: всегда одна запись с id=1 (см. AuthService bootstrap),
-- технолог/админ может менять через PUT /api/thresholds.
CREATE TABLE IF NOT EXISTS notification.kpi_thresholds (
    id                BIGINT       PRIMARY KEY,
    oee_min           NUMERIC(5,4) NOT NULL,
    availability_min  NUMERIC(5,4) NOT NULL,
    performance_min   NUMERIC(5,4) NOT NULL,
    quality_min       NUMERIC(5,4) NOT NULL,
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(100)
);

INSERT INTO notification.kpi_thresholds (id, oee_min, availability_min, performance_min, quality_min)
VALUES (1, 0.65, 0.80, 0.75, 0.95)
ON CONFLICT (id) DO NOTHING;
