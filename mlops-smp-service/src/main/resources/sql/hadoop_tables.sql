-- ============================================================
-- Hadoop 集群部署相关表结构
-- 创建时间: 2026-02-08
-- ============================================================

-- Hadoop 版本表
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_hadoop_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    version_type VARCHAR(20) DEFAULT 'stable' COMMENT '版本类型 (stable/alpha/beta)',
    release_date DATE COMMENT '发布日期',
    download_url VARCHAR(500) COMMENT '下载地址',
    is_latest BOOLEAN DEFAULT FALSE COMMENT '是否最新版本',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hadoop版本表';

-- Hadoop 集群节点表
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_hadoop_cluster (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    uid VARCHAR(36) NOT NULL UNIQUE COMMENT '唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '节点名称',
    description TEXT COMMENT '描述',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT DEFAULT 22 COMMENT 'SSH端口',
    ssh_user VARCHAR(100) COMMENT 'SSH用户名',
    ssh_password VARCHAR(500) COMMENT 'SSH密码(加密)',
    ssh_private_key TEXT COMMENT 'SSH私钥(加密)',
    hadoop_version VARCHAR(50) COMMENT 'Hadoop版本',
    os_type VARCHAR(50) COMMENT '操作系统类型',
    node_type VARCHAR(20) NOT NULL COMMENT '节点类型 (master/node)',
    deploy_mode VARCHAR(20) DEFAULT 'standard' COMMENT '部署模式 (standard/ha)',
    components TEXT COMMENT '组件列表 JSON',
    hdfs_data_dirs TEXT COMMENT 'HDFS数据目录 JSON',
    hdfs_replication INT DEFAULT 3 COMMENT 'HDFS副本数',
    hdfs_block_size BIGINT DEFAULT 134217728 COMMENT 'HDFS块大小(字节)',
    yarn_memory INT DEFAULT 8192 COMMENT 'YARN内存(MB)',
    yarn_cpu INT DEFAULT 4 COMMENT 'YARN CPU核数',
    ha_master_host VARCHAR(255) COMMENT 'HA备用Master地址',
    zk_cluster VARCHAR(500) COMMENT 'ZooKeeper集群地址',
    status VARCHAR(20) DEFAULT 'created' COMMENT '状态 (created/deploying/deployed/running/stopped/failed)',
    deploy_log LONGTEXT COMMENT '部署日志',
    master_id BIGINT COMMENT '关联的Master节点ID',
    created_by VARCHAR(100) COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_node_type (node_type),
    INDEX idx_status (status),
    INDEX idx_master_id (master_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hadoop集群节点表';

-- Hadoop 版本同步日志表
CREATE TABLE IF NOT EXISTS xnet_mlops_smp_hadoop_version_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(20) COMMENT '同步类型',
    sync_status VARCHAR(20) COMMENT '同步状态 (success/failed)',
    versions_count INT COMMENT '同步版本数量',
    error_message TEXT COMMENT '错误信息',
    sync_duration_ms BIGINT COMMENT '耗时(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hadoop版本同步日志表';


