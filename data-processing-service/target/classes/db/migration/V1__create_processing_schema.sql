CREATE TABLE IF NOT EXISTS processing.shifts (
                                                 id              BIGSERIAL PRIMARY KEY,
                                                 line_id         VARCHAR(50)  NOT NULL,
    operator_id     BIGINT,
    planned_start   TIMESTAMP    NOT NULL,
    planned_end     TIMESTAMP    NOT NULL,
    actual_start    TIMESTAMP,
    actual_end      TIMESTAMP,
    planned_output  INTEGER      NOT NULL DEFAULT 0,
    nominal_speed   NUMERIC(7,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS processing.shift_aggregates (
                                                           id               BIGSERIAL PRIMARY KEY,
                                                           shift_id         BIGINT       NOT NULL REFERENCES processing.shifts(id),
    total_output     INTEGER      NOT NULL DEFAULT 0,
    good_output      INTEGER      NOT NULL DEFAULT 0,
    scrap_count      INTEGER      NOT NULL DEFAULT 0,
    downtime_minutes NUMERIC(10,2) NOT NULL DEFAULT 0,
    avg_speed        NUMERIC(7,2),
    avg_temperature  NUMERIC(5,2),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_shift_aggregates_shift_id UNIQUE (shift_id)
    );

CREATE TABLE IF NOT EXISTS processing.downtime_events (
                                                          id            BIGSERIAL PRIMARY KEY,
                                                          shift_id      BIGINT       NOT NULL REFERENCES processing.shifts(id),
    start_time    TIMESTAMP    NOT NULL,
    end_time      TIMESTAMP,
    reason        VARCHAR(255),
    duration_minutes NUMERIC(10,2),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_shifts_line_id ON processing.shifts(line_id);
CREATE INDEX IF NOT EXISTS idx_shifts_status ON processing.shifts(status);
CREATE INDEX IF NOT EXISTS idx_downtime_events_shift_id ON processing.downtime_events(shift_id);