-- OpenClaw实例管理表
-- 用于MEP模块管理OpenClaw个人助手的部署实例

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_openclaw_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(200) NOT NULL COMMENT '实例名称',
    `description` TEXT COMMENT '实例描述',

    -- 部署配置
    `deploy_mode` VARCHAR(50) DEFAULT 'docker' COMMENT '部署模式: docker, npm, source',
    `deploy_node_id` VARCHAR(64) COMMENT '部署节点ID',
    `workstation_id` BIGINT COMMENT 'SMP工作站ID',
    `gateway_host` VARCHAR(200) DEFAULT 'localhost' COMMENT 'Gateway主机地址',
    `gateway_port` INT DEFAULT 18789 COMMENT 'Gateway端口',
    `gateway_token` VARCHAR(500) COMMENT 'Gateway访问令牌',

    -- 模型配置
    `default_model` VARCHAR(200) DEFAULT 'anthropic/claude-sonnet-4-5' COMMENT '默认模型（primary）',
    `fallback_models` VARCHAR(1000) DEFAULT NULL COMMENT '回退模型列表（JSON数组，主模型失败时按顺序尝试）',
    `subagent_model` VARCHAR(200) DEFAULT NULL COMMENT '子代理模型（用于子任务的轻量模型）',
    `llm_service_id` BIGINT COMMENT '关联的MEP LLM服务ID',
    `api_key_id` VARCHAR(64) COMMENT '加密后的API密钥（直接输入时AES加密存储）',
    `api_key_ref_id` BIGINT COMMENT '引用MEP API密钥管理模块中的密钥ID',

    -- 技能配置
    `enabled_skills` JSON COMMENT '启用的技能列表',
    `skills_config` JSON COMMENT '技能配置',

    -- 渠道配置
    `channels_config` JSON COMMENT '渠道配置',

    -- 运行状态
    `status` VARCHAR(50) DEFAULT 'stopped' COMMENT '状态: stopped, starting, running, error',
    `container_name` VARCHAR(200) COMMENT 'Docker容器名称',
    `process_id` VARCHAR(50) COMMENT '进程ID',
    `last_error` TEXT COMMENT '最后错误信息',

    -- 资源配置
    `workspace_path` VARCHAR(500) COMMENT '工作区路径',
    `config_path` VARCHAR(500) COMMENT '配置文件路径',
    `logs_path` VARCHAR(500) COMMENT '日志路径',

    -- 统计信息
    `total_conversations` BIGINT DEFAULT 0 COMMENT '总对话数',
    `total_messages` BIGINT DEFAULT 0 COMMENT '总消息数',
    `last_active_at` DATETIME COMMENT '最后活跃时间',

    -- 元数据
    `tenant_uid` VARCHAR(64) COMMENT '租户UID',
    `created_by` VARCHAR(64) COMMENT '创建者',
    `updated_by` VARCHAR(64) COMMENT '更新者',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant` (`tenant_uid`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OpenClaw实例表';


-- XAA智能助手表
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_assistant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(200) NOT NULL COMMENT '助手名称',
    `description` TEXT COMMENT '助手描述',
    `avatar` VARCHAR(500) COMMENT '头像URL',

    -- OpenClaw配置
    `openclaw_instance_id` BIGINT COMMENT '关联的OpenClaw实例ID',
    `gateway_url` VARCHAR(500) COMMENT 'Gateway WebSocket URL',

    -- 模型配置
    `llm_service_id` BIGINT COMMENT '关联的MEP LLM服务ID',
    `default_model` VARCHAR(200) COMMENT '默认模型',
    `system_prompt` TEXT COMMENT '系统提示词',
    `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
    `max_tokens` INT DEFAULT 4096 COMMENT '最大token数',

    -- 知识库配置
    `knowledge_base_ids` JSON COMMENT '关联的DPP知识库ID列表',
    `rag_enabled` BOOLEAN DEFAULT FALSE COMMENT '是否启用RAG',
    `rag_top_k` INT DEFAULT 5 COMMENT 'RAG返回结果数',

    -- 技能配置
    `skill_ids` JSON COMMENT '关联的XAA技能ID列表',
    `tools_enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用工具调用',

    -- UI配置
    `ui_config` JSON COMMENT 'UI配置(位置、颜色、大小等)',
    `welcome_message` TEXT COMMENT '欢迎消息',
    `placeholder` VARCHAR(200) DEFAULT '有什么可以帮助您的?' COMMENT '输入框占位符',

    -- 状态
    `status` VARCHAR(50) DEFAULT 'active' COMMENT '状态: active, disabled',
    `is_default` BOOLEAN DEFAULT FALSE COMMENT '是否为默认助手',

    -- 统计信息
    `total_conversations` BIGINT DEFAULT 0 COMMENT '总对话数',
    `total_messages` BIGINT DEFAULT 0 COMMENT '总消息数',
    `last_used_at` DATETIME COMMENT '最后使用时间',

    -- 元数据
    `tenant_uid` VARCHAR(64) COMMENT '租户UID',
    `created_by` VARCHAR(64) COMMENT '创建者',
    `updated_by` VARCHAR(64) COMMENT '更新者',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_status` (`status`),
    KEY `idx_openclaw` (`openclaw_instance_id`),
    KEY `idx_tenant` (`tenant_uid`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA智能助手表';


-- XAA智能助手会话表
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_assistant_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `assistant_id` BIGINT NOT NULL COMMENT '助手ID',
    `title` VARCHAR(200) COMMENT '会话标题',
    `summary` TEXT COMMENT '会话摘要',

    -- 用户信息
    `user_id` VARCHAR(64) COMMENT '用户ID',
    `user_name` VARCHAR(100) COMMENT '用户名',

    -- 状态
    `status` VARCHAR(50) DEFAULT 'active' COMMENT '状态: active, archived, deleted',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `token_count` BIGINT DEFAULT 0 COMMENT 'Token消耗',

    -- 时间
    `last_message_at` DATETIME COMMENT '最后消息时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_assistant` (`assistant_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA智能助手会话表';


-- XAA智能助手消息表
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_assistant_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user, assistant, system',
    `content` LONGTEXT NOT NULL COMMENT '消息内容',

    -- 元数据
    `token_count` INT COMMENT 'Token数量',
    `model_used` VARCHAR(200) COMMENT '使用的模型',
    `rag_sources` JSON COMMENT 'RAG来源文档',
    `tool_calls` JSON COMMENT '工具调用记录',
    `metadata` JSON COMMENT '其他元数据',

    -- 时间
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_conversation` (`conversation_id`),
    KEY `idx_role` (`role`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA智能助手消息表';
