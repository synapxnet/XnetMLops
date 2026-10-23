-- Copyright (C) 2026 Synapxnet. All rights reserved.
-- This file is Synapxnet Proprietary and Confidential. It is strictly
-- forbidden to copy, distribute, or use without explicit authorization.
-- 主线功能完整结构安装 / Complete mainline capability schema.
-- Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
-- Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
-- Prerequisite: initialize a NEW database with XnetMLops.sql, then run this file.
-- For existing databases, back up first. All additions are idempotent and preserve records.
-- No sample workflows, model services, nodes, users, runs, or competition data are inserted.
SET NAMES utf8mb4;


-- Source: feature_operator_sql.sql
CREATE TABLE IF NOT EXISTS xnet_mlops_dpp_feature_task_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL COMMENT '记录UID',
    task_uid VARCHAR(64) NOT NULL COMMENT '特征工程任务UID',
    job_uid VARCHAR(256) NOT NULL COMMENT 'Jenkins Job UID',
    job_status VARCHAR(32) DEFAULT NULL COMMENT '任务状态',
    job_content TEXT DEFAULT NULL COMMENT '任务详情(JSON)',
    start_at DATETIME DEFAULT NULL COMMENT '开始时间',
    end_at DATETIME DEFAULT NULL COMMENT '结束时间',
    schedule_active TINYINT(1) DEFAULT 0 COMMENT '0:立即执行,1:调度执行',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_uid (task_uid),
    INDEX idx_job_uid (job_uid),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DPP特征工程任务执行记录表';

-- Source: feature_operator_sql.sql
CREATE TABLE IF NOT EXISTS xnet_mlops_dpp_feature_operator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL COMMENT '算子UID',
    name VARCHAR(100) NOT NULL COMMENT '算子名称',
    code VARCHAR(50) NOT NULL COMMENT '算子代码',
    description TEXT COMMENT '算子描述',
    category VARCHAR(50) NOT NULL COMMENT '算子分类: format_conversion, feature_transform, data_cleaning',
    output_formats TEXT COMMENT '支持的输出格式(JSON数组)',
    feature_columns TEXT COMMENT '特征字段配置(JSON数组)',
    parameter_schema TEXT COMMENT '算子参数模式(JSON)',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_code (code),
    UNIQUE KEY uk_uid (uid),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DPP特征算子表';

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_rag_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识',
  `name` VARCHAR(200) NOT NULL COMMENT '知识库名称',
  `description` TEXT COMMENT '描述',
  `icon` VARCHAR(100) COMMENT '图标',

  -- 嵌入模型配置
  `embedding_model_id` BIGINT COMMENT '嵌入模型ID',
  `embedding_provider` VARCHAR(50) DEFAULT 'openai' COMMENT '嵌入模型提供商',
  `embedding_model` VARCHAR(100) DEFAULT 'text-embedding-ada-002' COMMENT '嵌入模型名称',
  `embedding_dimension` INT DEFAULT 1536 COMMENT '向量维度',

  -- 向量数据库配置
  `vector_db_type` VARCHAR(50) DEFAULT 'milvus' COMMENT '向量数据库类型',
  `vector_collection` VARCHAR(200) COMMENT '向量集合名称',
  `vector_index_type` VARCHAR(50) DEFAULT 'HNSW' COMMENT '索引类型',

  -- 分块配置
  `chunk_strategy` VARCHAR(50) DEFAULT 'recursive' COMMENT '分块策略: fixed/recursive/semantic',
  `chunk_size` INT DEFAULT 500 COMMENT '分块大小（token）',
  `chunk_overlap` INT DEFAULT 50 COMMENT '分块重叠',
  `chunk_separator` VARCHAR(100) COMMENT '分块分隔符',

  -- 检索配置
  `retrieval_method` VARCHAR(50) DEFAULT 'hybrid' COMMENT '检索方法: semantic/keyword/hybrid',
  `top_k` INT DEFAULT 5 COMMENT '返回结果数',
  `score_threshold` DECIMAL(5,4) DEFAULT 0.5000 COMMENT '分数阈值',
  `rerank_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用重排序',
  `rerank_model` VARCHAR(100) COMMENT '重排序模型',
  `rerank_top_k` INT DEFAULT 3 COMMENT '重排序后返回数量',

  -- 统计信息
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/indexing/error/archived',
  `doc_count` INT DEFAULT 0 COMMENT '文档数量',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总token数',
  `total_size_bytes` BIGINT DEFAULT 0 COMMENT '总文件大小',

  -- 租户与权限
  `tenant_uid` VARCHAR(64) COMMENT '租户ID',
  `creator_id` VARCHAR(64) COMMENT '创建者ID',
  `visibility` VARCHAR(20) DEFAULT 'private' COMMENT '可见性: private/team/public',

  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  INDEX `idx_tenant` (`tenant_uid`),
  INDEX `idx_status` (`status`),
  INDEX `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_rag_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',

  -- 文档信息
  `name` VARCHAR(500) NOT NULL COMMENT '文档名称',
  `original_name` VARCHAR(500) COMMENT '原始文件名',
  `type` VARCHAR(50) COMMENT '文档类型: pdf/docx/txt/md/html/csv',
  `mime_type` VARCHAR(100) COMMENT 'MIME类型',
  `file_path` VARCHAR(1000) COMMENT '文件存储路径',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_hash` VARCHAR(64) COMMENT '文件哈希',

  -- 处理状态
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/processing/completed/failed',
  `process_progress` INT DEFAULT 0 COMMENT '处理进度 0-100',
  `error_message` TEXT COMMENT '错误信息',

  -- 统计信息
  `word_count` INT DEFAULT 0 COMMENT '字数',
  `char_count` INT DEFAULT 0 COMMENT '字符数',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
  `token_count` INT DEFAULT 0 COMMENT 'token数量',
  `page_count` INT DEFAULT 0 COMMENT '页数（PDF等）',

  -- 元数据
  `metadata` JSON COMMENT '元数据',
  `source_url` VARCHAR(1000) COMMENT '来源URL',
  `source_type` VARCHAR(50) DEFAULT 'upload' COMMENT '来源类型: upload/url/sync',

  -- 自定义配置
  `custom_chunk_size` INT COMMENT '自定义分块大小',
  `custom_chunk_overlap` INT COMMENT '自定义分块重叠',

  -- 操作信息
  `uploaded_by` VARCHAR(64) COMMENT '上传者ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `indexed_at` DATETIME COMMENT '索引完成时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  INDEX `idx_kb_id` (`kb_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_type` (`type`),
  CONSTRAINT `fk_doc_kb` FOREIGN KEY (`kb_id`) REFERENCES `xnet_mlops_dpp_knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_rag_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',

  -- 内容
  `content` TEXT NOT NULL COMMENT '分块内容',
  `content_hash` VARCHAR(64) COMMENT '内容哈希',

  -- 位置信息
  `position` INT DEFAULT 0 COMMENT '在文档中的位置',
  `start_index` INT COMMENT '起始字符索引',
  `end_index` INT COMMENT '结束字符索引',
  `page_number` INT COMMENT '页码（PDF等）',

  -- 向量信息
  `embedding_id` VARCHAR(200) COMMENT '向量数据库中的ID',
  `token_count` INT DEFAULT 0 COMMENT 'token数量',
  `char_count` INT DEFAULT 0 COMMENT '字符数',

  -- 层级结构（Parent-Child索引）
  `parent_chunk_id` BIGINT COMMENT '父分块ID',
  `chunk_level` INT DEFAULT 0 COMMENT '层级: 0=叶子, 1=父级, 2=祖父级',

  -- 元数据
  `metadata` JSON COMMENT '元数据',
  `keywords` TEXT COMMENT '关键词（逗号分隔）',
  `summary` TEXT COMMENT '摘要',

  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  INDEX `idx_doc_id` (`doc_id`),
  INDEX `idx_kb_id` (`kb_id`),
  INDEX `idx_position` (`doc_id`, `position`),
  INDEX `idx_embedding` (`embedding_id`),
  CONSTRAINT `fk_chunk_doc` FOREIGN KEY (`doc_id`) REFERENCES `xnet_mlops_dpp_kb_document`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chunk_kb` FOREIGN KEY (`kb_id`) REFERENCES `xnet_mlops_dpp_knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_rag_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_embedding_model` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '模型显示名称',
  `provider` VARCHAR(50) NOT NULL COMMENT '提供商: openai/azure/huggingface/local',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型标识',

  -- 模型参数
  `dimension` INT NOT NULL COMMENT '向量维度',
  `max_tokens` INT DEFAULT 8192 COMMENT '最大token数',
  `batch_size` INT DEFAULT 100 COMMENT '批处理大小',

  -- API配置
  `api_endpoint` VARCHAR(500) COMMENT 'API端点',
  `api_key_encrypted` VARCHAR(500) COMMENT '加密的API Key',
  `extra_config` JSON COMMENT '额外配置',

  -- 状态
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/inactive',
  `tenant_uid` VARCHAR(64) COMMENT '租户ID（NULL表示全局）',

  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model_tenant` (`provider`, `model_name`, `tenant_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='嵌入模型配置表';

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_rag_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_retrieval_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',

  -- 查询信息
  `query` TEXT NOT NULL COMMENT '查询内容',
  `query_tokens` INT COMMENT '查询token数',

  -- 检索配置
  `retrieval_method` VARCHAR(50) COMMENT '检索方法',
  `top_k` INT COMMENT '请求结果数',
  `score_threshold` DECIMAL(5,4) COMMENT '分数阈值',
  `rerank_enabled` TINYINT(1) COMMENT '是否重排序',

  -- 检索结果
  `result_count` INT DEFAULT 0 COMMENT '返回结果数',
  `results` JSON COMMENT '检索结果详情',

  -- 性能指标
  `embedding_latency_ms` INT COMMENT '嵌入耗时(ms)',
  `retrieval_latency_ms` INT COMMENT '检索耗时(ms)',
  `rerank_latency_ms` INT COMMENT '重排序耗时(ms)',
  `total_latency_ms` INT COMMENT '总耗时(ms)',

  -- 来源信息
  `source` VARCHAR(50) DEFAULT 'api' COMMENT '来源: api/workflow/test',
  `session_id` VARCHAR(64) COMMENT '会话ID',
  `user_id` VARCHAR(64) COMMENT '用户ID',
  `tenant_uid` VARCHAR(64) COMMENT '租户ID',

  -- 反馈
  `feedback_score` INT COMMENT '反馈评分 1-5',
  `feedback_comment` TEXT COMMENT '反馈评论',

  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  INDEX `idx_kb_id` (`kb_id`),
  INDEX `idx_created_at` (`created_at`),
  INDEX `idx_user` (`user_id`),
  INDEX `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检索日志表';

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_rag_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_kb_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
  `tag` VARCHAR(100) NOT NULL COMMENT '标签',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_tag` (`kb_id`, `tag`),
  INDEX `idx_tag` (`tag`),
  CONSTRAINT `fk_tag_kb` FOREIGN KEY (`kb_id`) REFERENCES `xnet_mlops_dpp_knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库标签表';

-- Source: mlops-mep-service/src/main/resources/sql/openclaw_tables.sql
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

-- Source: mlops-mep-service/src/main/resources/sql/openclaw_tables.sql
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

-- Source: mlops-mep-service/src/main/resources/sql/openclaw_tables.sql
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

-- Source: mlops-mep-service/src/main/resources/sql/openclaw_tables.sql
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

-- Source: mlops-smp-service/src/main/resources/sql/feature_operator_table.sql
CREATE TABLE IF NOT EXISTS XnetMLops.xnet_mlops_smp_feature_operators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE COMMENT '唯一标识',
    url VARCHAR(255) NOT NULL COMMENT 'Git仓库URL',
    encrypted_token BLOB NOT NULL COMMENT 'AES-256加密的访问令牌',
    operator_name VARCHAR(100) NOT NULL COMMENT '算子名称',
    operator_code VARCHAR(50) NOT NULL COMMENT '算子代码（唯一标识符）',
    operator_version VARCHAR(50) NOT NULL COMMENT '算子版本',
    description TEXT COMMENT '描述信息',
    tenant_uid VARCHAR(50) NOT NULL COMMENT '租户UID',
    dept_uid VARCHAR(50) NOT NULL COMMENT '部门UID',
    team_uid VARCHAR(50) NOT NULL COMMENT '团队UID',
    authorized_tenants VARCHAR(500) NOT NULL COMMENT '授权访问的团队UID列表（逗号分隔）',
    created_by VARCHAR(255) NOT NULL COMMENT '创建人',
    updated_by VARCHAR(255) COMMENT '更新人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_url_code_version (url, operator_code, operator_version) COMMENT 'URL、算子代码和版本组合唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特征算子Git仓库配置表';

-- Source: mlops-smp-service/src/main/resources/sql/hadoop_tables.sql
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_hadoop_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    version_type VARCHAR(20) DEFAULT 'stable' COMMENT '版本类型 (stable/alpha/beta)',
    release_date DATE COMMENT '发布日期',
    download_url VARCHAR(500) COMMENT '下载地址',
    is_latest BOOLEAN DEFAULT FALSE COMMENT '是否最新版本',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hadoop版本表';

-- Source: mlops-smp-service/src/main/resources/sql/hadoop_tables.sql
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_hadoop_cluster (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    uid VARCHAR(36) NOT NULL UNIQUE COMMENT '唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '节点名称',
    description TEXT COMMENT '描述',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT DEFAULT 22 COMMENT 'SSH端口',
    ssh_user VARCHAR(100) COMMENT 'SSH用户名',
    ssh_password VARCHAR(500) COMMENT 'SSH密码(加密)',
    ssh_private_key TEXT COMMENT 'SSH私钥(加密)',
    hadoop_version VARCHAR(50) COMMENT 'Hadoop版本',
    os_type VARCHAR(50) COMMENT '操作系统类型',
    node_type VARCHAR(20) NOT NULL COMMENT '节点类型 (master/node)',
    deploy_mode VARCHAR(20) DEFAULT 'standard' COMMENT '部署模式 (standard/ha)',
    components TEXT COMMENT '组件列表 JSON',
    hdfs_data_dirs TEXT COMMENT 'HDFS数据目录 JSON',
    hdfs_replication INT DEFAULT 3 COMMENT 'HDFS副本数',
    hdfs_block_size BIGINT DEFAULT 134217728 COMMENT 'HDFS块大小(字节)',
    yarn_memory INT DEFAULT 8192 COMMENT 'YARN内存(MB)',
    yarn_cpu INT DEFAULT 4 COMMENT 'YARN CPU核数',
    ha_master_host VARCHAR(255) COMMENT 'HA备用Master地址',
    zk_cluster VARCHAR(500) COMMENT 'ZooKeeper集群地址',
    status VARCHAR(20) DEFAULT 'created' COMMENT '状态 (created/deploying/deployed/running/stopped/failed)',
    deploy_log LONGTEXT COMMENT '部署日志',
    master_id BIGINT COMMENT '关联的Master节点ID',
    created_by VARCHAR(100) COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_node_type (node_type),
    INDEX idx_status (status),
    INDEX idx_master_id (master_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hadoop集群节点表';

-- Source: mlops-smp-service/src/main/resources/sql/hadoop_tables.sql
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_hadoop_version_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(20) COMMENT '同步类型',
    sync_status VARCHAR(20) COMMENT '同步状态 (success/failed)',
    versions_count INT COMMENT '同步版本数量',
    error_message TEXT COMMENT '错误信息',
    sync_duration_ms BIGINT COMMENT '耗时(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hadoop版本同步日志表';

-- Source: mlops-smp-service/src/main/resources/sql/jenkins_master_table.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_masters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符(UUID)',
  `name` VARCHAR(128) NOT NULL COMMENT 'Master名称',

  -- SSH连接配置
  `host` VARCHAR(256) NOT NULL COMMENT '主机地址',
  `port` INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
  `username` VARCHAR(64) NOT NULL COMMENT 'SSH用户名',
  `encrypted_password` VARCHAR(512) NOT NULL COMMENT 'SSH密码(AES加密)',
  `os_type` VARCHAR(32) NOT NULL DEFAULT 'linux' COMMENT '操作系统类型(linux/macos/windows)',

  -- Jenkins配置
  `jenkins_port` INT NOT NULL DEFAULT 8080 COMMENT 'Jenkins HTTP端口',
  `jenkins_home` VARCHAR(512) DEFAULT '/var/jenkins_home' COMMENT 'Jenkins Home目录',
  `jenkins_version` VARCHAR(32) DEFAULT NULL COMMENT 'Jenkins版本',
  `java_version` VARCHAR(32) DEFAULT '17' COMMENT 'Java版本',
  `java_opts` VARCHAR(1024) DEFAULT '-Xmx2g -Xms1g' COMMENT 'JVM参数',

  -- 管理员配置
  `admin_username` VARCHAR(64) DEFAULT 'admin' COMMENT '管理员用户名',
  `encrypted_admin_password` VARCHAR(512) DEFAULT NULL COMMENT '管理员密码(AES加密)',

  -- 凭证配置(JSON格式存储)
  `credentials_config` JSON DEFAULT NULL COMMENT '凭证配置JSON(Git/Harbor/SSH凭证)',

  -- 状态信息
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态(pending/deploying/deployed/failed/running/stopped)',
  `initial_password` VARCHAR(256) DEFAULT NULL COMMENT 'Jenkins初始密码',
  `deploy_log` LONGTEXT DEFAULT NULL COMMENT '部署日志',
  `last_heartbeat` DATETIME DEFAULT NULL COMMENT '最后心跳时间',

  -- 资源配置
  `region` VARCHAR(32) DEFAULT 'guangzhou' COMMENT '地域(guangzhou/beijing/shanghai/...)',
  `cpu_cores` INT DEFAULT 4 COMMENT 'CPU核数',
  `ram_gb` INT DEFAULT 8 COMMENT '内存(GB)',
  `disk_gb` INT DEFAULT 100 COMMENT '磁盘空间(GB)',

  -- 关联字段
  `tenant_uid` VARCHAR(64) DEFAULT NULL COMMENT '租户UID',

  -- 审计字段
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_host` (`host`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_uid`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins Master配置表';

-- Source: mlops-smp-service/src/main/resources/sql/jenkins_node_table.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_nodes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
  `name` VARCHAR(128) NOT NULL COMMENT '节点名称',
  `host` VARCHAR(256) NOT NULL COMMENT '主机地址(IP或域名)',
  `port` INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
  `username` VARCHAR(64) NOT NULL COMMENT 'SSH用户名',
  `encrypted_password` VARCHAR(512) NOT NULL COMMENT 'SSH密码(AES加密)',
  `os_type` VARCHAR(32) NOT NULL DEFAULT 'linux' COMMENT '操作系统类型(linux/macos/windows)',
  `region` VARCHAR(32) DEFAULT 'guangzhou' COMMENT '地域(guangzhou/beijing/shanghai/shenzhen/hangzhou/nanjing/silicon_valley/singapore/tokyo/frankfurt)',
  `container_type` VARCHAR(32) DEFAULT 'docker' COMMENT '容器类型(cce/docker)',
  `resource_type` VARCHAR(32) DEFAULT 'cpu' COMMENT '资源类型(cpu/single_gpu/multi_gpu)',
  `resource_spec` VARCHAR(64) DEFAULT NULL COMMENT '资源规格ID',
  `cpu_cores` INT DEFAULT 2 COMMENT 'CPU核数',
  `ram_gb` INT DEFAULT 128 COMMENT '内存大小(GB)',
  `gpu_memory` INT DEFAULT NULL COMMENT 'GPU显存(GB)',
  `gpu_model` VARCHAR(64) DEFAULT NULL COMMENT 'GPU型号',
  `gpu_count` INT DEFAULT NULL COMMENT 'GPU数量',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态(pending/deploying/deployed/failed/offline)',
  `jenkins_url` VARCHAR(512) DEFAULT NULL COMMENT 'Jenkins Master URL',
  `agent_name` VARCHAR(128) DEFAULT NULL COMMENT 'Jenkins Agent名称',
  `work_dir` VARCHAR(512) DEFAULT NULL COMMENT 'Agent工作目录',
  `java_version` VARCHAR(32) DEFAULT NULL COMMENT 'Java版本',
  `python_version` VARCHAR(32) DEFAULT NULL COMMENT 'Python版本',
  `agent_version` VARCHAR(64) DEFAULT NULL COMMENT 'Jenkins Agent版本',
  `labels` VARCHAR(512) DEFAULT NULL COMMENT '节点标签(空格分隔)',
  `description` TEXT DEFAULT NULL COMMENT '描述信息',
  `deploy_log` LONGTEXT DEFAULT NULL COMMENT '部署日志',
  `last_heartbeat` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `tenant_uid` VARCHAR(64) DEFAULT NULL COMMENT '租户UID',
  `dept_uid` VARCHAR(64) DEFAULT NULL COMMENT '部门UID',
  `team_uid` VARCHAR(64) DEFAULT NULL COMMENT '团队UID',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_name` (`name`),
  KEY `idx_host` (`host`),
  KEY `idx_status` (`status`),
  KEY `idx_os_type` (`os_type`),
  KEY `idx_team_uid` (`team_uid`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins节点配置表';

-- Source: mlops-smp-service/src/main/resources/sql/jenkins_versions_table.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_versions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version` VARCHAR(32) NOT NULL COMMENT '版本号(如: 2.462.3)',
  `version_type` VARCHAR(16) NOT NULL DEFAULT 'stable' COMMENT '版本类型(stable/weekly)',
  `release_date` DATE DEFAULT NULL COMMENT '发布日期',
  `download_url` VARCHAR(512) DEFAULT NULL COMMENT '下载URL',
  `sha256` VARCHAR(128) DEFAULT NULL COMMENT 'SHA256校验值',
  `is_lts` TINYINT(1) DEFAULT 0 COMMENT '是否为LTS版本',
  `is_latest` TINYINT(1) DEFAULT 0 COMMENT '是否为最新版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_type` (`version`, `version_type`),
  KEY `idx_version_type` (`version_type`),
  KEY `idx_is_lts` (`is_lts`),
  KEY `idx_release_date` (`release_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins版本缓存表';

-- Source: mlops-smp-service/src/main/resources/sql/jenkins_versions_table.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_version_sync_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sync_type` VARCHAR(16) NOT NULL COMMENT '同步类型(stable/weekly/all)',
  `sync_status` VARCHAR(16) NOT NULL COMMENT '同步状态(success/failed)',
  `versions_count` INT DEFAULT 0 COMMENT '同步的版本数量',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `sync_duration_ms` BIGINT DEFAULT NULL COMMENT '同步耗时(毫秒)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',

  PRIMARY KEY (`id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins版本同步日志表';

-- Source: mlops-smp-service/src/main/resources/sql/workstation_table.sql
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_workstation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    uid VARCHAR(64) NOT NULL UNIQUE COMMENT '唯一标识符(UUID)',
    name VARCHAR(128) NOT NULL UNIQUE COMMENT '服务器名称(唯一)',

    -- 服务器基本信息。
    vendor VARCHAR(32) NOT NULL COMMENT '服务器厂商: huawei/tencent/alibaba/aws/azure/other',
    server_type VARCHAR(32) NOT NULL COMMENT '服务器类型: vps/cce/cvm/ecs/ec2/other',
    region VARCHAR(64) NOT NULL COMMENT '地域: beijing/shanghai/guangzhou/shenzhen 等',

    -- 操作系统信息
    os_type VARCHAR(16) NOT NULL COMMENT '系统类型: linux/macos/windows',
    os_version VARCHAR(64) NOT NULL COMMENT '系统版本: OpenCloudOS 9/CentOS 7/8/Ubuntu 20.04 等',

    -- 服务器资源 (可手动填写，测试连接时自动更新)
    cpu_cores INT DEFAULT 1 COMMENT 'CPU核数',
    ram_gb INT DEFAULT 1 COMMENT '内存(GB)',
    disk_gb INT DEFAULT 10 COMMENT '磁盘空间(GB)',
    available_disk_gb INT DEFAULT 0 COMMENT '可用磁盘空间(GB) - 自动检测',
    available_ram_gb INT DEFAULT 0 COMMENT '可用内存(GB) - 自动检测',

    -- 网络信息
    domain VARCHAR(256) COMMENT '域名(可选)',
    ip_address VARCHAR(64) NOT NULL COMMENT 'IP地址',

    -- SSH连接配置
    ssh_port INT DEFAULT 22 COMMENT 'SSH端口',
    ssh_user VARCHAR(64) NOT NULL COMMENT 'SSH用户名',
    auth_type VARCHAR(16) NOT NULL DEFAULT 'password' COMMENT '认证方式: password/privateKey',
    encrypted_password TEXT COMMENT 'SSH密码(AES加密存储)',
    encrypted_private_key TEXT COMMENT 'SSH私钥(AES加密存储)',

    -- 状态信息
    status VARCHAR(16) DEFAULT 'pending' COMMENT '状态: pending/online/offline/error',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    last_check_result TEXT COMMENT '最后检测结果(JSON)',

    -- 审计字段
    description TEXT COMMENT '描述',
    created_by VARCHAR(64) COMMENT '创建者',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_vendor (vendor),
    INDEX idx_region (region),
    INDEX idx_status (status),
    INDEX idx_os_type (os_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作站点表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符UUID',
  `name` VARCHAR(200) NOT NULL COMMENT '工作流名称',
  `description` TEXT COMMENT '工作流描述',
  `type` VARCHAR(50) DEFAULT 'workflow' COMMENT '工作流类型: workflow, pipeline, chat',
  `status` VARCHAR(50) DEFAULT 'draft' COMMENT '状态: draft, published, archived',
  `graph_json` LONGTEXT COMMENT '工作流图结构JSON (完整图定义)',
  `config_json` TEXT COMMENT '工作流配置JSON',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `creator_id` VARCHAR(64) COMMENT '创建者ID',
  `creator_name` VARCHAR(100) COMMENT '创建者名称',
  `tenant_uid` VARCHAR(64) COMMENT '租户ID',
  `dept_uid` VARCHAR(64) COMMENT '部门ID',
  `team_uid` VARCHAR(64) COMMENT '团队ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_tenant_uid` (`tenant_uid`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA工作流主表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符UUID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `node_type` VARCHAR(50) NOT NULL COMMENT '节点类型: start, end, dpp-task, mtp-task, mep-task, if-else, loop等',
  `title` VARCHAR(200) NOT NULL COMMENT '节点标题',
  `description` TEXT COMMENT '节点描述',
  `config_json` TEXT COMMENT '节点配置JSON (包含具体任务参数)',
  `position_x` DOUBLE DEFAULT 0 COMMENT '画布X坐标',
  `position_y` DOUBLE DEFAULT 0 COMMENT '画布Y坐标',
  `width` DOUBLE DEFAULT 200 COMMENT '节点宽度',
  `height` DOUBLE DEFAULT 80 COMMENT '节点高度',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_node_type` (`node_type`),
  CONSTRAINT `fk_xaa_node_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `xnet_mlops_xaa_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA工作流节点表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_workflow_edge` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符UUID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `source_node_id` BIGINT NOT NULL COMMENT '源节点ID',
  `source_handle` VARCHAR(100) COMMENT '源节点输出端口',
  `target_node_id` BIGINT NOT NULL COMMENT '目标节点ID',
  `target_handle` VARCHAR(100) COMMENT '目标节点输入端口',
  `edge_type` VARCHAR(50) DEFAULT 'default' COMMENT '边类型: default, success, fail',
  `condition_json` TEXT COMMENT '条件表达式JSON (用于条件分支)',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_source_node_id` (`source_node_id`),
  KEY `idx_target_node_id` (`target_node_id`),
  CONSTRAINT `fk_xaa_edge_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `xnet_mlops_xaa_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA工作流边表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_workflow_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符UUID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `status` VARCHAR(50) DEFAULT 'scheduled' COMMENT '执行状态: scheduled, running, succeeded, failed, stopped, paused',
  `inputs_json` LONGTEXT COMMENT '输入参数JSON',
  `outputs_json` LONGTEXT COMMENT '输出结果JSON',
  `error_message` TEXT COMMENT '错误信息',
  `started_at` DATETIME COMMENT '开始时间',
  `finished_at` DATETIME COMMENT '结束时间',
  `elapsed_time` BIGINT COMMENT '执行耗时(毫秒)',
  `triggered_by` VARCHAR(64) COMMENT '触发者ID',
  `trigger_type` VARCHAR(50) DEFAULT 'manual' COMMENT '触发类型: manual, scheduled, api',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered_by` (`triggered_by`),
  KEY `idx_started_at` (`started_at`),
  CONSTRAINT `fk_xaa_execution_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `xnet_mlops_xaa_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA工作流执行记录表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_node_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符UUID',
  `execution_id` BIGINT NOT NULL COMMENT '所属工作流执行ID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `node_id` BIGINT NOT NULL COMMENT '节点ID',
  `node_type` VARCHAR(50) NOT NULL COMMENT '节点类型',
  `node_title` VARCHAR(200) COMMENT '节点标题',
  `status` VARCHAR(50) DEFAULT 'pending' COMMENT '执行状态: pending, running, succeeded, failed, skipped, stopped',
  `inputs_json` LONGTEXT COMMENT '输入参数JSON',
  `outputs_json` LONGTEXT COMMENT '输出结果JSON',
  `metadata_json` TEXT COMMENT '执行元数据JSON (如token消耗、外部任务ID等)',
  `error_message` TEXT COMMENT '错误信息',
  `started_at` DATETIME COMMENT '开始时间',
  `finished_at` DATETIME COMMENT '结束时间',
  `elapsed_time` BIGINT COMMENT '执行耗时(毫秒)',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_node_id` (`node_id`),
  KEY `idx_status` (`status`),
  KEY `idx_started_at` (`started_at`),
  CONSTRAINT `fk_xaa_node_exec_execution` FOREIGN KEY (`execution_id`) REFERENCES `xnet_mlops_xaa_workflow_execution` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA节点执行记录表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_tables.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符UUID',
  `name` VARCHAR(200) NOT NULL COMMENT '技能名称',
  `description` TEXT COMMENT '技能描述',
  `type` VARCHAR(50) DEFAULT 'tool' COMMENT '技能类型: tool, prompt, chain',
  `status` VARCHAR(50) DEFAULT 'draft' COMMENT '状态: draft, published, archived',
  `config_json` LONGTEXT COMMENT '技能配置JSON',
  `icon` VARCHAR(500) COMMENT '技能图标URL',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `creator_id` VARCHAR(64) COMMENT '创建者ID',
  `tenant_uid` VARCHAR(64) COMMENT '租户ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_name` (`name`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_tenant_uid` (`tenant_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XAA技能表 (预留)';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_skill_update.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_skill_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` VARCHAR(50) NOT NULL COMMENT '分类标识',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称（中文）',
  `name_en` VARCHAR(100) COMMENT '分类名称（英文）',
  `description` VARCHAR(500) COMMENT '分类描述',
  `icon` VARCHAR(100) COMMENT '图标名称',
  `color` VARCHAR(20) COMMENT '主题颜色',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能分类表';

-- Source: mlops-xaa-service/src/main/resources/sql/xaa_skill_update.sql
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_skill_installation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `skill_id` BIGINT NOT NULL COMMENT '技能ID',
  `tenant_uid` VARCHAR(64) NOT NULL COMMENT '租户UID',
  `installed_by` VARCHAR(64) COMMENT '安装者ID',
  `installed_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `config_override` TEXT COMMENT '配置覆盖',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/disabled',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_tenant` (`skill_id`, `tenant_uid`),
  KEY `idx_tenant_uid` (`tenant_uid`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能安装记录表';

-- Source: mlops-login/src/main/resources/sql/organization_data_access_migration.sql
-- Copyright (C) 2026 Synapxnet. All rights reserved.
-- This file is Synapxnet Proprietary and Confidential. It is strictly
-- forbidden to copy, distribute, or use without explicit authorization.
-- 组织数据访问授权字段迁移 / Organization data-access permission migration.
-- Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
-- Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com

-- 在目标数据库中执行；现有成员不会自动获得业务数据权限。
-- Execute in the target database; existing members receive no implicit data access.
SET @mlops_permission_ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'xnet_mlops_usr_organization_membership'
       AND column_name = 'data_access_enabled') = 0,
    'ALTER TABLE xnet_mlops_usr_organization_membership ADD COLUMN data_access_enabled TINYINT NOT NULL DEFAULT 0 COMMENT ''Explicit business data authorization''',
    'SELECT ''organization data-access column already exists'' AS migration_status'
);
PREPARE mlops_permission_statement FROM @mlops_permission_ddl;
EXECUTE mlops_permission_statement;
DEALLOCATE PREPARE mlops_permission_statement;

-- Source: mlops-dpp-service/src/main/resources/sql/dpp_dataops_product_migration.sql
-- 为 DataOps 版本化数据产品导入增加真实来源、规模、摘要与血缘字段。
DROP PROCEDURE IF EXISTS migration_add_dataset_column;

DELIMITER //
CREATE PROCEDURE migration_add_dataset_column(
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512)
)
BEGIN
    -- 仅在字段不存在时执行 DDL，使迁移可在 MySQL 8.4 上安全重放。
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'xnet_mlops_dpp_dataset'
          AND column_name = target_column
    ) THEN
        SET @column_sql = CONCAT(
            'ALTER TABLE xnet_mlops_dpp_dataset ADD COLUMN `',
            REPLACE(target_column, '`', '``'),
            '` ',
            column_definition
        );
        PREPARE column_statement FROM @column_sql;
        EXECUTE column_statement;
        DEALLOCATE PREPARE column_statement;
    END IF;
END//
DELIMITER ;

CALL migration_add_dataset_column('source_platform', 'VARCHAR(64) NULL COMMENT ''来源平台''');
CALL migration_add_dataset_column('source_product_name', 'VARCHAR(128) NULL COMMENT ''来源数据产品名称''');
CALL migration_add_dataset_column('source_product_version', 'VARCHAR(128) NULL COMMENT ''来源数据产品版本''');
CALL migration_add_dataset_column('source_uri', 'VARCHAR(512) NULL COMMENT ''不含凭据的来源定位符''');
CALL migration_add_dataset_column('row_count', 'BIGINT NULL COMMENT ''真实记录数''');
CALL migration_add_dataset_column('byte_size', 'BIGINT NULL COMMENT ''导入制品字节数''');
CALL migration_add_dataset_column('schema_digest_sha256', 'CHAR(64) NULL COMMENT ''字段契约摘要''');
CALL migration_add_dataset_column('artifact_digest_sha256', 'CHAR(64) NULL COMMENT ''来源制品摘要''');
CALL migration_add_dataset_column('lineage_reference', 'VARCHAR(512) NULL COMMENT ''DataOps 血缘引用''');
CALL migration_add_dataset_column('import_status', 'VARCHAR(32) NULL COMMENT ''导入状态''');
CALL migration_add_dataset_column('imported_at', 'DATETIME NULL COMMENT ''导入时间''');

DROP PROCEDURE migration_add_dataset_column;

SET @source_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'xnet_mlops_dpp_dataset'
      AND index_name = 'uk_dataset_source_version'
);
SET @source_index_sql = IF(
    @source_index_exists = 0,
    'CREATE UNIQUE INDEX uk_dataset_source_version ON xnet_mlops_dpp_dataset(source_platform, source_product_version)',
    'SELECT 1'
);
PREPARE source_index_statement FROM @source_index_sql;
EXECUTE source_index_statement;
DEALLOCATE PREPARE source_index_statement;

-- Source: mlops-mep-service/src/main/resources/sql/V20260917_01__goai_governance.sql
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

DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
-- =====================================================
-- DPP 特征工程模块 - 数据库更新脚本
-- 版本: 2.0
-- 更新内容: 添加镜像、调度、通知、Jenkins集成相关字段
-- =====================================================

-- 1. 更新特征工程表，添加新字段（使用存储过程来确保幂等性）
DELIMITER $$
CREATE PROCEDURE AddColumnIfNotExists(
    IN tableName VARCHAR(64),
    IN columnName VARCHAR(64),
    IN columnDefinition VARCHAR(512)
)
BEGIN
    DECLARE columnCount INT;

    -- 检查列是否存在
    SELECT COUNT(*)
    INTO columnCount
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = tableName
    AND COLUMN_NAME = columnName;

    -- 如果列不存在，则添加
    IF columnCount = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 添加各个列
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'image_uid', 'VARCHAR(64) DEFAULT NULL COMMENT ''镜像UID''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'image_name', 'VARCHAR(128) DEFAULT NULL COMMENT ''镜像名称''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'image_tag', 'VARCHAR(64) DEFAULT NULL COMMENT ''镜像标签''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'harbor_url', 'VARCHAR(256) DEFAULT NULL COMMENT ''Harbor仓库地址''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'harbor_credentials_id', 'VARCHAR(64) DEFAULT NULL COMMENT ''Harbor凭证ID''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'schedule_config', 'TEXT DEFAULT NULL COMMENT ''调度配置(JSON)''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'schedule_active', 'TINYINT(1) DEFAULT 0 COMMENT ''是否启用调度''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'notification_config', 'TEXT DEFAULT NULL COMMENT ''通知配置(JSON)''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'notification_enabled', 'TINYINT(1) DEFAULT 0 COMMENT ''是否启用通知''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'job_uid', 'VARCHAR(256) DEFAULT NULL COMMENT ''Jenkins任务UID''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'last_build_status', 'VARCHAR(32) DEFAULT NULL COMMENT ''最后构建状态''');

-- 删除存储过程
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;



-- 仅增加缺失字段。 Add missing columns while preserving existing values.
DROP PROCEDURE IF EXISTS mlops_mainline_add_column;
DELIMITER //
CREATE PROCEDURE mlops_mainline_add_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN definition TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=target_table AND column_name=target_column) THEN
    SET @mlops_column_sql=CONCAT('ALTER TABLE `',target_table,'` ADD COLUMN `',target_column,'` ',definition);
    PREPARE mlops_column_statement FROM @mlops_column_sql;
    EXECUTE mlops_column_statement;
    DEALLOCATE PREPARE mlops_column_statement;
  END IF;
END//
DELIMITER ;

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','category','VARCHAR(50) DEFAULT ''custom'' COMMENT ''技能分类''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','tags','VARCHAR(500) COMMENT ''标签，逗号分隔''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','content_md','LONGTEXT COMMENT ''SKILL.md 内容''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','has_scripts','TINYINT(1) DEFAULT 0 COMMENT ''是否包含脚本''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','has_references','TINYINT(1) DEFAULT 0 COMMENT ''是否包含参考文档''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','has_assets','TINYINT(1) DEFAULT 0 COMMENT ''是否包含资源文件''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','install_count','INT DEFAULT 0 COMMENT ''安装次数''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','source_url','VARCHAR(500) COMMENT ''来源URL''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_skill','is_official','TINYINT(1) DEFAULT 0 COMMENT ''是否官方技能''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','hostname','VARCHAR(128) COMMENT ''服务器主机名(用于Hadoop集群等)''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','hostname_mode','VARCHAR(16) DEFAULT ''auto'' COMMENT ''主机名模式: auto(自动获取)/custom(自定义)''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','has_gpu','TINYINT(1) DEFAULT 0 COMMENT ''是否有GPU''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','gpu_count','INT DEFAULT 0 COMMENT ''GPU数量''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','gpu_type','VARCHAR(64) COMMENT ''GPU类型: single_gpu/multi_gpu''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','gpu_model','VARCHAR(128) COMMENT ''GPU型号: NVIDIA A100/V100/RTX 3090等''');

CALL mlops_mainline_add_column('xnet_mlops_smp_workstation','gpu_memory','INT COMMENT ''GPU显存(GB)''');

CALL mlops_mainline_add_column('xnet_mlops_xaa_assistant','gateway_token','VARCHAR(500) COMMENT ''Gateway authentication token''');


-- 补齐当前 Mapper 实际读写的字段。 Match the columns used by active mappers.
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','sub_data_area','VARCHAR(100)');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','data_type','VARCHAR(100)');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','encryption','TINYINT(1) DEFAULT 0');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','push_to_dataset','TINYINT(1) DEFAULT 0');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','target_dataset_id','BIGINT');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','target_dataset_name','VARCHAR(200)');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','operator_id','BIGINT');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','operator_code','VARCHAR(128)');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','operator_name','VARCHAR(200)');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','output_format','VARCHAR(32)');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','feature_config','LONGTEXT');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_engineering','operator_params','LONGTEXT');
CALL mlops_mainline_add_column('xnet_mlops_dpp_feature_operator','created_by','VARCHAR(255)');
CALL mlops_mainline_add_column('xnet_mlops_sys_bucket','access_key','VARCHAR(500)');
CALL mlops_mainline_add_column('xnet_mlops_sys_bucket','authorized_tenants','TEXT');

DROP PROCEDURE mlops_mainline_add_column;

-- 通用技能分类字典。 General skill category dictionary.
INSERT IGNORE INTO `xnet_mlops_xaa_skill_category` (`key`, `name`, `name_en`, `description`, `icon`, `color`, `sort_order`) VALUES
('document', '文档处理', 'Document Processing', '处理各类文档格式，包括PDF、Word、Excel、PPT等', 'file-text', '#1890ff', 1),
('creative', '创意设计', 'Creative Design', '图形设计、艺术创作、UI设计等创意类技能', 'highlight', '#722ed1', 2),
('development', '开发工具', 'Development', '编程、代码生成、开发辅助等技术技能', 'code', '#52c41a', 3),
('data', '数据处理', 'Data Processing', '数据分析、数据转换、数据可视化等', 'database', '#fa8c16', 4),
('enterprise', '企业应用', 'Enterprise', '企业级应用、办公自动化、流程管理等', 'apartment', '#eb2f96', 5),
('ai-ml', 'AI/机器学习', 'AI & Machine Learning', '模型训练、推理、AI辅助等', 'robot', '#13c2c2', 6),
('utilities', '实用工具', 'Utilities', '通用工具类技能', 'tool', '#faad14', 7),
('custom', '自定义', 'Custom', '用户自定义技能', 'star', '#8c8c8c', 99);

-- 保留旧算法仓数据和唯一索引，修正历史列名。 Preserve legacy repository data and indexes while correcting its column name.
DROP PROCEDURE IF EXISTS mlops_mainline_algorithm_column;
DELIMITER //
CREATE PROCEDURE mlops_mainline_algorithm_column()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='xnet_mlops_smp_algorithms' AND column_name='algorithm')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='xnet_mlops_smp_algorithms' AND column_name='algorithmRepository') THEN
    ALTER TABLE xnet_mlops_smp_algorithms RENAME COLUMN algorithmRepository TO algorithm;
  END IF;
END//
DELIMITER ;
CALL mlops_mainline_algorithm_column();
DROP PROCEDURE mlops_mainline_algorithm_column;
