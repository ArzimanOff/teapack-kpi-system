CREATE TABLE IF NOT EXISTS notification.notification_reads (
    notification_id BIGINT       NOT NULL REFERENCES notification.notifications(id) ON DELETE CASCADE,
    username        VARCHAR(100) NOT NULL,
    read_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (notification_id, username)
);

CREATE INDEX IF NOT EXISTS idx_notification_reads_username
    ON notification.notification_reads(username);

-- Глобальный is_read больше не нужен: статус прочтения теперь индивидуальный.
ALTER TABLE notification.notifications DROP COLUMN IF EXISTS is_read;
DROP INDEX IF EXISTS notification.idx_notifications_is_read;
