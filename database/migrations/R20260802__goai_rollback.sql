-- GOAI 1.0.0 回滚：执行前先导出部署动作、探针、修订和契约证据。
ALTER TABLE `xnet_mlops_mep_model_deployment`
    DROP COLUMN `last_verified_at`,
    DROP COLUMN `resource_version`,
    DROP COLUMN `active_revision`;
DROP TABLE IF EXISTS `xnet_mlops_mep_inference_probe`;
DROP TABLE IF EXISTS `xnet_mlops_mep_deployment_action`;
DROP TABLE IF EXISTS `xnet_mlops_mep_deployment_revision`;
DROP TABLE IF EXISTS `xnet_mlops_mep_model_contract`;
