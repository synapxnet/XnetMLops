-- XAA Skill 表结构更新脚本
-- 用于支持元技能仓库功能

SET NAMES utf8mb4;

-- 1. 添加新字段到 xnet_mlops_xaa_skill 表
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `category` VARCHAR(50) DEFAULT 'custom' COMMENT '技能分类';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `tags` VARCHAR(500) COMMENT '标签，逗号分隔';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `content_md` LONGTEXT COMMENT 'SKILL.md 内容';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `has_scripts` TINYINT(1) DEFAULT 0 COMMENT '是否包含脚本';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `has_references` TINYINT(1) DEFAULT 0 COMMENT '是否包含参考文档';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `has_assets` TINYINT(1) DEFAULT 0 COMMENT '是否包含资源文件';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `install_count` INT DEFAULT 0 COMMENT '安装次数';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `source_url` VARCHAR(500) COMMENT '来源URL';
ALTER TABLE `xnet_mlops_xaa_skill` ADD COLUMN `is_official` TINYINT(1) DEFAULT 0 COMMENT '是否官方技能';

-- 添加索引
ALTER TABLE `xnet_mlops_xaa_skill` ADD INDEX `idx_category` (`category`);
ALTER TABLE `xnet_mlops_xaa_skill` ADD INDEX `idx_is_official` (`is_official`);

-- 2. 创建技能分类表
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_skill_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` VARCHAR(50) NOT NULL COMMENT '分类标识',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称（中文）',
  `name_en` VARCHAR(100) COMMENT '分类名称（英文）',
  `description` VARCHAR(500) COMMENT '分类描述',
  `icon` VARCHAR(100) COMMENT '图标名称',
  `color` VARCHAR(20) COMMENT '主题颜色',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能分类表';

-- 3. 初始化分类数据
INSERT INTO `xnet_mlops_xaa_skill_category` (`key`, `name`, `name_en`, `description`, `icon`, `color`, `sort_order`) VALUES
('document', '文档处理', 'Document Processing', '处理各类文档格式，包括PDF、Word、Excel、PPT等', 'file-text', '#1890ff', 1),
('creative', '创意设计', 'Creative Design', '图形设计、艺术创作、UI设计等创意类技能', 'highlight', '#722ed1', 2),
('development', '开发工具', 'Development', '编程、代码生成、开发辅助等技术技能', 'code', '#52c41a', 3),
('data', '数据处理', 'Data Processing', '数据分析、数据转换、数据可视化等', 'database', '#fa8c16', 4),
('enterprise', '企业应用', 'Enterprise', '企业级应用、办公自动化、流程管理等', 'apartment', '#eb2f96', 5),
('ai-ml', 'AI/机器学习', 'AI & Machine Learning', '模型训练、推理、AI辅助等', 'robot', '#13c2c2', 6),
('utilities', '实用工具', 'Utilities', '通用工具类技能', 'tool', '#faad14', 7),
('custom', '自定义', 'Custom', '用户自定义技能', 'star', '#8c8c8c', 99);

-- 4. 创建已安装技能关联表（记录租户安装的技能）
CREATE TABLE IF NOT EXISTS `xnet_mlops_xaa_skill_installation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `skill_id` BIGINT NOT NULL COMMENT '技能ID',
  `tenant_uid` VARCHAR(64) NOT NULL COMMENT '租户UID',
  `installed_by` VARCHAR(64) COMMENT '安装者ID',
  `installed_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `config_override` TEXT COMMENT '配置覆盖',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/disabled',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_tenant` (`skill_id`, `tenant_uid`),
  KEY `idx_tenant_uid` (`tenant_uid`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能安装记录表';

-- 5. 插入示例技能数据（基于 Anthropic Skills）
INSERT INTO `xnet_mlops_xaa_skill` (`uid`, `name`, `description`, `type`, `status`, `category`, `tags`, `content_md`, `icon`, `is_official`, `version`) VALUES
('SKILL-PDF-001', 'PDF处理', '读取、创建、合并、拆分、旋转PDF文档；填写表单；OCR扫描文档', 'tool', 'published', 'document', 'pdf,文档,ocr,表单', '# PDF处理技能\n\n## 功能\n- 读取PDF内容\n- 创建新PDF\n- 合并多个PDF\n- 拆分PDF页面\n- 旋转页面\n- 填写表单\n- OCR识别', 'file-pdf', 1, 1),

('SKILL-DOCX-001', 'Word文档处理', '创建、编辑、分析Word文档，支持修订跟踪、批注、格式设置', 'tool', 'published', 'document', 'word,docx,文档,编辑', '# Word文档处理技能\n\n## 功能\n- 创建Word文档\n- 编辑现有文档\n- 添加修订跟踪\n- 插入批注\n- 格式设置', 'file-word', 1, 1),

('SKILL-XLSX-001', 'Excel表格处理', '创建、编辑Excel文件，支持公式、格式、图表等', 'tool', 'published', 'document', 'excel,xlsx,表格,数据', '# Excel表格处理技能\n\n## 功能\n- 创建Excel工作簿\n- 编辑单元格\n- 插入公式\n- 设置格式\n- 创建图表', 'file-excel', 1, 1),

('SKILL-PPTX-001', 'PPT演示文稿', '创建演示文稿，支持从模板创建，包含设计指导', 'tool', 'published', 'document', 'ppt,pptx,演示,幻灯片', '# PPT演示文稿技能\n\n## 功能\n- 创建演示文稿\n- 使用模板\n- 添加幻灯片\n- 插入图表和图片\n- 设计布局指导', 'file-ppt', 1, 1),

('SKILL-FRONTEND-001', '前端设计', '构建独特的、生产级别的Web界面组件', 'tool', 'published', 'creative', 'frontend,web,ui,设计', '# 前端设计技能\n\n## 功能\n- 创建Web组件\n- 响应式设计\n- UI/UX最佳实践\n- 组件库集成', 'layout', 1, 1),

('SKILL-MCP-001', 'MCP服务构建', '创建MCP(Model Context Protocol)服务器', 'tool', 'published', 'development', 'mcp,服务器,协议,开发', '# MCP服务构建技能\n\n## 功能\n- 创建TypeScript MCP服务器\n- 创建Python MCP服务器\n- 工具和资源定义\n- 调试和测试', 'api', 1, 1),

('SKILL-WEBAPP-TEST-001', 'Web应用测试', '使用Playwright进行Web应用自动化测试', 'tool', 'published', 'development', 'playwright,测试,自动化,e2e', '# Web应用测试技能\n\n## 功能\n- Playwright测试脚本\n- 端到端测试\n- 截图和录像\n- 测试报告生成', 'bug', 1, 1),

('SKILL-DATA-ANALYSIS-001', '数据分析', '数据探索、统计分析、可视化', 'tool', 'published', 'data', '数据,分析,可视化,统计', '# 数据分析技能\n\n## 功能\n- 数据加载和清洗\n- 统计分析\n- 数据可视化\n- 报告生成', 'bar-chart', 1, 1);
