-- ============================================
-- DPP RAG Knowledge Base 数据库表结构
-- ============================================

SET NAMES utf8mb4;

-- 1. 知识库表
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

-- 2. 文档表
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

-- 3. 文档分块表
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

-- 4. 嵌入模型配置表
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

-- 5. 检索日志表
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

-- 6. 知识库标签表
CREATE TABLE IF NOT EXISTS `xnet_mlops_dpp_kb_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
  `tag` VARCHAR(100) NOT NULL COMMENT '标签',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_tag` (`kb_id`, `tag`),
  INDEX `idx_tag` (`tag`),
  CONSTRAINT `fk_tag_kb` FOREIGN KEY (`kb_id`) REFERENCES `xnet_mlops_dpp_knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库标签表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化默认嵌入模型
INSERT INTO `xnet_mlops_dpp_embedding_model` (`name`, `provider`, `model_name`, `dimension`, `max_tokens`, `is_default`, `status`) VALUES
('OpenAI Ada-002', 'openai', 'text-embedding-ada-002', 1536, 8191, 1, 'active'),
('OpenAI Embedding-3-Small', 'openai', 'text-embedding-3-small', 1536, 8191, 0, 'active'),
('OpenAI Embedding-3-Large', 'openai', 'text-embedding-3-large', 3072, 8191, 0, 'active'),
('BGE-Large-ZH', 'huggingface', 'BAAI/bge-large-zh-v1.5', 1024, 512, 0, 'active'),
('BGE-M3', 'huggingface', 'BAAI/bge-m3', 1024, 8192, 0, 'active'),
('Text2Vec-Large', 'huggingface', 'GanymedeNil/text2vec-large-chinese', 1024, 512, 0, 'active');

-- 创建示例知识库
INSERT INTO `xnet_mlops_dpp_knowledge_base` (`uid`, `name`, `description`, `embedding_provider`, `embedding_model`, `embedding_dimension`, `vector_db_type`, `chunk_strategy`, `chunk_size`, `chunk_overlap`, `retrieval_method`, `status`) VALUES
('KB-DEMO-001', '产品文档知识库', '存储产品相关的技术文档、用户手册和FAQ', 'openai', 'text-embedding-ada-002', 1536, 'milvus', 'recursive', 500, 50, 'hybrid', 'active'),
('KB-DEMO-002', '技术规范知识库', '存储API文档、技术规范和开发指南', 'openai', 'text-embedding-ada-002', 1536, 'milvus', 'recursive', 800, 100, 'semantic', 'active');
