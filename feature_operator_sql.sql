-- =====================================================
-- DPP 特征工程模块 - 数据库更新脚本
-- 版本: 2.0
-- 更新内容: 添加镜像、调度、通知、Jenkins集成相关字段
-- =====================================================

-- 1. 更新特征工程表，添加新字段（使用存储过程来确保幂等性）
DELIMITER $$
CREATE PROCEDURE AddColumnIfNotExists(
    IN tableName VARCHAR(64),
    IN columnName VARCHAR(64),
    IN columnDefinition VARCHAR(512)
)
BEGIN
    DECLARE columnCount INT;

    -- 检查列是否存在
    SELECT COUNT(*)
    INTO columnCount
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = tableName
    AND COLUMN_NAME = columnName;

    -- 如果列不存在，则添加
    IF columnCount = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 添加各个列
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'image_uid', 'VARCHAR(64) DEFAULT NULL COMMENT ''镜像UID''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'image_name', 'VARCHAR(128) DEFAULT NULL COMMENT ''镜像名称''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'image_tag', 'VARCHAR(64) DEFAULT NULL COMMENT ''镜像标签''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'harbor_url', 'VARCHAR(256) DEFAULT NULL COMMENT ''Harbor仓库地址''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'harbor_credentials_id', 'VARCHAR(64) DEFAULT NULL COMMENT ''Harbor凭证ID''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'schedule_config', 'TEXT DEFAULT NULL COMMENT ''调度配置(JSON)''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'schedule_active', 'TINYINT(1) DEFAULT 0 COMMENT ''是否启用调度''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'notification_config', 'TEXT DEFAULT NULL COMMENT ''通知配置(JSON)''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'notification_enabled', 'TINYINT(1) DEFAULT 0 COMMENT ''是否启用通知''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'job_uid', 'VARCHAR(256) DEFAULT NULL COMMENT ''Jenkins任务UID''');
CALL AddColumnIfNotExists('xnet_mlops_dpp_feature_engineering', 'last_build_status', 'VARCHAR(32) DEFAULT NULL COMMENT ''最后构建状态''');

-- 删除存储过程
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;

-- 2. 创建特征工程任务执行记录表
CREATE TABLE IF NOT EXISTS xnet_mlops_dpp_feature_task_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL COMMENT '记录UID',
    task_uid VARCHAR(64) NOT NULL COMMENT '特征工程任务UID',
    job_uid VARCHAR(256) NOT NULL COMMENT 'Jenkins Job UID',
    job_status VARCHAR(32) DEFAULT NULL COMMENT '任务状态',
    job_content TEXT DEFAULT NULL COMMENT '任务详情(JSON)',
    start_at DATETIME DEFAULT NULL COMMENT '开始时间',
    end_at DATETIME DEFAULT NULL COMMENT '结束时间',
    schedule_active TINYINT(1) DEFAULT 0 COMMENT '0:立即执行,1:调度执行',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_uid (task_uid),
    INDEX idx_job_uid (job_uid),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DPP特征工程任务执行记录表';

-- 3. 特征算子表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS xnet_mlops_dpp_feature_operator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL COMMENT '算子UID',
    name VARCHAR(100) NOT NULL COMMENT '算子名称',
    code VARCHAR(50) NOT NULL COMMENT '算子代码',
    description TEXT COMMENT '算子描述',
    category VARCHAR(50) NOT NULL COMMENT '算子分类: format_conversion, feature_transform, data_cleaning',
    output_formats TEXT COMMENT '支持的输出格式(JSON数组)',
    feature_columns TEXT COMMENT '特征字段配置(JSON数组)',
    parameter_schema TEXT COMMENT '算子参数模式(JSON)',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_code (code),
    UNIQUE KEY uk_uid (uid),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DPP特征算子表';

-- 4. 初始化默认特征算子数据（如果表为空）
INSERT INTO xnet_mlops_dpp_feature_operator (uid, name, code, description, category, output_formats, feature_columns, parameter_schema, sort_order, enabled)
SELECT * FROM (
    SELECT
        'FOP-CSV2PARQ' as uid,
        'CSV转Parquet' as name,
        'csv_to_parquet' as code,
        '将CSV格式数据转换为Parquet格式，支持列压缩和类型推断' as description,
        'format_conversion' as category,
        '["parquet"]' as output_formats,
        '[{"key":"feature_type","title":"特征类型","type":"select","options":[{"value":"numeric","label":"数值型"},{"value":"categorical","label":"类别型"},{"value":"text","label":"文本型"},{"value":"datetime","label":"时间型"}],"default":"numeric"},{"key":"nullable","title":"允许空值","type":"switch","default":true}]' as feature_columns,
        '{"compression":{"type":"select","label":"压缩方式","options":[{"value":"snappy","label":"Snappy"},{"value":"gzip","label":"GZIP"},{"value":"none","label":"无压缩"}],"default":"snappy"}}' as parameter_schema,
        1 as sort_order,
        1 as enabled
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM xnet_mlops_dpp_feature_operator WHERE code = 'csv_to_parquet');

-- ... 其他INSERT语句保持不变 ...

-- 5. 添加索引优化查询性能（使用存储过程来确保幂等性）
DELIMITER $$
CREATE PROCEDURE AddIndexIfNotExists(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN indexDefinition VARCHAR(512)
)
BEGIN
    DECLARE indexCount INT;

    -- 检查索引是否存在
    SELECT COUNT(*)
    INTO indexCount
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = tableName
    AND INDEX_NAME = indexName;

    -- 如果索引不存在，则添加
    IF indexCount = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' ADD INDEX ', indexName, ' ', indexDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 添加各个索引
CALL AddIndexIfNotExists('xnet_mlops_dpp_feature_engineering', 'idx_job_uid', '(job_uid)');
CALL AddIndexIfNotExists('xnet_mlops_dpp_feature_engineering', 'idx_schedule_active', '(schedule_active)');
CALL AddIndexIfNotExists('xnet_mlops_dpp_feature_engineering', 'idx_last_build_status', '(last_build_status)');

-- 删除存储过程
DROP PROCEDURE IF EXISTS AddIndexIfNotExists;

-- 完成提示
SELECT 'DPP特征工程模块数据库更新完成!' AS message;
