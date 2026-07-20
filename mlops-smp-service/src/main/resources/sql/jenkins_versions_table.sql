-- ============================================================================
-- Jenkins 版本缓存表
-- 用于存储从 mirrors.jenkins.io 获取的 Jenkins 版本列表
-- ============================================================================

CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_versions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version` VARCHAR(32) NOT NULL COMMENT '版本号(如: 2.462.3)',
  `version_type` VARCHAR(16) NOT NULL DEFAULT 'stable' COMMENT '版本类型(stable/weekly)',
  `release_date` DATE DEFAULT NULL COMMENT '发布日期',
  `download_url` VARCHAR(512) DEFAULT NULL COMMENT '下载URL',
  `sha256` VARCHAR(128) DEFAULT NULL COMMENT 'SHA256校验值',
  `is_lts` TINYINT(1) DEFAULT 0 COMMENT '是否为LTS版本',
  `is_latest` TINYINT(1) DEFAULT 0 COMMENT '是否为最新版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_type` (`version`, `version_type`),
  KEY `idx_version_type` (`version_type`),
  KEY `idx_is_lts` (`is_lts`),
  KEY `idx_release_date` (`release_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins版本缓存表';

-- ============================================================================
-- 版本同步记录表
-- ============================================================================

CREATE TABLE IF NOT EXISTS `xnet_mlops_smp_jenkins_version_sync_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sync_type` VARCHAR(16) NOT NULL COMMENT '同步类型(stable/weekly/all)',
  `sync_status` VARCHAR(16) NOT NULL COMMENT '同步状态(success/failed)',
  `versions_count` INT DEFAULT 0 COMMENT '同步的版本数量',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `sync_duration_ms` BIGINT DEFAULT NULL COMMENT '同步耗时(毫秒)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',

  PRIMARY KEY (`id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Jenkins版本同步日志表';
