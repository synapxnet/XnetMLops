-- SynapXnet XnetMLops 1.0.0 showcase data.
-- This dataset is deterministic, credential-free, and safe to re-run.

SET NAMES utf8mb4;

SET @xnet_add_encrypted_key = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `xnet_mlops_mep_api_key` ADD COLUMN `encrypted_key` VARCHAR(500) DEFAULT NULL COMMENT ''AES encrypted API key for internal service references'' AFTER `key_masked`',
    'SELECT 1'
  )
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE()
    AND `TABLE_NAME` = 'xnet_mlops_mep_api_key'
    AND `COLUMN_NAME` = 'encrypted_key'
);
PREPARE xnet_add_encrypted_key_stmt FROM @xnet_add_encrypted_key;
EXECUTE xnet_add_encrypted_key_stmt;
DEALLOCATE PREPARE xnet_add_encrypted_key_stmt;

ALTER TABLE `xnet_mlops_xaa_assistant`
  ALTER COLUMN `placeholder` SET DEFAULT '有什么可以帮助您的?';

START TRANSACTION;

-- Demo tenant and shared resources
INSERT INTO `xnet_mlops_sys_tenant` (`uid`, `tenant_id`, `tenant_name`, `status`) VALUES
('default', 'synapxnet-showcase', 'SynapXnet 演示租户', 1)
ON DUPLICATE KEY UPDATE `tenant_name` = VALUES(`tenant_name`), `status` = VALUES(`status`);

INSERT INTO `xnet_mlops_sys_department` (`uid`, `tenant_uid`, `dept_id`, `dept_name`, `status`) VALUES
('DEPT-DEMO-AI', 'default', 'demo-ai-platform', 'AI 平台部', 1)
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`), `status` = VALUES(`status`);

INSERT INTO `xnet_mlops_sys_team` (`uid`, `dept_uid`, `team_id`, `team_name`, `status`) VALUES
('TEAM-DEMO-MLOPS', 'DEPT-DEMO-AI', 'demo-mlops', '模型工程组', 1)
ON DUPLICATE KEY UPDATE `team_name` = VALUES(`team_name`), `status` = VALUES(`status`);

INSERT INTO `xnet_mlops_sys_bucket`
(`uid`, `name`, `identifier`, `type`, `tenant_uid`, `dept_uid`, `team_uid`, `current_size`, `max_size`, `status`) VALUES
('BUCKET-DEMO-MLOPS', '演示模型资产桶', 'demo-mlops-assets', 'tenant', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', 28.6, 100, 'active')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `current_size` = VALUES(`current_size`), `status` = VALUES(`status`);

INSERT INTO `xnet_mlops_sys_datasource`
(`uid`, `name`, `type`, `host`, `port`, `username`, `password`, `default_database`, `allowed_databases`, `description`, `enabled`, `created_by`) VALUES
('DS-DEMO-CRM', '客户分析 MySQL', 'mysql', 'mysql.demo.invalid', 3306, NULL, NULL, 'customer_360', 'customer_360,feature_store', '展示用客户行为与标签数据源，不包含真实连接凭据', 1, 'demo-user'),
('DS-DEMO-IOT', '设备指标 PostgreSQL', 'postgresql', 'postgres.demo.invalid', 5432, NULL, NULL, 'device_metrics', 'device_metrics', '展示用设备时序数据源，不包含真实连接凭据', 1, 'demo-user')
ON DUPLICATE KEY UPDATE `type` = VALUES(`type`), `host` = VALUES(`host`), `port` = VALUES(`port`), `description` = VALUES(`description`), `enabled` = VALUES(`enabled`);

-- DPP: datasets, feature engineering, and RAG knowledge bases
DELETE FROM `xnet_mlops_dpp_dataset` WHERE `uid` LIKE 'DSET-DEMO-%';
INSERT INTO `xnet_mlops_dpp_dataset`
(`uid`, `userId`, `dataset_file`, `type`, `type_label`, `zone`, `zone_label`, `bucket_name`, `bucket_identifier`, `encryption`, `subdata_area`, `tenant_uid`, `dept_uid`, `team_uid`, `team_name`, `level`, `description`, `created_at`, `updated_at`) VALUES
('DSET-DEMO-CUSTOMER', 'demo-user', '客户流失训练集.csv', '1', '文本', '1', '训练区', '演示模型资产桶', 'demo-mlops-assets', 0, '客户运营', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', '模型工程组', 2, '包含客户画像、活跃度和流失标签的脱敏样例数据', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('DSET-DEMO-REVIEWS', 'demo-user', '产品评价语料.jsonl', '1', '文本', '1', '训练区', '演示模型资产桶', 'demo-mlops-assets', 0, '体验分析', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', '模型工程组', 1, '用于情感分类与主题分析的公开样例语料', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('DSET-DEMO-DEVICE', 'demo-user', '设备温度时序.parquet', '1', '文本', '2', '验证区', '演示模型资产桶', 'demo-mlops-assets', 0, '设备运维', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', '模型工程组', 2, '用于异常检测和剩余寿命预测的模拟时序数据', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
('DSET-DEMO-VISION', 'demo-user', '质检缺陷图像集.zip', '2', '图像', '2', '验证区', '演示模型资产桶', 'demo-mlops-assets', 1, '智能质检', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', '模型工程组', 3, '工业表面缺陷分类展示数据集', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 1 HOUR))
ON DUPLICATE KEY UPDATE `dataset_file` = VALUES(`dataset_file`), `type` = VALUES(`type`), `zone` = VALUES(`zone`), `description` = VALUES(`description`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_dpp_feature_operator`
(`uid`, `name`, `code`, `description`, `category`, `output_formats`, `feature_columns`, `parameter_schema`, `sort_order`, `enabled`) VALUES
('DPP-OP-DEMO-STANDARD', '数值标准化', 'standard_scaler', '对连续特征执行 Z-Score 标准化', 'transform', '["parquet","csv"]', '["numeric"]', '{"withMean":true,"withStd":true}', 10, 1),
('DPP-OP-DEMO-ONEHOT', '类别独热编码', 'one_hot_encoder', '将类别字段转换为模型可训练的稀疏特征', 'encoding', '["parquet"]', '["category"]', '{"handleUnknown":"ignore"}', 20, 1),
('DPP-OP-DEMO-WINDOW', '时序滑动窗口', 'rolling_window', '生成均值、极值和趋势窗口特征', 'timeseries', '["parquet"]', '["timestamp","numeric"]', '{"window":24,"step":1}', 30, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `parameter_schema` = VALUES(`parameter_schema`), `enabled` = VALUES(`enabled`);

INSERT INTO `xnet_mlops_dpp_feature_engineering`
(`uid`, `name`, `description`, `datasource_id`, `datasource_name`, `database`, `table_name`, `selected_columns`, `transform_config`, `output_path`, `zone`, `bucket_uid`, `bucket_name`, `status`, `created_by`, `team_uid`, `team_name`, `image_uid`, `image_name`, `image_tag`, `schedule_config`, `schedule_active`, `notification_config`, `notification_enabled`, `job_uid`, `last_build_status`, `created_at`, `updated_at`) VALUES
('FE-DEMO-CHURN', '客户流失特征流水线', '聚合近 30 天活跃度、消费频次和服务触点特征', (SELECT `id` FROM `xnet_mlops_sys_datasource` WHERE `uid` = 'DS-DEMO-CRM'), '客户分析 MySQL', 'customer_360', 'customer_events', '["customer_id","active_days","order_count","complaint_count"]', '{"operators":["standard_scaler","one_hot_encoder"]}', '/demo/features/churn', '1', 'BUCKET-DEMO-MLOPS', '演示模型资产桶', 'success', 'demo-user', 'TEAM-DEMO-MLOPS', '模型工程组', 'IMG-DEMO-SKLEARN', 'mlops/sklearn', '1.4', '{"cron":"0 2 * * *"}', 1, '{"channel":"platform"}', 1, 'JOB-DEMO-FE-CHURN', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('FE-DEMO-DEVICE', '设备健康时序特征', '构建设备温度、振动和压力的滑动窗口统计特征', (SELECT `id` FROM `xnet_mlops_sys_datasource` WHERE `uid` = 'DS-DEMO-IOT'), '设备指标 PostgreSQL', 'device_metrics', 'sensor_readings', '["device_id","event_time","temperature","vibration","pressure"]', '{"operators":["rolling_window","standard_scaler"]}', '/demo/features/device-health', '2', 'BUCKET-DEMO-MLOPS', '演示模型资产桶', 'running', 'demo-user', 'TEAM-DEMO-MLOPS', '模型工程组', 'IMG-DEMO-PYTORCH', 'mlops/pytorch', '2.3', '{"intervalMinutes":30}', 1, '{"channel":"platform"}', 1, 'JOB-DEMO-FE-DEVICE', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 10 MINUTE))
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`), `status` = VALUES(`status`), `last_build_status` = VALUES(`last_build_status`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_dpp_embedding_model`
(`name`, `provider`, `model_name`, `dimension`, `max_tokens`, `batch_size`, `api_endpoint`, `api_key_encrypted`, `extra_config`, `is_default`, `status`, `tenant_uid`)
SELECT '演示 BGE-M3', 'local', 'bge-m3', 1024, 8192, 64, NULL, NULL, '{"device":"cpu","demo":true}', 1, 'active', 'default'
WHERE NOT EXISTS (SELECT 1 FROM `xnet_mlops_dpp_embedding_model` WHERE `name` = '演示 BGE-M3');

-- Repair two legacy showcase rows that were imported with UTF-8 bytes decoded twice.
UPDATE `xnet_mlops_dpp_knowledge_base`
SET `name` = '产品文档知识库',
    `description` = '存储产品相关的技术文档、用户手册和 FAQ'
WHERE `uid` = 'KB-DEMO-001';

UPDATE `xnet_mlops_dpp_knowledge_base`
SET `name` = '技术规范知识库',
    `description` = '存储 API 文档、技术规范和开发指南'
WHERE `uid` = 'KB-DEMO-002';

INSERT INTO `xnet_mlops_dpp_knowledge_base`
(`uid`, `name`, `description`, `icon`, `embedding_model_id`, `embedding_provider`, `embedding_model`, `embedding_dimension`, `vector_db_type`, `vector_collection`, `vector_index_type`, `chunk_strategy`, `chunk_size`, `chunk_overlap`, `retrieval_method`, `top_k`, `score_threshold`, `rerank_enabled`, `rerank_model`, `rerank_top_k`, `status`, `doc_count`, `chunk_count`, `total_tokens`, `total_size_bytes`, `tenant_uid`, `creator_id`, `visibility`, `created_at`, `updated_at`) VALUES
('KB-DEMO-MLOPS', 'MLOps 运维知识库', '汇集模型训练、部署、监控与故障排查手册', 'book', (SELECT `id` FROM `xnet_mlops_dpp_embedding_model` WHERE `name` = '演示 BGE-M3' LIMIT 1), 'local', 'bge-m3', 1024, 'milvus', 'demo_mlops_ops', 'HNSW', 'recursive', 600, 80, 'hybrid', 5, 0.6200, 1, 'bge-reranker-v2-m3', 3, 'active', 2, 4, 3280, 248320, 'default', 'demo-user', 'tenant', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
('KB-DEMO-GOVERNANCE', '模型治理规范库', '模型准入、评审、版本管理和合规审计规范', 'safety', (SELECT `id` FROM `xnet_mlops_dpp_embedding_model` WHERE `name` = '演示 BGE-M3' LIMIT 1), 'local', 'bge-m3', 1024, 'milvus', 'demo_model_governance', 'HNSW', 'recursive', 500, 50, 'hybrid', 6, 0.6500, 0, NULL, 3, 'active', 2, 4, 2810, 182400, 'default', 'demo-user', 'tenant', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 1 HOUR))
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`), `doc_count` = VALUES(`doc_count`), `chunk_count` = VALUES(`chunk_count`), `total_tokens` = VALUES(`total_tokens`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_dpp_kb_document`
(`uid`, `kb_id`, `name`, `original_name`, `type`, `mime_type`, `file_path`, `file_size`, `file_hash`, `status`, `process_progress`, `word_count`, `char_count`, `chunk_count`, `token_count`, `page_count`, `metadata`, `source_type`, `uploaded_by`, `created_at`, `updated_at`, `indexed_at`) VALUES
('DOC-DEMO-DEPLOY', (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), '模型部署与回滚手册', '模型部署与回滚手册.pdf', 'pdf', 'application/pdf', '/demo/kb/deployment-runbook.pdf', 128320, SHA2('DOC-DEMO-DEPLOY', 256), 'completed', 100, 1820, 9340, 2, 1640, 12, '{"department":"AI平台部"}', 'upload', 'demo-user', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
('DOC-DEMO-ALERT', (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), '推理服务告警处理指南', '推理服务告警处理指南.md', 'markdown', 'text/markdown', '/demo/kb/inference-alerts.md', 12040, SHA2('DOC-DEMO-ALERT', 256), 'completed', 100, 980, 5120, 2, 1640, 4, '{"department":"SRE"}', 'upload', 'demo-user', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
('DOC-DEMO-REVIEW', (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '模型上线评审清单', '模型上线评审清单.docx', 'docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '/demo/kb/model-review.docx', 86400, SHA2('DOC-DEMO-REVIEW', 256), 'completed', 100, 1210, 6480, 2, 1510, 8, '{"owner":"模型治理委员会"}', 'upload', 'demo-user', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
('DOC-DEMO-CARD', (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '模型卡片编写规范', '模型卡片编写规范.md', 'markdown', 'text/markdown', '/demo/kb/model-card.md', 16000, SHA2('DOC-DEMO-CARD', 256), 'completed', 100, 1040, 5520, 2, 1300, 5, '{"owner":"模型治理委员会"}', 'upload', 'demo-user', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = VALUES(`status`), `process_progress` = VALUES(`process_progress`), `updated_at` = VALUES(`updated_at`), `indexed_at` = VALUES(`indexed_at`);

INSERT INTO `xnet_mlops_dpp_kb_chunk`
(`uid`, `doc_id`, `kb_id`, `content`, `content_hash`, `position`, `page_number`, `embedding_id`, `token_count`, `char_count`, `chunk_level`, `metadata`, `keywords`, `summary`) VALUES
('CHUNK-DEMO-DEPLOY-01', (SELECT `id` FROM `xnet_mlops_dpp_kb_document` WHERE `uid` = 'DOC-DEMO-DEPLOY'), (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), '部署前确认模型版本、镜像摘要、资源配额和健康检查路径。灰度发布完成后观察错误率与延迟。', SHA2('CHUNK-DEMO-DEPLOY-01', 256), 1, 3, 'demo-vector-001', 118, 92, 0, '{"section":"部署前检查"}', '部署,灰度,健康检查', '模型部署前检查与灰度验证步骤'),
('CHUNK-DEMO-DEPLOY-02', (SELECT `id` FROM `xnet_mlops_dpp_kb_document` WHERE `uid` = 'DOC-DEMO-DEPLOY'), (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), '当错误率持续超过阈值时，将流量切换到上一稳定版本并保留故障实例日志。', SHA2('CHUNK-DEMO-DEPLOY-02', 256), 2, 8, 'demo-vector-002', 96, 75, 0, '{"section":"回滚"}', '回滚,错误率,稳定版本', '模型服务回滚流程'),
('CHUNK-DEMO-REVIEW-01', (SELECT `id` FROM `xnet_mlops_dpp_kb_document` WHERE `uid` = 'DOC-DEMO-REVIEW'), (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '上线评审必须包含训练数据说明、核心指标、偏差评估、资源预算和应急回滚方案。', SHA2('CHUNK-DEMO-REVIEW-01', 256), 1, 2, 'demo-vector-003', 104, 82, 0, '{"section":"准入条件"}', '评审,偏差,资源预算', '模型上线评审必备材料'),
('CHUNK-DEMO-CARD-01', (SELECT `id` FROM `xnet_mlops_dpp_kb_document` WHERE `uid` = 'DOC-DEMO-CARD'), (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '模型卡片应记录适用范围、限制条件、训练数据版本、评估指标和责任人。', SHA2('CHUNK-DEMO-CARD-01', 256), 1, 1, 'demo-vector-004', 92, 70, 0, '{"section":"模型卡片"}', '模型卡片,责任人,限制条件', '模型卡片字段要求')
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`), `keywords` = VALUES(`keywords`), `summary` = VALUES(`summary`);

DELETE FROM `xnet_mlops_dpp_kb_tag` WHERE `kb_id` IN (SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` LIKE 'KB-DEMO-%');
INSERT INTO `xnet_mlops_dpp_kb_tag` (`kb_id`, `tag`) VALUES
((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), 'MLOps'),
((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), '运维'),
((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '治理'),
((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '合规');

DELETE FROM `xnet_mlops_dpp_retrieval_log` WHERE `session_id` LIKE 'DEMO-%';
INSERT INTO `xnet_mlops_dpp_retrieval_log`
(`kb_id`, `query`, `query_tokens`, `retrieval_method`, `top_k`, `score_threshold`, `rerank_enabled`, `result_count`, `results`, `embedding_latency_ms`, `retrieval_latency_ms`, `rerank_latency_ms`, `total_latency_ms`, `source`, `session_id`, `user_id`, `tenant_uid`, `feedback_score`, `created_at`) VALUES
((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS'), '模型部署失败后如何回滚?', 12, 'hybrid', 5, 0.6200, 1, 3, '[{"document":"模型部署与回滚手册","score":0.91}]', 38, 22, 18, 78, 'console', 'DEMO-SESSION-001', 'demo-user', 'default', 5, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE'), '上线评审需要哪些材料?', 10, 'hybrid', 6, 0.6500, 0, 2, '[{"document":"模型上线评审清单","score":0.94}]', 32, 19, NULL, 51, 'console', 'DEMO-SESSION-002', 'demo-user', 'default', 5, DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- MTP: algorithms and training lifecycle
INSERT INTO `xnet_mlops_mtp_algorithms`
(`uid`, `userId`, `algorithm_name`, `version`, `zone`, `zone_label`, `encryption`, `subdata_area`, `bucket_name`, `bucket_identifier`, `team_uid`, `team_name`, `description`, `tenant_uid`, `dept_uid`, `level`, `is_CAS`, `created_at`, `updated_at`) VALUES
('ALG-DEMO-XGBOOST', 'demo-user', '客户流失 XGBoost', '2.1.0', '1', '训练区', 0, '客户运营', '演示模型资产桶', 'demo-mlops-assets', 'TEAM-DEMO-MLOPS', '模型工程组', '基于客户行为与画像特征预测未来 30 天流失概率', 'default', 'DEPT-DEMO-AI', 2, 0, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('ALG-DEMO-BERT', 'demo-user', '中文评价情感分类', '1.3.2', '1', '训练区', 0, '体验分析', '演示模型资产桶', 'demo-mlops-assets', 'TEAM-DEMO-MLOPS', '模型工程组', '基于预训练语言模型识别正向、中性与负向评价', 'default', 'DEPT-DEMO-AI', 2, 0, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('ALG-DEMO-LSTM', 'demo-user', '设备温度趋势预测', '0.9.5', '2', '验证区', 0, '设备运维', '演示模型资产桶', 'demo-mlops-assets', 'TEAM-DEMO-MLOPS', '模型工程组', '基于多变量时序预测设备温度与异常趋势', 'default', 'DEPT-DEMO-AI', 2, 0, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW())
ON DUPLICATE KEY UPDATE `algorithm_name` = VALUES(`algorithm_name`), `version` = VALUES(`version`), `description` = VALUES(`description`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_mtp_train_task`
(`uid`, `userId`, `tenant_uid`, `task_name`, `task_type`, `encryption`, `task_zone`, `pod_type`, `resources`, `train_type`, `image_uid`, `image`, `description`, `algorithm_uid`, `algorithm_name`, `algorithm_version`, `task_route`, `train_config_content`, `train_config_format`, `notification_config`, `output_config`, `schedule_config`, `created_at`, `updated_at`) VALUES
('TASK-DEMO-CHURN', 'demo-user', 'default', '客户流失模型周训练', 'classification', 'none', '1', 'cpu', '4C8G', 'scheduled', 'IMG-DEMO-SKLEARN', 'demo/sklearn:1.4', '每周使用最新客户行为数据重新训练并自动评估', 'ALG-DEMO-XGBOOST', '客户流失 XGBoost', '2.1.0', 'train.py', 'max_depth=8\nlearning_rate=0.05\nn_estimators=500', 'txt', JSON_OBJECT('enabled', TRUE, 'channel', 'platform'), JSON_OBJECT('isActive', TRUE, 'outputPath', '/demo/models/churn', 'outputType', 'onnx', 'autoPublish', TRUE), JSON_OBJECT('isActive', TRUE, 'cronExpression', '0 3 * * 1'), DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('TASK-DEMO-SENTIMENT', 'demo-user', 'default', '中文评价情感微调', 'nlp', 'none', '1', 'gpu', '8C32G', 'manual', 'IMG-DEMO-PYTORCH', 'demo/pytorch:2.3', '对中文评价语料执行三分类微调和验证', 'ALG-DEMO-BERT', '中文评价情感分类', '1.3.2', 'finetune.py', 'epochs=3\nbatch_size=32\nlearning_rate=2e-5', 'txt', JSON_OBJECT('enabled', TRUE, 'channel', 'platform'), JSON_OBJECT('isActive', TRUE, 'outputPath', '/demo/models/sentiment', 'outputType', 'safetensors', 'autoPublish', FALSE), JSON_OBJECT('isActive', FALSE), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
('TASK-DEMO-FORECAST', 'demo-user', 'default', '设备温度夜间训练', 'timeseries', 'none', '2', 'gpu', '8C16G', 'scheduled', 'IMG-DEMO-PYTORCH', 'demo/pytorch:2.3', '每日增量训练设备温度趋势模型', 'ALG-DEMO-LSTM', '设备温度趋势预测', '0.9.5', 'forecast.py', 'window=168\nhorizon=24\nepochs=20', 'txt', JSON_OBJECT('enabled', TRUE, 'channel', 'email'), JSON_OBJECT('isActive', TRUE, 'outputPath', '/demo/models/device-forecast', 'outputType', 'torchscript', 'autoPublish', FALSE), JSON_OBJECT('isActive', TRUE, 'cronExpression', '0 1 * * *'), DATE_SUB(NOW(), INTERVAL 6 DAY), NOW())
ON DUPLICATE KEY UPDATE `task_name` = VALUES(`task_name`), `description` = VALUES(`description`), `train_config_content` = VALUES(`train_config_content`), `notification_config` = VALUES(`notification_config`), `output_config` = VALUES(`output_config`), `schedule_config` = VALUES(`schedule_config`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_mtp_task_dataset`
(`uid`, `task_uid`, `dataset_id`, `dataset_uid`, `dataset_name`, `dataset_file`, `bucket_identifier`) VALUES
('TD-DEMO-CHURN', 'TASK-DEMO-CHURN', '1', 'DSET-DEMO-CUSTOMER', '客户流失训练集', '客户流失训练集.csv', 'demo-mlops-assets'),
('TD-DEMO-SENTIMENT', 'TASK-DEMO-SENTIMENT', '2', 'DSET-DEMO-REVIEWS', '产品评价语料', '产品评价语料.jsonl', 'demo-mlops-assets'),
('TD-DEMO-FORECAST', 'TASK-DEMO-FORECAST', '3', 'DSET-DEMO-DEVICE', '设备温度时序', '设备温度时序.parquet', 'demo-mlops-assets')
ON DUPLICATE KEY UPDATE `dataset_uid` = VALUES(`dataset_uid`), `dataset_name` = VALUES(`dataset_name`), `dataset_file` = VALUES(`dataset_file`);

INSERT INTO `xnet_mlops_mtp_task_custom_variable` (`uid`, `task_uid`, `name`, `value`) VALUES
('TV-DEMO-CHURN-01', 'TASK-DEMO-CHURN', 'EVAL_METRIC', 'auc'),
('TV-DEMO-SENTIMENT-01', 'TASK-DEMO-SENTIMENT', 'LABEL_COUNT', '3'),
('TV-DEMO-FORECAST-01', 'TASK-DEMO-FORECAST', 'FORECAST_HORIZON', '24')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `value` = VALUES(`value`);

INSERT INTO `xnet_mlops_mtp_train_info`
(`uid`, `task_uid`, `job_uid`, `job_status`, `job_content`, `start_at`, `end_at`, `schedule_active`) VALUES
('RUN-DEMO-CHURN-01', 'TASK-DEMO-CHURN', 'JOB-DEMO-CHURN-20260719', 'SUCCESS', '{"overallStatus":"SUCCESS","stages":[{"name":"数据校验","status":"SUCCESS"},{"name":"训练","status":"SUCCESS"},{"name":"评估","status":"SUCCESS"}]}', DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), 1),
('RUN-DEMO-SENTIMENT-01', 'TASK-DEMO-SENTIMENT', 'JOB-DEMO-SENTIMENT-20260720', 'IN_PROGRESS', '{"overallStatus":"IN_PROGRESS","stages":[{"name":"数据加载","status":"SUCCESS"},{"name":"微调","status":"IN_PROGRESS"},{"name":"评估","status":"PENDING"}]}', DATE_SUB(NOW(), INTERVAL 18 MINUTE), NULL, 0),
('RUN-DEMO-FORECAST-01', 'TASK-DEMO-FORECAST', 'JOB-DEMO-FORECAST-20260720', 'FAILED', '{"overallStatus":"FAILED","stages":[{"name":"数据加载","status":"SUCCESS"},{"name":"训练","status":"FAILED"}],"message":"样例展示：训练数据时间窗口不足"}', DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), 1)
ON DUPLICATE KEY UPDATE `job_status` = VALUES(`job_status`), `job_content` = VALUES(`job_content`), `start_at` = VALUES(`start_at`), `end_at` = VALUES(`end_at`);

INSERT INTO `xnet_mlops_mtp_train_schedule_info`
(`uid`, `task_uid`, `job_uid`, `job_status`, `job_content`, `start_at`, `end_at`, `schedule_active`) VALUES
('SCH-DEMO-CHURN-01', 'TASK-DEMO-CHURN', 'SCHEDULE-DEMO-CHURN', 'scheduled', '{"jobName":"客户流失模型周训练","nextRun":"下周一 03:00"}', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), 1),
('SCH-DEMO-FORECAST-01', 'TASK-DEMO-FORECAST', 'SCHEDULE-DEMO-FORECAST', 'scheduled', '{"jobName":"设备温度夜间训练","nextRun":"明日 01:00"}', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 1)
ON DUPLICATE KEY UPDATE `job_status` = VALUES(`job_status`), `job_content` = VALUES(`job_content`), `schedule_active` = VALUES(`schedule_active`);

-- SMP: build infrastructure and compute resources
INSERT INTO `xnet_mlops_smp_harbor_repository`
(`uid`, `name`, `url`, `username`, `password`, `created_by`) VALUES
('HARBOR-DEMO-001', '演示镜像仓库', 'https://harbor.demo.invalid', NULL, NULL, 'demo-user')
ON DUPLICATE KEY UPDATE `url` = VALUES(`url`), `username` = NULL, `password` = NULL;

INSERT INTO `xnet_mlops_smp_docker_file`
(`uid`, `name`, `content`, `tags`, `push_status`, `harbor_uid`, `push_history`, `created_by`) VALUES
('IMG-DEMO-SKLEARN', 'Scikit-learn 训练镜像', 'FROM python:3.11-slim\nWORKDIR /workspace\nRUN pip install scikit-learn pandas\nCMD ["python","train.py"]', '["cpu","sklearn","demo"]', 'PUSHED', 'HARBOR-DEMO-001', '[{"status":"PUSHED","tag":"1.4","message":"演示镜像已就绪"}]', 'demo-user'),
('IMG-DEMO-PYTORCH', 'PyTorch GPU 训练镜像', 'FROM pytorch/pytorch:2.3.1-cuda12.1-cudnn8-runtime\nWORKDIR /workspace\nCMD ["python","train.py"]', '["gpu","pytorch","demo"]', 'PUSHED', 'HARBOR-DEMO-001', '[{"status":"PUSHED","tag":"2.3","message":"演示镜像已就绪"}]', 'demo-user')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `content` = VALUES(`content`), `tags` = VALUES(`tags`), `push_status` = VALUES(`push_status`), `push_history` = VALUES(`push_history`);

INSERT INTO `xnet_mlops_smp_algorithms`
(`uid`, `url`, `encrypted_token`, `algorithmRepository`, `algorithm_version`, `description`, `tenant_uid`, `dept_uid`, `team_uid`, `authorized_tenants`, `created_by`) VALUES
('REPO-DEMO-ALGORITHM', 'https://github.com/synapxnet/demo-algorithms', UNHEX(''), 'demo-algorithms', 'main', '展示用算法模板仓库，不包含访问令牌', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', '["default"]', 'demo-user')
ON DUPLICATE KEY UPDATE `url` = VALUES(`url`), `description` = VALUES(`description`), `authorized_tenants` = VALUES(`authorized_tenants`);

INSERT INTO `xnet_mlops_smp_feature_operators`
(`uid`, `url`, `encrypted_token`, `operator_name`, `operator_code`, `operator_version`, `description`, `tenant_uid`, `dept_uid`, `team_uid`, `authorized_tenants`, `created_by`) VALUES
('REPO-DEMO-FEATURE', 'https://github.com/synapxnet/demo-feature-operators', UNHEX(''), '客户特征算子包', 'customer_features', '1.2.0', '展示用特征算子仓库，不包含访问令牌', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', '["default"]', 'demo-user')
ON DUPLICATE KEY UPDATE `url` = VALUES(`url`), `operator_version` = VALUES(`operator_version`), `description` = VALUES(`description`);

INSERT INTO `xnet_mlops_smp_workstation`
(`uid`, `name`, `hostname`, `hostname_mode`, `vendor`, `server_type`, `region`, `os_type`, `os_version`, `cpu_cores`, `ram_gb`, `disk_gb`, `has_gpu`, `gpu_count`, `gpu_type`, `gpu_model`, `gpu_memory`, `available_disk_gb`, `available_ram_gb`, `domain`, `ip_address`, `ssh_port`, `ssh_user`, `auth_type`, `encrypted_password`, `encrypted_private_key`, `status`, `last_heartbeat`, `last_check_result`, `description`, `created_by`) VALUES
('WS-DEMO-CPU-01', 'CPU 训练节点 01', 'demo-cpu-01', 'custom', 'SynapXnet Lab', 'baremetal', 'guangzhou', 'linux', 'Ubuntu 22.04', 32, 128, 2048, 0, 0, NULL, NULL, NULL, 1420, 76, 'demo.invalid', '192.0.2.21', 22, 'demo', 'password', NULL, NULL, 'online', DATE_SUB(NOW(), INTERVAL 1 MINUTE), '{"cpuLoad":38,"diskUsage":31}', '用于特征工程和 CPU 模型训练的展示节点', 'demo-user'),
('WS-DEMO-GPU-01', 'GPU 训练节点 A100', 'demo-gpu-01', 'custom', 'SynapXnet Lab', 'baremetal', 'guangzhou', 'linux', 'Ubuntu 22.04', 64, 512, 4096, 1, 4, 'multi_gpu', 'NVIDIA A100', 80, 2860, 392, 'demo.invalid', '192.0.2.22', 22, 'demo', 'privateKey', NULL, NULL, 'online', DATE_SUB(NOW(), INTERVAL 2 MINUTE), '{"gpuUtilization":72,"gpuMemoryUsage":61}', '用于大模型微调和高性能推理的展示节点', 'demo-user'),
('WS-DEMO-GPU-02', 'GPU 验证节点 L40S', 'demo-gpu-02', 'custom', 'SynapXnet Lab', 'sh-server', 'shanghai', 'linux', 'Rocky Linux 9', 32, 256, 2048, 1, 2, 'multi_gpu', 'NVIDIA L40S', 48, 1260, 184, 'demo.invalid', '192.0.2.23', 22, 'demo', 'privateKey', NULL, NULL, 'maintenance', DATE_SUB(NOW(), INTERVAL 3 HOUR), '{"maintenance":"驱动升级"}', '用于模型验证和批量推理的展示节点', 'demo-user')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `last_heartbeat` = VALUES(`last_heartbeat`), `last_check_result` = VALUES(`last_check_result`), `description` = VALUES(`description`);

INSERT INTO `xnet_mlops_smp_jenkins_versions`
(`version`, `version_type`, `release_date`, `download_url`, `sha256`, `is_lts`, `is_latest`)
SELECT '2.492.3', 'lts', '2026-05-01', 'https://get.jenkins.io/war-stable/2.492.3/jenkins.war', NULL, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `xnet_mlops_smp_jenkins_versions` WHERE `version` = '2.492.3');

INSERT INTO `xnet_mlops_smp_jenkins_masters`
(`uid`, `name`, `host`, `port`, `username`, `encrypted_password`, `os_type`, `jenkins_port`, `jenkins_home`, `jenkins_version`, `java_version`, `java_opts`, `admin_username`, `encrypted_admin_password`, `credentials_config`, `status`, `initial_password`, `deploy_log`, `last_heartbeat`, `region`, `cpu_cores`, `ram_gb`, `disk_gb`, `tenant_uid`, `description`, `created_by`) VALUES
('JENKINS-DEMO-MASTER', '演示 Jenkins Master', '192.0.2.24', 22, 'demo', '', 'linux', 8080, '/var/jenkins_home', '2.492.3', '17', '-Xmx4g -Xms2g', 'demo-admin', NULL, '{"mode":"showcase","credentials":[]}', 'running', NULL, '演示环境：Master 已启动，3 个执行器在线', DATE_SUB(NOW(), INTERVAL 1 MINUTE), 'guangzhou', 8, 16, 200, 'default', '负责演示训练流水线编排，不包含真实凭据', 'demo-user')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `jenkins_version` = VALUES(`jenkins_version`), `last_heartbeat` = VALUES(`last_heartbeat`), `description` = VALUES(`description`);

INSERT INTO `xnet_mlops_smp_jenkins_nodes`
(`uid`, `name`, `host`, `port`, `username`, `encrypted_password`, `os_type`, `region`, `container_type`, `resource_type`, `resource_spec`, `cpu_cores`, `ram_gb`, `gpu_memory`, `gpu_model`, `gpu_count`, `status`, `jenkins_url`, `agent_name`, `work_dir`, `java_version`, `python_version`, `agent_version`, `labels`, `description`, `deploy_log`, `last_heartbeat`, `tenant_uid`, `dept_uid`, `team_uid`, `created_by`) VALUES
('JNODE-DEMO-CPU', 'demo-cpu-executor', '192.0.2.21', 22, 'demo', '', 'linux', 'guangzhou', 'docker', 'cpu', '32C128G', 32, 128, NULL, NULL, NULL, 'online', 'http://192.0.2.24:8080', 'demo-cpu-executor', '/data/jenkins', '17', '3.11', '1.0', 'cpu,dpp,mtp', 'CPU 特征工程与训练执行器', '演示执行器在线', DATE_SUB(NOW(), INTERVAL 1 MINUTE), 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', 'demo-user'),
('JNODE-DEMO-GPU', 'demo-gpu-executor', '192.0.2.22', 22, 'demo', '', 'linux', 'guangzhou', 'docker', 'gpu', '4xA100-80G', 64, 512, 80, 'NVIDIA A100', 4, 'online', 'http://192.0.2.24:8080', 'demo-gpu-executor', '/data/jenkins', '17', '3.11', '1.0', 'gpu,mtp,llm', 'GPU 微调与推理执行器', '演示执行器在线', DATE_SUB(NOW(), INTERVAL 2 MINUTE), 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', 'demo-user')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `resource_spec` = VALUES(`resource_spec`), `last_heartbeat` = VALUES(`last_heartbeat`), `description` = VALUES(`description`);

INSERT INTO `xnet_mlops_smp_hadoop_versions`
(`version`, `version_type`, `release_date`, `download_url`, `is_latest`) VALUES
('3.4.1', 'stable', '2025-10-15', 'https://archive.apache.org/dist/hadoop/common/hadoop-3.4.1/hadoop-3.4.1.tar.gz', 1),
('3.3.6', 'stable', '2023-06-23', 'https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz', 0)
ON DUPLICATE KEY UPDATE `version_type` = VALUES(`version_type`), `is_latest` = VALUES(`is_latest`);

INSERT INTO `xnet_mlops_smp_hadoop_cluster`
(`uid`, `name`, `description`, `host`, `port`, `ssh_user`, `ssh_password`, `ssh_private_key`, `hadoop_version`, `os_type`, `node_type`, `deploy_mode`, `components`, `hdfs_data_dirs`, `hdfs_replication`, `hdfs_block_size`, `yarn_memory`, `yarn_cpu`, `status`, `master_id`, `created_by`) VALUES
('HADOOP-DEMO-MASTER', '演示数据湖 Master', 'NameNode、ResourceManager 与 HistoryServer 展示节点', '192.0.2.25', 22, 'demo', NULL, NULL, '3.4.1', 'linux', 'master', 'ha', '["namenode","resourcemanager","historyserver"]', '["/data/hdfs/name"]', 2, 134217728, 32768, 16, 'running', NULL, 'demo-user')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `components` = VALUES(`components`), `description` = VALUES(`description`);

SET @demo_hadoop_master_id := (
  SELECT `id` FROM `xnet_mlops_smp_hadoop_cluster`
  WHERE `uid` = 'HADOOP-DEMO-MASTER'
);

INSERT INTO `xnet_mlops_smp_hadoop_cluster`
(`uid`, `name`, `description`, `host`, `port`, `ssh_user`, `ssh_password`, `ssh_private_key`, `hadoop_version`, `os_type`, `node_type`, `deploy_mode`, `components`, `hdfs_data_dirs`, `hdfs_replication`, `hdfs_block_size`, `yarn_memory`, `yarn_cpu`, `status`, `master_id`, `created_by`) VALUES
('HADOOP-DEMO-WORKER-01', '演示数据湖 Worker 01', 'DataNode 与 NodeManager 展示节点', '192.0.2.26', 22, 'demo', NULL, NULL, '3.4.1', 'linux', 'node', 'ha', '["datanode","nodemanager"]', '["/data/hdfs/data"]', 2, 134217728, 65536, 32, 'running', @demo_hadoop_master_id, 'demo-user'),
('HADOOP-DEMO-WORKER-02', '演示数据湖 Worker 02', 'DataNode 与 NodeManager 展示节点', '192.0.2.27', 22, 'demo', NULL, NULL, '3.4.1', 'linux', 'node', 'ha', '["datanode","nodemanager"]', '["/data/hdfs/data"]', 2, 134217728, 65536, 32, 'running', @demo_hadoop_master_id, 'demo-user')
ON DUPLICATE KEY UPDATE `node_type` = VALUES(`node_type`), `status` = VALUES(`status`), `master_id` = VALUES(`master_id`), `description` = VALUES(`description`);

-- MEP: providers, deployments, observability, and OpenClaw
INSERT INTO `xnet_mlops_mep_llm_service`
(`uid`, `name`, `type`, `description`, `endpoint`, `model_name`, `api_key`, `status`, `config`, `created_by`, `updated_by`, `created_at`, `updated_at`) VALUES
('LLM-DEMO-QWEN', 'Qwen2.5 企业助手', 'custom', '展示用大模型服务元数据，不连接真实推理端点', 'https://llm.demo.invalid/v1', 'Qwen2.5-72B-Instruct', NULL, 'running', '{"max_tokens":4096,"temperature":0.7,"demo":true}', 'demo-user', 'demo-user', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `status` = VALUES(`status`), `config` = VALUES(`config`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_mep_deploy_node`
(`uid`, `name`, `ip_address`, `port`, `status`, `cpu_cores`, `memory_gb`, `gpu_info`, `docker_version`, `nginx_status`, `labels`, `description`, `created_by`, `created_at`, `updated_at`) VALUES
('MEP-NODE-DEMO-GPU', '广州 GPU 推理节点', '192.0.2.31', 22, 'online', 64, 512, '4x NVIDIA A100 80GB', '27.5.1', 'running', '["gpu","production-like","demo"]', '承载高吞吐模型服务的展示节点', 'demo-user', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
('MEP-NODE-DEMO-CPU', '上海 CPU 推理节点', '192.0.2.32', 22, 'online', 32, 128, NULL, '27.5.1', 'running', '["cpu","edge","demo"]', '承载轻量模型与灰度流量的展示节点', 'demo-user', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW())
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `gpu_info` = VALUES(`gpu_info`), `nginx_status` = VALUES(`nginx_status`), `labels` = VALUES(`labels`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_mep_api_key`
(`uid`, `name`, `key_hash`, `key_masked`, `provider`, `description`, `status`, `usage_limit`, `usage_count`, `expires_at`, `created_by`, `created_at`, `updated_at`) VALUES
('APIKEY-DEMO-QWEN', '企业助手调用密钥', SHA2('disabled-showcase-key-qwen', 256), 'sk-demo-qwen-****', 'custom', '仅用于界面展示，不是可用密钥', 'active', 100000, 28640, DATE_ADD(NOW(), INTERVAL 180 DAY), 'demo-user', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
('APIKEY-DEMO-ARCHIVE', '历史评估密钥', SHA2('disabled-showcase-key-archive', 256), 'sk-demo-old-****', 'openai', '已停用的展示密钥', 'disabled', 10000, 9842, DATE_SUB(NOW(), INTERVAL 2 DAY), 'demo-user', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`), `status` = VALUES(`status`), `usage_limit` = VALUES(`usage_limit`), `usage_count` = VALUES(`usage_count`), `expires_at` = VALUES(`expires_at`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_mep_model_deployment`
(`uid`, `name`, `model_source`, `model_uid`, `model_name`, `model_version`, `node_uid`, `node_name`, `status`, `container_id`, `container_name`, `image_name`, `port`, `endpoint`, `replicas`, `resource_config`, `nginx_config`, `health_check`, `created_by`, `created_at`, `updated_at`) VALUES
('DEPLOY-DEMO-CHURN', '客户流失实时预测', 'mtp', 'TASK-DEMO-CHURN', '客户流失 XGBoost', '2.1.0', 'MEP-NODE-DEMO-CPU', '上海 CPU 推理节点', 'running', 'demo-container-churn', 'demo-churn-api', 'demo/churn-api:2.1.0', 18080, 'http://192.0.2.32:18080/predict', 3, '{"cpu_limit":"2","memory_limit":"4Gi","gpu_count":0}', '{"upstream_name":"demo_churn","listen_port":18080,"ssl_enabled":false}', '{"enabled":true,"path":"/health","interval":30,"timeout":5,"retries":3}', 'demo-user', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
('DEPLOY-DEMO-SENTIMENT', '评价情感分类服务', 'mtp', 'TASK-DEMO-SENTIMENT', '中文评价情感分类', '1.3.2', 'MEP-NODE-DEMO-GPU', '广州 GPU 推理节点', 'deploying', NULL, 'demo-sentiment-api', 'demo/sentiment-api:1.3.2', 18081, 'http://192.0.2.31:18081/classify', 2, '{"cpu_limit":"4","memory_limit":"16Gi","gpu_count":1,"gpu_memory":"20Gi"}', '{"upstream_name":"demo_sentiment","listen_port":18081,"ssl_enabled":false}', '{"enabled":true,"path":"/health","interval":30,"timeout":5,"retries":3}', 'demo-user', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 4 MINUTE)),
('DEPLOY-DEMO-FORECAST', '设备温度批量预测', 'mtp', 'TASK-DEMO-FORECAST', '设备温度趋势预测', '0.9.5', 'MEP-NODE-DEMO-GPU', '广州 GPU 推理节点', 'stopped', NULL, 'demo-device-forecast', 'demo/device-forecast:0.9.5', 18082, 'http://192.0.2.31:18082/forecast', 1, '{"cpu_limit":"4","memory_limit":"12Gi","gpu_count":1,"gpu_memory":"12Gi"}', '{"upstream_name":"demo_forecast","listen_port":18082,"ssl_enabled":false}', '{"enabled":true,"path":"/health","interval":60,"timeout":5,"retries":3}', 'demo-user', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 5 HOUR))
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `container_id` = VALUES(`container_id`), `replicas` = VALUES(`replicas`), `resource_config` = VALUES(`resource_config`), `updated_at` = VALUES(`updated_at`);

DELETE FROM `xnet_mlops_mep_deployment_log` WHERE `deployment_uid` LIKE 'DEPLOY-DEMO-%';
INSERT INTO `xnet_mlops_mep_deployment_log` (`deployment_uid`, `level`, `message`, `timestamp`) VALUES
('DEPLOY-DEMO-CHURN', 'info', '镜像拉取完成 demo/churn-api:2.1.0', DATE_SUB(NOW(), INTERVAL 8 DAY)),
('DEPLOY-DEMO-CHURN', 'info', '3 个副本健康检查通过，服务已接入流量', DATE_SUB(NOW(), INTERVAL 7 DAY)),
('DEPLOY-DEMO-SENTIMENT', 'info', '正在创建 GPU 推理容器', DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
('DEPLOY-DEMO-SENTIMENT', 'warn', '首次模型加载预计需要 6 分钟', DATE_SUB(NOW(), INTERVAL 4 MINUTE)),
('DEPLOY-DEMO-FORECAST', 'info', '服务已按计划停止', DATE_SUB(NOW(), INTERVAL 5 HOUR));

DELETE FROM `xnet_mlops_mep_service_metrics` WHERE `deployment_uid` LIKE 'DEPLOY-DEMO-%';
INSERT INTO `xnet_mlops_mep_service_metrics`
(`deployment_uid`, `cpu_usage`, `memory_usage`, `request_count`, `error_count`, `avg_response_time`, `timestamp`) VALUES
('DEPLOY-DEMO-CHURN', 38.40, 52.10, 18420, 26, 42, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('DEPLOY-DEMO-CHURN', 44.80, 55.70, 20110, 18, 39, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('DEPLOY-DEMO-CHURN', 41.20, 54.30, 22480, 21, 41, NOW()),
('DEPLOY-DEMO-SENTIMENT', 62.10, 71.80, 0, 0, NULL, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

INSERT INTO `xnet_mlops_mep_openclaw_instance`
(`uid`, `name`, `description`, `deploy_mode`, `deploy_node_id`, `gateway_host`, `gateway_port`, `gateway_token`, `default_model`, `fallback_models`, `subagent_model`, `llm_service_id`, `api_key_id`, `api_key_ref_id`, `enabled_skills`, `skills_config`, `channels_config`, `status`, `container_name`, `workspace_path`, `config_path`, `logs_path`, `total_conversations`, `total_messages`, `last_active_at`, `tenant_uid`, `created_by`, `updated_by`, `created_at`, `updated_at`) VALUES
('OPENCLAW-DEMO-001', '企业智能助手运行时', '连接知识库、模型服务和平台技能的展示实例', 'docker', 'MEP-NODE-DEMO-GPU', 'openclaw.demo.invalid', 18789, NULL, 'custom/Qwen2.5-72B-Instruct', '["custom/Qwen2.5-32B-Instruct"]', 'custom/Qwen2.5-14B-Instruct', (SELECT `id` FROM `xnet_mlops_mep_llm_service` WHERE `uid` = 'LLM-DEMO-QWEN'), 'APIKEY-DEMO-QWEN', (SELECT `id` FROM `xnet_mlops_mep_api_key` WHERE `uid` = 'APIKEY-DEMO-QWEN'), '["SKILL-PDF-001","SKILL-DATA-ANALYSIS-001"]', '{"autoInstall":false,"demo":true}', '{"web":{"enabled":true}}', 'running', 'openclaw-demo', '/demo/openclaw/workspace', '/demo/openclaw/config', '/demo/openclaw/logs', 128, 1860, DATE_SUB(NOW(), INTERVAL 6 MINUTE), 'default', 'demo-user', 'demo-user', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW())
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`), `status` = VALUES(`status`), `total_conversations` = VALUES(`total_conversations`), `total_messages` = VALUES(`total_messages`), `last_active_at` = VALUES(`last_active_at`), `updated_at` = VALUES(`updated_at`);

-- XAA: skills, workflows, executions, and assistants
INSERT INTO `xnet_mlops_xaa_skill_installation` (`skill_id`, `tenant_uid`, `installed_by`, `config_override`, `status`)
SELECT `id`, 'default', 'demo-user', '{"mode":"showcase"}', 'active' FROM `xnet_mlops_xaa_skill`
WHERE `uid` IN ('SKILL-PDF-001', 'SKILL-DATA-ANALYSIS-001', 'SKILL-WEBAPP-TEST-001')
  AND NOT EXISTS (
    SELECT 1 FROM `xnet_mlops_xaa_skill_installation` i
    WHERE i.`skill_id` = `xnet_mlops_xaa_skill`.`id` AND i.`tenant_uid` = 'default'
  );

UPDATE `xnet_mlops_xaa_skill` SET `install_count` = 1
WHERE `uid` IN ('SKILL-PDF-001', 'SKILL-DATA-ANALYSIS-001', 'SKILL-WEBAPP-TEST-001');

INSERT INTO `xnet_mlops_xaa_workflow`
(`uid`, `name`, `description`, `type`, `status`, `graph_json`, `config_json`, `version`, `creator_id`, `creator_name`, `tenant_uid`, `dept_uid`, `team_uid`, `created_at`, `updated_at`) VALUES
('WF-DEMO-CHURN', '客户流失训练与发布', '从数据集读取、特征工程、模型训练到灰度发布的完整 MLOps 流程', 'pipeline', 'published', '{"viewport":{"x":0,"y":0,"zoom":0.85},"nodeCount":5}', '{"timeoutMinutes":120,"notification":"platform"}', 3, 'demo-user', '演示管理员', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('WF-DEMO-KB', '知识库文档自动入库', '监听文档上传并执行解析、分块、向量化和索引更新', 'workflow', 'published', '{"viewport":{"x":0,"y":0,"zoom":1},"nodeCount":4}', '{"timeoutMinutes":30,"notification":"platform"}', 2, 'demo-user', '演示管理员', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('WF-DEMO-REVIEW', '模型上线评审', '结合自动指标门禁与人工审批的模型上线评审流程', 'workflow', 'draft', '{"viewport":{"x":0,"y":0,"zoom":0.9},"nodeCount":4}', '{"timeoutMinutes":240,"notification":"email"}', 1, 'demo-user', '演示管理员', 'default', 'DEPT-DEMO-AI', 'TEAM-DEMO-MLOPS', DATE_SUB(NOW(), INTERVAL 4 DAY), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `status` = VALUES(`status`), `graph_json` = VALUES(`graph_json`), `config_json` = VALUES(`config_json`), `version` = VALUES(`version`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_xaa_workflow_node`
(`uid`, `workflow_id`, `node_type`, `title`, `description`, `config_json`, `position_x`, `position_y`, `width`, `height`, `sort_order`) VALUES
('NODE-DEMO-CHURN-START', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'start', '开始', '接收训练批次参数', '{}', 40, 180, 160, 64, 1),
('NODE-DEMO-CHURN-DATA', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'dpp-feature', '生成客户特征', '运行客户流失特征流水线', '{"resourceUid":"FE-DEMO-CHURN"}', 260, 180, 200, 80, 2),
('NODE-DEMO-CHURN-TRAIN', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'mtp-train', '训练流失模型', '启动客户流失模型周训练', '{"resourceUid":"TASK-DEMO-CHURN"}', 520, 180, 200, 80, 3),
('NODE-DEMO-CHURN-DEPLOY', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'mep-deploy', '灰度发布', '更新客户流失实时预测服务', '{"resourceUid":"DEPLOY-DEMO-CHURN"}', 780, 180, 200, 80, 4),
('NODE-DEMO-CHURN-END', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'end', '完成', '输出部署结果', '{}', 1040, 180, 160, 64, 5),
('NODE-DEMO-KB-START', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), 'start', '文档上传', '接收待处理文档', '{}', 60, 160, 160, 64, 1),
('NODE-DEMO-KB-PARSE', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), 'dpp-task', '文档解析与分块', '提取正文并按语义分块', '{"knowledgeBaseUid":"KB-DEMO-MLOPS"}', 300, 160, 200, 80, 2),
('NODE-DEMO-KB-INDEX', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), 'dpp-task', '向量索引', '生成向量并更新索引', '{"embeddingModel":"bge-m3"}', 580, 160, 200, 80, 3),
('NODE-DEMO-KB-END', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), 'end', '入库完成', '返回索引统计', '{}', 860, 160, 160, 64, 4)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `description` = VALUES(`description`), `config_json` = VALUES(`config_json`), `position_x` = VALUES(`position_x`), `position_y` = VALUES(`position_y`), `sort_order` = VALUES(`sort_order`);

INSERT INTO `xnet_mlops_xaa_workflow_edge`
(`uid`, `workflow_id`, `source_node_id`, `source_handle`, `target_node_id`, `target_handle`, `edge_type`, `sort_order`) VALUES
('EDGE-DEMO-CHURN-01', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-START'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-DATA'), 'input', 'default', 1),
('EDGE-DEMO-CHURN-02', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-DATA'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-TRAIN'), 'input', 'success', 2),
('EDGE-DEMO-CHURN-03', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-TRAIN'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-DEPLOY'), 'input', 'success', 3),
('EDGE-DEMO-CHURN-04', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-DEPLOY'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-END'), 'input', 'success', 4),
('EDGE-DEMO-KB-01', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-KB-START'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-KB-PARSE'), 'input', 'default', 1),
('EDGE-DEMO-KB-02', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-KB-PARSE'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-KB-INDEX'), 'input', 'success', 2),
('EDGE-DEMO-KB-03', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-KB-INDEX'), 'output', (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-KB-END'), 'input', 'success', 3)
ON DUPLICATE KEY UPDATE `source_node_id` = VALUES(`source_node_id`), `target_node_id` = VALUES(`target_node_id`), `edge_type` = VALUES(`edge_type`), `sort_order` = VALUES(`sort_order`);

INSERT INTO `xnet_mlops_xaa_workflow_execution`
(`uid`, `workflow_id`, `status`, `inputs_json`, `outputs_json`, `error_message`, `started_at`, `finished_at`, `elapsed_time`, `triggered_by`, `trigger_type`, `created_at`, `updated_at`) VALUES
('EXEC-DEMO-CHURN-01', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'succeeded', '{"batch":"2026-W29"}', '{"modelVersion":"2.1.0","deploymentUid":"DEPLOY-DEMO-CHURN"}', NULL, DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), 3584000, 'demo-user', 'scheduled', DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR)),
('EXEC-DEMO-KB-01', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-KB'), 'succeeded', '{"documentUid":"DOC-DEMO-ALERT"}', '{"chunks":2,"tokens":1640}', NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR), 84200, 'demo-user', 'manual', DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR)),
('EXEC-DEMO-CHURN-02', (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), 'running', '{"batch":"2026-W30"}', NULL, NULL, DATE_SUB(NOW(), INTERVAL 18 MINUTE), NULL, NULL, 'demo-user', 'manual', DATE_SUB(NOW(), INTERVAL 18 MINUTE), NOW())
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `outputs_json` = VALUES(`outputs_json`), `finished_at` = VALUES(`finished_at`), `elapsed_time` = VALUES(`elapsed_time`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_xaa_node_execution`
(`uid`, `execution_id`, `workflow_id`, `node_id`, `node_type`, `node_title`, `status`, `inputs_json`, `outputs_json`, `metadata_json`, `started_at`, `finished_at`, `elapsed_time`, `retry_count`) VALUES
('NEXEC-DEMO-CHURN-DATA', (SELECT `id` FROM `xnet_mlops_xaa_workflow_execution` WHERE `uid` = 'EXEC-DEMO-CHURN-01'), (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-DATA'), 'dpp-feature', '生成客户特征', 'succeeded', '{"featureUid":"FE-DEMO-CHURN"}', '{"rows":1200000,"columns":86}', '{"cacheHit":false}', DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), 492000, 0),
('NEXEC-DEMO-CHURN-TRAIN', (SELECT `id` FROM `xnet_mlops_xaa_workflow_execution` WHERE `uid` = 'EXEC-DEMO-CHURN-01'), (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-TRAIN'), 'mtp-train', '训练流失模型', 'succeeded', '{"taskUid":"TASK-DEMO-CHURN"}', '{"auc":0.912,"f1":0.864}', '{"executor":"demo-gpu-executor"}', DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), 2680000, 0),
('NEXEC-DEMO-CHURN-DEPLOY', (SELECT `id` FROM `xnet_mlops_xaa_workflow_execution` WHERE `uid` = 'EXEC-DEMO-CHURN-01'), (SELECT `id` FROM `xnet_mlops_xaa_workflow` WHERE `uid` = 'WF-DEMO-CHURN'), (SELECT `id` FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` = 'NODE-DEMO-CHURN-DEPLOY'), 'mep-deploy', '灰度发布', 'succeeded', '{"deploymentUid":"DEPLOY-DEMO-CHURN"}', '{"replicas":3,"health":"passing"}', '{"strategy":"canary"}', DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), 412000, 0)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `outputs_json` = VALUES(`outputs_json`), `metadata_json` = VALUES(`metadata_json`), `elapsed_time` = VALUES(`elapsed_time`);

INSERT INTO `xnet_mlops_xaa_assistant`
(`uid`, `name`, `description`, `avatar`, `openclaw_instance_id`, `gateway_url`, `llm_service_id`, `default_model`, `system_prompt`, `temperature`, `max_tokens`, `knowledge_base_ids`, `rag_enabled`, `rag_top_k`, `skill_ids`, `tools_enabled`, `ui_config`, `welcome_message`, `placeholder`, `status`, `is_default`, `total_conversations`, `total_messages`, `last_used_at`, `tenant_uid`, `created_by`, `updated_by`, `created_at`, `updated_at`) VALUES
('ASSISTANT-DEMO-MLOPS', 'MLOps 运维助手', '回答训练、部署、监控和故障处置问题，并可编排平台技能', NULL, (SELECT `id` FROM `xnet_mlops_mep_openclaw_instance` WHERE `uid` = 'OPENCLAW-DEMO-001'), 'https://openclaw.demo.invalid', (SELECT `id` FROM `xnet_mlops_mep_llm_service` WHERE `uid` = 'LLM-DEMO-QWEN'), 'Qwen2.5-72B-Instruct', '你是 SynapXnet MLOps 运维助手。回答必须基于平台知识库，并明确区分演示数据与真实运行状态。', 0.35, 4096, JSON_ARRAY((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-MLOPS')), 1, 5, JSON_ARRAY((SELECT `id` FROM `xnet_mlops_xaa_skill` WHERE `uid` = 'SKILL-PDF-001'), (SELECT `id` FROM `xnet_mlops_xaa_skill` WHERE `uid` = 'SKILL-DATA-ANALYSIS-001')), 1, '{"position":{"right":24,"bottom":24},"size":{"width":420,"height":640},"theme":"light","primaryColor":"#1677ff","borderRadius":8,"showAvatar":true}', '您好，我可以帮助检查训练任务、模型部署和平台告警。', '请输入 MLOps 问题...', 'active', 1, 86, 642, DATE_SUB(NOW(), INTERVAL 12 MINUTE), 'default', 'demo-user', 'demo-user', DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
('ASSISTANT-DEMO-GOV', '模型治理助手', '围绕模型卡片、上线评审、合规与版本治理提供问答', NULL, NULL, NULL, (SELECT `id` FROM `xnet_mlops_mep_llm_service` WHERE `uid` = 'LLM-DEMO-QWEN'), 'Qwen2.5-72B-Instruct', '你是模型治理助手，优先引用治理知识库并给出检查清单。', 0.20, 3072, JSON_ARRAY((SELECT `id` FROM `xnet_mlops_dpp_knowledge_base` WHERE `uid` = 'KB-DEMO-GOVERNANCE')), 1, 6, JSON_ARRAY((SELECT `id` FROM `xnet_mlops_xaa_skill` WHERE `uid` = 'SKILL-PDF-001')), 1, '{"position":{"right":24,"bottom":24},"size":{"width":420,"height":640},"theme":"light","primaryColor":"#52c41a","borderRadius":8,"showAvatar":true}', '您好，我可以协助完成模型上线评审与合规检查。', '请输入治理问题...', 'active', 0, 34, 218, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'default', 'demo-user', 'demo-user', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `system_prompt` = VALUES(`system_prompt`), `knowledge_base_ids` = VALUES(`knowledge_base_ids`), `skill_ids` = VALUES(`skill_ids`), `welcome_message` = VALUES(`welcome_message`), `placeholder` = VALUES(`placeholder`), `status` = VALUES(`status`), `is_default` = VALUES(`is_default`), `total_conversations` = VALUES(`total_conversations`), `total_messages` = VALUES(`total_messages`), `last_used_at` = VALUES(`last_used_at`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_xaa_assistant_conversation`
(`uid`, `assistant_id`, `title`, `summary`, `user_id`, `user_name`, `status`, `message_count`, `token_count`, `last_message_at`, `created_at`, `updated_at`) VALUES
('CONV-DEMO-ROLLBACK', (SELECT `id` FROM `xnet_mlops_xaa_assistant` WHERE `uid` = 'ASSISTANT-DEMO-MLOPS'), '客户流失服务回滚方案', '根据错误率阈值生成灰度回滚步骤', 'demo-user', '演示管理员', 'active', 4, 1280, DATE_SUB(NOW(), INTERVAL 12 MINUTE), DATE_SUB(NOW(), INTERVAL 35 MINUTE), DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
('CONV-DEMO-REVIEW', (SELECT `id` FROM `xnet_mlops_xaa_assistant` WHERE `uid` = 'ASSISTANT-DEMO-GOV'), '上线评审材料检查', '检查模型上线前需要提交的材料', 'demo-user', '演示管理员', 'active', 2, 620, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR))
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `summary` = VALUES(`summary`), `message_count` = VALUES(`message_count`), `token_count` = VALUES(`token_count`), `last_message_at` = VALUES(`last_message_at`), `updated_at` = VALUES(`updated_at`);

INSERT INTO `xnet_mlops_xaa_assistant_message`
(`uid`, `conversation_id`, `role`, `content`, `token_count`, `model_used`, `rag_sources`, `tool_calls`, `metadata`, `created_at`) VALUES
('MSG-DEMO-ROLLBACK-01', (SELECT `id` FROM `xnet_mlops_xaa_assistant_conversation` WHERE `uid` = 'CONV-DEMO-ROLLBACK'), 'user', '客户流失预测服务错误率升高，应该如何回滚?', 42, NULL, NULL, NULL, '{"demo":true}', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
('MSG-DEMO-ROLLBACK-02', (SELECT `id` FROM `xnet_mlops_xaa_assistant_conversation` WHERE `uid` = 'CONV-DEMO-ROLLBACK'), 'assistant', '建议先确认错误率持续时间和当前灰度比例，然后将流量切换至上一稳定版本，保留故障实例日志并暂停自动扩容。', 168, 'Qwen2.5-72B-Instruct', '[{"document":"模型部署与回滚手册","score":0.91}]', '[{"tool":"deployment_status","deploymentUid":"DEPLOY-DEMO-CHURN"}]', '{"demo":true}', DATE_SUB(NOW(), INTERVAL 34 MINUTE)),
('MSG-DEMO-REVIEW-01', (SELECT `id` FROM `xnet_mlops_xaa_assistant_conversation` WHERE `uid` = 'CONV-DEMO-REVIEW'), 'user', '模型上线评审需要哪些材料?', 28, NULL, NULL, NULL, '{"demo":true}', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('MSG-DEMO-REVIEW-02', (SELECT `id` FROM `xnet_mlops_xaa_assistant_conversation` WHERE `uid` = 'CONV-DEMO-REVIEW'), 'assistant', '至少需要训练数据说明、模型卡片、核心指标、偏差评估、资源预算、监控方案和回滚预案。', 136, 'Qwen2.5-72B-Instruct', '[{"document":"模型上线评审清单","score":0.94}]', NULL, '{"demo":true}', DATE_SUB(NOW(), INTERVAL 2 HOUR))
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`), `token_count` = VALUES(`token_count`), `model_used` = VALUES(`model_used`), `rag_sources` = VALUES(`rag_sources`), `tool_calls` = VALUES(`tool_calls`), `metadata` = VALUES(`metadata`);

COMMIT;
