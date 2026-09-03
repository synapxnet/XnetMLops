-- 为 DataOps 版本化数据产品导入增加真实来源、规模、摘要与血缘字段。
DROP PROCEDURE IF EXISTS migration_add_dataset_column;

DELIMITER //
CREATE PROCEDURE migration_add_dataset_column(
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512)
)
BEGIN
    -- 仅在字段不存在时执行 DDL，使迁移可在 MySQL 8.4 上安全重放。
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'xnet_mlops_dpp_dataset'
          AND column_name = target_column
    ) THEN
        SET @column_sql = CONCAT(
            'ALTER TABLE xnet_mlops_dpp_dataset ADD COLUMN `',
            REPLACE(target_column, '`', '``'),
            '` ',
            column_definition
        );
        PREPARE column_statement FROM @column_sql;
        EXECUTE column_statement;
        DEALLOCATE PREPARE column_statement;
    END IF;
END//
DELIMITER ;

CALL migration_add_dataset_column('source_platform', 'VARCHAR(64) NULL COMMENT ''来源平台''');
CALL migration_add_dataset_column('source_product_name', 'VARCHAR(128) NULL COMMENT ''来源数据产品名称''');
CALL migration_add_dataset_column('source_product_version', 'VARCHAR(128) NULL COMMENT ''来源数据产品版本''');
CALL migration_add_dataset_column('source_uri', 'VARCHAR(512) NULL COMMENT ''不含凭据的来源定位符''');
CALL migration_add_dataset_column('row_count', 'BIGINT NULL COMMENT ''真实记录数''');
CALL migration_add_dataset_column('byte_size', 'BIGINT NULL COMMENT ''导入制品字节数''');
CALL migration_add_dataset_column('schema_digest_sha256', 'CHAR(64) NULL COMMENT ''字段契约摘要''');
CALL migration_add_dataset_column('artifact_digest_sha256', 'CHAR(64) NULL COMMENT ''来源制品摘要''');
CALL migration_add_dataset_column('lineage_reference', 'VARCHAR(512) NULL COMMENT ''DataOps 血缘引用''');
CALL migration_add_dataset_column('import_status', 'VARCHAR(32) NULL COMMENT ''导入状态''');
CALL migration_add_dataset_column('imported_at', 'DATETIME NULL COMMENT ''导入时间''');

DROP PROCEDURE migration_add_dataset_column;

SET @source_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'xnet_mlops_dpp_dataset'
      AND index_name = 'uk_dataset_source_version'
);
SET @source_index_sql = IF(
    @source_index_exists = 0,
    'CREATE UNIQUE INDEX uk_dataset_source_version ON xnet_mlops_dpp_dataset(source_platform, source_product_version)',
    'SELECT 1'
);
PREPARE source_index_statement FROM @source_index_sql;
EXECUTE source_index_statement;
DEALLOCATE PREPARE source_index_statement;
