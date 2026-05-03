CREATE TABLE IF NOT EXISTS reporting.reports (
                                                 id           BIGSERIAL PRIMARY KEY,
                                                 shift_id     BIGINT        NOT NULL,
                                                 line_id      VARCHAR(50)   NOT NULL,
    type         VARCHAR(50)   NOT NULL DEFAULT 'SHIFT',
    status       VARCHAR(20)   NOT NULL DEFAULT 'GENERATED',
    oee          NUMERIC(5,4),
    availability NUMERIC(5,4),
    performance  NUMERIC(5,4),
    quality      NUMERIC(5,4),
    total_output INTEGER,
    scrap_count  INTEGER,
    downtime     NUMERIC(10,2),
    report_data  JSONB,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_reports_shift_id ON reporting.reports(shift_id);
CREATE INDEX IF NOT EXISTS idx_reports_line_id ON reporting.reports(line_id);