-- GOAI 1.0.0: 可回放且不含凭据明文的部署修订。
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deployment_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` VARCHAR(64) NOT NULL,
    `deployment_uid` VARCHAR(64) NOT NULL,
    `revision_number` BIGINT NOT NULL,
    `model_uid` VARCHAR(64) NOT NULL,
    `model_version` VARCHAR(32) NOT NULL,
    `model_contract_uid` VARCHAR(64) NOT NULL,
    `image_name` VARCHAR(255) NOT NULL,
    `spec_json` JSON NOT NULL,
    `spec_hash` CHAR(64) NOT NULL,
    `created_by` VARCHAR(64) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `source_action_id` VARCHAR(64) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mep_revision_uid` (`uid`),
    UNIQUE KEY `uk_mep_deployment_revision` (`deployment_uid`, `revision_number`),
    KEY `idx_mep_revision_contract` (`model_contract_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 部署修订';
