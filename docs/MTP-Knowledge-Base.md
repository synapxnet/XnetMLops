# MTP（模型训练平台）模块知识库

> **版本**: 1.0
> **更新日期**: 2026-02-12
> **适用范围**: XnetMLops 平台 MTP 模块前后端完整架构
> **说明**: 本文档为 RAG 知识库源文件，涵盖 MTP 模块的架构、API、数据模型、业务流程等全部细节

---

## 一、模块概述

### 1.1 基本信息

| 属性 | 值 |
|------|-----|
| 模块名称 | MTP (Model Training Platform) 模型训练平台 |
| 后端服务 | `mlops-mtp-service` (Spring Boot 3.4.6, Java 17) |
| 服务端口 | 8183 |
| 数据库 | MySQL `XnetMLops` (192.168.1.5:3306) |
| 存储系统 | HDFS (hdfs://192.168.1.5:8020) |
| CI/CD | Jenkins (192.168.1.5:8080) |
| 缓存 | Redis (192.168.1.5:6379)，用于构建状态缓存和调度配置存储 |
| 前端路径 | `XnetMLops-web/apps/web-antd/src/views/MTP/` |
| API 路径 | `XnetMLops-web/apps/web-antd/src/views/SMP/api/` (共享 API 层) |
| 路由前缀 | `/MTP/` |
| 包基础路径 | `com.synapxnet.mlopsmtpservice` |

### 1.2 三大子系统

MTP 模块由三个核心子系统组成：

| 子系统 | 功能 | 状态 |
|--------|------|------|
| 算法管理 | 算法 CRUD、HDFS 文件存储、CAS 云仓对接、分块上传 | 已完成 |
| 训练任务管理 | 4步向导创建、Jenkins Pipeline 执行、实时监控、定时调度 | 已完成 |
| 模型输出管理 | 输出模型列表、创建 | 部分完成（静态数据） |

### 1.3 技术栈

**后端:**
- Spring Boot 3.4.6 + MyBatis 3.0.4
- Hadoop Client 3.3.4 (HDFS)
- Jenkins REST API (直接 HTTP 调用)
- Spring Data Redis (构建状态 + 调度配置)
- Quartz 2.3.2 (Cron 表达式解析)
- Jackson Databind + org.json (JSON 处理)
- Lombok (代码简化)
- Fabric8 Kubernetes Client 6.3.1 (已声明，尚未实现)

**前端:**
- Vue 3 Composition API + TypeScript
- Ant Design Vue (UI 组件库)
- Vben Admin Pro 框架
- 多服务请求客户端: `mtpRequestClient` (MTP) / `smpRequestClient` (SMP)

---

## 二、系统架构

### 2.1 后端架构

```
mlops-mtp-service/src/main/java/com/synapxnet/mlopsmtpservice/
├── MlopsMtpServiceApplication.java          # Spring Boot 入口
├── config/
│   ├── RedisConfig.java                     # Redis 模板配置
│   └── PipelineParamEnricher.java           # Pipeline 参数增强器
├── controller/
│   ├── AlgorithmController.java             # 算法 CRUD + HDFS 文件管理
│   ├── AlgorithmManagerController.java      # 算法文件浏览器 (HDFS)
│   ├── JobController.java                   # Jenkins Pipeline 创建/执行/调度
│   ├── TrainTaskController.java             # 训练任务 CRUD
│   └── UploadController.java               # 通用文件上传
├── entity/
│   ├── Algorithm.java                       # MTP 本地算法元数据
│   ├── AlgorithmRepository.java             # SMP 云算法仓库 (JOIN)
│   ├── DockerFile.java                      # Docker 镜像 (SMP 表)
│   ├── HarborRepository.java               # Harbor 仓库 (SMP 表)
│   ├── HdfsFile.java                        # HDFS 文件 DTO
│   ├── JenkinsBuildStatus.java              # Jenkins 构建状态 (Redis)
│   ├── PipelineConfigParams.java            # Pipeline 模板参数 DTO
│   ├── ScheduleConfig.java                  # 调度配置 DTO
│   ├── TaskCustomVariable.java              # 任务自定义变量
│   ├── TaskDataset.java                     # 任务数据集关联
│   ├── TaskInfo.java                        # 作业执行记录
│   └── TrainTask.java                       # 核心训练任务实体
├── exception/
│   └── JobNotFoundException.java            # 自定义异常
├── mapper/
│   ├── AlgorithmMapper.java                 # 算法 + SMP JOIN
│   ├── TaskCustomVariableMapper.java        # 自定义变量
│   ├── TaskDatasetMapper.java               # 数据集关联
│   ├── TaskInfoMapper.java                  # 一次性作业记录
│   ├── TaskScheduleInfoMapper.java          # 定时作业记录
│   └── TrainTaskMapper.java                 # 训练任务 + SMP Docker/Harbor
├── service/
│   ├── AlgorithmService.java                # 算法接口
│   ├── AlgorithmServiceImpl.java            # 算法实现
│   ├── HdfsService.java                     # HDFS 接口
│   ├── HdfsServiceImpl.java                 # HDFS 实现
│   ├── JenkinsService.java                  # 一次性 Pipeline
│   ├── JenkinsScheduleService.java          # 定时 Pipeline
│   ├── JenkinsTemplateService.java          # 模板加载替换
│   ├── ScheduleService.java                 # Cron 表达式生成
│   ├── TaskInfoService.java                 # 一次性作业 CRUD
│   ├── TaskScheduleInfoService.java         # 定时作业 CRUD
│   └── TrainTaskService.java               # 训练任务 CRUD
└── Utils/
    └── HadoopUtil.java                      # HDFS 连接管理
```

### 2.2 前端架构

```
XnetMLops-web/apps/web-antd/src/
├── views/MTP/
│   ├── algorithm/
│   │   ├── index.vue                        # 算法列表页
│   │   ├── algorithmCreate.vue              # 新增算法
│   │   ├── algorithmModify.vue              # 修改算法
│   │   ├── algorithmFileManager.vue         # HDFS 文件管理器
│   │   └── table-data.ts                    # Mock 数据
│   ├── train/
│   │   ├── index.vue                        # 训练任务列表 (~2600行)
│   │   ├── task/TaskCreate.vue              # 4步创建向导
│   │   ├── step/Step1.vue                   # 基础信息
│   │   ├── step/Step2.vue                   # 算法与数据集
│   │   ├── step/Step3.vue                   # 自定义参数
│   │   ├── step/Step4.vue                   # 调度与输出
│   │   ├── job/JobManager.vue               # Pipeline 可视化
│   │   └── taskcommon/
│   │       ├── task.ts                      # TypeScript 类型定义
│   │       └── form-styles.scss             # 共享表单样式
│   └── output/
│       ├── index.vue                        # 输出列表 (Mock)
│       └── outputCreate.vue                 # 新增输出 (Mock)
├── views/SMP/
│   ├── api/
│   │   ├── algorithm.ts                     # 算法 CRUD API
│   │   ├── algorithmManager.ts              # HDFS 文件 API
│   │   ├── algorithmConfig.ts               # CAS 算法 API
│   │   ├── traintask.ts                     # 训练任务 + 调度 API
│   │   ├── trainjob.ts                      # Jenkins Pipeline API
│   │   ├── jenkinsNode.ts                   # Jenkins 节点 API
│   │   └── types.ts                         # 共享类型定义
│   └── MTPManage/
│       ├── index.vue                        # MTP 管理仪表板
│       ├── ImageCreate.vue                  # Docker 镜像创建
│       ├── ImageManage.vue                  # Harbor 仓库管理
│       └── GitManage.vue                    # Git 云仓管理
└── router/routes/modules/MTP.ts             # MTP 路由配置
```

### 2.3 路由结构

| 路由名 | 路径 | 组件 | 菜单可见 | 说明 |
|--------|------|------|----------|------|
| `MTP:algorithm` | `/MTP/algorithm/index` | `algorithm/index.vue` | 是 | 算法列表 |
| `MTP:modeltrain` | `/MTP/train/index` | `train/index.vue` | 是 | 训练任务列表 |
| `MTP:modeloutput:index` | `/MTP/modeloutput/index` | `output/index.vue` | 是 | 模型输出列表 |
| `MTP:algorithm:algorithmCreate` | `/MTP/algorithm/algorithmCreate` | `algorithmCreate.vue` | 隐藏 | 新增算法 |
| `MTP:algorithm:algorithmModify` | `/MTP/algorithm/algorithmModify` | `algorithmModify.vue` | 隐藏 | 修改算法 |
| `MTP:algorithm:algorithmFileManager` | `/MTP/algorithm/algorithmFileManager` | `algorithmFileManager.vue` | 隐藏 | 文件管理器 |
| `MTP:train:task` | `/MTP/train/task` | `task/TaskCreate.vue` | 隐藏 | 创建/编辑任务 |
| `MTP:train:job` | `/MTP/train/job` | `job/JobManager.vue` | 隐藏 | Pipeline 查看 |
| `MTP:modeloutput:outputcreate` | `/MTP/modeloutput/outputcreate` | `outputCreate.vue` | 隐藏 | 新增输出 |

---

## 三、数据模型

### 3.1 MTP 自有表

#### xnet_mlops_mtp_algorithms — 算法元数据

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (PK, auto) | 自增主键 |
| `uid` | String | 生成 UID，格式 `ALG-XXXXXXXX` |
| `userId` | String | 创建者用户 ID |
| `algorithm_name` | String | 算法名称（唯一） |
| `version` | String | 版本号，默认 `1.0.0` |
| `zone` | String | 数据区域 |
| `zone_label` | String | 区域显示标签 |
| `encryption` | boolean | 是否加密 |
| `subdata_area` | String | 子数据区 |
| `bucket_name` | String | HDFS 存储桶名称 |
| `bucket_identifier` | String | HDFS 存储桶标识 |
| `team_uid` | String | 所属团队 UID |
| `team_name` | String | 团队名称 |
| `description` | String | 描述 |
| `tenant_uid` | String | 租户 UID |
| `dept_uid` | String | 部门 UID |
| `level` | int | 级别 |
| `is_CAS` | Boolean | CAS（云算法仓库）模式标记 |
| `cloud_algorithm_id` | String | SMP 云算法引用 ID |
| `created_at` | Date | 创建时间 |
| `updated_at` | Date | 更新时间 |
| (瞬态) `tempFilePath` | String | 临时 HDFS 上传路径 |

#### xnet_mlops_mtp_train_task — 训练任务

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (PK, auto) | 自增主键 |
| `uid` | String | UUID |
| `userId` | String | 创建者用户 ID |
| `tenant_uid` | String | 租户 UID |
| `task_name` | String | 任务名称（租户内唯一） |
| `task_type` | String | 任务类型 (训练任务/聚类任务) |
| `encryption` | String | 加密标记 |
| `task_zone` | String | 任务区域 |
| `pod_type` | String | 容器类型 (CCE/Docker) |
| `resources` | String | 资源规格 |
| `train_type` | String | 训练类型 (CPU/单卡GPU/多卡GPU) |
| `image_uid` | String | Docker 镜像 UID |
| `image` | String | 镜像名称 |
| `description` | String | 描述 |
| `algorithm_uid` | String | 算法 UID 引用 |
| `algorithm_name` | String | 算法名称 |
| `algorithm_version` | String | 算法版本 |
| `task_route` | String | Python 入口脚本路径 |
| `train_config_content` | String | 训练配置文件内容 |
| `train_config_format` | String | 配置格式 (yaml/json/python/txt) |
| `notification_config` | String | 通知配置 JSON |
| `output_config` | String | 输出配置 JSON（含 outputPath） |
| `schedule_config` | String | 调度配置 JSON |
| `created_at` | Date | 创建时间 |
| `updated_at` | Date | 更新时间 |
| (1:N) `datasets` | List\<TaskDataset\> | 关联数据集 |
| (1:N) `custom_variables` | List\<TaskCustomVariable\> | 自定义变量 |

#### xnet_mlops_mtp_task_dataset — 任务数据集关联

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String (PK) | 自增主键 |
| `uid` | String | UUID |
| `task_uid` | String (FK) | 父任务 UID |
| `dataset_id` | String | 数据集 ID |
| `dataset_uid` | String | 数据集 UID (HDFS 路径段) |
| `dataset_name` | String | 显示名称 |
| `dataset_file` | String | 文件名 |
| `bucket_identifier` | String | HDFS 存储桶标识 |

#### xnet_mlops_mtp_task_custom_variable — 自定义变量

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (PK) | 自增主键 |
| `uid` | String | UUID |
| `task_uid` | String (FK) | 父任务 UID |
| `name` | String | 变量名 |
| `value` | String | 变量值 |

#### xnet_mlops_mtp_train_info — 一次性作业记录

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String (PK) | 自增主键 |
| `uid` | String | UUID |
| `task_uid` | String (FK) | 父训练任务 UID |
| `job_uid` | String | Jenkins 作业名/UID |
| `job_status` | String | 状态: QUEUED/IN_PROGRESS/SUCCESS/FAILURE/ABORTED/ERROR/TIMEOUT |
| `job_content` | String | 序列化的 JenkinsBuildStatus JSON |
| `start_at` | Date | 作业开始时间 |
| `end_at` | Date | 作业结束时间 |
| `schedule_active` | int | 0=一次性, 1=定时 |

#### xnet_mlops_mtp_train_schedule_info — 定时作业记录

与 `xnet_mlops_mtp_train_info` 结构完全相同，用于存储定时调度的作业记录。

### 3.2 跨模块引用表（只读，来自 SMP）

| 表名 | 说明 |
|------|------|
| `xnet_mlops_smp_algorithms` | 云算法仓库（Git URL、加密 token、版本） |
| `xnet_mlops_smp_docker_file` | Docker 镜像定义（名称、标签、推送状态、Harbor UID） |
| `xnet_mlops_smp_harbor_repository` | Harbor 仓库连接（URL、用户名、密码） |
| `xnet_mlops_sys_tenant` | 租户信息（JOIN 获取显示名称） |
| `xnet_mlops_sys_department` | 部门信息（JOIN 获取显示名称） |
| `xnet_mlops_sys_team` | 团队信息（JOIN 获取显示名称） |

### 3.3 DTO/非持久化实体

#### JenkinsBuildStatus (Redis 可序列化)

```java
private String parentTaskUID;         // 父训练任务 UID
private String jobName;               // Jenkins 作业名
private String buildUrl;              // Jenkins 构建 URL
private String queueUrl;              // Jenkins 队列 URL
private List<StageInfo> stages;       // Pipeline 阶段列表
private String overallStatus;         // 总体状态
private String consoleOutput;         // 控制台输出
private Date startTime;               // 开始时间
private Date endTime;                 // 结束时间

// StageInfo 内嵌类:
private String stageName;             // 阶段名
private String status;                // SUCCESS/FAILED/IN_PROGRESS/NOT_STARTED
private long durationMillis;          // 持续时间
private Date startTime;               // 阶段开始时间
```

#### PipelineConfigParams (Jenkins 模板参数)

```java
private String description;           // 作业描述
private String defaultBranch;         // Git 分支
private String defaultEnvironment;    // 部署环境
private String datasetPath;           // HDFS 数据集路径
private String datasetFolder;         // 本地数据集文件夹
private String gitRepoUrl;            // Git 仓库 URL
private String gitRepoPassword;       // Base64 编码的 Git token
private String credentialsId;         // Jenkins 凭证 ID
private String pythonEntryPoint;      // Python 入口脚本
private String containerName;         // Docker 容器名
private String modelOutputPath;       // 模型输出路径
private String dockerImageName;       // Docker 镜像名
private String dockerImageTags;       // Docker 镜像标签
private String harborUrl;             // Harbor URL
private String harborCredentialsId;   // Harbor Jenkins 凭证
private String scheduleConfig;        // 调度配置 JSON
```

#### ScheduleConfig (调度配置)

```java
private Boolean isActive;             // 是否启用
private String intervalType;          // daily/weekly/hourly/monthly/interval
private String dailyTime;             // "HH:mm"
private List<String> weeklyDays;      // 星期几
private String weeklyTime;            // 每周执行时间
private String hourlyMinute;          // 每小时第几分钟
private String cronExpression;        // 手动 cron 表达式
private Integer intervalDuration;     // 间隔时长
private String intervalUnit;          // hours/minutes/days
private String offsetTime;            // 偏移时间
private List<String> dateRange;       // 日期范围
```

---

## 四、后端 API 完整清单

所有 Controller 使用基础路径 `/api/mtp`，统一响应格式:
```json
{ "code": 0, "message": "success", "data": <payload>, "error": "null" }
```

### 4.1 AlgorithmController — 算法 CRUD

| 方法 | URL | 参数 | 返回 | 说明 |
|------|-----|------|------|------|
| POST | `/algorithms-create` | `@RequestBody Algorithm` | `Algorithm` | 创建算法，自动移动 HDFS 临时文件到 `/algorithms/{bucket}/{uid}/`，ZIP 自动解压 |
| GET | `/algorithms` | -- | `List<Algorithm>` | 列出所有算法 |
| GET | `/algorithms/{id}` | `@PathVariable Long id` | `Algorithm` | 按 ID 获取算法 |
| PUT | `/algorithms/{id}` | `@PathVariable Long id`, `@RequestBody Algorithm` | `Algorithm` | 更新算法（名称唯一性校验） |
| DELETE | `/algorithms/{id}` | `@PathVariable Long id` | -- | 删除算法 + HDFS 目录 `/algorithms/{bucket}/{uid}` |

### 4.2 AlgorithmManagerController — 文件管理

基础路径: `/api/mtp/algorithms/{algorithmId}`

| 方法 | URL | 参数 | 返回 | 说明 |
|------|-----|------|------|------|
| GET | `/files` | `@RequestParam path` (默认 `/`) | `List<HdfsFile>` + `isCAS` | 列出 HDFS 目录内容 |
| POST | `/upload` | `@RequestParam file, path` | `{filePath}` | 上传文件到 HDFS（CAS 模式阻止） |
| GET | `/download` | `@RequestParam filePath` | 二进制流 | 下载 HDFS 文件 |
| DELETE | `/delete` | `@RequestBody {path}` | -- | 删除文件/目录（CAS 模式阻止） |
| POST | `/mkdir` | `@RequestBody {path, folderName}` | -- | 创建目录（CAS 模式阻止） |

### 4.3 TrainTaskController — 训练任务 CRUD

| 方法 | URL | 参数 | 返回 | 说明 |
|------|-----|------|------|------|
| POST | `/tasks-creat` | `@RequestBody TrainTask`, `@RequestHeader X-Tenant-Uid, X-User-Id` | `TrainTask` | 创建任务（含数据集 + 自定义变量） |
| PUT | `/tasks/{uid}` | `@PathVariable uid`, `@RequestBody TrainTask`, headers | `TrainTask` | 更新任务 |
| GET | `/tasks/{uid}` | `@PathVariable uid`, `@RequestHeader X-Tenant-Uid` | `TrainTask` | 获取任务（含嵌套数据集 + 变量） |
| GET | `/tasks` | `@RequestHeader X-Tenant-Uid` | `List<TrainTask>` | 列出租户所有任务 |
| DELETE | `/tasks/{uid}` | `@PathVariable uid`, `@RequestHeader X-Tenant-Uid` | -- | 级联删除：数据集、变量、任务 |

### 4.4 JobController — 一次性 Pipeline

| 方法 | URL | 参数 | 返回 | 说明 |
|------|-----|------|------|------|
| POST | `/pipeline` | `@RequestParam taskUID`, `@RequestHeader X-Tenant-Uid`, `@RequestBody PipelineConfigParams` | `{jobName, queueUrl, monitorUrl}` | 创建 Jenkins Pipeline，参数增强，触发构建，启动异步跟踪 |
| GET | `/build/status/{jobName}` | `@PathVariable jobName`, `@RequestParam includeConsole` (默认 false) | 构建状态 + 阶段 | 从 Jenkins 实时获取构建状态 |
| GET | `/task/{taskUid}/jobs` | `@PathVariable taskUid` | `List<{jobUid, jobStatus, startAt, endAt}>` | 列出任务的所有作业 |
| GET | `/job/{jobUid}` | `@PathVariable jobUid` | `{jobUid, jobStatus, jobContent, ...}` | 获取单个作业详情 |
| DELETE | `/job/{jobUid}` | `@PathVariable jobUid`, `@RequestParam forceStop` (默认 false) | `{jenkinsDeleted, dbDeleted}` | 停止构建 + 删除 Jenkins 作业 + DB + Redis |
| DELETE | `/task/{taskUid}/jobs` | `@PathVariable taskUid` | 批量结果 | 批量删除任务的所有作业 |

### 4.5 JobController — 定时 Pipeline

| 方法 | URL | 参数 | 返回 | 说明 |
|------|-----|------|------|------|
| POST | `/schedule/pipeline` | `@RequestParam taskUID`, headers, `@RequestBody {scheduleConfig, ...}` | `{jobName, scheduleActive, cronExpression}` | 创建定时 Jenkins Pipeline |
| DELETE | `/schedule/stop/{jobName}` | `@PathVariable jobName`, `@RequestParam deleteJob` (默认 false) | `{buildsStopped, scheduleConfigDeleted}` | 停止定时 Pipeline |
| GET | `/schedule/status/{jobName}` | `@PathVariable jobName` | `{scheduleConfig, nextBuildNumber, jobHistoryBuild}` | 获取调度状态 + 全部构建历史 |
| PUT | `/schedule/update/{jobName}` | `@PathVariable jobName`, `@RequestBody newScheduleConfig` | 更新结果 | 更新调度：停止 → 更新 Redis 配置 → 重启 |
| GET | `/schedule/{taskUid}/jobs` | `@PathVariable taskUid` | `List<job>` | 列出任务的所有定时作业 |
| GET | `/schedule/{jobUid}` | `@PathVariable jobUid` | 作业详情 | 获取定时作业详情 |

### 4.6 UploadController — 通用上传

| 方法 | URL | 参数 | 返回 | 说明 |
|------|-----|------|------|------|
| POST | `/upload` | `@RequestParam file` (MultipartFile) | `String` (HDFS 路径) | 上传到 HDFS `/temp/{uuid}_{originalName}.temp` |

### 4.7 分块上传 API (前端内联)

| 端点 | 方法 | URL | 说明 |
|------|------|-----|------|
| 初始化 | POST | `/mtp/init-chunked-upload` | 初始化分块上传 |
| 上传块 | POST | `/mtp/upload-chunk` | 上传单个块 |
| 完成 | POST | `/mtp/complete-chunked-upload` | 完成上传 |
| 取消 | POST | `/mtp/cancel-chunked-upload` | 取消上传 |

---

## 五、Service 层详细方法

### 5.1 AlgorithmService

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `createAlgorithm` | `Algorithm createAlgorithm(Algorithm)` | 验证名称非空，`countByAlgorithmName` 检查唯一性，生成 UID `ALG-XXXXXXXX`，默认版本 `1.0.0`，插入返回 |
| `getAllAlgorithms` | `List<Algorithm> getAllAlgorithms()` | `findAll()` |
| `getAlgorithmById` | `Algorithm getAlgorithmById(Long id)` | `findById(id)` 或抛 `RuntimeException` |
| `getAlgorithmByUID` | `Algorithm getAlgorithmByUID(String uid)` | 从 MTP 表查询 |
| `updateAlgorithm` | `Algorithm updateAlgorithm(Long, Algorithm)` | 检查存在，验证名称，名称变更时检查唯一性 |
| `deleteAlgorithm` | `void deleteAlgorithm(Long)` | 检查存在，`deleteById` |
| `getAlgorithmByUid` | `AlgorithmRepository getAlgorithmByUid(String)` | 从 SMP 表 JOIN 租户/部门/团队获取 |

### 5.2 HdfsService

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `uploadToTemp` | `String uploadToTemp(MultipartFile)` | 生成 `{uuid}_{originalName}.temp`，写入 HDFS `/temp/`，返回路径 |
| `moveAndProcessFile` | `void moveAndProcessFile(String, String)` | `fs.rename()` 移动文件；如果是 `.zip` 则解压到 HDFS 目录，删除原始 ZIP |
| `deleteDirectory` | `void deleteDirectory(String)` | 递归删除 HDFS 目录 |
| `getBucketPath` | `String getBucketPath(String uid)` | 返回 `/buckets/{uid}` |

### 5.3 TrainTaskService

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `createOrUpdateTask` | `TrainTask createOrUpdateTask(TrainTask, userId, tenantUid)` | UID 存在则更新 + 删除旧数据集/变量；否则生成新 UUID 插入。任务名租户内唯一 |
| `findByUidAndTenantUid` | `Optional<TrainTask>` | 返回任务 + `@Many` 嵌套数据集 + 变量 |
| `findImageByUid` | `Optional<DockerFile>` | 读 SMP `xnet_mlops_smp_docker_file` |
| `findByHarborUid` | `Optional<HarborRepository>` | 读 SMP `xnet_mlops_smp_harbor_repository` |
| `findAllByTenantUid` | `List<TrainTask>` | 获取所有任务，逐个查询数据集 + 变量 |
| `deleteTask` | `void` | 级联删除：数据集 → 变量 → 任务 |
| `batchInsertDatasets` | `int` | 批量插入，自动生成 UID |

### 5.4 JenkinsService（一次性 Pipeline）

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `createJob` | `String createJob(jobPath, jobConfig)` | POST Jenkins `/createItem` + XML 配置。支持文件夹路径 |
| `createDynamicJob` | `String createDynamicJob(String, PipelineConfigParams)` | 构建模板参数 Map，加载 `pipeline-template.xml`，替换占位符，调用 `createJob` |
| `triggerBuild` | `String triggerBuild(jobPath, parentTaskUID)` | 检查作业存在，POST `/buildWithParameters`，创建初始 Redis 状态，启动异步 `trackBuildStatus` |
| `trackBuildStatus` | (private, 异步) | 等待构建开始（最多 30 次），每 5 秒轮询 `/wfapi/describe`（最长 30 分钟），检测完成状态，保存到 Redis + DB |
| `getBuildStatus` | `JenkinsBuildStatus` | 从 Redis 获取 |
| `getBuildStatusFromJenkins` | `JenkinsBuildStatus` | 实时从 Jenkins 获取：最后构建、wfapi 解析、包含阶段 + 控制台输出 |
| `deleteJob` | `boolean` | POST `/doDelete` |
| `stopBuild` | `boolean` | POST `{buildUrl}/stop` |
| `isBuildRunning` | `boolean` | 检查状态是否为 IN_PROGRESS 或 QUEUED |
| `getBuildConsoleOutput` | `String` | GET `/consoleText` |
| `jobExists` | `boolean` | HEAD 检查作业 URL |

### 5.5 JenkinsScheduleService（定时 Pipeline）

与 `JenkinsService` 基本一致，区别：
- 使用 `TaskScheduleInfoService` 代替 `TaskInfoService`
- 保存时设置 `schedule_active = 1`
- 额外方法 `getAllBuildStatusFromJenkins(String)` 返回所有构建 + `nextBuildNumber` + `jobHistoryBuild`

### 5.6 ScheduleService — Cron 表达式生成

| 方法 | 逻辑 |
|------|------|
| `convertToCronExpression(json)` | 解析 `ScheduleConfig`，优先使用 `cronExpression`，否则根据 `intervalType` 生成 |
| `isScheduleActive(json)` | 检查 `isActive` 标志 |
| `generateDailyCron` | `HH:mm` → `minute hour * * *` |
| `generateWeeklyCron` | 时间 + 星期名 → `minute hour * * dayList` |
| `generateHourlyCron` | `minute * * * *` |
| `generateMonthlyCron` | `minute hour 1 * *` |
| `generateIntervalCron` | `*/duration unit` 模式 |

### 5.7 PipelineParamEnricher — 参数增强

`enrichParamsWithTaskInfo(PipelineConfigParams, TrainTask, taskUID)` 方法从 TrainTask 填充所有 Pipeline 参数：
- 数据集路径：从 `TaskDataset` 列表提取
- Git 仓库 URL + Base64 token：从 SMP `AlgorithmRepository` 获取
- Python 入口：从 `task_route`
- 输出路径：从 `output_config` JSON 解析
- Docker 镜像：从 SMP `DockerFile` 获取
- Harbor URL：从 SMP `HarborRepository` 获取

---

## 六、Mapper 层 SQL 操作

### 6.1 AlgorithmMapper (`xnet_mlops_mtp_algorithms`)

| 方法 | SQL 概要 |
|------|----------|
| `insertAlgorithm` | `INSERT INTO xnet_mlops_mtp_algorithms (uid, userId, algorithm_name, version, zone, zone_label, encryption, subdata_area, bucket_name, bucket_identifier, team_uid, team_name, description, tenant_uid, dept_uid, level, is_CAS, cloud_algorithm_id)` |
| `countByAlgorithmName` | `SELECT COUNT(*) WHERE algorithm_name = ?` |
| `findById` | `SELECT *, is_CAS AS isCAS WHERE id = ?` |
| `findAlgorithmsByUID` | `SELECT *, is_CAS AS isCAS WHERE uid = ?` |
| `findAll` | `SELECT *, is_CAS AS isCAS FROM xnet_mlops_mtp_algorithms` |
| `updateAlgorithm` | `UPDATE ... SET ... WHERE id = ?` |
| `deleteById` | `DELETE WHERE id = ?` |
| `findByUid` | **跨模块 JOIN**: `SELECT a.*, t.tenant_name, d.dept_name, tm.team_name FROM xnet_mlops_smp_algorithms a LEFT JOIN xnet_mlops_sys_tenant t ... LEFT JOIN xnet_mlops_sys_department d ... LEFT JOIN xnet_mlops_sys_team tm ... WHERE a.uid = ?` |

### 6.2 TrainTaskMapper (`xnet_mlops_mtp_train_task`)

| 方法 | SQL 概要 |
|------|----------|
| `insertTrainTask` | `INSERT INTO xnet_mlops_mtp_train_task (uid, userId, tenant_uid, task_name, task_type, encryption, task_zone, pod_type, resources, train_type, image_uid, image, description, algorithm_uid, algorithm_name, algorithm_version, task_route, train_config_content, train_config_format, notification_config, output_config, schedule_config, created_at)` |
| `countByUid` | `SELECT COUNT(*) WHERE uid = ?` |
| `updateTrainTask` | `UPDATE ... WHERE uid = ? AND tenant_uid = ?` |
| `findByUidAndTenantUid` | `SELECT * WHERE uid = ? AND tenant_uid = ?` + `@Many` 嵌套查询 datasets + custom_variables |
| `findAllByTenantUid` | `SELECT * WHERE tenant_uid = ?` |
| `deleteByUidAndTenantUid` | `DELETE WHERE uid = ? AND tenant_uid = ?` |
| `countByTaskName` | `SELECT COUNT(*) WHERE task_name = ? AND tenant_uid = ?` |
| `findByImageUid` | **跨模块**: `SELECT * FROM xnet_mlops_smp_docker_file WHERE uid = ?` |
| `findByHarborUid` | **跨模块**: `SELECT * FROM xnet_mlops_smp_harbor_repository WHERE uid = ?` |

### 6.3 TaskDatasetMapper (`xnet_mlops_mtp_task_dataset`)

| 方法 | SQL 概要 |
|------|----------|
| `batchInsertTaskDataset` | 动态 `INSERT ... VALUES <foreach>` |
| `insertTaskDataset` | 单条 `INSERT` |
| `deleteByTaskUid` | `DELETE WHERE task_uid = ?` |
| `findByTaskUid` | `SELECT * WHERE task_uid = ?` |
| `countByTaskUid` | `SELECT COUNT(*) WHERE task_uid = ?` |
| `deleteOrphanDatasets` | `DELETE WHERE task_uid NOT IN (SELECT uid FROM xnet_mlops_mtp_train_task)` |

### 6.4 TaskCustomVariableMapper (`xnet_mlops_mtp_task_custom_variable`)

| 方法 | SQL 概要 |
|------|----------|
| `insertTaskCustomVariable` | `INSERT INTO ... (uid, task_uid, name, value)` |
| `deleteByTaskUid` | `DELETE WHERE task_uid = ?` |
| `findByTaskUid` | `SELECT * WHERE task_uid = ?` |
| `existsByTaskUid` | `SELECT COUNT(*) > 0 WHERE task_uid = ?` |

### 6.5 TaskInfoMapper / TaskScheduleInfoMapper

两者结构完全一致，分别操作 `xnet_mlops_mtp_train_info` 和 `xnet_mlops_mtp_train_schedule_info`。

| 方法 | SQL 概要 |
|------|----------|
| `insertTaskInfo` | `INSERT INTO ... (uid, task_uid, job_uid, job_status, job_content, start_at, end_at, schedule_active)` |
| `updateByJobUid` | `UPDATE ... SET ... WHERE job_uid = ?` |
| `findByTaskUid` | `SELECT job_uid, job_status, start_at, end_at, schedule_active WHERE task_uid = ?` |
| `findByJobUid` | `SELECT ... WHERE job_uid = ?` |
| `deleteByJobUid` | `DELETE WHERE job_uid = ?` |

---

## 七、前端 API 层

### 7.1 请求客户端配置

文件: `api/request.ts`

MTP 使用 `mtpRequestClient`，配置 `responseReturn: 'data'` + `defaultResponseInterceptor({ codeField: 'code', dataField: 'data', successCode: 0 })`，双层解包，API 函数直接接收 `data` 字段。

自动注入 Header: `Authorization: Bearer <token>`, `Accept-Language`, `X-User-Id`

### 7.2 算法 CRUD API (`SMP/api/algorithm.ts`)

| 函数 | 方法 | URL | 参数 | 返回 |
|------|------|-----|------|------|
| `createAlgorithm(payload)` | POST | `/mtp/algorithms-create` | `AlgorithmItem` + `X-Tenant-Id` | `AlgorithmItem` |
| `fetchAlgorithmList()` | GET | `/mtp/algorithms` | -- | `AlgorithmItem[]` |
| `deleteAlgorithm(id)` | DELETE | `/mtp/algorithms/{id}` | `id: number` | `void` |
| `fetchAlgorithmDetail(id)` | GET | `/mtp/algorithms/{id}` | `id: number` | `AlgorithmItem` |
| `updateAlgorithm(id, payload)` | PUT | `/mtp/algorithms/{id}` | 合并当前数据 | `void` |

### 7.3 HDFS 文件管理 API (`SMP/api/algorithmManager.ts`)

| 函数 | 方法 | URL | 参数 | 返回 |
|------|------|-----|------|------|
| `fetchAlgorithmFileList(id, path)` | GET | `/mtp/algorithms/{id}/files` | `path` query | `HdfsFileListData` |
| `uploadAlgorithmFile(id, path, file, onProgress)` | POST | `/mtp/algorithms/{id}/upload` | FormData | `FileUploadData` |
| `downloadAlgorithmFile(id, filePath)` | GET | `/mtp/algorithms/{id}/download` | `filePath` query | `Blob` |
| `deleteAlgorithmFile(id, path)` | DELETE | `/mtp/algorithms/{id}/delete` | `{path}` body | `void` |
| `createAlgorithmDirectory(id, path, folderName)` | POST | `/mtp/algorithms/{id}/mkdir` | `{path, folderName}` body | `void` |

### 7.4 训练任务 API (`SMP/api/traintask.ts`)

| 函数 | 方法 | URL | 说明 |
|------|------|-----|------|
| `createTrainTask(formState, userId, tenantUid)` | POST | `/mtp/tasks-creat` | 创建任务 |
| `updateTrainTask(uid, formState, userId, tenantUid)` | PUT | `/mtp/tasks/{uid}` | 更新任务 |
| `fetchTrainTaskDetail(uid, tenantUid)` | GET | `/mtp/tasks/{uid}` | 获取任务详情 |
| `deleteTrainTask(uid, tenantUid)` | DELETE | `/mtp/tasks/{uid}` | 删除任务 |
| `fetchAllTrainTasks(tenantUid)` | GET | `/mtp/tasks` | 列出所有任务 |
| `TrainTaskStart(uid, tenantUid, userId)` | POST | `/mtp/pipeline` | 执行任务 |
| `fetchPipelineStatus(jobName, tenantUid)` | GET | `/mtp/build/status/{jobName}` | 获取构建状态 |
| `fetchJobsByTaskUid(taskUid, tenantUid)` | GET | `/mtp/task/{taskUid}/jobs` | 列出作业 |
| `deleteJenkinsJob(jobUid, forceStop)` | DELETE | `/mtp/job/{jobUid}` | 删除作业 |
| `startSchedule(taskUID, tenantUid, userId, scheduleConfig)` | POST | `/mtp/schedule/pipeline` | 开始调度 |
| `stopSchedule(jobName, tenantUid, userId, deleteJob)` | DELETE | `/mtp/schedule/stop/{jobName}` | 停止调度 |
| `getScheduleStatus(jobName, tenantUid)` | GET | `/mtp/schedule/status/{jobName}` | 调度状态 |
| `updateSchedule(jobName, tenantUid, scheduleConfig)` | PUT | `/mtp/schedule/update/{jobName}` | 更新调度 |
| `fetchScheduleRecords(taskUid, tenantUid)` | GET | `/mtp/schedule/{taskUid}/jobs` | 调度记录 |

`convertFormToRequest()` 辅助函数：将前端 `TaskFormState` 4步表单转换为后端请求格式。

### 7.5 Jenkins Pipeline API (`SMP/api/trainjob.ts`)

| 函数 | 方法 | URL |
|------|------|-----|
| `createJenkinsPipeline(jobName, params)` | POST | `/api/mtp/pipeline` |
| `triggerJenkinsBuild(jobName)` | POST | `/api/mtp/build/{jobName}` |
| `getJenkinsJobInfo(jobName)` | GET | `/api/mtp/job/{jobName}` |
| `getJenkinsConsoleOutput(jobName, buildNumber)` | GET | `/api/mtp/build/{jobName}/{buildNumber}/console` |
| `stopJenkinsBuild(jobName, buildNumber)` | POST | `/api/mtp/build/{jobName}/{buildNumber}/stop` |
| `getJenkinsBuildStatus(jobName, buildNumber)` | GET | `/api/mtp/build/{jobName}/{buildNumber}/status` |

### 7.6 CAS 算法配置 API (`SMP/api/algorithmConfig.ts`)

使用 `smpRequestClient`（SMP 后端）。

| 函数 | 方法 | URL |
|------|------|-----|
| `getAlgorithmConfig()` | GET | `/smp/algorithms` |
| `createAlgorithmConfig(data)` | POST | `/smp/algorithms` |
| `updateAlgorithmConfig(id, data)` | PUT | `/smp/algorithms/{id}` |
| `deleteAlgorithmConfig(id)` | DELETE | `/smp/algorithms/{id}` |

---

## 八、前端页面组件详解

### 8.1 算法列表 (`algorithm/index.vue`)

- 搜索栏 + "新增算法" / "批量删除" 按钮
- `Table` 可展开行，含两个 Tab: "基本信息" (`Descriptions`) + "关联任务" (下游任务列表)
- 导航: 点击名称 → 修改页，"查看目录" → 文件管理器
- 响应式数据: `algorithmList`, `loading`, `expandedRowKeys`, `searchName`, `selectedRowKeys`
- `onMounted` 调用 `fetchAlgorithms()`

### 8.2 新增算法 (`algorithmCreate.vue`)

- 两列动态表单 + 组织 Cascader + 存储桶 Modal 选择
- **双模式**: 文件上传（支持 > 500MB 分块上传）vs. CAS 云算法选择
- `formState.is_CAS`: 0 = 文件模式, 1 = CAS 模式
- 分块上传流程: `init-chunked-upload` → 循环 `upload-chunk` → `complete-chunked-upload`
- 提交调用 `createAlgorithm(payload)`

### 8.3 修改算法 (`algorithmModify.vue`)

- 预填表单，名称/存储桶/团队不可编辑
- CAS 模式: 显示云算法信息 + "更换云算法" 按钮
- 非 CAS: 显示 HDFS 文件列表 + 上传区域（覆盖模式）
- 加载: `fetchAlgorithmDetail(id)` → 填充表单 → 检测 CAS → 加载文件列表

### 8.4 文件管理器 (`algorithmFileManager.vue`)

- 完整 HDFS 文件浏览器：面包屑导航、目录遍历
- 操作: 上传、下载(Blob)、删除、创建目录、批量删除
- CAS 模式限制: 禁用上传/删除/创建目录，显示警告横幅

### 8.5 训练任务列表 (`train/index.vue`, ~2600行)

MTP 模块最大最复杂的组件:

- 搜索面板: 名称、类型、状态、调度状态 4 个过滤器
- `Table` 可展开行，含 **5 个 TabPane**:
  1. **任务详情**: 4 个 Card 展示基础信息、算法数据、训练配置、输出调度
  2. **执行记录**: 执行列表，含状态标签、阶段进度条、操作（取消/查看日志/删除/强制删除/重新执行）
  3. **调度记录**: 调度作业信息 + 可折叠构建记录网格（卡片展示构建号、状态、时间）
  4. **上游依赖**: 添加/编辑/删除上游任务依赖（Mock）
  5. **下游任务**: 下游任务列表（Mock）

- **执行流程**: `TrainTaskStart()` → 创建 ExecuteRecord → `startPolling()` (每 5 秒) → `fetchPipelineStatus()` → 终态时停止轮询
- **调度流程**: `startSchedule()` → `startSchedulePolling()` (每 10 秒) → `fetchScheduleRecordsForTask()`
- `onUnmounted` 清理所有轮询定时器

### 8.6 任务创建向导 (`task/TaskCreate.vue`)

- 4 步 `Steps` + `KeepAlive` + 动态 `component :is`
- 支持 3 种模式: 创建、编辑 (`?id=X`)、复制 (`?copyFrom=X`)
- 每步组件暴露 `validate()` via `defineExpose`
- 提交: `createTrainTask()` 或 `updateTrainTask()`

### 8.7 Step1 — 基础信息

- 任务名（不允许中文，`[a-zA-Z0-9_-]`）、任务类型、加密、区域
- 容器类型 (CCE/Docker)、训练模式 (CPU/单卡GPU/多卡GPU)
- **资源选择 Modal**: 加载已部署 Jenkins 节点 (`fetchJenkinsNodesByStatus('deployed')`)，按训练类型过滤
- **镜像选择 Modal**: 加载 Docker 镜像 (`getDockerFiles()`)，Radio 选择

### 8.8 Step2 — 算法与数据集

- **算法选择 Modal**: Radio 选择，自动填充版本
- **数据集参数**: 动态行，每行含目录名 + 数据集选择器 (`fetchDatasetList()`)
- 入口脚本路径 (`taskroute`)

### 8.9 Step3 — 自定义参数

- **自定义变量**: 动态 Key-Value 行（最多 10 个）
- **训练配置文件**: 格式选择 (TXT/JSON/Python/YAML) + TextArea

### 8.10 Step4 — 调度与输出

- **周期调度**: 6 种类型 — 每天/每周/每小时/Cron表达式/周期间隔/预约调度
- **输出配置**: 路径选择 Modal + 自动发布
- **通知模板**: 接收人、标题、内容、触发条件 (on_failure/on_success/always)

### 8.11 Pipeline 查看器 (`job/JobManager.vue`)

- 蓝色渐变头部卡片: 任务标题、状态标签、ID、创建者、时间信息
- `Steps` 组件展示 Pipeline 阶段（可点击查看每阶段）
- 阶段详情: 时间范围、指标网格、终端风格日志查看器（500px 高度）
- 操作: 刷新、查看全部日志、暂停、停止

### 8.12 SMP MTP 管理仪表板 (`SMP/MTPManage/`)

| 页面 | 说明 |
|------|------|
| `index.vue` | 3 个快捷导航卡片: Git仓库、镜像仓库、镜像创建 |
| `ImageCreate.vue` | Dockerfile CRUD + 推送到 Harbor（Jenkins Pipeline 构建推送） |
| `ImageManage.vue` | Harbor 仓库 CRUD（名称、URL、用户名、密码） |
| `GitManage.vue` | CAS 算法 Git 配置管理（团队权限、多租户授权） |

---

## 九、业务流程

### 9.1 算法管理流程

```
用户点击"新增算法"
    ↓
填写表单（名称、版本、区域、加密、团队、存储桶）
    ↓
选择上传方式:
    ├── 文件上传模式 (is_CAS=0):
    │   ├── 文件 < 500MB → POST /mtp/upload → HDFS /temp/{uuid}_{name}.temp
    │   └── 文件 > 500MB → 分块上传 (init → upload-chunk × N → complete)
    └── CAS 云仓模式 (is_CAS=1):
        └── 从 Modal 选择云算法包
    ↓
POST /mtp/algorithms-create
    ↓
后端生成 UID (ALG-XXXXXXXX)
    ↓
非 CAS: HDFS rename /temp → /algorithms/{bucket}/{uid}/{filename}
    ├── ZIP 文件自动解压到 HDFS 目录
    └── 删除原始 ZIP
    ↓
返回算法列表
```

**CAS 模式限制**: 文件管理器中 上传/删除/创建目录 操作被阻止，仅允许浏览和下载。

### 9.2 训练任务创建流程

```
用户点击"新增任务"
    ↓
4步向导:
    Step1: 基础信息（名称、类型、容器、资源、镜像）
        └── 从已部署 Jenkins 节点选择资源
        └── 从 SMP Docker 镜像列表选择镜像
    ↓
    Step2: 算法与数据集
        └── 从算法列表选择算法（自动填充版本）
        └── 配置数据集映射（目录名 + 数据集选择）
        └── 设置入口脚本路径
    ↓
    Step3: 自定义参数
        └── 添加 Key-Value 变量（最多 10 个）
        └── 配置训练配置文件（格式 + 内容）
    ↓
    Step4: 调度与输出
        └── 可选: 周期调度配置
        └── 可选: 输出路径配置
        └── 可选: 通知模板
    ↓
提交: POST /mtp/tasks-creat
    ↓
后端: 验证任务名唯一 → 生成 UUID → 插入 train_task → 批量插入 datasets + custom_variables
```

### 9.3 训练任务执行流程

```
用户点击"执行"
    ↓
前端调用 TrainTaskStart(uid, tenantUid, userId)
    → POST /mtp/pipeline?taskUID=xxx
    ↓
后端:
    1. PipelineParamEnricher 解析所有引用:
       - 从 SMP 获取算法 Git URL + Base64 token
       - 从 SMP 获取 Docker 镜像 + Harbor URL
       - 从 output_config JSON 解析输出路径
    2. 加载 pipeline-template.xml
    3. 替换所有 ${...} 占位符
    4. Jenkins API: POST /createItem (创建 Pipeline 作业)
    5. Jenkins API: POST /buildWithParameters (触发构建)
    6. Redis: 存储初始 JenkinsBuildStatus
    7. 启动异步 trackBuildStatus 线程
    ↓
前端创建 ExecuteRecord (状态: QUEUED)
前端启动轮询 (每 5 秒):
    GET /mtp/build/status/{jobName}
    ↓
    更新阶段状态、构建 URL、整体状态
    ↓
    终态 (SUCCESS/FAILURE/ABORTED/ERROR/TIMEOUT):
        停止轮询 → 设置结束时间
```

### 9.4 Jenkins Pipeline 7 阶段

```
Stage 1: 检查调度状态
    └── 检测触发类型 (定时 vs 手动)
    └── 定时触发但调度未启用 → 中止

Stage 2: 拉取训练数据
    └── hdfs dfs -get /datasets/{DATASET_FOLDER} → /opt/mlops/datasets/{DATASET_PATH}

Stage 3: 构建训练任务
    └── git clone 算法代码

Stage 4: 构建训练环境
    └── docker login Harbor
    └── docker pull {HARBOR_URL}/xnet-mlops-smp-python/{IMAGE}:{tag}

Stage 5: 模型训练
    └── docker run --rm \
        -v workspace -v dataset -v model_output \
        {image} python {PYTHON_ENTRY}

Stage 6: 模型输出
    └── hdfs dfs -put -f 模型输出到 /output/{DATASET_FOLDER}/{OUTPUT_PATH}/

Stage 7: 调度状态报告
    └── 记录执行摘要

Post-actions (always):
    └── 清理数据集目录
    └── 删除 Docker 镜像
    └── 退出 Harbor 登录
```

### 9.5 定时调度流程

```
用户点击"开始调度"
    ↓
前端调用 startSchedule(taskUID, tenantUid, userId, scheduleConfig)
    → POST /mtp/schedule/pipeline
    ↓
后端:
    1. ScheduleService.convertToCronExpression(scheduleConfig)
       - daily → "minute hour * * *"
       - weekly → "minute hour * * dayList"
       - hourly → "minute * * * *"
       - interval → "*/duration unit"
       - cron → 直接使用用户输入
    2. 注入 CRON_EXPRESSION + SCHEDULE_ENABLED 到 Jenkins 模板
    3. Jenkins TimerTrigger 处理定时执行
    4. 调度配置存入 Redis (30天 TTL, key = schedule:{jobName})
    5. 作业记录存入 xnet_mlops_mtp_train_schedule_info (schedule_active=1)
    ↓
前端开始轮询调度记录 (每 10 秒)
    GET /mtp/schedule/{taskUid}/jobs
    ↓
    每次定时构建显示为卡片 (构建号、状态、时间)
    ↓
停止调度:
    DELETE /mtp/schedule/stop/{jobName}
    → 停止构建 → 删除调度配置 → 可选删除作业
```

---

## 十、跨模块依赖

### 10.1 MTP → SMP (系统管理平台)

MTP 直接读取 SMP 表（共享数据库）:

| SMP 表 | MTP 用途 |
|--------|----------|
| `xnet_mlops_smp_algorithms` | 云算法仓库 (Git URL, 加密 token, 版本) |
| `xnet_mlops_smp_docker_file` | Docker 镜像定义 (名称, 标签, 推送状态, Harbor UID) |
| `xnet_mlops_smp_harbor_repository` | Harbor 仓库连接 (URL, 凭证) |
| `xnet_mlops_sys_tenant` | 租户名称 (JOIN) |
| `xnet_mlops_sys_department` | 部门名称 (JOIN) |
| `xnet_mlops_sys_team` | 团队名称 (JOIN) |

### 10.2 MTP → DPP (数据处理平台)

- 数据集通过 UID (`dataset_uid`) 在 `TaskDataset` 中引用
- `bucket_identifier` 链接到 DPP 存储桶结构
- HDFS 路径: `/datasets/{bucket_identifier}/{dataset_uid}`

### 10.3 MTP → Jenkins

- 直接 HTTP API 调用（非 jenkins-client 库）
- 从 XML 模板动态创建 Pipeline 作业
- CSRF crumb 认证
- 通过 `/wfapi/describe` 和 `/consoleText` 轮询构建状态

### 10.4 MTP → HDFS (Hadoop)

| HDFS 路径 | 用途 |
|-----------|------|
| `/algorithms/{bucket}/{uid}/` | 算法文件存储 |
| `/datasets/{bucket}/{dataset_uids}` | 数据集获取 |
| `/temp/` | 临时上传 |
| `/output/{dataset_folder}/{output_path}/` | 模型输出 |

通过 `HadoopUtil` 连接，支持 Kerberos 认证。

### 10.5 MTP → Redis

| Key 模式 | TTL | 用途 |
|----------|-----|------|
| `{jobName}` | 24 小时 | 构建状态缓存 |
| `schedule:{jobName}` | 30 天 | 调度配置存储 |

### 10.6 MTP → Kubernetes (未实现)

`io.fabric8:kubernetes-client:6.3.1` 已声明在 pom.xml，但代码中尚未使用。预留用于未来 K8s 部署集成。

---

## 十一、配置信息

### 11.1 application.properties

```properties
spring.application.name=mlops-mtp-service
server.port=8183

# MySQL
spring.datasource.url=jdbc:mysql://192.168.1.5:3306/XnetMLops?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}

# HDFS
hdfs.path=hdfs://192.168.1.5:8020
hdfs.user=atguigu

# 文件上传限制
spring.servlet.multipart.max-file-size=100GB
spring.servlet.multipart.max-request-size=100GB

# 超时配置
server.tomcat.connection-timeout=1800000  # 30分钟
server.tomcat.keep-alive-timeout=30000

# 临时目录
chunked.temp.dir=/data/tmp/uploads
app.temp.dir=/data/tmp

# Jenkins
jenkins.url=http://192.168.1.5:8080
jenkins.username=atguigu
jenkins.api-token=${JENKINS_API_TOKEN}

# Redis
spring.data.redis.host=192.168.1.5
spring.data.redis.port=6379
```

### 11.2 Redis 配置

两个 `RedisTemplate` Bean:
1. `objectRedisTemplate` — `RedisTemplate<String, Object>`，JSON 序列化
2. `jenkinsBuildStatusRedisTemplate` — `RedisTemplate<String, JenkinsBuildStatus>`，JSON 序列化

### 11.3 Jenkins Pipeline 模板

| 模板文件 | 说明 |
|----------|------|
| `pipeline-template.xml` | **主模板**（生产使用），支持调度，安全的 `getBuildCauses()` 触发检测，7 阶段 + 清理 |
| `pipeline-template-schedule.xml` | 旧版调度模板，使用 `rawBuild.getCause(TimerTriggerCause)`（需脚本审批） |
| `pipeline-template.copy.xml` | 基础模板（无调度），5 阶段简化 Pipeline |

---

## 十二、前端状态管理

### 12.1 Provide/Inject 模式

MTP 不使用 Vuex/Pinia Store，依赖 Vue `provide/inject`:

| Inject Key | 类型 | 使用位置 | 用途 |
|-----------|------|----------|------|
| `currentUserInfo` | `Ref<any>` | algorithmCreate, train/index, TaskCreate | 用户信息 (userId) |
| `selectedOrganization` | `Ref<{tenantUid, deptUid, teamUid, level}>` | algorithmCreate, train/index, TaskCreate | 当前组织上下文 |
| `organizationTree` | `Ref<DeptTreeDataItem[]>` | algorithmCreate | 组织树（Cascader 用） |

### 12.2 组件本地状态

每个页面通过 `ref()` / `reactive()` 管理自己的状态:
- **轮询定时器**: `record.pollingTimer` (执行) 和 `record.schedulePollingTimer` (调度) 存在每个任务的数据项上
- **活跃执行记录**: `record.activeExecuteRecord` 追踪当前运行的执行
- **展开行状态**: `expandedRowKeys` 管理表格展开
- `onUnmounted` 清理所有定时器

### 12.3 跨组件通信

- **路由 Query 参数**: 主要的跨页数据传递方式 (`?id=X`, `?copyFrom=X`, `?isCAS=true`)
- **Router 导航**: `router.push()` / `router.replace()`
- 无事件总线或全局 Store

---

## 十三、TypeScript 类型定义

### 13.1 核心类型 (`SMP/api/types.ts`)

```typescript
interface AlgorithmItem {
  id: number; uid: string; userId: string;
  algorithm_name: string; version: string;
  zone: string; zone_label?: string;
  encryption: boolean; subdata_area: string;
  bucket_name: string; bucket_identifier: string;
  team_uid: string; team_name: string;
  description: string; tenant_uid: string;
  dept_uid: string; level: number;
  is_CAS: boolean;
}

interface HdfsFile {
  id: string; name: string; path: string;
  isDirectory: boolean; size: number;
  modificationTime: number; permissions: string;
  owner: string; group: string;
}

interface DockerFile {
  id: number; uid: string; name: string;
  content: string; tags: string;
  push_status: 'FAILED'|'PENDING'|'PUSHED'|'PUSHING';
  harbor_uid: string; push_history: string;
}

interface HarborRepository {
  id: number; uid: string; name: string;
  url: string; username: string; password: string;
}
```

### 13.2 任务表单类型 (`task.ts` + `types.ts`)

```typescript
interface TaskFormState {
  taskStep1: TaskFormStep1;
  taskStep2: TaskFormStep2;
  taskStep3: TaskFormStep3;
  taskStep4: TaskFormStep4;
}

interface TaskFormStep1 {
  taskName: string; taskType: string; encryption: string;
  taskZone: string; podType: string; resources: string;
  trainType: string; imageUid: string; image: string;
  describe: string;
}

interface TaskFormStep2 {
  algorithmUID?: string; algorithmName?: string;
  algorithmVersion?: string;
  datasets?: Array<{
    id?: string; name: string; selectedId: string;
    selectedName: string; selectedUID: string;
    bucketIdentifier: string; datasetName: string;
  }>;
  taskroute?: string;
}

interface TaskFormStep3 {
  customVariables: Array<{ id?: string; name: string; value: string }>;
  trainConfig: { content: string; format: string };
}

interface TaskFormStep4 {
  notificationConfig: {
    isActive: boolean; notificationContent: string;
    notificationTitle: string; notificationUserID: string;
    notificationTrigger: string;
  };
  outputConfig: {
    autoPublish: boolean; isActive: boolean;
    outputPath: string; outputType: string;
  };
  scheduleConfig: {
    intervalType: 'cron'|'daily'|'hourly'|'interval'|'once'|'weekly';
    isActive: boolean; cronExpression?: string;
    dailyTime?: string; weeklyDays?: string[];
    weeklyTime?: string; hourlyMinute?: string;
    intervalDuration?: number; intervalUnit?: string;
    offsetTime?: string; dateRange?: Date[];
    onceTime?: Date;
  };
}
```

### 13.3 Pipeline/作业类型

```typescript
interface PipelineStage {
  stageName: string; status: string;
  durationMillis: number; startTime: Date;
}

interface PipelineBuildStatus {
  jobName: string; buildUrl: string; queueUrl: string;
  overallStatus: string; stages: PipelineStage[];
  consoleOutput?: string; startTime: Date; endTime?: Date;
}

interface ScheduleConfig {
  intervalType: string; cronExpression?: string;
  dailyTime?: string; isActive: boolean;
  weeklyDays?: string[]; weeklyTime?: string;
  hourlyMinute?: string; intervalDuration?: number;
  intervalUnit?: string; offsetTime?: string;
  dateRange?: Date[];
}
```

### 13.4 Jenkins 节点类型 (`jenkinsNode.ts`)

```typescript
interface JenkinsNode {
  id?: number; uid?: string; name: string;
  host: string; port: number; username: string;
  os_type: 'linux'|'macos'|'windows';
  region?: string; container_type?: 'cce'|'docker';
  resource_type?: 'cpu'|'single_gpu'|'multi_gpu';
  cpu_cores?: number; ram_gb?: number;
  gpu_memory?: number; gpu_model?: string; gpu_count?: number;
  status?: 'pending'|'deploying'|'deployed'|'failed'|'offline';
  jenkins_url?: string; agent_name?: string; work_dir?: string;
  java_version?: string; python_version?: string;
  labels?: string; description?: string;
}
```

---

## 十四、开发者注意事项

### 14.1 响应解包模式

MTP 使用双层解包: `defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'`。API 函数直接收到 `data` 字段内容。处理列表时注意: `Array.isArray(res) ? res : res.data || []`

### 14.2 跨模块表访问

MTP 直接通过 MyBatis JOIN 读取 SMP 表，无 HTTP 调用。AlgorithmMapper.findByUid() 和 TrainTaskMapper.findByImageUid()/findByHarborUid() 是跨模块查询。

### 14.3 异步构建跟踪

`JenkinsService.trackBuildStatus()` 是异步线程，每 5 秒轮询 Jenkins `/wfapi/describe`，最长 30 分钟。状态通过 Redis 在前后端之间同步。

### 14.4 CAS 模式

当 `is_CAS = true` 时，算法代码来自 SMP 云算法仓库（Git），HDFS 文件管理被限制为只读。AlgorithmManagerController 在 upload/delete/mkdir 前检查 CAS 标志。

### 14.5 调度实现

调度通过 Jenkins `TimerTrigger` 实现，而非 Spring Scheduler。Pipeline 自身在每次触发时检查 `SCHEDULE_ENABLED` 标志，如果定时触发但调度已禁用则自动中止。

### 14.6 标签格式转换

Docker 镜像标签在 DB 中以破折号分隔存储，传给 Pipeline 时转换为逗号分隔。

### 14.7 未实现功能

- 模型输出管理: 当前使用 Mock 数据，API 未对接
- K8s 部署: pom.xml 已声明 Fabric8 客户端，代码未实现
- 上游/下游任务依赖: 前端 UI 存在，但是 Mock 功能
