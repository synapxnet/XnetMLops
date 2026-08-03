-- GOAI 1.0.0: 复盘 Skill 以草稿状态进入 XAA，发布前必须人工审核。
INSERT INTO xnet_mlops_xaa_skill
(uid, name, description, type, status, category, tags, content_md, config_json, icon,
 has_scripts, has_references, has_assets, install_count, source_url, is_official,
 version, creator_id, tenant_uid, created_at, updated_at)
VALUES
('skill_model_regression_response', 'model-regression-response',
 '模型输入契约回归的跨平台取证、审批回滚与独立验证 Skill', 'workflow', 'draft', 'ai-ml',
 'GOAI,Trace,DataOps,MLOps,AIOps,rollback',
 '# model-regression-response\n\n使用 GOAI 1.0.0 工具契约完成取证、审批、回滚和独立验证。发布前必须人工审核。',
 '{"contractVersion":"1.0.0","reviewRequired":true,"tools":["aiops.alert.get","aiops.service.health","aiops.k8s.workload.get","dataops.quality.report.get","dataops.schema.snapshot.get","dataops.lineage.get","dataops.workflow.instance.get","mlops.deployment.get","mlops.inference.probe","mlops.deployment.rollback"]}',
 'lucide:workflow', FALSE, TRUE, FALSE, 0, 'https://openxnet.synapxnet.com', TRUE,
 1, 'goai-fixture', 'ws_goai_demo', '2026-08-02 10:30:00', '2026-08-02 10:30:00')
ON DUPLICATE KEY UPDATE description = VALUES(description), status = 'draft', tags = VALUES(tags),
content_md = VALUES(content_md), config_json = VALUES(config_json), version = VALUES(version), updated_at = VALUES(updated_at);
