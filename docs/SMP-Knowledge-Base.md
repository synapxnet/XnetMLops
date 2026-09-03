# SMP（服务管理平台）模块知识库

> **版本**: 1.0
> **更新日期**: 2026-02-12
> **适用范围**: XnetMLops 平台 SMP 模块前后端完整架构
> **说明**: 本文档为 RAG 知识库源文件，涵盖 SMP 模块的架构、API、数据模型、业务流程等全部细节

---

## 一、模块概述

### 1.1 基本信息

| 属性 | 值 |
|------|-----|
| 模块名称 | SMP (Service Management Platform) 服务管理平台 |
| 后端服务 | `mlops-smp-service` (Spring Boot 3.4.6, Java 17) |
| 服务端口 | 8185 |
| 数据库 | MySQL `XnetMLops` (192.168.1.5:3306) |
| 连接池 | HikariCP (最大 20 连接) |
| HDFS | hdfs://192.168.1.5:8020 (Hadoop Client 3.3.4) |
| Jenkins | http://192.168.1.5:8080 |
| SSH 库 | JSch 0.1.55 |
| HTML 解析 | Jsoup 1.17.2 |
| 前端路径 | `XnetMLops-web/apps/web-antd/src/views/SMP/` |
| API 路径 | `XnetMLops-web/apps/web-antd/src/views/SMP/api/`（被全部模块共享） |
| 路由前缀 | `/SMP/` |
| 包基础路径 | `com.synapxnet.mlopssmpservice` |

### 1.2 核心子系统

SMP 是整个 XnetMLops 平台的**基础设施层**，为其他模块（MEP/MTP/DPP/XAA）提供底层服务。

| 子系统 | 功能 | 状态 |
|--------|------|------|
| 工作站管理 | 服务器注册、SSH 连通测试、凭据加密存储、心跳检测 | 已完成 |
| Jenkins 管理 | Master/Agent 远程部署、版本管理、Jenkins API 集成、凭据配置 | 已完成 |
| Hadoop 集群管理 | Master/Node 远程部署、版本管理、集群拓扑、Hosts 同步 | 已完成 |
| 集群可视化 | SVG 拓扑图、统一节点格式、实时状态刷新 | 已完成（Hadoop/Jenkins） |
| Docker/Harbor 管理 | Dockerfile CRUD、Harbor 仓库注册、Jenkins Pipeline 构建推送 | 已完成 |
| 存储桶管理 | HDFS 目录管理、存储配额、组织归属 | 已完成 |
| 数据源管理 | 多类型数据库连接（MySQL/PG/Oracle/Hive 等）、Schema 自省 | 已完成 |
| 算法仓库管理 | Git 仓库注册、组织归属、CAS 模式支持 | 已完成 |
| 特征算子管理 | Git 仓库注册、算子代码版本管理 | 已完成 |
| 数据集配置 | 数据集类型/区域配置项管理 | 已完成 |
| 组织架构 | 租户→部门→团队三级层次结构 | 已完成 |

### 1.3 技术栈

**后端:**
- Spring Boot 3.4.6 + `@EnableAsync`
- MyBatis 3.0.4（注解 SQL 为主，部分 `<script>` 动态 SQL）
- MySQL Connector/J + HikariCP
- Hadoop Client 3.3.4（HDFS 存储桶）
- JSch 0.1.55（SSH/SFTP 远程部署）
- Jsoup 1.17.2（版本目录 HTML 爬取）
- Jackson Databind 2.17.0
- Lombok

**前端:**
- Vue 3 Composition API + TypeScript
- Ant Design Vue
- Vben Admin Pro 框架
- 6 个请求客户端：`requestClient`(通用)、`smpRequestClient`(SMP)、`mtpRequestClient`(MTP)、`dppRequestClient`(DPP)、`mepRequestClient`(MEP)、`xaaRequestClient`(XAA)
- 响应双重解包：`defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'`
- SVG 集群拓扑可视化

---

## 二、系统架构

### 2.1 后端架构

```
mlops-smp-service/src/main/java/com/synapxnet/mlopssmpservice/
├── MlopsSmpServiceApplication.java          # Spring Boot 入口，@EnableAsync
├── config/
│   └── PipelineParamEnricher.java           # Pipeline 参数增强器（Harbor URL、凭据）
├── Utils/
│   └── HadoopUtil.java                      # HDFS 工具（FileSystem 单例、Kerberos 支持）
├── controller/ (16 个控制器)
│   ├── WorkstationController.java           # 工作站 CRUD + SSH 测试 + 凭据
│   ├── JenkinsMasterController.java         # Jenkins Master CRUD + 部署 + 凭据配置
│   ├── JenkinsNodeController.java           # Jenkins Agent CRUD + 部署 + 启停
│   ├── JenkinsVersionController.java        # Jenkins 版本管理
│   ├── HadoopClusterController.java         # Hadoop CRUD + 部署 + HDFS/YARN 状态
│   ├── HadoopVersionController.java         # Hadoop 版本管理
│   ├── ClusterManagementController.java     # 统一集群拓扑 + Hosts 同步
│   ├── HarborRepositoryController.java      # Harbor 仓库 CRUD
│   ├── DockerFileController.java            # Dockerfile CRUD + Pipeline 创建
│   ├── AlgorithmRepositoryController.java   # 算法 Git 仓库 CRUD + 搜索
│   ├── FeatureOperatorController.java       # 特征算子 Git 仓库 CRUD + 搜索
│   ├── BucketController.java                # 存储桶 CRUD（含 HDFS 目录操作）
│   ├── DataSourceController.java            # 数据源 CRUD + JDBC 测试 + Schema 自省
│   ├── DatasetConfigController.java         # 数据集配置项管理
│   └── DeptTreeDataController.java          # 组织架构树
├── entity/ (22 个实体)
│   ├── Workstation.java                     # 工作站（camelCase，AES 加密密码）
│   ├── JenkinsMaster.java                   # Jenkins Master（snake_case，~1817 行服务）
│   ├── JenkinsNode.java                     # Jenkins Agent（snake_case）
│   ├── JenkinsMasterDeployConfig.java       # 部署配置（含 Git/Harbor/SSH 凭据内部类）
│   ├── JenkinsNodeDeployConfig.java         # Agent 部署配置
│   ├── JenkinsVersion.java                  # Jenkins 版本（camelCase）
│   ├── HadoopCluster.java                   # Hadoop 节点（camelCase）
│   ├── HadoopDeployConfig.java              # Hadoop 部署配置（含端口默认值）
│   ├── HadoopVersion.java                   # Hadoop 版本（camelCase）
│   ├── AlgorithmRepository.java             # 算法仓库（snake_case，组织 JOIN）
│   ├── FeatureOperator.java                 # 特征算子（snake_case，组织 JOIN）
│   ├── Bucket.java                          # 存储桶（snake_case，组织 JOIN）
│   ├── DataSource.java                      # 数据源（camelCase）
│   ├── DockerFile.java                      # Docker 文件（snake_case，PushStatus 枚举）
│   ├── HarborRepository.java               # Harbor 仓库（snake_case）
│   ├── PipelineConfigParams.java            # Pipeline 参数
│   ├── DatasetConfig.java                   # 数据集配置项
│   ├── ConfigItem.java                      # 通用配置项
│   ├── Tenant.java                          # 租户
│   ├── Department.java                      # 部门
│   ├── Team.java                            # 团队
│   └── DeptTreeData.java                    # 组织树节点（递归）
├── exception/
│   ├── DuplicateEntryException.java
│   ├── EntityNotFoundException.java
│   └── JobNotFoundException.java
├── mapper/ (15 个 Mapper)
│   └── ...（全部使用 MyBatis 注解 SQL）
├── service/ (17 个服务 = 9 接口 + 9 实现 + 6 独立服务)
│   └── ...
└── resources/
    ├── application.properties
    ├── sql/ (7 个 DDL 文件)
    └── scripts/ (5 个 Shell 脚本模板)
```

### 2.2 前端架构

```
XnetMLops-web/apps/web-antd/src/views/SMP/
├── api/ (22 个 API 文件 — 平台级共享 API 层)
│   ├── types.ts                             # 中央类型定义
│   ├── workstation.ts                       # 工作站 API (smpRequestClient)
│   ├── clusterManagement.ts                 # 集群管理 API (smpRequestClient)
│   ├── hadoopCluster.ts                     # Hadoop 集群 API (smpRequestClient)
│   ├── jenkinsMaster.ts                     # Jenkins Master API (smpRequestClient)
│   ├── jenkinsNode.ts                       # Jenkins Node API (smpRequestClient)
│   ├── harborRepository.ts                  # Harbor 仓库 API (smpRequestClient)
│   ├── dockerFileManager.ts                 # Docker 文件 API (smpRequestClient)
│   ├── algorithmConfig.ts                   # 算法 Git 配置 API (smpRequestClient)
│   ├── algorithm.ts                         # 算法 CRUD (mtpRequestClient → MTP 后端)
│   ├── algorithmManager.ts                  # 算法 HDFS 操作 (mtpRequestClient → MTP 后端)
│   ├── bucketConfig.ts                      # 存储桶配置 API (smpRequestClient)
│   ├── dataset.ts                           # 数据集 CRUD (dppRequestClient → DPP 后端)
│   ├── datasetConfig.ts                     # 数据集配置 API (smpRequestClient)
│   ├── datasetManager.ts                    # 数据集 HDFS 操作 (dppRequestClient → DPP 后端)
│   ├── datasource.ts                        # 数据源 API (smpRequestClient)
│   ├── deptTreeData.ts                      # 组织架构树 API (smpRequestClient)
│   ├── featureEngineering.ts                # 特征工程 API (dppRequestClient → DPP 后端)
│   ├── featureOperator.ts                   # 特征算子 API (dppRequestClient → DPP 后端)
│   ├── featureOperatorConfig.ts             # 特征算子 Git 配置 API (smpRequestClient)
│   ├── trainjob.ts                          # Jenkins 构建 API (mtpRequestClient → MTP 后端)
│   └── traintask.ts                         # 训练任务 API (mtpRequestClient → MTP 后端)
├── utils/
│   └── crypto.ts                            # generateAccessKey()
├── WorkstationManage/
│   ├── index.vue                            # 工作站列表
│   └── components/WorkstationForm.vue       # 工作站表单模态框
├── components/
│   └── WorkstationSelector.vue              # 可复用工作站选择器（手动/选择模式）
├── DPPManage/
│   ├── index.vue                            # DPP 配置导航（4 张卡片）
│   ├── DatasetConfigManage.vue              # 数据集类型/区域配置
│   ├── BucketConfigManage.vue               # 存储桶管理（级联组织筛选）
│   ├── DataSourceManage.vue                 # 数据源管理
│   ├── FeatureOperatorGitManage.vue         # 特征算子 Git 管理
│   ├── EditConfigModal.vue                  # 通用配置项模态框
│   └── EditBucketModal.vue                  # 存储桶模态框
├── MTPManage/
│   ├── index.vue                            # MTP 配置导航（3 张卡片）
│   ├── GitManage.vue                        # 算法 Git 仓库管理
│   ├── ImageManage.vue                      # Harbor 仓库管理
│   └── ImageCreate.vue                      # Docker 文件管理 + 推送
├── JenkinsDeployment/
│   ├── index.vue                            # Jenkins 资源仪表盘（30s 刷新）
│   ├── DeployMaster.vue                     # Jenkins Master 5 步部署向导
│   └── DeployNode.vue                       # Jenkins Node 4 步部署向导
├── JenkinsNodeManage/
│   ├── index.vue                            # Jenkins 节点列表（30s 刷新）
│   └── DeployNode.vue                       # Jenkins 节点部署（另一入口）
├── HadoopDeployment/
│   ├── index.vue                            # Hadoop 集群仪表盘（30s 刷新）
│   ├── DeployMaster.vue                     # Hadoop Master 5 步部署向导
│   └── DeployNode.vue                       # Hadoop Node 单页部署
└── ClusterManagement/
    ├── index.vue                            # 集群管理主页
    └── components/
        ├── ClusterTopology.vue              # SVG 拓扑图（1200x560 画布）
        ├── ClusterTypeSelector.vue          # 集群类型选择器
        ├── ConnectionLine.vue               # SVG 贝塞尔曲线连接（动画）
        ├── NodeDetailDrawer.vue             # 节点详情抽屉
        └── NodeIcon3D.vue                   # SVG 节点图标（160x120px）
```

### 2.3 配置

```properties
# application.properties
server.port=8185
spring.datasource.url=jdbc:mysql://192.168.1.5:3306/XnetMLops
spring.datasource.hikari.maximum-pool-size=20
mybatis.configuration.map-underscore-to-camel-case=true
hdfs.path=hdfs://192.168.1.5:8020
hdfs.user=root
jenkins.url=http://192.168.1.5:8080
jenkins.user=admin
jenkins.token=<configured>
```

**K8s 部署**: Namespace `xnet-mlops`，1 副本，端口 8185，Ingress path `/smp`（rewrite 至 `/`）

---

## 三、数据模型

### 3.1 Workstation — 工作站

**表名**: `xnet_mlops_smp_workstation`
**命名风格**: camelCase（使用 `@Results` 映射）
**AES 密钥**: `"SmpWorkstation16"`

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` | `Long` | 主键 |
| `uid` | `String` | UUID |
| `name` | `String` | 工作站名称 |
| `hostname` | `String` | 主机名（自动检测） |
| `hostnameMode` | `String` | 主机名模式 |
| `vendor` | `String` | 厂商（aliyun/huawei/tencent/aws/azure/self-hosted） |
| `serverType` | `String` | 服务器类型 |
| `region` | `String` | 区域 |
| `osType` | `String` | 操作系统（linux/windows/macos） |
| `osVersion` | `String` | 系统版本 |
| `cpuCores` | `Integer` | CPU 核数 |
| `ramGb` | `Integer` | 内存 GB |
| `diskGb` | `Integer` | 磁盘 GB |
| `hasGpu` | `Boolean` | 是否有 GPU |
| `gpuCount` | `Integer` | GPU 数量 |
| `gpuType` / `gpuModel` / `gpuMemory` | `String` | GPU 信息 |
| `availableDiskGb` / `availableRamGb` | `Integer` | 可用资源（心跳更新） |
| `domain` | `String` | 域名 |
| `ipAddress` | `String` | IP 地址 |
| `sshPort` | `Integer` | SSH 端口 |
| `sshUser` | `String` | SSH 用户名 |
| `authType` | `String` | 认证类型 |
| `encryptedPassword` | `String` | AES 加密密码（@JsonProperty WRITE_ONLY） |
| `encryptedPrivateKey` | `String` | AES 加密私钥（@JsonProperty WRITE_ONLY） |
| `status` | `String` | 状态：pending/online/offline |
| `lastHeartbeat` | `LocalDateTime` | 最后心跳时间 |
| `description` | `String` | 描述 |
| `createdBy` | `String` | 创建者 |
| `createdAt` / `updatedAt` | `LocalDateTime` | 时间戳 |

### 3.2 JenkinsMaster — Jenkins 主节点

**表名**: `xnet_mlops_smp_jenkins_masters`
**命名风格**: snake_case
**AES 密钥**: `"XnetMLopsJenkins"`

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` | `Long` | 主键 |
| `uid` | `String` | UUID |
| `name` | `String` | 名称 |
| `host` / `port` / `username` / `encrypted_password` | 连接信息 | SSH 连接 |
| `os_type` | `String` | 操作系统 |
| `jenkins_port` | `Integer` | Jenkins 端口 |
| `jenkins_home` / `jenkins_version` / `java_version` / `java_opts` | `String` | Jenkins 配置 |
| `admin_username` / `encrypted_admin_password` | `String` | Jenkins 管理员 |
| `credentials_config` | `String` | JSON 凭据配置 |
| `status` | `String` | 状态：pending/deploying/deployed/failed |
| `initial_password` / `deploy_log` | `String` | 部署信息 |
| `region` / `cpu_cores` / `ram_gb` / `disk_gb` | 资源信息 | |
| `tenant_uid` / `description` | `String` | 元数据 |
| `created_by` / `updated_by` / `created_at` / `updated_at` / `last_heartbeat` | 时间戳 | |

### 3.3 JenkinsNode — Jenkins 代理节点

**表名**: `xnet_mlops_smp_jenkins_nodes`
**命名风格**: snake_case

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` / `uid` / `name` | 基本信息 | |
| `host` / `port` / `username` / `encrypted_password` | 连接信息 | SSH |
| `os_type` / `region` | `String` | 系统/区域 |
| `container_type` | `String` | 容器类型：CCE/Docker |
| `resource_type` | `String` | 资源类型：CPU/single_gpu/multi_gpu |
| `resource_spec` | `String` | 资源规格（如 8c16g-1gpu） |
| `cpu_cores` / `ram_gb` / `gpu_memory` / `gpu_model` / `gpu_count` | 硬件规格 | |
| `status` | `String` | 状态 |
| `jenkins_url` / `agent_name` / `work_dir` | `String` | Agent 配置 |
| `java_version` / `python_version` / `agent_version` | `String` | 环境版本 |
| `labels` / `description` / `deploy_log` | `String` | 元数据 |

### 3.4 HadoopCluster — Hadoop 集群节点

**表名**: `xnet_mlops_smp_hadoop_cluster`
**命名风格**: camelCase

| 字段 | Java 类型 | 说明 |
|------|-----------|------|
| `id` / `uid` / `name` / `description` | 基本信息 | |
| `host` / `port` / `sshUser` / `sshPassword` / `sshPrivateKey` | SSH 连接 | |
| `hadoopVersion` / `osType` | `String` | 版本/系统 |
| `nodeType` | `String` | 节点类型：master/node |
| `deployMode` | `String` | 部署模式：standard/ha |
| `components` / `hdfsDataDirs` | `String` | 组件列表/数据目录 |
| `hdfsReplication` | `Integer` | HDFS 副本因子 |
| `hdfsBlockSize` | `Long` | HDFS 块大小 |
| `yarnMemory` / `yarnCpu` | `Integer` | YARN 资源 |
| `haMasterHost` / `zkCluster` | `String` | HA 配置 |
| `status` / `deployLog` | `String` | 状态/日志 |
| `masterId` | `Long` | 关联 Master ID |

### 3.5 其他实体

| 实体 | 表名 | 命名风格 | 说明 |
|------|------|----------|------|
| `JenkinsVersion` | `xnet_mlops_smp_jenkins_versions` | camelCase | 版本号/类型/下载 URL/LTS/Latest |
| `HadoopVersion` | `xnet_mlops_smp_hadoop_versions` | camelCase | 版本号/类型/下载 URL/Latest |
| `AlgorithmRepository` | `xnet_mlops_smp_algorithms` | snake_case | Git URL/token(byte[])/algorithm/version + 组织 JOIN |
| `FeatureOperator` | `xnet_mlops_smp_feature_operators` | snake_case | Git URL/token(byte[])/operator_name/code/version + 组织 JOIN |
| `Bucket` | `xnet_mlops_sys_bucket` | snake_case | identifier/type/current_size/max_size + 组织 JOIN + HDFS 目录 |
| `DataSource` | `xnet_mlops_sys_datasource` | camelCase | type(mysql/pg/oracle/...)/host/port/username/password |
| `DockerFile` | `xnet_mlops_smp_docker_file` | snake_case | content/tags/push_status(枚举)/harbor_uid/push_history |
| `HarborRepository` | `xnet_mlops_smp_harbor_repository` | snake_case | url/username/password |
| `DatasetConfig` | `xnet_mlops_smp_dataset_config_info` | snake_case | config_type/label/value |
| `Tenant` / `Department` / `Team` | `xnet_mlops_sys_*` | camelCase | 组织层级（租户→部门→团队） |
| `DeptTreeData` | — | — | 递归树节点（title/value/children） |

**部署配置实体**（非持久化）：
- `JenkinsMasterDeployConfig` — 含内部类：`GitCredential`, `HarborCredential`, `SSHCredential`
- `JenkinsNodeDeployConfig` — Agent 部署参数
- `HadoopDeployConfig` — 含端口默认值 getter 方法
- `PipelineConfigParams` — Docker Pipeline 参数

---

## 四、数据库表

### 4.1 SMP 拥有的表（`xnet_mlops_smp_*`）

| 表名 | SQL 文件 | 说明 |
|------|----------|------|
| `xnet_mlops_smp_workstation` | `workstation_table.sql` | 工作站注册表 |
| `xnet_mlops_smp_jenkins_masters` | `jenkins_master_table.sql` | Jenkins Master 表 |
| `xnet_mlops_smp_jenkins_nodes` | `jenkins_node_table.sql` | Jenkins Agent 表 |
| `xnet_mlops_smp_jenkins_versions` | `jenkins_versions_table.sql` | Jenkins 版本目录 |
| `xnet_mlops_smp_jenkins_version_sync_log` | `jenkins_versions_table.sql` | Jenkins 版本同步日志 |
| `xnet_mlops_smp_hadoop_cluster` | `hadoop_tables.sql` | Hadoop 集群节点表 |
| `xnet_mlops_smp_hadoop_versions` | `hadoop_tables.sql` | Hadoop 版本目录 |
| `xnet_mlops_smp_hadoop_version_sync_log` | `hadoop_tables.sql` | Hadoop 版本同步日志 |
| `xnet_mlops_smp_algorithms` | — | 算法 Git 仓库表 |
| `xnet_mlops_smp_feature_operators` | `feature_operator_table.sql` | 特征算子 Git 仓库表 |
| `xnet_mlops_smp_docker_file` | — | Docker 文件表 |
| `xnet_mlops_smp_harbor_repository` | — | Harbor 仓库表 |
| `xnet_mlops_smp_dataset_config_info` | — | 数据集配置表 |

### 4.2 系统共享表（`xnet_mlops_sys_*`）

| 表名 | 说明 |
|------|------|
| `xnet_mlops_sys_tenant` | 租户表 |
| `xnet_mlops_sys_department` | 部门表 |
| `xnet_mlops_sys_team` | 团队表 |
| `xnet_mlops_sys_bucket` | 存储桶表 |
| `xnet_mlops_sys_datasource` | 数据源表 |

---

## 五、后端 API 端点

### 5.1 工作站管理 — `WorkstationController`（`/api/smp/workstations`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 查询全部工作站 |
| `GET` | `/{id}` | 按 ID 查询 |
| `POST` | `/` | 创建工作站（AES 加密密码/私钥，SSH 自动检测主机名） |
| `PUT` | `/{id}` | 更新工作站（未提供密码时保留加密值） |
| `DELETE` | `/{id}` | 删除工作站 |
| `POST` | `/test-connection` | SSH 连通测试（检测 OS/CPU/RAM/磁盘/GPU） |
| `GET` | `/{id}/status` | SSH 心跳检查（更新 online/offline） |
| `GET` | `/{id}/credentials` | **获取解密凭据**（MEP 等模块调用） |
| `GET` | `/options/vendors` | 静态厂商列表 |
| `GET` | `/options/regions` | 静态区域列表 |
| `GET` | `/options/os-types` | 静态 OS 类型列表 |
| `GET` | `/options/os-versions` | 按 OS 类型的版本列表 |

**定时任务**: `@Scheduled(fixedRate=300000)` 每 5 分钟心跳检测所有 "online" 工作站。

### 5.2 Jenkins Master — `JenkinsMasterController`（`/api/smp/jenkins-masters`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 查询全部 Master |
| `GET` | `/{id}` | 按 ID 查询 |
| `POST` | `/` | 创建 Master（AES 加密 SSH + 管理员密码） |
| `PUT` | `/{id}` | 更新 |
| `DELETE` | `/{id}` | 删除 |
| `POST` | `/{id}/deploy` | **异步部署**（@Async，SFTP+SSH 执行脚本） |
| `POST` | `/{id}/start` | 启动（SSH `systemctl start jenkins`） |
| `POST` | `/{id}/stop` | 停止 |
| `POST` | `/{id}/restart` | 重启 |
| `GET` | `/{id}/initial-password` | 获取初始管理员密码（SSH 读取文件） |
| `POST` | `/{id}/configure-credentials` | 配置 Git/Harbor/SSH 凭据（Jenkins Groovy 脚本） |
| `POST` | `/{id}/create-node` | 在 Master 上创建 Agent 节点（Jenkins API） |
| `GET` | `/{id}/node-secret/{nodeName}` | 获取节点密钥（Jenkins Groovy 脚本） |
| `POST` | `/{id}/uninstall` | 卸载 Jenkins |

### 5.3 Jenkins Agent — `JenkinsNodeController`（`/api/smp/jenkins-nodes`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 查询全部 Agent |
| `GET` | `/{id}` | 按 ID 查询 |
| `POST` | `/` | 创建 |
| `PUT` | `/{id}` | 更新 |
| `DELETE` | `/{id}` | 删除 |
| `POST` | `/test-connection` | SSH 连通测试 |
| `POST` | `/{id}/deploy` | **异步部署**（先在 Master 创建节点，获取 secret，再部署 Agent） |
| `POST` | `/preview-script` | 预览部署脚本 |
| `GET` | `/{id}/status` | 检查 Agent 状态（SSH 检查 java 进程） |
| `POST` | `/{id}/start` | 启动 Agent |
| `POST` | `/{id}/stop` | 停止 Agent |
| `POST` | `/{id}/uninstall` | 卸载 Agent |

### 5.4 Jenkins 版本 — `JenkinsVersionController`（`/api/smp/jenkins-versions`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 全部版本 |
| `GET` | `/stable` | 稳定版 |
| `GET` | `/lts` | LTS 版 |
| `POST` | `/refresh` | 从镜像刷新（Jsoup 爬取 3 个源） |
| `GET` | `/stats` | 版本统计 |

### 5.5 Hadoop 集群 — `HadoopClusterController`（`/api/hadoop`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 查询全部 |
| `GET` | `/{id}` | 按 ID 查询 |
| `POST` | `/` | 创建节点（创建 node 时自动触发 Hosts 同步） |
| `PUT` | `/{id}` | 更新 |
| `DELETE` | `/{id}` | 删除 |
| `GET` | `/masters` | 查询 Master 节点 |
| `GET` | `/nodes` | 查询 Worker 节点 |
| `POST` | `/test-connection` | SSH 连通测试 |
| `POST` | `/{id}/deploy` | **异步部署** |
| `POST` | `/preview-script` | 预览部署脚本 |
| `GET` | `/{id}/status` | 检查状态 |
| `POST` | `/{id}/start` | 启动（start-dfs.sh / start-yarn.sh） |
| `POST` | `/{id}/stop` | 停止 |
| `POST` | `/{id}/restart` | 重启 |
| `GET` | `/{masterId}/health` | 集群健康 |
| `GET` | `/{masterId}/hdfs-status` | HDFS 报告（`hdfs dfsadmin -report`） |
| `GET` | `/{masterId}/yarn-status` | YARN 节点列表（`yarn node -list`） |

### 5.6 Hadoop 版本 — `HadoopVersionController`（`/api/hadoop/versions`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 全部版本 |
| `GET` | `/stable` | 稳定版（仅 3.x） |
| `POST` | `/refresh` | 从 Apache 镜像刷新 |
| `GET` | `/stats` | 版本统计 |

### 5.7 集群管理 — `ClusterManagementController`（`/api/cluster`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/{clusterType}/topology` | 获取统一拓扑（Hadoop/Jenkins → 统一节点格式） |
| `GET` | `/overview` | 全部集群概览 |
| `POST` | `/{clusterType}/{masterId}/sync-hosts` | 同步 /etc/hosts 到集群节点 |
| `GET` | `/{clusterType}/{masterId}/health` | 集群健康 |
| `GET` | `/supported-types` | 支持的类型（hadoop/jenkins 启用；redis/mysql/spark "即将推出"） |

### 5.8 Docker/Harbor — `DockerFileController` + `HarborRepositoryController`

**DockerFileController** (`/api/smp/dockerfile`):

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET/POST/PUT/DELETE` | `/` / `/{id}` | Dockerfile CRUD |
| `POST` | `/{id}/pipeline` | 创建 Jenkins Docker 构建 Pipeline |
| `PUT` | `/{uid}/update-push-status` | 更新推送状态 |

**HarborRepositoryController** (`/api/smp/harbor`):

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` / `/{id}` | 查询 |
| `POST` | `/create` | 创建（非标准路径） |
| `PUT` | `/update/{id}` | 更新 |
| `DELETE` | `/delete/{id}` | 删除 |

### 5.9 其他控制器

| 控制器 | 路径 | 关键端点 |
|--------|------|----------|
| `AlgorithmRepositoryController` | `/api/smp/algorithms` | CRUD + `GET /search`（多条件筛选：组织/算法/版本） |
| `FeatureOperatorController` | `/api/smp/feature-operators` | CRUD + `GET /search` |
| `BucketController` | `/api/smp/bucket` | CRUD（创建/删除同步操作 HDFS 目录） |
| `DataSourceController` | `/api/smp/datasource` | CRUD + `POST /test-connection` + `GET /{id}/databases\|tables\|columns` + `POST /{id}/preview` |
| `DatasetConfigController` | `/api/smp/dataset-config` | `GET /types\|zones` + `POST`(添加) + `DELETE`(删除) |
| `DeptTreeDataController` | `/api/smp/dept-tree-data` | `GET /` 获取组织树 |

---

## 六、后端服务层

### 6.1 WorkstationServiceImpl

- **AES 密钥**: `"SmpWorkstation16"`（16 字节 AES-128/ECB）
- **创建**: 生成 UUID → AES 加密密码/私钥 → SSH 自动检测主机名 → 初始状态 "pending"
- **更新**: 未提供密码时保留已有加密值
- **testConnection**: JSch SSH 连接 → 检测 OS 信息、CPU、RAM、磁盘、GPU（nvidia-smi）
- **checkStatus**: SSH 心跳 → 更新 "online"/"offline"，记录可用磁盘/RAM
- **getCredentials**: AES 解密密码/私钥 → 返回明文（供 MEP 等模块使用）
- **定时心跳**: `@Scheduled(fixedRate=300000)` 每 5 分钟检查所有 "online" 工作站
- **静态选项**: vendors/regions/osTypes/osVersions 为硬编码列表

### 6.2 JenkinsMasterServiceImpl（~1817 行，最大文件）

- **AES 密钥**: `"XnetMLopsJenkins"`
- **SSH 超时**: 30000ms
- **部署流程**（`@Async` + `ApplicationContext.getBean()` 代理模式）:
  1. 加载 `scripts/jenkins-master-linux.sh`（或 macos）
  2. 变量替换（版本/端口/管理员凭据/镜像源等）
  3. SFTP 上传脚本到远程 `/tmp/jenkins_deploy_*.sh`
  4. SSH 执行，实时流式写入 deploy_log
  5. 更新状态 "deployed" 或 "failed"
- **启停**: SSH 执行 `systemctl start/stop jenkins`
- **初始密码**: SSH 读取 `/var/lib/jenkins/secrets/initialAdminPassword`
- **凭据配置**: Jenkins HTTP API + CSRF crumb + Groovy 脚本（配置 Git/Harbor/SSH 凭据）
- **节点创建**: Jenkins API POST `/computer/doCreateItem`（JNLP 启动器）
- **节点密钥**: Jenkins Groovy 脚本获取 node secret
- **卸载**: SSH 停止服务 → 删除文件 → 重置状态 "pending"

### 6.3 JenkinsNodeServiceImpl

- 同 AES 密钥 `"XnetMLopsJenkins"`
- 同异步部署模式
- 加载 `scripts/jenkins-agent-linux.sh` 或 `jenkins-agent-macos.sh`
- **部署前**: 先在 Master 上创建节点 → 获取 secret → 再部署 Agent
- **Agent 启停**: SSH 命令（Linux: systemctl，macOS: launchctl）
- **状态检查**: SSH 检查 `java.*agent.jar` 或 `remoting.jar` 进程
- **脚本预览**: 生成部署脚本但不执行

### 6.4 JenkinsVersionServiceImpl / HadoopVersionServiceImpl

**版本爬取模式**（两者相同）:
1. 使用 Jsoup 爬取 HTML 目录页面（3 个镜像源：Apache 主站 + 清华 + 华为）
2. 解析 `<a href>` 链接匹配版本模式
3. 提取版本号、发布日期、下载 URL
4. Upsert 到数据库（新增/更新），标记最新版本
5. 记录同步日志含耗时

**Jenkins 镜像**: `updates.jenkins.io`, `mirrors.tuna.tsinghua.edu.cn`, `mirrors.huaweicloud.com`
**Hadoop 镜像**: `archive.apache.org` + 同上两个国内镜像，仅 3.x 为 "stable"

### 6.5 HadoopClusterServiceImpl

- 同 SSH 部署模式（JSch, SFTP 上传, 异步执行）
- **创建 node 类型时**: 自动触发异步 Hosts 同步（CompletableFuture）
- **部署**: 加载 `hadoop-master-linux.sh` 或 `hadoop-node-linux.sh`，广泛变量替换（端口/副本/块大小/YARN 资源）
- **Node 部署**: 查找 Master 的 host 设置 `MASTER_HOST`
- **跨云模式**: 生成 `/etc/hosts` 配置脚本，使用主机名代替 IP
- **启停**: `start-dfs.sh`/`start-yarn.sh`（Master）, `hdfs --daemon`（Node）
- **HDFS/YARN 状态**: SSH 执行 `hdfs dfsadmin -report` / `yarn node -list`

### 6.6 ClusterManagementServiceImpl

- **统一集群管理**: 跨 Hadoop 和 Jenkins
- **getClusterTopology**: 将 Hadoop/Jenkins 实体转换为统一节点格式（role/clusterType/extra）
- **实时状态刷新**: Hadoop 通过 SSH `jps` 命令检测（Master: NameNode/ResourceManager, Node: DataNode/NodeManager）
- **syncHadoopHosts**: 生成 `/etc/hosts` 条目 → SSH 部署到每个节点（备份→sed 删除旧条目→追加新块）
- **支持类型**: hadoop/jenkins 已启用；redis/mysql/spark "即将推出"

### 6.7 其他服务

| 服务 | 关键逻辑 |
|------|----------|
| `AlgorithmRepositoryServiceImpl` | CRUD + UUID + 重复检查（URL+算法名+版本）+ 组织筛选搜索 |
| `FeatureOperatorServiceImpl` | 同上模式 + `getOperatorsByCode()` |
| `BucketService` | CRUD + HDFS 目录管理（创建 `/xnet-mlops/{identifier}`，删除同步） |
| `DockerFileService` | CRUD + Harbor 验证 + `updatePushStatus` |
| `HarborRepositoryService` | 简单 CRUD 包装 |
| `JenkinsService` | Jenkins HTTP API（创建 Job/触发构建/获取状态）+ CSRF crumb |
| `DeptTreeDataService` | 查询组织层级 → 构建递归树（Tenant→Department→Team，过滤 status=1） |
| `PipelineParamEnricher` | 增强 Pipeline 参数（Harbor URL: `http://{url}:80`，凭据 ID: `"Synap-Xnet-Harbor"`） |
| `HadoopUtil` | HDFS FileSystem 单例 + 连接健康检查 + Kerberos 支持 + `dfs.client.use.datanode.hostname=true` |

---

## 七、Mapper/DAO 层

全部使用 **MyBatis 注解 SQL**（无 XML 文件），部分使用 `<script>` 内联动态 SQL。

### 7.1 关键模式

| 模式 | 说明 |
|------|------|
| **组织 JOIN** | AlgorithmRepository/FeatureOperator/Bucket Mapper 均 LEFT JOIN `sys_tenant`/`sys_department`/`sys_team` |
| **动态搜索** | `<script>` + `<if test>` 实现可选条件 WHERE |
| **自增主键** | 所有 INSERT 使用 `@Options(useGeneratedKeys=true, keyProperty="id")` |
| **ResultMap** | Workstation/DataSource Mapper 使用 `@Results`/`@ResultMap` 显式映射 camelCase |
| **条件更新** | HarborRepository 使用 `<if test>` 跳过 null 密码 |
| **同步日志** | JenkinsVersion/HadoopVersion 有 `insertSyncLog()` 方法 |

### 7.2 Mapper 与表映射

| Mapper | 表 |
|--------|-----|
| `WorkstationMapper` | `xnet_mlops_smp_workstation` |
| `JenkinsMasterMapper` | `xnet_mlops_smp_jenkins_masters` |
| `JenkinsNodeMapper` | `xnet_mlops_smp_jenkins_nodes` |
| `JenkinsVersionMapper` | `xnet_mlops_smp_jenkins_versions` |
| `HadoopClusterMapper` | `xnet_mlops_smp_hadoop_cluster` |
| `HadoopVersionMapper` | `xnet_mlops_smp_hadoop_versions` |
| `AlgorithmRepositoryMapper` | `xnet_mlops_smp_algorithms` |
| `FeatureOperatorMapper` | `xnet_mlops_smp_feature_operators` |
| `BucketMapper` | `xnet_mlops_sys_bucket` |
| `DataSourceMapper` | `xnet_mlops_sys_datasource` |
| `DockerFileMapper` | `xnet_mlops_smp_docker_file` |
| `HarborRepositoryMapper` | `xnet_mlops_smp_harbor_repository` |
| `DatasetConfigMapper` | `xnet_mlops_smp_dataset_config_info` |
| `DeptTreeDataMapper` | `xnet_mlops_sys_tenant` / `sys_department` / `sys_team` |

---

## 八、前端路由与页面

### 8.1 路由配置（`router/routes/modules/SMP.ts`）

**父路由**: `/SMP`，order: 4，authority: `['super', 'admin', 'user']`

**菜单可见路由（6 个）**:

| 路径 | 标题 | 说明 |
|------|------|------|
| `/SMP/DPPManage` | DPP-数据集配置项 | DPP 配置导航（4 张卡片） |
| `/SMP/MTPManage` | MTP-训练配置项 | MTP 配置导航（3 张卡片） |
| `/SMP/WorkstationManage` | 工作站点注册 | 工作站列表管理 |
| `/SMP/JenkinsDeployment` | 资源配置 | Jenkins 资源仪表盘 |
| `/SMP/HadoopDeployment` | Hadoop集群部署 | Hadoop 集群仪表盘 |
| `/SMP/ClusterManagement` | 集群管理 | SVG 集群拓扑可视化 |

**隐藏路由（11 个）**: DPP 配置子页面(4)、MTP 配置子页面(3)、Jenkins 部署向导(2)、Hadoop 部署向导(2)

### 8.2 关键页面组件

#### WorkstationManage/index.vue — 工作站列表
- **搜索**: 名称/厂商/区域/OS/状态
- **表格列**: 名称/主机名、SSH 连接（host:port）、OS、硬件（CPU/RAM/磁盘/GPU）、状态（彩色 Tag）、操作
- **操作**: 新增（模态框）、编辑、检查状态、删除
- **子组件**: `WorkstationForm`（含 SSH 测试，成功后自动填充硬件信息）

#### WorkstationSelector.vue — 可复用工作站选择器
- **两种模式**: 手动输入（host/port/username/password）或从已注册工作站选择
- **被使用**: Jenkins/Hadoop 部署向导的第 1 步

#### JenkinsDeployment/index.vue — Jenkins 资源仪表盘
- **布局**: 统计卡片（Master/Node 数量、运行/停止计数）+ Master/Node 数据表
- **自动刷新**: 30 秒间隔
- **操作**: 部署 Master/Node、启停/重启、查看日志、删除

#### JenkinsDeployment/DeployMaster.vue — 5 步部署向导
1. **连接配置**: WorkstationSelector（手动/选择）
2. **测试连接**: SSH 测试 + OS 自动检测
3. **Jenkins 配置**: 版本选择、端口、管理员凭据、插件
4. **部署配置**: Java home、内存设置
5. **执行部署**: 3 秒轮询日志，实时显示

#### HadoopDeployment/DeployMaster.vue — 5 步部署向导
1. **连接配置**: WorkstationSelector
2. **测试连接**: SSH 测试
3. **Hadoop 配置**: 版本、集群名、副本因子、块大小、端口
4. **部署配置**: Java home、HADOOP_HOME、内存
5. **执行部署**: 日志轮询

#### ClusterManagement — SVG 集群拓扑
- **ClusterTypeSelector**: 集群类型切换（hadoop/jenkins 可用；redis/mysql/spark 显示"即将支持"）
- **ClusterTopology**: SVG 1200x560 画布，Master 节点居中上方，Worker 弧形分布下方
- **ConnectionLine**: 贝塞尔曲线连接线（运行: 实线+动画，部署中: 虚线，停止: 暗色）
- **NodeIcon3D**: SVG 节点卡片（160x120px，角色徽章 M/W，状态脉冲动画）
- **NodeDetailDrawer**: 侧边抽屉（连接信息/集群关联/属性/操作按钮）

#### DPPManage — 配置中枢
- **index.vue**: 4 张导航卡片 → DatasetConfigManage / BucketConfigManage / DataSourceManage / FeatureOperatorGitManage
- **DatasetConfigManage**: 双表格（数据集类型 + 数据集区域）
- **BucketConfigManage**: 级联组织筛选（租户→部门→团队）+ 容量进度条
- **DataSourceManage**: 多数据库类型（MySQL/PG/Oracle/SQLServer/ClickHouse/Hive）+ 测试连接
- **FeatureOperatorGitManage**: Git 仓库管理 + 团队归属

#### MTPManage — 配置中枢
- **index.vue**: 3 张导航卡片 → GitManage / ImageManage / ImageCreate
- **GitManage**: 算法 Git 仓库 CRUD
- **ImageManage**: Harbor 仓库 CRUD
- **ImageCreate**: Dockerfile CRUD + 推送到 Harbor（Pipeline 创建）

---

## 九、前端 API 层

### 9.1 HTTP 客户端配置

**文件**: `api/request.ts`

6 个请求客户端，相同配置：
- `responseReturn: 'data'` + `defaultResponseInterceptor({dataField:'data'})` — **双重解包**
- 超时: 600,000ms
- 认证: `Bearer {token}` + `X-User-Id`
- Token 自动刷新: 401 时自动续期

### 9.2 SMP API 文件概览

**路由到 SMP 后端的 API**（使用 `smpRequestClient`）:

| 文件 | 端点前缀 | 函数数 |
|------|----------|--------|
| `workstation.ts` | `/smp/workstations` | 16 个函数 |
| `clusterManagement.ts` | `/smp/cluster` | 5 + 辅助函数 |
| `hadoopCluster.ts` | `/smp/hadoop` | 18 |
| `jenkinsMaster.ts` | `/smp/jenkins/masters` | 9 |
| `jenkinsNode.ts` | `/smp/jenkins/nodes` | 11 + 静态常量 |
| `harborRepository.ts` | `/smp/harbor-repositories` | 4 |
| `dockerFileManager.ts` | `/smp/docker-files` | 6 |
| `algorithmConfig.ts` | `/smp/algorithm-config` | 7 |
| `bucketConfig.ts` | `/smp/bucket-config` | 4 |
| `datasetConfig.ts` | `/smp/dataset-config` | 3 |
| `datasource.ts` | `/smp/datasources` | 11 |
| `deptTreeData.ts` | `/smp/dept-tree-data` | 1 |
| `featureOperatorConfig.ts` | `/smp/feature-operator-config` | 4 |

**路由到其他后端的 API**（共享 API 层特殊设计）:

| 文件 | 请求客户端 | 目标后端 |
|------|-----------|----------|
| `algorithm.ts` | `mtpRequestClient` | MTP (8183) |
| `algorithmManager.ts` | `mtpRequestClient` | MTP (8183) |
| `dataset.ts` | `dppRequestClient` | DPP |
| `datasetManager.ts` | `dppRequestClient` | DPP |
| `featureEngineering.ts` | `dppRequestClient` | DPP |
| `featureOperator.ts` | `dppRequestClient` | DPP |
| `trainjob.ts` | `mtpRequestClient` | MTP (8183) |
| `traintask.ts` | `mtpRequestClient` | MTP (8183) |

### 9.3 关键 API 函数

**workstation.ts** (16 函数):
- `getWorkstations(params?)` / `getWorkstationById(id)` — 查询
- `testConnection(data)` — SSH 测试 → 返回 OS/CPU/RAM/磁盘/GPU
- `createWorkstation(data)` / `updateWorkstation(id, data)` / `deleteWorkstation(id)` — CRUD
- `checkWorkstationStatus(id)` — 心跳检查
- `getOnlineWorkstations()` — 在线工作站列表（MEP 调用）
- `getWorkstationCredentials(id)` — 解密凭据
- `getVendorOptions()` / `getRegionOptions()` / `getOsTypeOptions()` / `getOsVersionOptions()` — 选项

**jenkinsNode.ts** 静态常量 `resourceSpecOptions`:
```typescript
[
  { label: '4核8G', value: '4c8g', cpu: 4, ram: 8 },
  { label: '8核16G', value: '8c16g', cpu: 8, ram: 16 },
  { label: '16核32G', value: '16c32g', cpu: 16, ram: 32 },
  { label: '32核64G', value: '32c64g', cpu: 32, ram: 64 },
  { label: '8核16G+1GPU', value: '8c16g-1gpu', cpu: 8, ram: 16, gpu: 1 },
  { label: '16核32G+2GPU', value: '16c32g-2gpu', cpu: 16, ram: 32, gpu: 2 },
  { label: '32核64G+4GPU', value: '32c64g-4gpu', cpu: 32, ram: 64, gpu: 4 },
]
```

---

## 十、业务流程

### 10.1 工作站注册流程
1. 用户导航至 `/SMP/WorkstationManage`
2. 点击"新增工作站" → `WorkstationForm` 模态框
3. 填写连接信息（host/port/username/password 或 privateKey）
4. 点击"测试连接" → SSH 检测 OS/CPU/RAM/磁盘/GPU → 自动填充硬件字段
5. 填写名称/主机名/厂商/类型/区域
6. 提交 → AES 加密凭据存储
7. 列表中可"检查状态"（SSH 心跳更新 online/offline）

### 10.2 Jenkins Master 部署流程
1. 导航至 `/SMP/JenkinsDeployment` 仪表盘
2. 点击"部署 Master" → 5 步向导
3. **Step 1**: WorkstationSelector 选择工作站
4. **Step 2**: SSH 连通测试 → 检测 OS
5. **Step 3**: Jenkins 版本选择/端口/管理员凭据/插件
6. **Step 4**: Java home/内存配置
7. **Step 5**: 执行部署（3s 日志轮询，实时显示）
8. 成功后获取初始密码 → 配置 Git/Harbor/SSH 凭据（Groovy 脚本）
9. 仪表盘 30 秒自动刷新状态

### 10.3 Jenkins Agent 部署流程
1. 从仪表盘或 JenkinsNodeManage 点击"部署节点" → 4 步向导
2. **Step 1**: WorkstationSelector + 资源配置（区域/容器类型 CCE/Docker/资源类型 CPU/GPU/规格）
3. **Step 2**: SSH 测试
4. **Step 3**: Jenkins URL/Agent 名/secret/工作目录/Java/Python 版本/可选组件
5. **Step 4**: 执行（部署前先在 Master 创建节点 → 获取 secret → 再部署 Agent）
6. 部署后支持：启停 Agent、刷新状态、查看日志

### 10.4 Hadoop 集群部署流程
1. 导航至 `/SMP/HadoopDeployment` 仪表盘
2. **Master 部署**（5 步向导）: 连接→测试→Hadoop 配置（版本/集群名/副本因子/块大小/端口）→部署配置→执行
3. **Node 部署**（单页表单）: 选工作站→选集群→选角色（DataNode/NodeManager）→部署
4. 创建 Node 时自动触发 Hosts 同步到所有集群节点
5. 支持跨云模式（使用主机名替代 IP）
6. 仪表盘 30 秒自动刷新

### 10.5 集群拓扑可视化流程
1. 导航至 `/SMP/ClusterManagement`
2. 选择集群类型（hadoop/jenkins；其他 "即将支持"）
3. SVG 渲染拓扑图（Master 居中上方，Worker 弧形下方）
4. 连接线动画反映状态（运行: 实线动画，部署中: 虚线，停止: 暗色）
5. 点击节点 → 侧边抽屉显示详情和操作按钮

### 10.6 Docker 镜像构建推送流程
1. 创建 Harbor 仓库配置
2. 创建 Dockerfile（base image + 内容 + 标签）
3. 关联 Harbor 仓库
4. "推送" → `PipelineParamEnricher` 增强参数 → `JenkinsService` 加载 Pipeline XML 模板 → 创建 Jenkins Pipeline Job → 触发构建
5. 推送状态追踪：pending → pushing → success/failed

### 10.7 版本同步流程（Jenkins/Hadoop）
1. 用户或定时器触发刷新
2. 尝试多个镜像 URL（Apache + 清华 + 华为）
3. Jsoup 解析 HTML 目录列表
4. 提取版本号/发布日期/下载 URL
5. Upsert 到数据库，标记最新版本
6. 记录同步日志含耗时

### 10.8 凭据跨模块获取流程
1. 外部模块（如 MEP）需要工作站 SSH 凭据
2. 调用 `GET /api/smp/workstations/{id}/credentials`
3. WorkstationService AES 解密密码/私钥
4. 返回解密后的凭据

---

## 十一、前端类型定义

### 11.1 核心实体类型（`types.ts`）

```typescript
interface ConfigItem { id: number; configType: string; name: string; description?: string; }
interface DeptTreeDataItem { id: number; name: string; parentId: number; level: number; children?: DeptTreeDataItem[]; }
interface ApiResponse<T> { code: number; data: T; message?: string; }
interface BucketItem { id: number; name: string; endpoint: string; accessKey: string; secretKey: string; tenantId?: number; tenantName?: string; ... }
interface DatasetItem { id: number; uid: string; name: string; datasetType: string; datasetZone: string; bucketId: number; storageMode?: string; ... }
interface AlgorithmItem { id: number; uid: string; name: string; algorithmType: string; framework: string; storageMode?: string; gitConfigId?: number; ... }
interface HdfsFile { name: string; path: string; isDirectory: boolean; size?: number; modificationTime?: string; permission?: string; owner?: string; }
interface DockerFile { id: number; name: string; baseImage: string; content: string; harborRepositoryId?: number; pushStatus?: string; }
interface HarborRepository { id: number; name: string; endpoint: string; project: string; repository: string; username?: string; password?: string; }
```

### 11.2 工作站类型（`workstation.ts`）

```typescript
interface Workstation {
  id: number; uid: string; name: string; hostname: string; host: string; port: number; username: string;
  status: string; osType: string; osVersion?: string; vendor?: string; serverType?: string; region?: string;
  cpuCores?: number; memoryTotal?: number; diskTotal?: number; gpuCount?: number; gpuModel?: string; gpuMemory?: number;
}
interface TestConnectionResult { success: boolean; message: string; osType?: string; cpuCores?: number; memoryTotal?: number; diskTotal?: number; gpuInfo?: any; }
interface WorkstationCredentials { host: string; port: number; username: string; password?: string; privateKey?: string; }
```

### 11.3 集群管理类型（`clusterManagement.ts`）

```typescript
type ClusterType = 'hadoop' | 'jenkins' | 'mysql' | 'redis' | 'spark';
type NodeRole = 'master' | 'worker' | 'namenode' | 'datanode' | 'resourcemanager' | 'nodemanager';
type NodeStatus = 'running' | 'stopped' | 'failed' | 'deploying' | 'deployed' | 'unknown';
interface ClusterNode { id: number; uid: string; name: string; host: string; port: number; role: NodeRole; status: NodeStatus; clusterType: ClusterType; masterId?: number; masterHost?: string; properties?: Record<string, any>; }
interface ClusterStats { totalNodes: number; runningNodes: number; stoppedNodes: number; failedNodes: number; masterCount: number; workerCount: number; }
interface ClusterTopology { clusterType: ClusterType; nodes: ClusterNode[]; stats: ClusterStats; }
interface HostsSyncResult { success: boolean; syncedNodes: number; failedNodes: number; errors?: string[]; }
interface ClusterHealth { status: string; checks: Array<{ name: string; status: string; message?: string; }>; }
```

### 11.4 Jenkins/Pipeline 类型

```typescript
interface JenkinsBuildStatus { building: boolean; result?: string; duration?: number; timestamp?: number; }
interface PipelineStage { name: string; status: string; startTime?: string; endTime?: string; duration?: number; log?: string; steps?: Array<{ name: string; status: string; log?: string; }>; }
interface TaskFormState { step1: TaskFormStep1; step2: TaskFormStep2; step3: TaskFormStep3; step4: TaskFormStep4; }
```

---

## 十二、跨模块依赖

### 12.1 SMP 提供给其他模块的服务

SMP 是平台基础设施层，**不依赖**其他 XnetMLops 模块，但**被所有模块依赖**：

| 消费模块 | 使用的 SMP 服务 |
|----------|----------------|
| **全局** | `getOrganizationTree()` — basic.vue 布局 provide/inject 到所有页面 |
| **MEP** | `getOnlineWorkstations()` + `Workstation` 类型 — OpenClaw 部署目标选择 |
| | `GET /api/smp/workstations/{id}/credentials` — SSH 凭据获取 |
| **MTP** (9 文件) | `algorithm` CRUD + `algorithmManager` HDFS 操作 + `algorithmConfig` Git 配置 |
| | `traintask` CRUD/调度/Pipeline + `jenkinsNode` 节点查询 |
| | `dockerFileManager` Docker 镜像 + `dataset` 数据集 + `bucketConfig` 存储桶 + `datasetConfig` 配置 |
| **DPP** (6 文件) | `dataset` CRUD + `datasetManager` HDFS 操作 + `bucketConfig` + `datasetConfig` |
| | `featureEngineering` CRUD/执行/调度 + `featureOperator` 算子 |
| | `datasource` 数据源 Schema 自省 + `dockerFileManager` |

### 12.2 SMP 依赖的外部系统

| 系统 | 用途 |
|------|------|
| MySQL (192.168.1.5:3306) | 共享数据库 |
| HDFS (192.168.1.5:8020) | 存储桶目录管理 |
| Jenkins 实例 | Pipeline/Job 管理（由 SMP 自身部署） |
| Harbor 注册中心 | Docker 镜像管理 |
| Apache/清华/华为镜像站 | 版本目录爬取 |

### 12.3 跨模块 API 层依赖图

```
SMP API Layer (views/SMP/api/)
  |
  |-- 全局: basic.vue (组织树 → provide/inject → 所有页面)
  |
  |-- MEP: openclaw/create.vue (工作站选择)
  |
  |-- MTP:
  |     |-- algorithm/ (4 文件): algorithm CRUD, algorithmManager HDFS, algorithmConfig, bucketConfig, datasetConfig
  |     |-- train/ (5 文件): traintask, jenkinsNode, dockerFileManager, algorithm, dataset
  |
  |-- DPP:
        |-- dataset/ (4 文件): dataset CRUD, datasetManager HDFS, bucketConfig, datasetConfig
        |-- FeatureEngineering/ (2 文件): featureEngineering, featureOperator, datasource, bucketConfig, dockerFileManager
```

---

## 十三、Shell 脚本模板

### 13.1 Jenkins Master 部署（`jenkins-master-linux.sh`，~500 行）
- 变量替换：版本/端口/管理员凭据/Java 版本/镜像源
- 安装 Java → 下载 Jenkins WAR → 配置 systemd → 启动 → 等待就绪

### 13.2 Jenkins Agent 部署（`jenkins-agent-linux.sh`，~800 行）
- 变量替换：Jenkins URL/Agent 名/secret/工作目录/Java/Python/可选组件
- 安装 Java → 下载 Agent JAR → 配置 systemd → 启动连接 Master

### 13.3 Jenkins Agent macOS（`jenkins-agent-macos.sh`）
- 同上但使用 launchctl 代替 systemd

### 13.4 Hadoop Master/Node（`hadoop-master-linux.sh` / `hadoop-node-linux.sh`）
- 广泛变量替换：版本/端口/副本因子/块大小/YARN 资源/Java home
- 安装 Java → 下载 Hadoop → 配置（core-site.xml/hdfs-site.xml/yarn-site.xml/mapred-site.xml）→ 格式化 NameNode → 启动
- Node 脚本使用 Master host 作为 NameNode/ResourceManager 地址

**公共模式**: 模板变量 `${VAR}` 和 `${VAR:-default}` 格式；Regex 替换默认值格式，简单 `.replace()` 处理直接变量；SFTP 上传时 `\r\n` → `\n` 转换。

---

## 十四、响应格式与注意事项

### 14.1 后端响应格式

标准响应：
```json
{
  "code": 0,
  "message": "success message",
  "data": <payload>,
  "error": "null"
}
```

**注意**: `"error": "null"` 是字符串 "null"，不是 null 值（使用 `Map.of()` 构建）。

`JenkinsMasterController` 使用 `buildResponse()` 辅助方法保证一致性，其他控制器格式不完全统一。

### 14.2 前端状态管理

- **无 SMP 专用 Pinia Store** — 所有状态为组件局部
- **全局组织树**: `basic.vue` 通过 Vue provide/inject 提供给所有子组件
- **自动刷新轮询**: Jenkins 仪表盘(30s)、Hadoop 仪表盘(30s)、Jenkins 节点列表(30s)
- **部署日志轮询**: 3 秒间隔（部署执行步骤中）
- **构建状态轮询**: 5 秒间隔（特征工程处理中/训练任务活跃中）

### 14.3 重要模式与已知问题

| 模式/问题 | 说明 |
|-----------|------|
| **AES 加密双密钥** | `"SmpWorkstation16"`（工作站）vs `"XnetMLopsJenkins"`（Jenkins/Agent） |
| **ECB 模式** | 所有 AES 加密使用 ECB（无 IV），安全性较弱但功能正常 |
| **实体命名不一致** | Workstation/DataSource/HadoopCluster 用 camelCase；JenkinsMaster/Node/Algorithm/Bucket 用 snake_case |
| **异步部署代理模式** | 所有部署服务使用 `@Async` + `applicationContext.getBean()` 解决自调用问题 |
| **SQL 注入风险** | `DataSourceController.preview` 端点直接执行用户提供的 SQL（`statement.execute(sql)`） |
| **硬编码值** | Harbor 凭据 ID: `"Synap-Xnet-Harbor"`; Harbor URL: `"http://" + url + ":80"`; Jenkins Docker push 路径 |
| **未完成功能** | Redis/MySQL/Spark 集群类型返回 "coming soon"；`JenkinsTemplateService` 仅有 `escapeXml()`；无分页支持；无 Spring Security |
| **Shell 脚本 CRLF** | 上传前 `\r\n` → `\n` 转换（Windows → Linux） |
| **版本爬取多源回退** | 3 个镜像源，30s 超时，浏览器 User-Agent |
| **组织 JOIN 模式** | Algorithm/FeatureOperator/Bucket 查询时 LEFT JOIN 租户/部门/团队表获取名称 |
| **前端 API 层特殊设计** | `views/SMP/api/` 包含路由到不同后端的 API（smpRequestClient/mtpRequestClient/dppRequestClient） |
