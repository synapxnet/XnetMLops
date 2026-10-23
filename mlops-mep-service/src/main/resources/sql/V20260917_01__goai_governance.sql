-- Copyright (C) 2026 Synapxnet. All rights reserved.
-- This file is Synapxnet Proprietary and Confidential. It is strictly
-- forbidden to copy, distribute, or use without explicit authorization.
-- MEP 治理结构迁移 / MEP governed schema migration.
-- Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
-- Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
-- Based on read-only SHOW CREATE TABLE on 2026-09-17; contains no business rows.
-- 审阅并备份后在已选定的数据库执行；新环境先导入基础表。
-- Review and back up before execution in the selected database; initialize base tables first.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_model_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_version` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `input_dimension` int NOT NULL,
  `input_schema_json` json NOT NULL,
  `contract_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mep_contract_uid` (`uid`),
  UNIQUE KEY `uk_mep_model_version` (`model_uid`,`model_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 模型输入契约';

CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deployment_revision` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `deployment_uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision_number` bigint NOT NULL,
  `model_uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_version` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_contract_uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spec_json` json NOT NULL,
  `spec_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `source_action_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mep_revision_uid` (`uid`),
  UNIQUE KEY `uk_mep_deployment_revision` (`deployment_uid`,`revision_number`),
  KEY `idx_mep_revision_contract` (`model_contract_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 部署修订';

CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deployment_action` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `deployment_uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_revision` bigint DEFAULT NULL,
  `target_revision` bigint NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `workspace_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `incident_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approval_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `idempotency_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_digest` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_resource_version` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `verification_policy_json` json NOT NULL,
  `dry_run` tinyint(1) NOT NULL DEFAULT '0',
  `error_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `started_at` datetime(3) DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `created_by` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mep_action_uid` (`uid`),
  UNIQUE KEY `uk_mep_action_idempotency` (`workspace_id`,`action_type`,`idempotency_key`),
  KEY `idx_mep_action_trace` (`workspace_id`,`incident_id`,`trace_id`),
  KEY `idx_mep_action_status` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 部署动作';

CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_inference_probe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `deployment_uid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision_number` bigint NOT NULL,
  `test_dataset_ref` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_count` int NOT NULL,
  `success_count` int NOT NULL,
  `error_count` int NOT NULL,
  `error_rate` decimal(8,6) NOT NULL,
  `p50_ms` decimal(12,3) DEFAULT NULL,
  `p95_ms` decimal(12,3) DEFAULT NULL,
  `input_dimension` int NOT NULL,
  `contract_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_digest` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `incident_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `started_at` datetime(3) NOT NULL,
  `completed_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mep_probe_uid` (`uid`),
  KEY `idx_mep_probe_deployment` (`deployment_uid`,`started_at`),
  KEY `idx_mep_probe_trace` (`incident_id`,`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 推理探针';

-- 仅补缺失字段，保留已有值。 / Add missing columns while retaining existing values.
SET @mlops_schema_statement = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'xnet_mlops_mep_model_deployment' AND COLUMN_NAME = 'active_revision'),
  'SELECT 1',
  'ALTER TABLE `xnet_mlops_mep_model_deployment` ADD COLUMN `active_revision` bigint DEFAULT NULL COMMENT ''当前修订'''
);
PREPARE mlops_schema_update FROM @mlops_schema_statement;
EXECUTE mlops_schema_update;
DEALLOCATE PREPARE mlops_schema_update;

-- 仅补缺失字段，保留已有值。 / Add missing columns while retaining existing values.
SET @mlops_schema_statement = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'xnet_mlops_mep_model_deployment' AND COLUMN_NAME = 'resource_version'),
  'SELECT 1',
  'ALTER TABLE `xnet_mlops_mep_model_deployment` ADD COLUMN `resource_version` bigint NOT NULL DEFAULT ''1'' COMMENT ''乐观锁版本'''
);
PREPARE mlops_schema_update FROM @mlops_schema_statement;
EXECUTE mlops_schema_update;
DEALLOCATE PREPARE mlops_schema_update;

-- 仅补缺失字段，保留已有值。 / Add missing columns while retaining existing values.
SET @mlops_schema_statement = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'xnet_mlops_mep_model_deployment' AND COLUMN_NAME = 'last_verified_at'),
  'SELECT 1',
  'ALTER TABLE `xnet_mlops_mep_model_deployment` ADD COLUMN `last_verified_at` datetime(3) DEFAULT NULL COMMENT ''最近验证时间'''
);
PREPARE mlops_schema_update FROM @mlops_schema_statement;
EXECUTE mlops_schema_update;
DEALLOCATE PREPARE mlops_schema_update;

SET @mlops_schema_statement = NULL;
