-- 推荐 DCN 训练的审批、幂等、输入摘要、模型摘要和指标审计表。
CREATE TABLE IF NOT EXISTS xnet_mlops_mtp_recommendation_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_uid VARCHAR(64) NOT NULL UNIQUE COMMENT '推荐训练运行标识',
    dataset_id BIGINT NOT NULL COMMENT 'MLOps 数据集主键',
    tenant_uid VARCHAR(128) NOT NULL COMMENT '租户边界',
    user_id VARCHAR(128) NOT NULL COMMENT '发起用户',
    product_version VARCHAR(128) NOT NULL COMMENT 'DataOps 数据产品版本',
    approval_id VARCHAR(128) NOT NULL COMMENT '人工审批号',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE COMMENT '幂等键',
    status VARCHAR(32) NOT NULL COMMENT '运行状态',
    schema_digest_sha256 CHAR(64) NOT NULL COMMENT 'Schema 摘要',
    artifact_digest_sha256 CHAR(64) NOT NULL COMMENT '训练输入摘要',
    model_digest_sha256 CHAR(64) NULL COMMENT '模型摘要',
    artifact_reference VARCHAR(256) NULL COMMENT '不含物理路径的制品引用',
    metrics_json TEXT NULL COMMENT '训练和测试指标',
    error_summary VARCHAR(1000) NULL COMMENT '失败摘要',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL
) COMMENT '推荐 DCN 训练运行审计表';
