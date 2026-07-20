-- =====================================================
-- XAA (Xnet-AI-Agent) 模块数据库表结构
-- 包含 XAW (Xnet-Agent-Workflow) 和 XAS (Xnet-Agent-Skill) 两个子模块
-- 数据库: XnetMLops
-- 表名规范: xnet_mlops_xaa_*
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 使用数据库
USE `XnetMLops`;

-- =====================================================
-- XAW - 工作流相关表
-- =====================================================

-- 工作流主表
DROP TABLE IF EXISTS `xnet_mlops_xaa_workflow`;
CREATE TABLE `xnet_mlops_xaa_workflow` (
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

-- 工作流节点表
DROP TABLE IF EXISTS `xnet_mlops_xaa_workflow_node`;
CREATE TABLE `xnet_mlops_xaa_workflow_node` (
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

-- 工作流边(连接线)表
DROP TABLE IF EXISTS `xnet_mlops_xaa_workflow_edge`;
CREATE TABLE `xnet_mlops_xaa_workflow_edge` (
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

-- 工作流执行记录表
DROP TABLE IF EXISTS `xnet_mlops_xaa_workflow_execution`;
CREATE TABLE `xnet_mlops_xaa_workflow_execution` (
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

-- 节点执行记录表
DROP TABLE IF EXISTS `xnet_mlops_xaa_node_execution`;
CREATE TABLE `xnet_mlops_xaa_node_execution` (
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

-- =====================================================
-- XAS - 技能相关表 (预留)
-- =====================================================

-- 技能表
DROP TABLE IF EXISTS `xnet_mlops_xaa_skill`;
CREATE TABLE `xnet_mlops_xaa_skill` (
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

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入示例工作流
INSERT INTO `xnet_mlops_xaa_workflow` (`uid`, `name`, `description`, `type`, `status`, `version`, `creator_id`, `creator_name`)
VALUES
(UUID(), '数据处理-训练-部署流水线', '一个完整的MLOps流水线示例，包含数据处理、模型训练和模型部署三个阶段', 'pipeline', 'draft', 1, 'system', '系统'),
(UUID(), '特征工程工作流', '用于数据特征提取和转换的工作流', 'workflow', 'draft', 1, 'system', '系统');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 表名清单
-- =====================================================
-- XnetMLops.xnet_mlops_xaa_workflow           - 工作流主表
-- XnetMLops.xnet_mlops_xaa_workflow_node      - 工作流节点表
-- XnetMLops.xnet_mlops_xaa_workflow_edge      - 工作流边表
-- XnetMLops.xnet_mlops_xaa_workflow_execution - 工作流执行记录表
-- XnetMLops.xnet_mlops_xaa_node_execution     - 节点执行记录表
-- XnetMLops.xnet_mlops_xaa_skill              - 技能表 (预留)
--
-- =====================================================
-- 说明
-- =====================================================
--
-- 节点类型 (node_type) 说明:
-- - start: 开始节点
-- - end: 结束节点
-- - dpp-dataset: DPP数据集节点
-- - dpp-feature: DPP特征工程节点
-- - dpp-task: DPP数据任务节点
-- - mtp-algorithm: MTP算法节点
-- - mtp-train: MTP训练任务节点
-- - mtp-output: MTP模型输出节点
-- - mep-deploy: MEP部署节点
-- - mep-service: MEP服务节点
-- - if-else: 条件分支节点
-- - loop: 循环节点
-- - parallel: 并行节点
-- - http-request: HTTP请求节点
-- - code: 代码执行节点
-- - variable-assigner: 变量赋值节点
-- - template-transform: 模板转换节点
-- - wait: 等待节点
-- - human-input: 人工输入节点
--
-- 执行状态 (status) 说明:
-- - scheduled: 已调度，等待执行
-- - pending: 等待中
-- - running: 运行中
-- - succeeded: 成功
-- - failed: 失败
-- - stopped: 已停止
-- - paused: 暂停中
-- - skipped: 已跳过
