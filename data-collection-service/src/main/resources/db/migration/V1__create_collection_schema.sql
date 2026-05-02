CREATE TABLE IF NOT EXISTS collection.equipment_readings (
                                                             id          BIGSERIAL PRIMARY KEY,
                                                             line_id     VARCHAR(50)    NOT NULL,
    timestamp   TIMESTAMP      NOT NULL,
    temperature NUMERIC(5,2),
    line_speed  NUMERIC(7,2),
    status      VARCHAR(20)    NOT NULL,
    output_count INTEGER       DEFAULT 0,
    shift_id    BIGINT,
    raw_data    JSONB,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS collection.operator_events (
                                                          id          BIGSERIAL PRIMARY KEY,
                                                          shift_id    BIGINT         NOT NULL,
                                                          line_id     VARCHAR(50)    NOT NULL,
    timestamp   TIMESTAMP      NOT NULL,
    event_type  VARCHAR(20)    NOT NULL,
    reason      VARCHAR(255),
    scrap_count INTEGER        DEFAULT 0,
    comment     TEXT,
    operator_id BIGINT,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_equipment_readings_line_id ON collection.equipment_readings(line_id);
CREATE INDEX IF NOT EXISTS idx_equipment_readings_timestamp ON collection.equipment_readings(timestamp);
CREATE INDEX IF NOT EXISTS idx_equipment_readings_shift_id ON collection.equipment_readings(shift_id);
CREATE INDEX IF NOT EXISTS idx_operator_events_shift_id ON collection.operator_events(shift_id);