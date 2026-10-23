-- ============================================================================
-- Jenkins Master 配置表
-- 用于存储Jenkins主节点的部署配置和状态信息
-- ============================================================================

CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_masters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符(UUID)',
  `name` VARCHAR(128) NOT NULL COMMENT 'Master名称',

  -- SSH连接配置
  `host` VARCHAR(256) NOT NULL COMMENT '主机地址',
  `port` INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
  `username` VARCHAR(64) NOT NULL COMMENT 'SSH用户名',
  `encrypted_password` VARCHAR(512) NOT NULL COMMENT 'SSH密码(AES加密)',
  `os_type` VARCHAR(32) NOT NULL DEFAULT 'linux' COMMENT '操作系统类型(linux/macos/windows)',

  -- Jenkins配置
  `jenkins_port` INT NOT NULL DEFAULT 8080 COMMENT 'Jenkins HTTP端口',
  `jenkins_home` VARCHAR(512) DEFAULT '/var/jenkins_home' COMMENT 'Jenkins Home目录',
  `jenkins_version` VARCHAR(32) DEFAULT NULL COMMENT 'Jenkins版本',
  `java_version` VARCHAR(32) DEFAULT '17' COMMENT 'Java版本',
  `java_opts` VARCHAR(1024) DEFAULT '-Xmx2g -Xms1g' COMMENT 'JVM参数',

  -- 管理员配置
  `admin_username` VARCHAR(64) DEFAULT 'admin' COMMENT '管理员用户名',
  `encrypted_admin_password` VARCHAR(512) DEFAULT NULL COMMENT '管理员密码(AES加密)',

  -- 凭证配置(JSON格式存储)
  `credentials_config` JSON DEFAULT NULL COMMENT '凭证配置JSON(Git/Harbor/SSH凭证)',

  -- 状态信息
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态(pending/deploying/deployed/failed/running/stopped)',
  `initial_password` VARCHAR(256) DEFAULT NULL COMMENT 'Jenkins初始密码',
  `deploy_log` LONGTEXT DEFAULT NULL COMMENT '部署日志',
  `last_heartbeat` DATETIME DEFAULT NULL COMMENT '最后心跳时间',

  -- 资源配置
  `region` VARCHAR(32) DEFAULT 'guangzhou' COMMENT '地域(guangzhou/beijing/shanghai/...)',
  `cpu_cores` INT DEFAULT 4 COMMENT 'CPU核数',
  `ram_gb` INT DEFAULT 8 COMMENT '内存(GB)',
  `disk_gb` INT DEFAULT 100 COMMENT '磁盘空间(GB)',

  -- 关联字段
  `tenant_uid` VARCHAR(64) DEFAULT NULL COMMENT '租户UID',

  -- 审计字段
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_host` (`host`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_uid`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins Master配置表';

-- ============================================================================
-- 示例数据(可选)
-- ============================================================================
-- INSERT INTO `xnet_mlops_smp_jenkins_masters`
-- (`uid`, `name`, `host`, `port`, `username`, `encrypted_password`, `os_type`, `jenkins_port`, `status`)
-- VALUES
-- (UUID(), 'jenkins-master-01', '192.168.1.100', 22, 'root', 'encrypted_password_here', 'linux', 8080, 'pending');
