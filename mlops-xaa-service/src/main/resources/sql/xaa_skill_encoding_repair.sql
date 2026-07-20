-- Repair official XAA seed metadata imported through a latin1 client session.
-- The byte-pattern guard makes this migration safe to run more than once.

SET NAMES utf8mb4;

START TRANSACTION;

UPDATE `xnet_mlops_xaa_skill_category`
SET
  `name` = CASE
    WHEN HEX(`name`) REGEXP '^([0-9A-F]{2})*(C2|C3|E280)'
      THEN CONVERT(CAST(CONVERT(`name` USING latin1) AS BINARY) USING utf8mb4)
    ELSE `name`
  END,
  `description` = CASE
    WHEN HEX(`description`) REGEXP '^([0-9A-F]{2})*(C2|C3|E280)'
      THEN CONVERT(CAST(CONVERT(`description` USING latin1) AS BINARY) USING utf8mb4)
    ELSE `description`
  END
WHERE `key` IN (
  'document',
  'creative',
  'development',
  'data',
  'enterprise',
  'ai-ml',
  'utilities',
  'custom'
);

UPDATE `xnet_mlops_xaa_skill`
SET
  `name` = CASE
    WHEN HEX(`name`) REGEXP '^([0-9A-F]{2})*(C2|C3|E280)'
      THEN CONVERT(CAST(CONVERT(`name` USING latin1) AS BINARY) USING utf8mb4)
    ELSE `name`
  END,
  `description` = CASE
    WHEN HEX(`description`) REGEXP '^([0-9A-F]{2})*(C2|C3|E280)'
      THEN CONVERT(CAST(CONVERT(`description` USING latin1) AS BINARY) USING utf8mb4)
    ELSE `description`
  END,
  `tags` = CASE
    WHEN HEX(`tags`) REGEXP '^([0-9A-F]{2})*(C2|C3|E280)'
      THEN CONVERT(CAST(CONVERT(`tags` USING latin1) AS BINARY) USING utf8mb4)
    ELSE `tags`
  END,
  `content_md` = CASE
    WHEN HEX(`content_md`) REGEXP '^([0-9A-F]{2})*(C2|C3|E280)'
      THEN CONVERT(CAST(CONVERT(`content_md` USING latin1) AS BINARY) USING utf8mb4)
    ELSE `content_md`
  END
WHERE `uid` IN (
  'SKILL-PDF-001',
  'SKILL-DOCX-001',
  'SKILL-XLSX-001',
  'SKILL-PPTX-001',
  'SKILL-FRONTEND-001',
  'SKILL-MCP-001',
  'SKILL-WEBAPP-TEST-001',
  'SKILL-DATA-ANALYSIS-001'
);

COMMIT;
