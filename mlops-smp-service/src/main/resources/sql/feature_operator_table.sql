-- 特征算子Git仓库配置表
CREATE TABLE IF NOT EXISTS XnetMLops.xnet_mlops_smp_feature_operators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE COMMENT '唯一标识',
    url VARCHAR(255) NOT NULL COMMENT 'Git仓库URL',
    encrypted_token BLOB NOT NULL COMMENT 'AES-256加密的访问令牌',
    operator_name VARCHAR(100) NOT NULL COMMENT '算子名称',
    operator_code VARCHAR(50) NOT NULL COMMENT '算子代码（唯一标识符）',
    operator_version VARCHAR(50) NOT NULL COMMENT '算子版本',
    description TEXT COMMENT '描述信息',
    tenant_uid VARCHAR(50) NOT NULL COMMENT '租户UID',
    dept_uid VARCHAR(50) NOT NULL COMMENT '部门UID',
    team_uid VARCHAR(50) NOT NULL COMMENT '团队UID',
    authorized_tenants VARCHAR(500) NOT NULL COMMENT '授权访问的团队UID列表（逗号分隔）',
    created_by VARCHAR(255) NOT NULL COMMENT '创建人',
    updated_by VARCHAR(255) COMMENT '更新人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_url_code_version (url, operator_code, operator_version) COMMENT 'URL、算子代码和版本组合唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特征算子Git仓库配置表';

-- 添加索引
CREATE INDEX idx_feature_operator_team_uid ON XnetMLops.xnet_mlops_smp_feature_operators(team_uid);
CREATE INDEX idx_feature_operator_code ON XnetMLops.xnet_mlops_smp_feature_operators(operator_code);
CREATE INDEX idx_feature_operator_name ON XnetMLops.xnet_mlops_smp_feature_operators(operator_name);
