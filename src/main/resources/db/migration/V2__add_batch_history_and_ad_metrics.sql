-- Batch Execution History
CREATE TABLE batch_execution_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    instance_id VARCHAR(200) NOT NULL,
    started_at DATETIME,
    finished_at DATETIME,
    processed_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    summary VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_beh_job_status (job_name, status),
    INDEX idx_beh_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ad Daily Metrics
CREATE TABLE ad_daily_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ad_platform_id BIGINT NOT NULL,
    metric_date DATE NOT NULL,
    requests BIGINT NOT NULL DEFAULT 0,
    impressions BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    revenue DOUBLE NOT NULL DEFAULT 0.0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_adm_platform FOREIGN KEY (ad_platform_id) REFERENCES ad_platforms(id),
    UNIQUE KEY uk_adm_platform_date (ad_platform_id, metric_date),
    INDEX idx_adm_platform_date (ad_platform_id, metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
