-- =====================================================
-- 工作站点表升级脚本 V2
-- 添加 hostname 和 GPU 相关字段
-- =====================================================

-- 添加 hostname 相关字段
ALTER TABLE `xnet_mlops_smp_workstation`
ADD COLUMN `hostname` VARCHAR(128) COMMENT '服务器主机名(用于Hadoop集群等)' AFTER `name`,
ADD COLUMN `hostname_mode` VARCHAR(16) DEFAULT 'auto' COMMENT '主机名模式: auto(自动获取)/custom(自定义)' AFTER `hostname`;

-- 添加 GPU 相关字段
ALTER TABLE `xnet_mlops_smp_workstation`
ADD COLUMN `has_gpu` TINYINT(1) DEFAULT 0 COMMENT '是否有GPU' AFTER `disk_gb`,
ADD COLUMN `gpu_count` INT DEFAULT 0 COMMENT 'GPU数量' AFTER `has_gpu`,
ADD COLUMN `gpu_type` VARCHAR(64) COMMENT 'GPU类型: single_gpu/multi_gpu' AFTER `gpu_count`,
ADD COLUMN `gpu_model` VARCHAR(128) COMMENT 'GPU型号: NVIDIA A100/V100/RTX 3090等' AFTER `gpu_type`,
ADD COLUMN `gpu_memory` INT COMMENT 'GPU显存(GB)' AFTER `gpu_model`;

-- 添加索引
ALTER TABLE `xnet_mlops_smp_workstation`
ADD INDEX `idx_hostname` (`hostname`),
ADD INDEX `idx_has_gpu` (`has_gpu`);

-- 更新现有数据，将 name 复制到 hostname（如果为空）
UPDATE `xnet_mlops_smp_workstation` SET `hostname` = `name` WHERE `hostname` IS NULL OR `hostname` = '';
