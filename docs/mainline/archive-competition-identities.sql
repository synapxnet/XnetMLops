-- Copyright (C) 2026 Synapxnet. All rights reserved.
-- This file is Synapxnet Proprietary and Confidential. It is strictly
-- forbidden to copy, distribute, or use without explicit authorization.
-- 比赛身份定向归档 / Scoped archival of competition identities.
-- Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
-- Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com

USE XnetMLops;
-- 默认只预览；完成数据库备份并确认对象后手动设为 1。
-- Preview by default; set to 1 only after backing up and reviewing the exact identities.
SET @apply_cleanup = 0;
SET @competition_user_id = (
  SELECT Id FROM xnet_mlops_user_infos
  WHERE uid = 'USR-GOAI-OPERATOR' AND userId = 'USR-GOAI-OPERATOR'
    AND username = 'goai_operator' AND realName = 'GOAI 比赛操作员'
  LIMIT 1
);
SELECT Id, uid, username, realName, roles, permission FROM xnet_mlops_user_infos
WHERE Id = @competition_user_id;
SELECT * FROM xnet_mlops_usr_organization_membership
WHERE user_id = @competition_user_id AND team_uid = 'TEAM-GOAI-INFRA';
SELECT uid, team_name, status FROM xnet_mlops_sys_team
WHERE uid = 'TEAM-GOAI-INFRA' AND team_id = 'goai_infra'
  AND team_name = 'GOAI Infrastructure 联合团队';

-- 归档表保留原记录结构；权限变更不会删除审计主体。
-- Archive original rows without removing identities referenced by audit records.
SET @archive_sql = IF(@apply_cleanup = 1, 'CREATE TABLE IF NOT EXISTS xnet_mainline_archive_goai_users LIKE xnet_mlops_user_infos', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'CREATE TABLE IF NOT EXISTS xnet_mainline_archive_goai_memberships LIKE xnet_mlops_usr_organization_membership', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'CREATE TABLE IF NOT EXISTS xnet_mainline_archive_goai_teams LIKE xnet_mlops_sys_team', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
START TRANSACTION;
SET @archive_sql = IF(@apply_cleanup = 1, 'INSERT IGNORE INTO xnet_mainline_archive_goai_users
SELECT * FROM xnet_mlops_user_infos WHERE Id = @competition_user_id AND @apply_cleanup = 1', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'INSERT IGNORE INTO xnet_mainline_archive_goai_memberships
SELECT * FROM xnet_mlops_usr_organization_membership
WHERE user_id = @competition_user_id AND team_uid = ''TEAM-GOAI-INFRA'' AND @apply_cleanup = 1', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'INSERT IGNORE INTO xnet_mainline_archive_goai_teams
SELECT * FROM xnet_mlops_sys_team
WHERE uid = ''TEAM-GOAI-INFRA'' AND team_id = ''goai_infra''
  AND team_name = ''GOAI Infrastructure 联合团队'' AND @apply_cleanup = 1', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'UPDATE xnet_mlops_user_infos
SET roles = JSON_ARRAY(), permission = JSON_ARRAY(), roles_failure_time = NOW(), update_time = NOW()
WHERE Id = @competition_user_id AND @apply_cleanup = 1', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'UPDATE xnet_mlops_usr_organization_membership SET status = 0
WHERE user_id = @competition_user_id AND team_uid = ''TEAM-GOAI-INFRA'' AND @apply_cleanup = 1', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
SET @archive_sql = IF(@apply_cleanup = 1, 'UPDATE xnet_mlops_sys_team t SET status = 0
WHERE t.uid = ''TEAM-GOAI-INFRA'' AND t.team_id = ''goai_infra''
  AND t.team_name = ''GOAI Infrastructure 联合团队'' AND @apply_cleanup = 1
  AND NOT EXISTS (SELECT 1 FROM xnet_mlops_usr_organization_membership m
    WHERE m.team_uid = t.uid AND m.status = 1)', 'DO 0');
PREPARE archive_statement FROM @archive_sql;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;
COMMIT;

-- 模型、任务、证据和审计表不在身份清理范围；按事件来源另行精确归档。
-- Model/task/evidence/audit tables remain intact; archive their records by verified provenance separately.
