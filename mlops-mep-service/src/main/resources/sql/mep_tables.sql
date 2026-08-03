-- MEP (Model Endpoint Platform) 模型部署平台数据库表结构
-- 创建时间: 2024
-- 描述: 用于管理大模型服务、API密钥、部署节点和模型部署

-- ==========================================
-- 1. 大模型服务表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_llm_service` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '服务名称',
    `type` VARCHAR(32) NOT NULL COMMENT '服务类型: ollama, openai, deepseek, custom',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '服务描述',
    `endpoint` VARCHAR(255) NOT NULL COMMENT '服务端点URL',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `api_key` VARCHAR(255) DEFAULT NULL COMMENT 'API密钥(加密存储)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'stopped' COMMENT '服务状态: running, stopped, error, deploying',
    `config` JSON DEFAULT NULL COMMENT '服务配置(JSON格式)',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型服务表';

-- ==========================================
-- 2. API密钥表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_api_key` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '密钥名称',
    `key_hash` VARCHAR(128) NOT NULL COMMENT '密钥哈希值',
    `key_masked` VARCHAR(64) NOT NULL COMMENT '脱敏显示的密钥',
    `encrypted_key` VARCHAR(500) DEFAULT NULL COMMENT 'AES加密存储的原始密钥（用于OpenClaw等服务引用）',
    `provider` VARCHAR(32) NOT NULL COMMENT '服务商: ollama, openai, deepseek, custom',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态: active, disabled, expired',
    `usage_limit` BIGINT DEFAULT NULL COMMENT '使用次数限制',
    `usage_count` BIGINT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `expires_at` DATETIME DEFAULT NULL COMMENT '过期时间',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_provider` (`provider`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API密钥表';

-- ==========================================
-- 3. 部署节点表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deploy_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '节点名称',
    `ip_address` VARCHAR(64) NOT NULL COMMENT 'IP地址',
    `port` INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
    `status` VARCHAR(32) NOT NULL DEFAULT 'offline' COMMENT '状态: online, offline, maintenance',
    `cpu_cores` INT NOT NULL DEFAULT 4 COMMENT 'CPU核心数',
    `memory_gb` INT NOT NULL DEFAULT 8 COMMENT '内存大小(GB)',
    `gpu_info` VARCHAR(255) DEFAULT NULL COMMENT 'GPU信息',
    `docker_version` VARCHAR(32) DEFAULT NULL COMMENT 'Docker版本',
    `nginx_status` VARCHAR(32) DEFAULT 'stopped' COMMENT 'Nginx状态: running, stopped',
    `labels` JSON DEFAULT NULL COMMENT '标签(JSON数组)',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_status` (`status`),
    KEY `idx_ip` (`ip_address`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部署节点表';

-- ==========================================
-- 4. 模型部署表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_model_deployment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '部署名称',
    `model_source` VARCHAR(32) NOT NULL COMMENT '模型来源: mtp, llm',
    `model_uid` VARCHAR(64) NOT NULL COMMENT '模型UID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `model_version` VARCHAR(32) DEFAULT NULL COMMENT '模型版本',
    `node_uid` VARCHAR(64) NOT NULL COMMENT '部署节点UID',
    `node_name` VARCHAR(100) DEFAULT NULL COMMENT '部署节点名称',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending, deploying, running, failed, stopped',
    `container_id` VARCHAR(128) DEFAULT NULL COMMENT '容器ID',
    `container_name` VARCHAR(100) DEFAULT NULL COMMENT '容器名称',
    `image_name` VARCHAR(255) DEFAULT NULL COMMENT '镜像名称',
    `port` INT DEFAULT 8080 COMMENT '服务端口',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT '服务端点URL',
    `replicas` INT NOT NULL DEFAULT 1 COMMENT '副本数',
    `resource_config` JSON DEFAULT NULL COMMENT '资源配置(JSON)',
    `nginx_config` JSON DEFAULT NULL COMMENT 'Nginx配置(JSON)',
    `health_check` JSON DEFAULT NULL COMMENT '健康检查配置(JSON)',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_model_source` (`model_source`),
    KEY `idx_node_uid` (`node_uid`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型部署表';

-- ==========================================
-- 5. 部署日志表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deployment_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `deployment_uid` VARCHAR(64) NOT NULL COMMENT '部署UID',
    `level` VARCHAR(16) NOT NULL DEFAULT 'info' COMMENT '日志级别: info, warn, error',
    `message` TEXT NOT NULL COMMENT '日志消息',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    PRIMARY KEY (`id`),
    KEY `idx_deployment_uid` (`deployment_uid`),
    KEY `idx_level` (`level`),
    KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部署日志表';

-- ==========================================
-- 6. 服务监控指标表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_service_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `deployment_uid` VARCHAR(64) NOT NULL COMMENT '部署UID',
    `cpu_usage` DECIMAL(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
    `memory_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '内存使用率(%)',
    `request_count` BIGINT DEFAULT 0 COMMENT '请求数',
    `error_count` BIGINT DEFAULT 0 COMMENT '错误数',
    `avg_response_time` INT DEFAULT NULL COMMENT '平均响应时间(ms)',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
    PRIMARY KEY (`id`),
    KEY `idx_deployment_uid` (`deployment_uid`),
    KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务监控指标表';

-- ==========================================
-- 插入示例数据
-- ==========================================

-- 示例大模型服务
INSERT INTO `xnet_mlops_mep_llm_service` (`uid`, `name`, `type`, `description`, `endpoint`, `model_name`, `status`, `config`, `created_by`) VALUES
('llm-001', 'Ollama本地服务', 'ollama', '本地部署的Ollama大模型服务', 'http://localhost:11434', 'llama2', 'stopped', '{"max_tokens": 2048, "temperature": 0.7}', 'admin'),
('llm-002', 'DeepSeek API', 'deepseek', 'DeepSeek满血版API服务', 'https://api.deepseek.com/v1', 'deepseek-chat', 'stopped', '{"max_tokens": 4096, "temperature": 0.8}', 'admin');

-- 示例部署节点
INSERT INTO `xnet_mlops_mep_deploy_node` (`uid`, `name`, `ip_address`, `port`, `status`, `cpu_cores`, `memory_gb`, `gpu_info`, `docker_version`, `nginx_status`, `labels`, `description`, `created_by`) VALUES
('node-001', 'GPU节点-01', '127.0.0.1', 22, 'online', 32, 64, 'NVIDIA RTX 4090 x2', '24.0.7', 'running', '["gpu", "production"]', '生产环境GPU计算节点', 'admin'),
('node-002', 'CPU节点-01', '127.0.0.1', 22, 'online', 16, 32, NULL, '24.0.7', 'running', '["cpu", "production"]', '生产环境CPU计算节点', 'admin'),
('node-003', '测试节点-01', '127.0.0.1', 22, 'offline', 8, 16, NULL, '23.0.6', 'stopped', '["test"]', '测试环境节点', 'admin');


-- ==========================================
-- GOAI Competition 1.0.0
-- Canonical source: database/migrations
-- ==========================================

-- V20260802_11__mep_model_contract.sql
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

-- V20260802_12__mep_deployment_revision.sql
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

-- V20260802_13__mep_deployment_action.sql
-- GOAI 1.0.0: 受控部署动作和幂等事实。
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deployment_action` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` VARCHAR(64) NOT NULL,
    `deployment_uid` VARCHAR(64) NOT NULL,
    `action_type` VARCHAR(32) NOT NULL,
    `from_revision` BIGINT NULL,
    `target_revision` BIGINT NOT NULL,
    `status` VARCHAR(16) NOT NULL,
    `stage` VARCHAR(32) NOT NULL,
    `request_id` VARCHAR(128) NOT NULL,
    `workspace_id` VARCHAR(64) NOT NULL,
    `incident_id` VARCHAR(64) NOT NULL,
    `trace_id` VARCHAR(64) NOT NULL,
    `approval_id` VARCHAR(64) NOT NULL,
    `idempotency_key` VARCHAR(128) NOT NULL,
    `request_digest` CHAR(64) NOT NULL,
    `expected_resource_version` VARCHAR(64) NOT NULL,
    `verification_policy_json` JSON NOT NULL,
    `dry_run` BOOLEAN NOT NULL DEFAULT FALSE,
    `error_code` VARCHAR(64) NULL,
    `error_message` VARCHAR(500) NULL,
    `started_at` DATETIME(3) NULL,
    `completed_at` DATETIME(3) NULL,
    `created_by` VARCHAR(128) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mep_action_uid` (`uid`),
    UNIQUE KEY `uk_mep_action_idempotency` (`workspace_id`, `action_type`, `idempotency_key`),
    KEY `idx_mep_action_trace` (`workspace_id`, `incident_id`, `trace_id`),
    KEY `idx_mep_action_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 部署动作';

-- V20260802_14__mep_inference_probe.sql
-- GOAI 1.0.0: 仅保存聚合指标和摘要的推理探针记录。
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_inference_probe` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` VARCHAR(64) NOT NULL,
    `deployment_uid` VARCHAR(64) NOT NULL,
    `revision_number` BIGINT NOT NULL,
    `test_dataset_ref` VARCHAR(255) NOT NULL,
    `sample_count` INT NOT NULL,
    `success_count` INT NOT NULL,
    `error_count` INT NOT NULL,
    `error_rate` DECIMAL(8,6) NOT NULL,
    `p50_ms` DECIMAL(12,3) NULL,
    `p95_ms` DECIMAL(12,3) NULL,
    `input_dimension` INT NOT NULL,
    `contract_status` VARCHAR(16) NOT NULL,
    `result_digest` CHAR(64) NOT NULL,
    `incident_id` VARCHAR(64) NOT NULL,
    `trace_id` VARCHAR(64) NOT NULL,
    `started_at` DATETIME(3) NOT NULL,
    `completed_at` DATETIME(3) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mep_probe_uid` (`uid`),
    KEY `idx_mep_probe_deployment` (`deployment_uid`, `started_at`),
    KEY `idx_mep_probe_trace` (`incident_id`, `trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 推理探针';

-- V20260802_15__mep_deployment_version.sql
-- GOAI 1.0.0: 为现有部署增加乐观锁和验证时间；脚本可重复执行。
SET @schema_name = DATABASE();
SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name
     AND TABLE_NAME = 'xnet_mlops_mep_model_deployment' AND COLUMN_NAME = 'active_revision') = 0,
    'ALTER TABLE xnet_mlops_mep_model_deployment ADD COLUMN active_revision BIGINT NULL COMMENT ''当前修订''',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt; EXECUTE goai_stmt; DEALLOCATE PREPARE goai_stmt;

SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name
     AND TABLE_NAME = 'xnet_mlops_mep_model_deployment' AND COLUMN_NAME = 'resource_version') = 0,
    'ALTER TABLE xnet_mlops_mep_model_deployment ADD COLUMN resource_version BIGINT NOT NULL DEFAULT 1 COMMENT ''乐观锁版本''',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt; EXECUTE goai_stmt; DEALLOCATE PREPARE goai_stmt;

SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name
     AND TABLE_NAME = 'xnet_mlops_mep_model_deployment' AND COLUMN_NAME = 'last_verified_at') = 0,
    'ALTER TABLE xnet_mlops_mep_model_deployment ADD COLUMN last_verified_at DATETIME(3) NULL COMMENT ''最近验证时间''',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt; EXECUTE goai_stmt; DEALLOCATE PREPARE goai_stmt;

-- V20260802_16__goai_fixture.sql
-- GOAI 1.0.0 固定 MLOps Fixture。镜像必须由受控演示环境预先提供。
SET time_zone = '+00:00';

INSERT INTO xnet_mlops_mep_deploy_node
(uid, name, ip_address, port, status, cpu_cores, memory_gb, gpu_info, docker_version,
 nginx_status, labels, description, created_by)
VALUES
('node_goai_local', 'GOAI 本地 Docker 节点', '127.0.0.1', 22, 'online', 8, 16, NULL,
 '24+', 'running', '["goai","competition","docker"]', '受控比赛演示节点，不保存 SSH 凭据', 'goai-fixture')
ON DUPLICATE KEY UPDATE status = VALUES(status), docker_version = VALUES(docker_version),
labels = VALUES(labels), description = VALUES(description);

INSERT INTO xnet_mlops_mep_model_contract
(uid, model_uid, model_version, input_dimension, input_schema_json, contract_hash, source, created_by, created_at)
VALUES
('contract_risk_v17', 'risk-model', 'v17', 120, CAST('[{"name":"risk_feature_001","type":"FLOAT64","nullable":false,"ordinal":1},{"name":"risk_feature_002","type":"FLOAT64","nullable":false,"ordinal":2},{"name":"risk_feature_003","type":"FLOAT64","nullable":false,"ordinal":3},{"name":"risk_feature_004","type":"FLOAT64","nullable":false,"ordinal":4},{"name":"risk_feature_005","type":"FLOAT64","nullable":false,"ordinal":5},{"name":"risk_feature_006","type":"FLOAT64","nullable":false,"ordinal":6},{"name":"risk_feature_007","type":"FLOAT64","nullable":false,"ordinal":7},{"name":"risk_feature_008","type":"FLOAT64","nullable":false,"ordinal":8},{"name":"risk_feature_009","type":"FLOAT64","nullable":false,"ordinal":9},{"name":"risk_feature_010","type":"FLOAT64","nullable":false,"ordinal":10},{"name":"risk_feature_011","type":"FLOAT64","nullable":false,"ordinal":11},{"name":"risk_feature_012","type":"FLOAT64","nullable":false,"ordinal":12},{"name":"risk_feature_013","type":"FLOAT64","nullable":false,"ordinal":13},{"name":"risk_feature_014","type":"FLOAT64","nullable":false,"ordinal":14},{"name":"risk_feature_015","type":"FLOAT64","nullable":false,"ordinal":15},{"name":"risk_feature_016","type":"FLOAT64","nullable":false,"ordinal":16},{"name":"risk_feature_017","type":"FLOAT64","nullable":false,"ordinal":17},{"name":"risk_feature_018","type":"FLOAT64","nullable":false,"ordinal":18},{"name":"risk_feature_019","type":"FLOAT64","nullable":false,"ordinal":19},{"name":"risk_feature_020","type":"FLOAT64","nullable":false,"ordinal":20},{"name":"risk_feature_021","type":"FLOAT64","nullable":false,"ordinal":21},{"name":"risk_feature_022","type":"FLOAT64","nullable":false,"ordinal":22},{"name":"risk_feature_023","type":"FLOAT64","nullable":false,"ordinal":23},{"name":"risk_feature_024","type":"FLOAT64","nullable":false,"ordinal":24},{"name":"risk_feature_025","type":"FLOAT64","nullable":false,"ordinal":25},{"name":"risk_feature_026","type":"FLOAT64","nullable":false,"ordinal":26},{"name":"risk_feature_027","type":"FLOAT64","nullable":false,"ordinal":27},{"name":"risk_feature_028","type":"FLOAT64","nullable":false,"ordinal":28},{"name":"risk_feature_029","type":"FLOAT64","nullable":false,"ordinal":29},{"name":"risk_feature_030","type":"FLOAT64","nullable":false,"ordinal":30},{"name":"risk_feature_031","type":"FLOAT64","nullable":false,"ordinal":31},{"name":"risk_feature_032","type":"FLOAT64","nullable":false,"ordinal":32},{"name":"risk_feature_033","type":"FLOAT64","nullable":false,"ordinal":33},{"name":"risk_feature_034","type":"FLOAT64","nullable":false,"ordinal":34},{"name":"risk_feature_035","type":"FLOAT64","nullable":false,"ordinal":35},{"name":"risk_feature_036","type":"FLOAT64","nullable":false,"ordinal":36},{"name":"risk_feature_037","type":"FLOAT64","nullable":false,"ordinal":37},{"name":"risk_feature_038","type":"FLOAT64","nullable":false,"ordinal":38},{"name":"risk_feature_039","type":"FLOAT64","nullable":false,"ordinal":39},{"name":"risk_feature_040","type":"FLOAT64","nullable":false,"ordinal":40},{"name":"risk_feature_041","type":"FLOAT64","nullable":false,"ordinal":41},{"name":"risk_feature_042","type":"FLOAT64","nullable":false,"ordinal":42},{"name":"risk_feature_043","type":"FLOAT64","nullable":false,"ordinal":43},{"name":"risk_feature_044","type":"FLOAT64","nullable":false,"ordinal":44},{"name":"risk_feature_045","type":"FLOAT64","nullable":false,"ordinal":45},{"name":"risk_feature_046","type":"FLOAT64","nullable":false,"ordinal":46},{"name":"risk_feature_047","type":"FLOAT64","nullable":false,"ordinal":47},{"name":"risk_feature_048","type":"FLOAT64","nullable":false,"ordinal":48},{"name":"risk_feature_049","type":"FLOAT64","nullable":false,"ordinal":49},{"name":"risk_feature_050","type":"FLOAT64","nullable":false,"ordinal":50},{"name":"risk_feature_051","type":"FLOAT64","nullable":false,"ordinal":51},{"name":"risk_feature_052","type":"FLOAT64","nullable":false,"ordinal":52},{"name":"risk_feature_053","type":"FLOAT64","nullable":false,"ordinal":53},{"name":"risk_feature_054","type":"FLOAT64","nullable":false,"ordinal":54},{"name":"risk_feature_055","type":"FLOAT64","nullable":false,"ordinal":55},{"name":"risk_feature_056","type":"FLOAT64","nullable":false,"ordinal":56},{"name":"risk_feature_057","type":"FLOAT64","nullable":false,"ordinal":57},{"name":"risk_feature_058","type":"FLOAT64","nullable":false,"ordinal":58},{"name":"risk_feature_059","type":"FLOAT64","nullable":false,"ordinal":59},{"name":"risk_feature_060","type":"FLOAT64","nullable":false,"ordinal":60},{"name":"risk_feature_061","type":"FLOAT64","nullable":false,"ordinal":61},{"name":"risk_feature_062","type":"FLOAT64","nullable":false,"ordinal":62},{"name":"risk_feature_063","type":"FLOAT64","nullable":false,"ordinal":63},{"name":"risk_feature_064","type":"FLOAT64","nullable":false,"ordinal":64},{"name":"risk_feature_065","type":"FLOAT64","nullable":false,"ordinal":65},{"name":"risk_feature_066","type":"FLOAT64","nullable":false,"ordinal":66},{"name":"risk_feature_067","type":"FLOAT64","nullable":false,"ordinal":67},{"name":"risk_feature_068","type":"FLOAT64","nullable":false,"ordinal":68},{"name":"risk_feature_069","type":"FLOAT64","nullable":false,"ordinal":69},{"name":"risk_feature_070","type":"FLOAT64","nullable":false,"ordinal":70},{"name":"risk_feature_071","type":"FLOAT64","nullable":false,"ordinal":71},{"name":"risk_feature_072","type":"FLOAT64","nullable":false,"ordinal":72},{"name":"risk_feature_073","type":"FLOAT64","nullable":false,"ordinal":73},{"name":"risk_feature_074","type":"FLOAT64","nullable":false,"ordinal":74},{"name":"risk_feature_075","type":"FLOAT64","nullable":false,"ordinal":75},{"name":"risk_feature_076","type":"FLOAT64","nullable":false,"ordinal":76},{"name":"risk_feature_077","type":"FLOAT64","nullable":false,"ordinal":77},{"name":"risk_feature_078","type":"FLOAT64","nullable":false,"ordinal":78},{"name":"risk_feature_079","type":"FLOAT64","nullable":false,"ordinal":79},{"name":"risk_feature_080","type":"FLOAT64","nullable":false,"ordinal":80},{"name":"risk_feature_081","type":"FLOAT64","nullable":false,"ordinal":81},{"name":"risk_feature_082","type":"FLOAT64","nullable":false,"ordinal":82},{"name":"risk_feature_083","type":"FLOAT64","nullable":false,"ordinal":83},{"name":"risk_feature_084","type":"FLOAT64","nullable":false,"ordinal":84},{"name":"risk_feature_085","type":"FLOAT64","nullable":false,"ordinal":85},{"name":"risk_feature_086","type":"FLOAT64","nullable":false,"ordinal":86},{"name":"risk_feature_087","type":"FLOAT64","nullable":false,"ordinal":87},{"name":"risk_feature_088","type":"FLOAT64","nullable":false,"ordinal":88},{"name":"risk_feature_089","type":"FLOAT64","nullable":false,"ordinal":89},{"name":"risk_feature_090","type":"FLOAT64","nullable":false,"ordinal":90},{"name":"risk_feature_091","type":"FLOAT64","nullable":false,"ordinal":91},{"name":"risk_feature_092","type":"FLOAT64","nullable":false,"ordinal":92},{"name":"risk_feature_093","type":"FLOAT64","nullable":false,"ordinal":93},{"name":"risk_feature_094","type":"FLOAT64","nullable":false,"ordinal":94},{"name":"risk_feature_095","type":"FLOAT64","nullable":false,"ordinal":95},{"name":"risk_feature_096","type":"FLOAT64","nullable":false,"ordinal":96},{"name":"risk_feature_097","type":"FLOAT64","nullable":false,"ordinal":97},{"name":"risk_feature_098","type":"FLOAT64","nullable":false,"ordinal":98},{"name":"risk_feature_099","type":"FLOAT64","nullable":false,"ordinal":99},{"name":"risk_feature_100","type":"FLOAT64","nullable":false,"ordinal":100},{"name":"risk_feature_101","type":"FLOAT64","nullable":false,"ordinal":101},{"name":"risk_feature_102","type":"FLOAT64","nullable":false,"ordinal":102},{"name":"risk_feature_103","type":"FLOAT64","nullable":false,"ordinal":103},{"name":"risk_feature_104","type":"FLOAT64","nullable":false,"ordinal":104},{"name":"risk_feature_105","type":"FLOAT64","nullable":false,"ordinal":105},{"name":"risk_feature_106","type":"FLOAT64","nullable":false,"ordinal":106},{"name":"risk_feature_107","type":"FLOAT64","nullable":false,"ordinal":107},{"name":"risk_feature_108","type":"FLOAT64","nullable":false,"ordinal":108},{"name":"risk_feature_109","type":"FLOAT64","nullable":false,"ordinal":109},{"name":"risk_feature_110","type":"FLOAT64","nullable":false,"ordinal":110},{"name":"risk_feature_111","type":"FLOAT64","nullable":false,"ordinal":111},{"name":"risk_feature_112","type":"FLOAT64","nullable":false,"ordinal":112},{"name":"risk_feature_113","type":"FLOAT64","nullable":false,"ordinal":113},{"name":"risk_feature_114","type":"FLOAT64","nullable":false,"ordinal":114},{"name":"risk_feature_115","type":"FLOAT64","nullable":false,"ordinal":115},{"name":"risk_feature_116","type":"FLOAT64","nullable":false,"ordinal":116},{"name":"risk_feature_117","type":"FLOAT64","nullable":false,"ordinal":117},{"name":"risk_feature_118","type":"FLOAT64","nullable":false,"ordinal":118},{"name":"risk_feature_119","type":"FLOAT64","nullable":false,"ordinal":119},{"name":"risk_feature_120","type":"FLOAT64","nullable":false,"ordinal":120}]' AS JSON), 'c5418359865186174c0683d4dd520f3342e935adb7f6504e3a01ffb22a457480', 'MTP_RELEASE', 'goai-fixture', '2026-08-01 09:00:00.000'),
('contract_risk_v18', 'risk-model', 'v18', 128, CAST('[{"name":"risk_feature_001","type":"FLOAT64","nullable":false,"ordinal":1},{"name":"risk_feature_002","type":"FLOAT64","nullable":false,"ordinal":2},{"name":"risk_feature_003","type":"FLOAT64","nullable":false,"ordinal":3},{"name":"risk_feature_004","type":"FLOAT64","nullable":false,"ordinal":4},{"name":"risk_feature_005","type":"FLOAT64","nullable":false,"ordinal":5},{"name":"risk_feature_006","type":"FLOAT64","nullable":false,"ordinal":6},{"name":"risk_feature_007","type":"FLOAT64","nullable":false,"ordinal":7},{"name":"risk_feature_008","type":"FLOAT64","nullable":false,"ordinal":8},{"name":"risk_feature_009","type":"FLOAT64","nullable":false,"ordinal":9},{"name":"risk_feature_010","type":"FLOAT64","nullable":false,"ordinal":10},{"name":"risk_feature_011","type":"FLOAT64","nullable":false,"ordinal":11},{"name":"risk_feature_012","type":"FLOAT64","nullable":false,"ordinal":12},{"name":"risk_feature_013","type":"FLOAT64","nullable":false,"ordinal":13},{"name":"risk_feature_014","type":"FLOAT64","nullable":false,"ordinal":14},{"name":"risk_feature_015","type":"FLOAT64","nullable":false,"ordinal":15},{"name":"risk_feature_016","type":"FLOAT64","nullable":false,"ordinal":16},{"name":"risk_feature_017","type":"FLOAT64","nullable":false,"ordinal":17},{"name":"risk_feature_018","type":"FLOAT64","nullable":false,"ordinal":18},{"name":"risk_feature_019","type":"FLOAT64","nullable":false,"ordinal":19},{"name":"risk_feature_020","type":"FLOAT64","nullable":false,"ordinal":20},{"name":"risk_feature_021","type":"FLOAT64","nullable":false,"ordinal":21},{"name":"risk_feature_022","type":"FLOAT64","nullable":false,"ordinal":22},{"name":"risk_feature_023","type":"FLOAT64","nullable":false,"ordinal":23},{"name":"risk_feature_024","type":"FLOAT64","nullable":false,"ordinal":24},{"name":"risk_feature_025","type":"FLOAT64","nullable":false,"ordinal":25},{"name":"risk_feature_026","type":"FLOAT64","nullable":false,"ordinal":26},{"name":"risk_feature_027","type":"FLOAT64","nullable":false,"ordinal":27},{"name":"risk_feature_028","type":"FLOAT64","nullable":false,"ordinal":28},{"name":"risk_feature_029","type":"FLOAT64","nullable":false,"ordinal":29},{"name":"risk_feature_030","type":"FLOAT64","nullable":false,"ordinal":30},{"name":"risk_feature_031","type":"FLOAT64","nullable":false,"ordinal":31},{"name":"risk_feature_032","type":"FLOAT64","nullable":false,"ordinal":32},{"name":"risk_feature_033","type":"FLOAT64","nullable":false,"ordinal":33},{"name":"risk_feature_034","type":"FLOAT64","nullable":false,"ordinal":34},{"name":"risk_feature_035","type":"FLOAT64","nullable":false,"ordinal":35},{"name":"risk_feature_036","type":"FLOAT64","nullable":false,"ordinal":36},{"name":"risk_feature_037","type":"FLOAT64","nullable":false,"ordinal":37},{"name":"risk_feature_038","type":"FLOAT64","nullable":false,"ordinal":38},{"name":"risk_feature_039","type":"FLOAT64","nullable":false,"ordinal":39},{"name":"risk_feature_040","type":"FLOAT64","nullable":false,"ordinal":40},{"name":"risk_feature_041","type":"FLOAT64","nullable":false,"ordinal":41},{"name":"risk_feature_042","type":"FLOAT64","nullable":false,"ordinal":42},{"name":"risk_feature_043","type":"FLOAT64","nullable":false,"ordinal":43},{"name":"risk_feature_044","type":"FLOAT64","nullable":false,"ordinal":44},{"name":"risk_feature_045","type":"FLOAT64","nullable":false,"ordinal":45},{"name":"risk_feature_046","type":"FLOAT64","nullable":false,"ordinal":46},{"name":"risk_feature_047","type":"FLOAT64","nullable":false,"ordinal":47},{"name":"risk_feature_048","type":"FLOAT64","nullable":false,"ordinal":48},{"name":"risk_feature_049","type":"FLOAT64","nullable":false,"ordinal":49},{"name":"risk_feature_050","type":"FLOAT64","nullable":false,"ordinal":50},{"name":"risk_feature_051","type":"FLOAT64","nullable":false,"ordinal":51},{"name":"risk_feature_052","type":"FLOAT64","nullable":false,"ordinal":52},{"name":"risk_feature_053","type":"FLOAT64","nullable":false,"ordinal":53},{"name":"risk_feature_054","type":"FLOAT64","nullable":false,"ordinal":54},{"name":"risk_feature_055","type":"FLOAT64","nullable":false,"ordinal":55},{"name":"risk_feature_056","type":"FLOAT64","nullable":false,"ordinal":56},{"name":"risk_feature_057","type":"FLOAT64","nullable":false,"ordinal":57},{"name":"risk_feature_058","type":"FLOAT64","nullable":false,"ordinal":58},{"name":"risk_feature_059","type":"FLOAT64","nullable":false,"ordinal":59},{"name":"risk_feature_060","type":"FLOAT64","nullable":false,"ordinal":60},{"name":"risk_feature_061","type":"FLOAT64","nullable":false,"ordinal":61},{"name":"risk_feature_062","type":"FLOAT64","nullable":false,"ordinal":62},{"name":"risk_feature_063","type":"FLOAT64","nullable":false,"ordinal":63},{"name":"risk_feature_064","type":"FLOAT64","nullable":false,"ordinal":64},{"name":"risk_feature_065","type":"FLOAT64","nullable":false,"ordinal":65},{"name":"risk_feature_066","type":"FLOAT64","nullable":false,"ordinal":66},{"name":"risk_feature_067","type":"FLOAT64","nullable":false,"ordinal":67},{"name":"risk_feature_068","type":"FLOAT64","nullable":false,"ordinal":68},{"name":"risk_feature_069","type":"FLOAT64","nullable":false,"ordinal":69},{"name":"risk_feature_070","type":"FLOAT64","nullable":false,"ordinal":70},{"name":"risk_feature_071","type":"FLOAT64","nullable":false,"ordinal":71},{"name":"risk_feature_072","type":"FLOAT64","nullable":false,"ordinal":72},{"name":"risk_feature_073","type":"FLOAT64","nullable":false,"ordinal":73},{"name":"risk_feature_074","type":"FLOAT64","nullable":false,"ordinal":74},{"name":"risk_feature_075","type":"FLOAT64","nullable":false,"ordinal":75},{"name":"risk_feature_076","type":"FLOAT64","nullable":false,"ordinal":76},{"name":"risk_feature_077","type":"FLOAT64","nullable":false,"ordinal":77},{"name":"risk_feature_078","type":"FLOAT64","nullable":false,"ordinal":78},{"name":"risk_feature_079","type":"FLOAT64","nullable":false,"ordinal":79},{"name":"risk_feature_080","type":"FLOAT64","nullable":false,"ordinal":80},{"name":"risk_feature_081","type":"FLOAT64","nullable":false,"ordinal":81},{"name":"risk_feature_082","type":"FLOAT64","nullable":false,"ordinal":82},{"name":"risk_feature_083","type":"FLOAT64","nullable":false,"ordinal":83},{"name":"risk_feature_084","type":"FLOAT64","nullable":false,"ordinal":84},{"name":"risk_feature_085","type":"FLOAT64","nullable":false,"ordinal":85},{"name":"risk_feature_086","type":"FLOAT64","nullable":false,"ordinal":86},{"name":"risk_feature_087","type":"FLOAT64","nullable":false,"ordinal":87},{"name":"risk_feature_088","type":"FLOAT64","nullable":false,"ordinal":88},{"name":"risk_feature_089","type":"FLOAT64","nullable":false,"ordinal":89},{"name":"risk_feature_090","type":"FLOAT64","nullable":false,"ordinal":90},{"name":"risk_feature_091","type":"FLOAT64","nullable":false,"ordinal":91},{"name":"risk_feature_092","type":"FLOAT64","nullable":false,"ordinal":92},{"name":"risk_feature_093","type":"FLOAT64","nullable":false,"ordinal":93},{"name":"risk_feature_094","type":"FLOAT64","nullable":false,"ordinal":94},{"name":"risk_feature_095","type":"FLOAT64","nullable":false,"ordinal":95},{"name":"risk_feature_096","type":"FLOAT64","nullable":false,"ordinal":96},{"name":"risk_feature_097","type":"FLOAT64","nullable":false,"ordinal":97},{"name":"risk_feature_098","type":"FLOAT64","nullable":false,"ordinal":98},{"name":"risk_feature_099","type":"FLOAT64","nullable":false,"ordinal":99},{"name":"risk_feature_100","type":"FLOAT64","nullable":false,"ordinal":100},{"name":"risk_feature_101","type":"FLOAT64","nullable":false,"ordinal":101},{"name":"risk_feature_102","type":"FLOAT64","nullable":false,"ordinal":102},{"name":"risk_feature_103","type":"FLOAT64","nullable":false,"ordinal":103},{"name":"risk_feature_104","type":"FLOAT64","nullable":false,"ordinal":104},{"name":"risk_feature_105","type":"FLOAT64","nullable":false,"ordinal":105},{"name":"risk_feature_106","type":"FLOAT64","nullable":false,"ordinal":106},{"name":"risk_feature_107","type":"FLOAT64","nullable":false,"ordinal":107},{"name":"risk_feature_108","type":"FLOAT64","nullable":false,"ordinal":108},{"name":"risk_feature_109","type":"FLOAT64","nullable":false,"ordinal":109},{"name":"risk_feature_110","type":"FLOAT64","nullable":false,"ordinal":110},{"name":"risk_feature_111","type":"FLOAT64","nullable":false,"ordinal":111},{"name":"risk_feature_112","type":"FLOAT64","nullable":false,"ordinal":112},{"name":"risk_feature_113","type":"FLOAT64","nullable":false,"ordinal":113},{"name":"risk_feature_114","type":"FLOAT64","nullable":false,"ordinal":114},{"name":"risk_feature_115","type":"FLOAT64","nullable":false,"ordinal":115},{"name":"risk_feature_116","type":"FLOAT64","nullable":false,"ordinal":116},{"name":"risk_feature_117","type":"FLOAT64","nullable":false,"ordinal":117},{"name":"risk_feature_118","type":"FLOAT64","nullable":false,"ordinal":118},{"name":"risk_feature_119","type":"FLOAT64","nullable":false,"ordinal":119},{"name":"risk_feature_120","type":"FLOAT64","nullable":false,"ordinal":120},{"name":"risk_feature_121","type":"FLOAT64","nullable":false,"ordinal":121},{"name":"risk_feature_122","type":"FLOAT64","nullable":false,"ordinal":122},{"name":"risk_feature_123","type":"FLOAT64","nullable":false,"ordinal":123},{"name":"risk_feature_124","type":"FLOAT64","nullable":false,"ordinal":124},{"name":"risk_feature_125","type":"FLOAT64","nullable":false,"ordinal":125},{"name":"risk_feature_126","type":"FLOAT64","nullable":false,"ordinal":126},{"name":"risk_feature_127","type":"FLOAT64","nullable":false,"ordinal":127},{"name":"risk_feature_128","type":"FLOAT64","nullable":false,"ordinal":128}]' AS JSON), 'e8a74ae1585a079a21dd6843cb21b048de0ddb38ffa0ff21ee6c84b83fa4e43d', 'MTP_RELEASE', 'goai-fixture', '2026-08-02 09:00:00.000')
ON DUPLICATE KEY UPDATE input_dimension = VALUES(input_dimension), input_schema_json = VALUES(input_schema_json),
contract_hash = VALUES(contract_hash), source = VALUES(source);

INSERT INTO xnet_mlops_mep_model_deployment
(uid, name, model_source, model_uid, model_name, model_version, node_uid, node_name, status,
 container_id, container_name, image_name, port, endpoint, replicas, resource_config, nginx_config,
 health_check, created_by, created_at, updated_at, active_revision, resource_version, last_verified_at)
VALUES
('deploy_risk_prod', '生产风险推理服务', 'mtp', 'risk-model', 'risk-model', 'v18',
 'node_goai_local', 'GOAI 本地 Docker 节点', 'running', NULL, 'risk-inference',
 'synapxnet/risk-model:v18', 18018, 'http://127.0.0.1:18018/predict', 1,
 '{"cpu":"2","memory":"4Gi"}', '{"enabled":false}',
 '{"path":"/health","predictionPath":"/predict","readyTimeoutSeconds":90}', 'goai-fixture',
 '2026-08-02 09:10:00', '2026-08-02 10:00:00', 18, 42, '2026-08-02 09:12:00.000')
ON DUPLICATE KEY UPDATE model_version = VALUES(model_version), status = VALUES(status),
container_name = VALUES(container_name), image_name = VALUES(image_name), endpoint = VALUES(endpoint),
active_revision = VALUES(active_revision), resource_version = VALUES(resource_version),
last_verified_at = VALUES(last_verified_at);

INSERT INTO xnet_mlops_mep_deployment_revision
(uid, deployment_uid, revision_number, model_uid, model_version, model_contract_uid,
 image_name, spec_json, spec_hash, created_by, created_at, source_action_id)
VALUES
('revision_risk_17', 'deploy_risk_prod', 17, 'risk-model', 'v17', 'contract_risk_v17',
 'synapxnet/risk-model:v17', CAST('{"runtime":"docker","containerName":"risk-inference","image":"synapxnet/risk-model:v17","containerPort":8080,"hostPort":18018,"replicas":1,"secretRefs":[]}' AS JSON), '8c5cdbd7adbcd8561ef0797d33f8641668d73453846b90da1747627d368db0f9', 'goai-fixture', '2026-08-01 09:10:00.000', NULL),
('revision_risk_18', 'deploy_risk_prod', 18, 'risk-model', 'v18', 'contract_risk_v18',
 'synapxnet/risk-model:v18', CAST('{"runtime":"docker","containerName":"risk-inference","image":"synapxnet/risk-model:v18","containerPort":8080,"hostPort":18018,"replicas":1,"secretRefs":[]}' AS JSON), '7127e43fa46df178688e6ee4ddc50f0de4112b18b744ceb65c99605388c60f9e', 'goai-fixture', '2026-08-02 09:10:00.000', NULL)
ON DUPLICATE KEY UPDATE model_version = VALUES(model_version), model_contract_uid = VALUES(model_contract_uid),
image_name = VALUES(image_name), spec_json = VALUES(spec_json), spec_hash = VALUES(spec_hash);

SELECT
 (SELECT input_dimension FROM xnet_mlops_mep_model_contract WHERE uid = 'contract_risk_v18') = 128 AS v18_contract_ready,
 (SELECT input_dimension FROM xnet_mlops_mep_model_contract WHERE uid = 'contract_risk_v17') = 120 AS v17_contract_ready,
 (SELECT resource_version FROM xnet_mlops_mep_model_deployment WHERE uid = 'deploy_risk_prod') = 42 AS deployment_version_ready;
