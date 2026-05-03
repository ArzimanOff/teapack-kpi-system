CREATE TABLE IF NOT EXISTS kpi.shift_kpi (
                                             id                  BIGSERIAL PRIMARY KEY,
                                             shift_id            BIGINT        NOT NULL UNIQUE,
                                             line_id             VARCHAR(50)   NOT NULL,

    -- Время
    planned_time        NUMERIC(10,2) NOT NULL DEFAULT 0,
    operating_time      NUMERIC(10,2) NOT NULL DEFAULT 0,
    downtime            NUMERIC(10,2) NOT NULL DEFAULT 0,
    downtime_rate       NUMERIC(5,4)  NOT NULL DEFAULT 0,
    number_of_stops     INTEGER       NOT NULL DEFAULT 0,
    avg_downtime        NUMERIC(10,2) NOT NULL DEFAULT 0,

    -- Выпуск
    total_output        INTEGER       NOT NULL DEFAULT 0,
    good_output         INTEGER       NOT NULL DEFAULT 0,
    scrap_count         INTEGER       NOT NULL DEFAULT 0,
    planned_output      INTEGER       NOT NULL DEFAULT 0,

    -- Скорость
    nominal_speed       NUMERIC(7,2)  NOT NULL DEFAULT 0,
    avg_speed           NUMERIC(7,2)  NOT NULL DEFAULT 0,
    output_rate         NUMERIC(10,4) NOT NULL DEFAULT 0,
    speed_loss          NUMERIC(7,2)  NOT NULL DEFAULT 0,

    -- Базовые KPI
    availability        NUMERIC(5,4)  NOT NULL DEFAULT 0,
    performance         NUMERIC(5,4)  NOT NULL DEFAULT 0,
    quality             NUMERIC(5,4)  NOT NULL DEFAULT 0,
    oee                 NUMERIC(5,4)  NOT NULL DEFAULT 0,

    -- Производные
    performance_loss    NUMERIC(5,4)  NOT NULL DEFAULT 0,
    plan_fulfillment    NUMERIC(5,4)  NOT NULL DEFAULT 0,
    scrap_rate          NUMERIC(5,4)  NOT NULL DEFAULT 0,

    calculated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_shift_kpi_shift_id ON kpi.shift_kpi(shift_id);
CREATE INDEX IF NOT EXISTS idx_shift_kpi_line_id ON kpi.shift_kpi(line_id);