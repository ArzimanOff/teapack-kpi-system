CREATE TABLE IF NOT EXISTS processing.audit_events (
    id          BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    actor       VARCHAR(100),
    actor_role  VARCHAR(50),
    action      VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id   VARCHAR(100),
    details     TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_desc
    ON processing.audit_events (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor
    ON processing.audit_events (actor);
CREATE INDEX IF NOT EXISTS idx_audit_events_target
    ON processing.audit_events (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_action
    ON processing.audit_events (action);
