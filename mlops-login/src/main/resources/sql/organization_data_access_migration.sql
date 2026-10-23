-- Copyright (C) 2026 Synapxnet. All rights reserved.
-- This file is Synapxnet Proprietary and Confidential. It is strictly
-- forbidden to copy, distribute, or use without explicit authorization.
-- 组织数据访问授权字段迁移 / Organization data-access permission migration.
-- Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
-- Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com

-- 在目标数据库中执行；现有成员不会自动获得业务数据权限。
-- Execute in the target database; existing members receive no implicit data access.
SET @mlops_permission_ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'xnet_mlops_usr_organization_membership'
       AND column_name = 'data_access_enabled') = 0,
    'ALTER TABLE xnet_mlops_usr_organization_membership ADD COLUMN data_access_enabled TINYINT NOT NULL DEFAULT 0 COMMENT ''Explicit business data authorization''',
    'SELECT ''organization data-access column already exists'' AS migration_status'
);
PREPARE mlops_permission_statement FROM @mlops_permission_ddl;
EXECUTE mlops_permission_statement;
DEALLOCATE PREPARE mlops_permission_statement;
