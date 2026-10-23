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
('node-001', 'GPU节点-01', '192.168.10.101', 22, 'online', 32, 64, 'NVIDIA RTX 4090 x2', '24.0.7', 'running', '["gpu", "production"]', '生产环境GPU计算节点', 'admin'),
('node-002', 'CPU节点-01', '192.168.10.102', 22, 'online', 16, 32, NULL, '24.0.7', 'running', '["cpu", "production"]', '生产环境CPU计算节点', 'admin'),
('node-003', '测试节点-01', '192.168.10.103', 22, 'offline', 8, 16, NULL, '23.0.6', 'stopped', '["test"]', '测试环境节点', 'admin');
