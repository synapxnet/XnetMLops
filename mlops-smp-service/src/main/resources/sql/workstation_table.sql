-- =====================================================
-- 工作站点表 (SMP Workstation Table)
-- 用于存储服务器注册信息
-- =====================================================

CREATE TABLE IF NOT EXISTS xnet_mlops_smp_workstation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    uid VARCHAR(64) NOT NULL UNIQUE COMMENT '唯一标识符(UUID)',
    name VARCHAR(128) NOT NULL UNIQUE COMMENT '服务器名称(唯一)',
    
    -- 服务器基本信息。
    vendor VARCHAR(32) NOT NULL COMMENT '服务器厂商: huawei/tencent/alibaba/aws/azure/other',
    server_type VARCHAR(32) NOT NULL COMMENT '服务器类型: vps/cce/cvm/ecs/ec2/other',
    region VARCHAR(64) NOT NULL COMMENT '地域: beijing/shanghai/guangzhou/shenzhen 等',
    
    -- 操作系统信息
    os_type VARCHAR(16) NOT NULL COMMENT '系统类型: linux/macos/windows',
    os_version VARCHAR(64) NOT NULL COMMENT '系统版本: OpenCloudOS 9/CentOS 7/8/Ubuntu 20.04 等',
    
    -- 服务器资源 (可手动填写，测试连接时自动更新)
    cpu_cores INT DEFAULT 1 COMMENT 'CPU核数',
    ram_gb INT DEFAULT 1 COMMENT '内存(GB)',
    disk_gb INT DEFAULT 10 COMMENT '磁盘空间(GB)',
    available_disk_gb INT DEFAULT 0 COMMENT '可用磁盘空间(GB) - 自动检测',
    available_ram_gb INT DEFAULT 0 COMMENT '可用内存(GB) - 自动检测',
    
    -- 网络信息
    domain VARCHAR(256) COMMENT '域名(可选)',
    ip_address VARCHAR(64) NOT NULL COMMENT 'IP地址',
    
    -- SSH连接配置
    ssh_port INT DEFAULT 22 COMMENT 'SSH端口',
    ssh_user VARCHAR(64) NOT NULL COMMENT 'SSH用户名',
    auth_type VARCHAR(16) NOT NULL DEFAULT 'password' COMMENT '认证方式: password/privateKey',
    encrypted_password TEXT COMMENT 'SSH密码(AES加密存储)',
    encrypted_private_key TEXT COMMENT 'SSH私钥(AES加密存储)',
    
    -- 状态信息
    status VARCHAR(16) DEFAULT 'pending' COMMENT '状态: pending/online/offline/error',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    last_check_result TEXT COMMENT '最后检测结果(JSON)',
    
    -- 审计字段
    description TEXT COMMENT '描述',
    created_by VARCHAR(64) COMMENT '创建者',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_vendor (vendor),
    INDEX idx_region (region),
    INDEX idx_status (status),
    INDEX idx_os_type (os_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作站点表';

-- 插入初始数据示例（可选）
-- INSERT INTO smp_workstation (uid, name, vendor, server_type, region, os_type, os_version, ip_address, ssh_user, auth_type)
-- VALUES (UUID(), '测试服务器', 'tencent', 'cvm', 'guangzhou', 'linux', 'OpenCloudOS 9', '127.0.0.1', 'root', 'password');
