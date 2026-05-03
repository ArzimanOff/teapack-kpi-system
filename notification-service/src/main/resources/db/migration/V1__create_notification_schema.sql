CREATE TABLE IF NOT EXISTS notification.notifications (
                                                          id           BIGSERIAL PRIMARY KEY,
                                                          shift_id     BIGINT        NOT NULL,
                                                          line_id      VARCHAR(50)   NOT NULL,
    type         VARCHAR(50)   NOT NULL,
    severity     VARCHAR(20)   NOT NULL DEFAULT 'WARNING',
    message      TEXT          NOT NULL,
    metric_name  VARCHAR(50),
    metric_value NUMERIC(10,4),
    threshold    NUMERIC(10,4),
    is_read      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_notifications_shift_id ON notification.notifications(shift_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notification.notifications(is_read);