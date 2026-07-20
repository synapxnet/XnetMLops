-- Remove only the deterministic records created by showcase_data.sql.
-- Run this script only in a disposable showcase environment.

SET NAMES utf8mb4;
START TRANSACTION;

DELETE FROM `xnet_mlops_xaa_assistant_message` WHERE `uid` LIKE 'MSG-DEMO-%';
DELETE FROM `xnet_mlops_xaa_assistant_conversation` WHERE `uid` LIKE 'CONV-DEMO-%';
DELETE FROM `xnet_mlops_xaa_node_execution` WHERE `uid` LIKE 'NEXEC-DEMO-%';
DELETE FROM `xnet_mlops_xaa_workflow_execution` WHERE `uid` LIKE 'EXEC-DEMO-%';
DELETE FROM `xnet_mlops_xaa_workflow_edge` WHERE `uid` LIKE 'EDGE-DEMO-%';
DELETE FROM `xnet_mlops_xaa_workflow_node` WHERE `uid` LIKE 'NODE-DEMO-%';
DELETE FROM `xnet_mlops_xaa_workflow` WHERE `uid` LIKE 'WF-DEMO-%';
DELETE FROM `xnet_mlops_xaa_assistant` WHERE `uid` LIKE 'ASSISTANT-DEMO-%';
DELETE FROM `xnet_mlops_xaa_skill_installation`
WHERE `tenant_uid` = 'default'
  AND `skill_id` IN (
    SELECT `id` FROM `xnet_mlops_xaa_skill`
    WHERE `uid` IN ('SKILL-PDF-001', 'SKILL-DATA-ANALYSIS-001', 'SKILL-WEBAPP-TEST-001')
  );
UPDATE `xnet_mlops_xaa_skill` SET `install_count` = 0
WHERE `uid` IN ('SKILL-PDF-001', 'SKILL-DATA-ANALYSIS-001', 'SKILL-WEBAPP-TEST-001');

DELETE FROM `xnet_mlops_mep_deployment_log` WHERE `deployment_uid` LIKE 'DEPLOY-DEMO-%';
DELETE FROM `xnet_mlops_mep_service_metrics` WHERE `deployment_uid` LIKE 'DEPLOY-DEMO-%';
DELETE FROM `xnet_mlops_mep_model_deployment` WHERE `uid` LIKE 'DEPLOY-DEMO-%';
DELETE FROM `xnet_mlops_mep_openclaw_instance` WHERE `uid` LIKE 'OPENCLAW-DEMO-%';
DELETE FROM `xnet_mlops_mep_api_key` WHERE `uid` LIKE 'APIKEY-DEMO-%';
DELETE FROM `xnet_mlops_mep_llm_service` WHERE `uid` LIKE 'LLM-DEMO-%';
DELETE FROM `xnet_mlops_mep_deploy_node` WHERE `uid` LIKE 'MEP-NODE-DEMO-%';

DELETE FROM `xnet_mlops_mtp_train_schedule_info` WHERE `uid` LIKE 'SCH-DEMO-%';
DELETE FROM `xnet_mlops_mtp_train_info` WHERE `uid` LIKE 'RUN-DEMO-%';
DELETE FROM `xnet_mlops_mtp_task_custom_variable` WHERE `uid` LIKE 'TV-DEMO-%';
DELETE FROM `xnet_mlops_mtp_task_dataset` WHERE `uid` LIKE 'TD-DEMO-%';
DELETE FROM `xnet_mlops_mtp_train_task` WHERE `uid` LIKE 'TASK-DEMO-%';
DELETE FROM `xnet_mlops_mtp_algorithms` WHERE `uid` LIKE 'ALG-DEMO-%';

DELETE FROM `xnet_mlops_dpp_retrieval_log` WHERE `session_id` LIKE 'DEMO-%';
DELETE FROM `xnet_mlops_dpp_kb_chunk` WHERE `uid` LIKE 'CHUNK-DEMO-%';
DELETE FROM `xnet_mlops_dpp_kb_document` WHERE `uid` LIKE 'DOC-DEMO-%';
DELETE FROM `xnet_mlops_dpp_kb_tag` WHERE `kb_id` IN (
  SELECT `id` FROM `xnet_mlops_dpp_knowledge_base`
  WHERE `uid` IN ('KB-DEMO-MLOPS', 'KB-DEMO-GOVERNANCE')
);
DELETE FROM `xnet_mlops_dpp_knowledge_base`
WHERE `uid` IN ('KB-DEMO-MLOPS', 'KB-DEMO-GOVERNANCE');
DELETE FROM `xnet_mlops_dpp_embedding_model` WHERE `name` = '演示 BGE-M3';
DELETE FROM `xnet_mlops_dpp_feature_engineering` WHERE `uid` LIKE 'FE-DEMO-%';
DELETE FROM `xnet_mlops_dpp_feature_operator` WHERE `uid` LIKE 'DPP-OP-DEMO-%';
DELETE FROM `xnet_mlops_dpp_dataset` WHERE `uid` LIKE 'DSET-DEMO-%';

DELETE FROM `xnet_mlops_smp_jenkins_nodes` WHERE `uid` LIKE 'JNODE-DEMO-%';
DELETE FROM `xnet_mlops_smp_jenkins_masters` WHERE `uid` LIKE 'JENKINS-DEMO-%';
DELETE FROM `xnet_mlops_smp_hadoop_cluster` WHERE `uid` LIKE 'HADOOP-DEMO-WORKER-%';
DELETE FROM `xnet_mlops_smp_hadoop_cluster` WHERE `uid` = 'HADOOP-DEMO-MASTER';
DELETE FROM `xnet_mlops_smp_workstation` WHERE `uid` LIKE 'WS-DEMO-%';
DELETE FROM `xnet_mlops_smp_docker_file` WHERE `uid` LIKE 'IMG-DEMO-%';
DELETE FROM `xnet_mlops_smp_harbor_repository` WHERE `uid` LIKE 'HARBOR-DEMO-%';
DELETE FROM `xnet_mlops_smp_algorithms` WHERE `uid` LIKE 'REPO-DEMO-ALGORITHM%';
DELETE FROM `xnet_mlops_smp_feature_operators` WHERE `uid` LIKE 'REPO-DEMO-FEATURE%';
DELETE FROM `xnet_mlops_sys_datasource` WHERE `uid` LIKE 'DS-DEMO-%';
DELETE FROM `xnet_mlops_sys_bucket` WHERE `uid` LIKE 'BUCKET-DEMO-%';
DELETE FROM `xnet_mlops_sys_team` WHERE `uid` = 'TEAM-DEMO-MLOPS';
DELETE FROM `xnet_mlops_sys_department` WHERE `uid` = 'DEPT-DEMO-AI';

COMMIT;
