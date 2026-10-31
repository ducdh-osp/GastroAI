CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at TIMESTAMP NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id BIGINT,
    actor_email VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    detail VARCHAR(1000),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(1024) NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_audit_logs_occurred_at (occurred_at),
    INDEX idx_audit_logs_actor (actor_type, actor_id, occurred_at),
    INDEX idx_audit_logs_action (action, occurred_at)
);