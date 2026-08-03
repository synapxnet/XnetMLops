-- GOAI 1.0.0 迁移校验。
SELECT COUNT(*) = 4 AS goai_tables_ready
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (
  'xnet_mlops_mep_model_contract', 'xnet_mlops_mep_deployment_revision',
  'xnet_mlops_mep_deployment_action', 'xnet_mlops_mep_inference_probe');
SELECT COUNT(*) = 3 AS deployment_version_columns_ready
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xnet_mlops_mep_model_deployment'
  AND COLUMN_NAME IN ('active_revision', 'resource_version', 'last_verified_at');
