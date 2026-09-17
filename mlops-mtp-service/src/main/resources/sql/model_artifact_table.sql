/* Copyright (C) 2026 Synapxnet. All rights reserved. */
CREATE TABLE IF NOT EXISTS xnet_mlops_mtp_model_artifact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(36) NOT NULL UNIQUE,
    output_name VARCHAR(255) NOT NULL,
    framework VARCHAR(64) NOT NULL,
    domain_name VARCHAR(128) NOT NULL,
    team_uid VARCHAR(36) NOT NULL,
    team_name VARCHAR(255),
    tenant_uid VARCHAR(36) NOT NULL,
    dept_uid VARCHAR(36),
    user_id VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    artifact_path VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_artifact_tenant_name (tenant_uid, output_name),
    KEY idx_model_artifact_tenant (tenant_uid)
) COMMENT='MLOps模型制品登记表';
