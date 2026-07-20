-- Jenkins节点配置表
-- 用于存储作业节点配置信息，支持一键部署Jenkins Agent

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

-- 如果表已存在但字符集不匹配，运行以下命令修复:
-- ALTER TABLE xnet_mlops_smp_jenkins_nodes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 添加资源配置字段 (如果表已存在，运行以下ALTER语句):
-- ALTER TABLE xnet_mlops_smp_jenkins_nodes
--   ADD COLUMN `region` VARCHAR(32) DEFAULT 'guangzhou' COMMENT '地域' AFTER `os_type`,
--   ADD COLUMN `container_type` VARCHAR(32) DEFAULT 'docker' COMMENT '容器类型(cce/docker)' AFTER `region`,
--   ADD COLUMN `resource_type` VARCHAR(32) DEFAULT 'cpu' COMMENT '资源类型(cpu/single_gpu/multi_gpu)' AFTER `container_type`,
--   ADD COLUMN `resource_spec` VARCHAR(64) DEFAULT NULL COMMENT '资源规格ID' AFTER `resource_type`,
--   ADD COLUMN `cpu_cores` INT DEFAULT 2 COMMENT 'CPU核数' AFTER `resource_spec`,
--   ADD COLUMN `ram_gb` INT DEFAULT 128 COMMENT '内存大小(GB)' AFTER `cpu_cores`,
--   ADD COLUMN `gpu_memory` INT DEFAULT NULL COMMENT 'GPU显存(GB)' AFTER `ram_gb`,
--   ADD COLUMN `gpu_model` VARCHAR(64) DEFAULT NULL COMMENT 'GPU型号' AFTER `gpu_memory`,
--   ADD COLUMN `gpu_count` INT DEFAULT NULL COMMENT 'GPU数量' AFTER `gpu_model`;
