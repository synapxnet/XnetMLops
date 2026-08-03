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
