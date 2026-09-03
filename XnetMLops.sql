CREATE DATABASE IF NOT EXISTS XnetMLops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE XnetMLops;
SET NAMES utf8mb4;

CREATE TABLE XnetMLops.xnet_mlops_user_infos (
                                                 Id                  INT AUTO_INCREMENT PRIMARY KEY NOT NULL COMMENT '自增ID',
                                                 uid                 VARCHAR(255) NOT NULL COMMENT '唯一编码ID',
                                                 phone               VARCHAR(255) NOT NULL COMMENT '电话号码',
                                                 realName            VARCHAR(255) NOT NULL COMMENT '真实姓名',
                                                 username            VARCHAR(255) NOT NULL COMMENT '用户姓名',
                                                 userId            VARCHAR(255) NOT NULL COMMENT '用户ID',
                                                 roles               JSON NOT NULL COMMENT '角色列表（JSON数组）',
                                                 permission          JSON NOT NULL COMMENT '权限列表（JSON数组）',
                                                 roles_failure_time  DATETIME NOT NULL COMMENT '权限失效时间',
                                                 create_time         DATETIME NOT NULL COMMENT '创建时间',
                                                 update_time         DATETIME NOT NULL COMMENT '更新时间'
);

INSERT INTO XnetMLops.xnet_mlops_user_infos
(uid, phone, realName, username,userId, roles, permission, roles_failure_time, create_time, update_time)
VALUES (
           'USR-GOAI-OPERATOR',
           '17870171303',
           'GOAI 比赛操作员',
           'goai_operator',
           'USR-GOAI-OPERATOR',
           '["user"]',
           '["AC_100100"]',
           NOW() + INTERVAL 360 DAY,
           NOW(),
           NOW()
       );
INSERT INTO XnetMLops.xnet_mlops_user_infos
(uid, phone, realName, username, userId,roles, permission, roles_failure_time, create_time, update_time)
VALUES (
           '08626596588',
           '13800138001',
           '李四',
           '玖沐',
           'c00000001',
           '["super"]',
           '["AC_100100","AC_100110","AC_100120","AC_100010"]',
           NOW() - INTERVAL 185 DAY,
           NOW() - INTERVAL 180 DAY,
           NOW() - INTERVAL 180 DAY
       );

-- smp配置表
CREATE TABLE XnetMLops.xnet_mlops_smp_dataset_config_info (
                                                         id INT AUTO_INCREMENT PRIMARY KEY,
                                                         config_type ENUM('DATASET_TYPE', 'DATASET_ZONE') NOT NULL COMMENT '配置类型',
                                                         label VARCHAR(100) NOT NULL COMMENT '配置项名称',
                                                         value VARCHAR(50) NOT NULL COMMENT '配置项值',
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                                         UNIQUE KEY unique_config (config_type, value)
) COMMENT='数据集配置表';

INSERT INTO XnetMLops.xnet_mlops_smp_dataset_config_info(config_type,label,value)
VALUES
    ('DATASET_TYPE','文本','1'),
    ('DATASET_TYPE','图像','2'),
    ('DATASET_ZONE','江西','1'),
    ('DATASET_ZONE','南京','2');
COMMIT ;

-- 租户表
CREATE TABLE XnetMLops.xnet_mlops_sys_tenant (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '租户ID',
                                                 uid VARCHAR(50) NOT NULL UNIQUE COMMENT '租户唯一编码',
                                                 tenant_id VARCHAR(50) NOT NULL COMMENT '租户英文标识',
                                                 tenant_name VARCHAR(100) NOT NULL COMMENT '租户中文名称',
                                                 status TINYINT DEFAULT 1 COMMENT '状态(1:启用 0:禁用)',
                                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='租户表';

-- 部门表
CREATE TABLE XnetMLops.xnet_mlops_sys_department (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
                                                     uid VARCHAR(50) NOT NULL UNIQUE COMMENT '部门唯一编码',
                                                     tenant_uid VARCHAR(50) NOT NULL COMMENT '租户唯一编码',
                                                     dept_id VARCHAR(50) NOT NULL COMMENT '部门英文标识',
                                                     dept_name VARCHAR(100) NOT NULL COMMENT '部门中文名称',
                                                     status TINYINT DEFAULT 1 COMMENT '状态(1:启用 0:禁用)',
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                     FOREIGN KEY (tenant_uid) REFERENCES XnetMLops.xnet_mlops_sys_tenant(uid) ON DELETE CASCADE
) COMMENT='部门表';

-- 团队表
CREATE TABLE XnetMLops.xnet_mlops_sys_team (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '团队ID',
                                               uid VARCHAR(50) NOT NULL UNIQUE COMMENT '团队唯一编码',
                                               dept_uid VARCHAR(50) NOT NULL COMMENT '部门唯一编码',
                                               team_id VARCHAR(50) NOT NULL COMMENT '团队英文标识',
                                               team_name VARCHAR(100) NOT NULL COMMENT '团队中文名称',
                                               status TINYINT DEFAULT 1 COMMENT '状态(1:启用 0:禁用)',
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                               FOREIGN KEY (dept_uid) REFERENCES XnetMLops.xnet_mlops_sys_department(uid) ON DELETE CASCADE
) COMMENT='团队表';

CREATE TABLE XnetMLops.xnet_mlops_usr_organization_membership (
                                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                               user_id INT NOT NULL,
                                                               tenant_uid VARCHAR(50) NOT NULL,
                                                               dept_uid VARCHAR(50),
                                                               team_uid VARCHAR(50),
                                                               status TINYINT NOT NULL DEFAULT 1,
                                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                               UNIQUE KEY uk_mlops_user_organization (user_id, tenant_uid, dept_uid, team_uid),
                                                               INDEX idx_mlops_membership_user (user_id)
) COMMENT='用户组织成员关系表';

-- 插入租户数据
INSERT INTO XnetMLops.xnet_mlops_sys_tenant (uid, tenant_id, tenant_name, status) VALUES
                                                                                      ('TEN-SYNAPXNET', 'synapxnet', 'SynapXnet', 1),
                                                                                      ('TEN-ALIBABA', 'alibaba', '阿里巴巴集团', 1),
                                                                                      ('TEN-TENCENT', 'tencent', '腾讯控股有限公司', 1),
                                                                                      ('TEN-BAIDU', 'baidu', '百度网络技术有限公司', 1),
                                                                                      ('TEN-JD', 'jd', '京东集团', 1),
                                                                                      ('TEN-HUAWEI', 'huawei', '华为技术有限公司', 1);

-- 插入阿里巴巴的部门数据
INSERT INTO XnetMLops.xnet_mlops_sys_department (uid, tenant_uid, dept_id, dept_name, status) VALUES
                                                                                                  ('DEPT-SYNAPXNET-PLATFORM', 'TEN-SYNAPXNET', 'intelligent_platform', '智能平台部', 1),
                                                                                                  ('DEPT-ALI-TECH', 'TEN-ALIBABA', 'tech_center', '技术研发中心', 1),
                                                                                                  ('DEPT-ALI-MARKET', 'TEN-ALIBABA', 'marketing', '市场运营部', 1),
                                                                                                  ('DEPT-ALI-HR', 'TEN-ALIBABA', 'hr', '人力资源部', 1),
                                                                                                  ('DEPT-ALI-FINANCE', 'TEN-ALIBABA', 'finance', '财务部', 1);

-- 插入腾讯的部门数据
INSERT INTO XnetMLops.xnet_mlops_sys_department (uid, tenant_uid, dept_id, dept_name, status) VALUES
                                                                                                  ('DEPT-TENCENT-SOCIAL', 'TEN-TENCENT', 'social_network', '社交网络事业群', 1),
                                                                                                  ('DEPT-TENCENT-ENTERTAIN', 'TEN-TENCENT', 'entertainment', '互动娱乐事业群', 1),
                                                                                                  ('DEPT-TENCENT-ENGINEER', 'TEN-TENCENT', 'engineering', '技术工程事业群', 1);

-- 插入阿里巴巴技术研发中心的团队数据
INSERT INTO XnetMLops.xnet_mlops_sys_team (uid, dept_uid, team_id, team_name, status) VALUES
                                                                                          ('TEAM-GOAI-INFRA', 'DEPT-SYNAPXNET-PLATFORM', 'goai_infra', 'GOAI Infrastructure 联合团队', 1),
                                                                                          ('TEAM-ALI-JAVA', 'DEPT-ALI-TECH', 'java_dev', 'Java开发组', 1),
                                                                                          ('TEAM-ALI-FRONTEND', 'DEPT-ALI-TECH', 'frontend', '前端架构组', 1),
                                                                                          ('TEAM-ALI-DATA', 'DEPT-ALI-TECH', 'data_intel', '数据智能组', 1),
                                                                                          ('TEAM-ALI-CLOUD', 'DEPT-ALI-TECH', 'cloud_platform', '云计算平台组', 1);

-- 插入阿里巴巴市场运营部的团队数据
INSERT INTO XnetMLops.xnet_mlops_sys_team (uid, dept_uid, team_id, team_name, status) VALUES
                                                                                          ('TEAM-ALI-BRAND', 'DEPT-ALI-MARKET', 'brand_promo', '品牌推广组', 1),
                                                                                          ('TEAM-ALI-DIGITAL', 'DEPT-ALI-MARKET', 'digital_market', '数字营销组', 1),
                                                                                          ('TEAM-ALI-ANALYSIS', 'DEPT-ALI-MARKET', 'market_analysis', '市场分析组', 1);

-- 插入腾讯社交网络事业群的团队数据
INSERT INTO XnetMLops.xnet_mlops_sys_team (uid, dept_uid, team_id, team_name, status) VALUES
                                                                                          ('TEAM-TENCENT-WECHAT', 'DEPT-TENCENT-SOCIAL', 'wechat_dev', '微信开发组', 1),
                                                                                          ('TEAM-TENCENT-QQ', 'DEPT-TENCENT-SOCIAL', 'qq_product', 'QQ产品组', 1),
                                                                                          ('TEAM-TENCENT-AD', 'DEPT-TENCENT-SOCIAL', 'social_ad', '社交广告组', 1);

-- 插入腾讯互动娱乐事业群的团队数据
INSERT INTO XnetMLops.xnet_mlops_sys_team (uid, dept_uid, team_id, team_name, status) VALUES
                                                                                          ('TEAM-TENCENT-HONOR', 'DEPT-TENCENT-ENTERTAIN', 'king_glory', '王者荣耀组', 1),
                                                                                          ('TEAM-TENCENT-LEAGUE', 'DEPT-TENCENT-ENTERTAIN', 'lol', '英雄联盟组', 1),
                                                                                          ('TEAM-TENCENT-PUBLISH', 'DEPT-TENCENT-ENTERTAIN', 'game_publish', '游戏发行组', 1);

SET @mlops_competition_user_id = (
  SELECT Id FROM XnetMLops.xnet_mlops_user_infos WHERE phone = '17870171303' LIMIT 1
);
INSERT INTO XnetMLops.xnet_mlops_usr_organization_membership (
  user_id,
  tenant_uid,
  dept_uid,
  team_uid,
  status
) VALUES (
  @mlops_competition_user_id,
  'TEN-SYNAPXNET',
  'DEPT-SYNAPXNET-PLATFORM',
  'TEAM-GOAI-INFRA',
  1
);

-- 存储桶表
CREATE TABLE XnetMLops.xnet_mlops_sys_bucket (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '存储桶ID',
                                                 uid VARCHAR(50) NOT NULL UNIQUE COMMENT '存储桶唯一编码',
                                                 name VARCHAR(100) NOT NULL COMMENT '存储桶名称',
                                                 identifier VARCHAR(100) NOT NULL UNIQUE COMMENT '存储桶标识符（唯一）',
                                                 type ENUM('public', 'tenant') NOT NULL COMMENT '存储桶类型',
                                                 tenant_uid VARCHAR(50) COMMENT '所属租户唯一编码',
                                                 dept_uid VARCHAR(50) COMMENT '所属部门唯一编码',
                                                 team_uid VARCHAR(50) COMMENT '所属团队唯一编码',
                                                 current_size DOUBLE NOT NULL DEFAULT 0 COMMENT '当前大小(GB)',
                                                 max_size DOUBLE NOT NULL DEFAULT 100 COMMENT '最大大小(GB)',
                                                 status ENUM('active', 'disabled') NOT NULL DEFAULT 'active' COMMENT '状态',
                                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                 FOREIGN KEY (tenant_uid) REFERENCES  XnetMLops.xnet_mlops_sys_tenant(uid) ON DELETE SET NULL,
                                                 FOREIGN KEY (dept_uid) REFERENCES  XnetMLops.xnet_mlops_sys_department(uid) ON DELETE SET NULL,
                                                 FOREIGN KEY (team_uid) REFERENCES  XnetMLops.xnet_mlops_sys_team(uid) ON DELETE SET NULL
) COMMENT='存储桶表';

-- 插入示例数据
INSERT INTO XnetMLops.xnet_mlops_sys_bucket (uid, name, identifier, type, current_size, max_size, status) VALUES
                                                                                                              ('BUCKET-PUBLIC-IMAGES', '公共图片存储', 'public-images', 'public', 125, 500, 'active'),
                                                                                                              ('BUCKET-PUBLIC-DOCS', '公共文档存储', 'public-docs', 'public', 210, 500, 'active');

INSERT INTO XnetMLops.xnet_mlops_sys_bucket (uid, name, identifier, type, tenant_uid, current_size, max_size, status) VALUES
    ('BUCKET-TENANT-ALIBABA', '阿里巴巴用户数据', 'alibaba-userdata', 'tenant', 'TEN-ALIBABA', 342, 1000, 'active');

INSERT INTO XnetMLops.xnet_mlops_sys_bucket (uid, name, identifier, type, dept_uid, current_size, max_size, status) VALUES
    ('BUCKET-DEPT-ALI-TECH', '技术研发中心-代码仓库', 'dept-ali-tech-code', 'tenant', 'DEPT-ALI-TECH', 78, 200, 'active');

INSERT INTO XnetMLops.xnet_mlops_sys_bucket (uid, name, identifier, type, team_uid, current_size, max_size, status) VALUES
    ('BUCKET-TEAM-ALI-JAVA', 'Java开发组-项目存储', 'team-ali-java-projects', 'tenant', 'TEAM-ALI-JAVA', 45, 100, 'active');

CREATE TABLE XnetMLops.xnet_mlops_dpp_dataset (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  uid VARCHAR(36) NOT NULL COMMENT '数据集UID,唯一标识',
                                                  userId VARCHAR(36) NOT NULL COMMENT '用户id',
                                                  dataset_file VARCHAR(255) NOT NULL COMMENT '数据集名称',
                                                  type VARCHAR(50) NOT NULL COMMENT '数据类型值',
                                                  type_label VARCHAR(100) COMMENT '数据类型标签',
                                                  zone VARCHAR(50) NOT NULL COMMENT '数据区域值',
                                                  zone_label VARCHAR(100) COMMENT '数据区域标签',
                                                  bucket_name VARCHAR(255) NOT NULL COMMENT '存储桶名称',
                                                  bucket_identifier VARCHAR(255) NOT NULL COMMENT '存储桶唯一标识符',
                                                  encryption TINYINT NOT NULL COMMENT '是否加密(0:否,1:是)',
                                                  subdata_area VARCHAR(255) NOT NULL COMMENT '子数据域',
                                                  tenant_uid VARCHAR(36) NOT NULL COMMENT '租户UID',
                                                  dept_uid VARCHAR(36) COMMENT '部门UID',
                                                  team_uid VARCHAR(36) NOT NULL COMMENT '团队UID',
                                                  team_name VARCHAR(255) COMMENT '团队名称',
                                                  level TINYINT NOT NULL COMMENT '组织层级(0-3)',
                                                  description VARCHAR(500) COMMENT '数据集描述',
                                                  source_platform VARCHAR(64) COMMENT '来源平台',
                                                  source_product_name VARCHAR(128) COMMENT '来源数据产品名称',
                                                  source_product_version VARCHAR(128) COMMENT '来源数据产品版本',
                                                  source_uri VARCHAR(512) COMMENT '不含凭据的来源定位符',
                                                  row_count BIGINT COMMENT '真实记录数',
                                                  byte_size BIGINT COMMENT '导入制品字节数',
                                                  schema_digest_sha256 CHAR(64) COMMENT '字段契约摘要',
                                                  artifact_digest_sha256 CHAR(64) COMMENT '来源制品摘要',
                                                  lineage_reference VARCHAR(512) COMMENT 'DataOps 血缘引用',
                                                  import_status VARCHAR(32) COMMENT '导入状态',
                                                  imported_at DATETIME COMMENT '导入时间',
                                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '数据集表';

CREATE INDEX idx_dataset_tenant ON XnetMLops.xnet_mlops_dpp_dataset(tenant_uid);
CREATE INDEX idx_dataset_team ON XnetMLops.xnet_mlops_dpp_dataset(team_uid);
CREATE UNIQUE INDEX uk_dataset_source_version
    ON XnetMLops.xnet_mlops_dpp_dataset(source_platform, source_product_version);


CREATE TABLE XnetMLops.xnet_mlops_mtp_algorithms (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                            uid VARCHAR(36) NOT NULL UNIQUE COMMENT '算法唯一标识符',
                            userId VARCHAR(36) NOT NULL COMMENT '用户id',
                            algorithm_name VARCHAR(255) NOT NULL COMMENT '算法名称',
                            version VARCHAR(50) NOT NULL COMMENT '版本号',
                            zone VARCHAR(50) NOT NULL COMMENT '数据区域值',
                            zone_label VARCHAR(100) COMMENT '数据区域标签',
                            encryption BOOLEAN NOT NULL DEFAULT false COMMENT '是否需要加密',
                            subdata_area VARCHAR(100) NOT NULL COMMENT '子数据域',
                            bucket_name VARCHAR(255) NOT NULL COMMENT '存储桶名称',
                            bucket_identifier VARCHAR(255) NOT NULL COMMENT '存储桶标识符',
                            team_uid VARCHAR(36) NOT NULL COMMENT '团队UID',
                            team_name VARCHAR(36) NOT NULL COMMENT '团队名称',
                            description VARCHAR(500) COMMENT '算法描述',
                            tenant_uid VARCHAR(36) COMMENT '租户UID',
                            dept_uid VARCHAR(36) COMMENT '部门UID',
                            level INT NOT NULL DEFAULT 0 COMMENT '组织级别',
                            is_CAS BOOLEAN NOT NULL DEFAULT false COMMENT '是否使用云仓算法包',
                            cloud_algorithm_id VARCHAR(36) COMMENT '云仓算法包ID',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                            INDEX idx_team_uid (team_uid),
                            INDEX idx_tenant_uid (tenant_uid),
                            INDEX idx_is_cas (is_CAS),
                            UNIQUE INDEX idx_algorithm_version (algorithm_name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法信息表';


-- 训练任务表
CREATE TABLE XnetMLops.xnet_mlops_mtp_train_task (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 自增主键
                                                     uid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),  -- 唯一标识
                                                     userId VARCHAR(36) NOT NULL,  -- 关联用户
                                                     tenant_uid VARCHAR(36) NOT NULL,
                                                     task_name VARCHAR(255) NOT NULL,
                                                     task_type VARCHAR(50) NOT NULL,
                                                     encryption VARCHAR(50) NOT NULL,
                                                     task_zone VARCHAR(50) NOT NULL,
                                                     pod_type VARCHAR(50) NOT NULL,
                                                     resources VARCHAR(50) NOT NULL,
                                                     train_type VARCHAR(50) NOT NULL,
                                                     image_uid VARCHAR(255) NOT NULL,
                                                     image VARCHAR(255) NOT NULL,
                                                     description TEXT,
                                                     algorithm_uid VARCHAR(255) NOT NULL,
                                                     algorithm_name VARCHAR(255) NOT NULL,
                                                     algorithm_version VARCHAR(50) NOT NULL,
                                                     task_route VARCHAR(255) NOT NULL,
                                                     train_config_content TEXT,
                                                     train_config_format VARCHAR(10) NOT NULL DEFAULT 'txt',
                                                     notification_config JSON,
                                                     output_config JSON,
                                                     schedule_config JSON,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 任务数据集关联表
CREATE TABLE XnetMLops.xnet_mlops_mtp_task_dataset (
                                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                       uid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),  -- 唯一标识
                                                       task_uid VARCHAR(36) NOT NULL,  -- 关联train_task的uid
                                                       dataset_id VARCHAR(36) NOT NULL,
                                                       dataset_uid VARCHAR(36) NOT NULL,
                                                       dataset_name VARCHAR(36) NOT NULL,
                                                       dataset_file VARCHAR(36) NOT NULL,
                                                       bucket_identifier VARCHAR(36) NOT NULL,
                                                       FOREIGN KEY (task_uid) REFERENCES XnetMLops.xnet_mlops_mtp_train_task(uid) ON DELETE CASCADE
);

-- 任务自定义变量表
CREATE TABLE XnetMLops.xnet_mlops_mtp_task_custom_variable (
                                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                               uid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),  -- 唯一标识
                                                               task_uid VARCHAR(36) NOT NULL,  -- 关联train_task的uid
                                                               name VARCHAR(255) NOT NULL,
                                                               value VARCHAR(255) NOT NULL,
                                                               FOREIGN KEY (task_uid) REFERENCES XnetMLops.xnet_mlops_mtp_train_task(uid) ON DELETE CASCADE
);

CREATE TABLE XnetMLops.xnet_mlops_smp_algorithms (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '算法ID',
                                                     uid VARCHAR(36) NOT NULL UNIQUE COMMENT '算法唯一标识符',
                                                     url VARCHAR(255) NOT NULL COMMENT '算法仓库URL',
                                                     encrypted_token BLOB NOT NULL COMMENT 'AES-256加密的访问令牌',
                                                     algorithmRepository VARCHAR(50) NOT NULL COMMENT '算法名称',
                                                     algorithm_version VARCHAR(50) NOT NULL COMMENT '算法版本',
                                                     description TEXT COMMENT '算法描述',

    -- 组织架构
                                                     tenant_uid VARCHAR(50) NOT NULL COMMENT '租户唯一编码',
                                                     dept_uid VARCHAR(50) NOT NULL COMMENT '部门唯一编码',
                                                     team_uid VARCHAR(50) NOT NULL COMMENT '团队唯一编码',

    -- 授权租户列表
                                                     authorized_tenants VARCHAR(255) NOT NULL COMMENT '授权租户列表',

    -- 审计字段
                                                     created_by VARCHAR(255) NOT NULL COMMENT '创建者',
                                                     updated_by VARCHAR(255) COMMENT '更新者',
                                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='算法模型表';

-- 索引
CREATE INDEX idx_smp_algorithms_uid ON XnetMLops.xnet_mlops_smp_algorithms(uid);
CREATE INDEX idx_smp_algorithms_team ON XnetMLops.xnet_mlops_smp_algorithms(team_uid);
CREATE INDEX idx_smp_algorithms_tenant ON XnetMLops.xnet_mlops_smp_algorithms(tenant_uid);
CREATE INDEX idx_smp_algorithms_dept ON XnetMLops.xnet_mlops_smp_algorithms(dept_uid);

-- 添加复合唯一约束
ALTER TABLE XnetMLops.xnet_mlops_smp_algorithms
    ADD CONSTRAINT unique_algorithm_entry
        UNIQUE (url, algorithmRepository, algorithm_version);


CREATE TABLE `xnet_mlops_smp_harbor_repository` (
                                                    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
                                                    `uid` varchar(36) NOT NULL DEFAULT (uuid()) COMMENT '全局唯一ID',
                                                    `name` varchar(100) NOT NULL COMMENT '仓库名称',
                                                    `url` varchar(200) NOT NULL COMMENT '仓库URL',
                                                    `username` varchar(100) DEFAULT NULL COMMENT '登录用户名',
                                                    `password` varchar(255) DEFAULT NULL COMMENT '加密密码',
                                                    `created_by` varchar(255) NOT NULL COMMENT '创建者',
                                                    `updated_by` varchar(255) DEFAULT NULL COMMENT '更新者',
                                                    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                                    `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                    PRIMARY KEY (`id`),
                                                    UNIQUE KEY `idx_uid` (`uid`),
                                                    UNIQUE KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Harbor仓库信息表';

CREATE TABLE `xnet_mlops_smp_docker_file` (
                                              `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
                                              `uid` varchar(36) NOT NULL DEFAULT (uuid()) COMMENT '全局唯一ID',
                                              `name` varchar(100) NOT NULL COMMENT '文件名',
                                              `content` text NOT NULL COMMENT 'Dockerfile内容',
                                              `tags` text COMMENT '逗号分隔的标签',
                                              `push_status` enum('PENDING', 'PUSHING', 'PUSHED', 'FAILED') DEFAULT 'PENDING' COMMENT '推送状态',
                                              `harbor_uid` varchar(36) DEFAULT NULL COMMENT '关联的Harbor仓库UID',
                                              `push_history` text COMMENT '推送历史(JSON格式)',
                                              `created_by` varchar(255) NOT NULL COMMENT '创建者',
                                              `updated_by` varchar(255) DEFAULT NULL COMMENT '更新者',
                                              `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                              `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                              PRIMARY KEY (`id`),
                                              UNIQUE KEY `idx_uid` (`uid`),
                                              KEY `harbor_uid` (`harbor_uid`),
                                              CONSTRAINT `xnet_mlops_smp_docker_file_ibfk_1` FOREIGN KEY (`harbor_uid`) REFERENCES `xnet_mlops_smp_harbor_repository` (`uid`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Docker文件主表';

-- 任务自定义变量表
CREATE TABLE XnetMLops.xnet_mlops_mtp_train_info (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     uid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),  -- 唯一标识
                                                     task_uid VARCHAR(36) NOT NULL,  -- 关联train_task的uid
                                                     job_uid VARCHAR(36) NOT NULL,
                                                     job_status VARCHAR(255) NOT NULL,
                                                     job_content TEXT,
                                                     start_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
                                                     end_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
                                                     schedule_active int NOT NULL,
                                                     FOREIGN KEY (task_uid) REFERENCES XnetMLops.xnet_mlops_mtp_train_task(uid) ON DELETE CASCADE
);
ALTER TABLE xnet_mlops_mtp_train_info ADD UNIQUE INDEX idx_job_uid (job_uid);

CREATE TABLE XnetMLops.xnet_mlops_mtp_recommendation_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_uid VARCHAR(64) NOT NULL UNIQUE COMMENT '推荐训练运行标识',
    dataset_id BIGINT NOT NULL COMMENT 'MLOps 数据集主键',
    tenant_uid VARCHAR(128) NOT NULL COMMENT '租户边界',
    user_id VARCHAR(128) NOT NULL COMMENT '发起用户',
    product_version VARCHAR(128) NOT NULL COMMENT 'DataOps 数据产品版本',
    approval_id VARCHAR(128) NOT NULL COMMENT '人工审批号',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE COMMENT '幂等键',
    status VARCHAR(32) NOT NULL COMMENT '运行状态',
    schema_digest_sha256 CHAR(64) NOT NULL COMMENT 'Schema 摘要',
    artifact_digest_sha256 CHAR(64) NOT NULL COMMENT '训练输入摘要',
    model_digest_sha256 CHAR(64) COMMENT '模型摘要',
    artifact_reference VARCHAR(256) COMMENT '不含物理路径的制品引用',
    metrics_json TEXT COMMENT '训练和测试指标',
    error_summary VARCHAR(1000) COMMENT '失败摘要',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL
) COMMENT '推荐 DCN 训练运行审计表';

CREATE INDEX idx_recommendation_run_tenant
    ON XnetMLops.xnet_mlops_mtp_recommendation_run(tenant_uid, started_at);

-- 调度训练任务表
CREATE TABLE XnetMLops.xnet_mlops_mtp_train_schedule_info (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     uid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),  -- 唯一标识
                                                     task_uid VARCHAR(36) NOT NULL,  -- 关联train_task的uid
                                                     job_uid VARCHAR(36) NOT NULL,
                                                     job_status VARCHAR(255) NOT NULL,
                                                     job_content TEXT,
                                                     start_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
                                                     end_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
                                                     schedule_active int NOT NULL,
                                                     FOREIGN KEY (task_uid) REFERENCES XnetMLops.xnet_mlops_mtp_train_task(uid) ON DELETE CASCADE
);
ALTER TABLE xnet_mlops_mtp_train_schedule_info ADD UNIQUE INDEX idx_job_uid (job_uid);

CREATE TABLE XnetMLops.xnet_mlops_sys_datasource (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                                     uid VARCHAR(36) NOT NULL COMMENT '唯一标识符',
                                                     name VARCHAR(100) NOT NULL COMMENT '数据源名称',
                                                     type VARCHAR(50) NOT NULL COMMENT '数据源类型: mysql, postgresql, oracle, sqlserver, hive, clickhouse',
                                                     host VARCHAR(255) NOT NULL COMMENT '主机地址',
                                                     port INT NOT NULL COMMENT '端口号',
                                                     username VARCHAR(100) COMMENT '用户名',
                                                     password VARCHAR(255) COMMENT '密码(加密存储)',
                                                     default_database VARCHAR(100) COMMENT '默认数据库',
                                                     description VARCHAR(500) COMMENT '描述',
                                                     enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0:否,1:是)',
                                                     created_by VARCHAR(255) NOT NULL COMMENT '创建者',
                                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                     UNIQUE KEY uk_uid (uid),
                                                     UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据源配置表';
ALTER TABLE xnet_mlops_sys_datasource
    ADD COLUMN allowed_databases VARCHAR(1000) COMMENT '允许访问的数据库列表，用逗号分隔'
AFTER default_database;


CREATE TABLE XnetMLops.xnet_mlops_dpp_feature_engineering (
                                                              id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                                              uid VARCHAR(36) NOT NULL COMMENT '唯一标识符',
                                                              name VARCHAR(100) NOT NULL COMMENT '特征工程名称',
                                                              description VARCHAR(500) COMMENT '描述',
                                                              datasource_id BIGINT NOT NULL COMMENT '关联数据源ID',
                                                              datasource_name VARCHAR(100) COMMENT '数据源名称',
                                                              `database` VARCHAR(100) COMMENT '数据库名',
                                                              table_name VARCHAR(100) COMMENT '表名',
                                                              selected_columns TEXT COMMENT '选中的字段(JSON格式)',
                                                              transform_config TEXT COMMENT '转换配置(JSON格式)',
                                                              output_path VARCHAR(255) COMMENT '输出路径',
                                                              status VARCHAR(50) NOT NULL DEFAULT 'draft' COMMENT '状态: draft, processing, completed, failed',
                                                              created_by VARCHAR(255) NOT NULL COMMENT '创建者',
                                                              team_uid VARCHAR(36) COMMENT '团队UID',
                                                              team_name VARCHAR(100) COMMENT '团队名称',
                                                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                              UNIQUE KEY uk_uid (uid),
                                                              UNIQUE KEY uk_name (name),
                                                              INDEX idx_team_uid (team_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='特征工程表';
-- 为特征工程表添加数据区域和存储桶字段
ALTER TABLE xnet_mlops_dpp_feature_engineering
    ADD COLUMN zone VARCHAR(50) COMMENT '数据区域' AFTER output_path,
ADD COLUMN bucket_uid VARCHAR(100) COMMENT '存储桶UID' AFTER zone,
ADD COLUMN bucket_name VARCHAR(255) COMMENT '存储桶名称' AFTER bucket_uid;



-- MEP (Model Endpoint Platform) 模型部署平台数据库表结构
-- 创建时间: 2024
-- 描述: 用于管理大模型服务、API密钥、部署节点和模型部署

-- ==========================================
-- 1. API密钥表（基础表，无依赖）
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_api_key` (
                                                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                        `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '密钥名称',
    `key_hash` VARCHAR(128) NOT NULL COMMENT '密钥哈希值',
    `key_masked` VARCHAR(64) NOT NULL COMMENT '脱敏显示的密钥',
    `encrypted_key` VARCHAR(500) DEFAULT NULL COMMENT 'AES加密存储的原始密钥（用于OpenClaw等服务引用）',
    `provider` VARCHAR(32) NOT NULL COMMENT '服务商: ollama, openai, deepseek, custom',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态: active, disabled, expired',
    `usage_limit` BIGINT DEFAULT NULL COMMENT '使用次数限制',
    `usage_count` BIGINT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `expires_at` DATETIME DEFAULT NULL COMMENT '过期时间',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_provider` (`provider`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API密钥表';

-- ==========================================
-- 2. 部署节点表（基础表，无依赖）
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deploy_node` (
                                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                            `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '节点名称',
    `ip_address` VARCHAR(64) NOT NULL COMMENT 'IP地址',
    `port` INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
    `status` VARCHAR(32) NOT NULL DEFAULT 'offline' COMMENT '状态: online, offline, maintenance',
    `cpu_cores` INT NOT NULL DEFAULT 4 COMMENT 'CPU核心数',
    `memory_gb` INT NOT NULL DEFAULT 8 COMMENT '内存大小(GB)',
    `gpu_info` VARCHAR(255) DEFAULT NULL COMMENT 'GPU信息',
    `docker_version` VARCHAR(32) DEFAULT NULL COMMENT 'Docker版本',
    `nginx_status` VARCHAR(32) DEFAULT 'stopped' COMMENT 'Nginx状态: running, stopped',
    `labels` JSON DEFAULT NULL COMMENT '标签(JSON数组)',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_status` (`status`),
    KEY `idx_ip` (`ip_address`),
    KEY `idx_created_at` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部署节点表';

-- ==========================================
-- 3. 大模型服务表
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_llm_service` (
                                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                            `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '服务名称',
    `type` VARCHAR(32) NOT NULL COMMENT '服务类型: ollama, openai, deepseek, custom',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '服务描述',
    `endpoint` VARCHAR(255) NOT NULL COMMENT '服务端点URL',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `api_key` VARCHAR(255) DEFAULT NULL COMMENT 'API密钥(加密存储)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'stopped' COMMENT '服务状态: running, stopped, error, deploying',
    `config` JSON DEFAULT NULL COMMENT '服务配置(JSON格式)',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型服务表';

-- ==========================================
-- 4. 模型部署表（依赖于部署节点表）
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_model_deployment` (
                                                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                                 `uid` VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    `name` VARCHAR(100) NOT NULL COMMENT '部署名称',
    `model_source` VARCHAR(32) NOT NULL COMMENT '模型来源: mtp, llm',
    `model_uid` VARCHAR(64) NOT NULL COMMENT '模型UID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `model_version` VARCHAR(32) DEFAULT NULL COMMENT '模型版本',
    `node_uid` VARCHAR(64) NOT NULL COMMENT '部署节点UID',
    `node_name` VARCHAR(100) DEFAULT NULL COMMENT '部署节点名称',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending, deploying, running, failed, stopped',
    `container_id` VARCHAR(128) DEFAULT NULL COMMENT '容器ID',
    `container_name` VARCHAR(100) DEFAULT NULL COMMENT '容器名称',
    `image_name` VARCHAR(255) DEFAULT NULL COMMENT '镜像名称',
    `port` INT DEFAULT 8080 COMMENT '服务端口',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT '服务端点URL',
    `replicas` INT NOT NULL DEFAULT 1 COMMENT '副本数',
    `resource_config` JSON DEFAULT NULL COMMENT '资源配置(JSON)',
    `nginx_config` JSON DEFAULT NULL COMMENT 'Nginx配置(JSON)',
    `health_check` JSON DEFAULT NULL COMMENT '健康检查配置(JSON)',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_model_source` (`model_source`),
    KEY `idx_node_uid` (`node_uid`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型部署表';

-- ==========================================
-- 5. 部署日志表（依赖于模型部署表）
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_deployment_log` (
                                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                               `deployment_uid` VARCHAR(64) NOT NULL COMMENT '部署UID',
    `level` VARCHAR(16) NOT NULL DEFAULT 'info' COMMENT '日志级别: info, warn, error',
    `message` TEXT NOT NULL COMMENT '日志消息',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    PRIMARY KEY (`id`),
    KEY `idx_deployment_uid` (`deployment_uid`),
    KEY `idx_level` (`level`),
    KEY `idx_timestamp` (`timestamp`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部署日志表';

-- ==========================================
-- 6. 服务监控指标表（依赖于模型部署表）
-- ==========================================
CREATE TABLE IF NOT EXISTS `xnet_mlops_mep_service_metrics` (
                                                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                                `deployment_uid` VARCHAR(64) NOT NULL COMMENT '部署UID',
    `cpu_usage` DECIMAL(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
    `memory_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '内存使用率(%)',
    `request_count` BIGINT DEFAULT 0 COMMENT '请求数',
    `error_count` BIGINT DEFAULT 0 COMMENT '错误数',
    `avg_response_time` INT DEFAULT NULL COMMENT '平均响应时间(ms)',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
    PRIMARY KEY (`id`),
    KEY `idx_deployment_uid` (`deployment_uid`),
    KEY `idx_timestamp` (`timestamp`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务监控指标表';

-- ==========================================
-- 插入示例数据
-- ==========================================

-- 示例大模型服务（使用正确的JSON格式）
INSERT INTO `xnet_mlops_mep_llm_service` (`uid`, `name`, `type`, `description`, `endpoint`, `model_name`, `status`, `config`, `created_by`) VALUES
                                                                                                                                                ('llm-001', 'Ollama本地服务', 'ollama', '本地部署的Ollama大模型服务', 'http://localhost:11434', 'llama2', 'stopped', '{"max_tokens": 2048, "temperature": 0.7}', 'admin'),
                                                                                                                                                ('llm-002', 'DeepSeek API', 'deepseek', 'DeepSeek满血版API服务', 'https://api.deepseek.com/v1', 'deepseek-chat', 'stopped', '{"max_tokens": 4096, "temperature": 0.8}', 'admin');

-- 示例部署节点（使用正确的JSON格式）
INSERT INTO `xnet_mlops_mep_deploy_node` (`uid`, `name`, `ip_address`, `port`, `status`, `cpu_cores`, `memory_gb`, `gpu_info`, `docker_version`, `nginx_status`, `labels`, `description`, `created_by`) VALUES
                                                                                                                                                                                                            ('node-001', 'GPU节点-01', '192.168.10.101', 22, 'online', 32, 64, 'NVIDIA RTX 4090 x2', '24.0.7', 'running', '["gpu", "production"]', '生产环境GPU计算节点', 'admin'),
                                                                                                                                                                                                            ('node-002', 'CPU节点-01', '192.168.10.102', 22, 'online', 16, 32, NULL, '24.0.7', 'running', '["cpu", "production"]', '生产环境CPU计算节点', 'admin'),
                                                                                                                                                                                                            ('node-003', '测试节点-01', '192.168.10.103', 22, 'offline', 8, 16, NULL, '23.0.6', 'stopped', '["test"]', '测试环境节点', 'admin');

