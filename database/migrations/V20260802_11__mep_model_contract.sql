-- GOAI 1.0.0: 模型输入契约，不保存训练或推理样本。
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_model_contract` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` VARCHAR(64) NOT NULL,
    `model_uid` VARCHAR(64) NOT NULL,
    `model_version` VARCHAR(32) NOT NULL,
    `input_dimension` INT NOT NULL,
    `input_schema_json` JSON NOT NULL,
    `contract_hash` CHAR(64) NOT NULL,
    `source` VARCHAR(64) NOT NULL,
    `created_by` VARCHAR(64) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mep_contract_uid` (`uid`),
    UNIQUE KEY `uk_mep_model_version` (`model_uid`, `model_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 模型输入契约';
