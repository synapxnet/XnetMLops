# DPP（数据处理平台）模块知识库

> **版本**: 1.0  
> **更新日期**: 2026-02-12  
> **适用范围**: XnetMLops 平台 DPP 模块前后端完整架构  
> **说明**: 本文档为 RAG 知识库源文件，涵盖 DPP 模块的架构、API、数据模型、业务流程等全部细节

---

## 一、模块概述

### 1.1 基本信息

| 属性 | 值 |
|------|-----|
| 模块名称 | DPP (Data Processing Platform) 数据处理平台 |
| 后端服务 | `mlops-dpp-service` (Spring Boot) |
| 服务端口 | 8182 |
| 数据库 | MySQL `XnetMLops` (127.0.0.1:3306) |
| 存储系统 | HDFS (hdfs://127.0.0.1:8020) |
| CI/CD | Jenkins (127.0.0.1:8080) |
| 缓存 | Redis（条件加载，用于 Jenkins 构建状态缓存） |
| 前端路径 | `XnetMLops-web/apps/web-antd/src/views/DPP/` |
| 路由前缀 | `/DPP/` |
| K8s 命名空间 | `xnet-mlops`，Ingress 路径 `/dpp` |

### 1.2 三大子系统

DPP 模块由三个核心子系统组成：

1. **数据集管理 (Dataset Management)** — 数据集的创建、上传、HDFS 文件浏览与管理
2. **特征工程 (Feature Engineering)** — 基于 Jenkins Pipeline 的数据处理任务配置、执行与调度
3. **RAG 知识库 (Knowledge Base)** — 文档上传、分块、向量化、检索（部分功能开发中）

---

## 二、系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                     前端 (Vue 3 + Ant Design)            │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────┐     │
│  │ 数据集管理 │  │  特征工程     │  │  RAG 知识库    │     │
│  │ dataset/  │  │ FeatureEng/  │  │ KnowledgeBase/ │     │
│  └─────┬────┘  └──────┬───────┘  └───────┬────────┘     │
│        │               │                  │              │
│        ▼               ▼                  ▼              │
│  ┌─────────────────────────────────────────────┐         │
│  │   dppRequestClient   │   smpRequestClient   │         │
│  └──────────┬───────────┴──────────┬───────────┘         │
└─────────────┼──────────────────────┼─────────────────────┘
              │                      │
              ▼                      ▼
┌─────────────────────┐  ┌──────────────────────┐
│  DPP 后端 (:8182)    │  │  SMP 后端 (:8185)     │
│  /api/dpp/*          │  │  /api/smp/*           │
│  ┌─────────────────┐ │  │  数据源配置/Bucket/    │
│  │ DatasetController│ │  │  组织架构/Docker镜像   │
│  │ FeatureEng Ctrl  │ │  └──────────────────────┘
│  │ KnowledgeBase Ctrl│ │
│  │ Upload Controller │ │
│  └────────┬─────────┘ │
│           ▼            │
│  ┌─────────────────┐   │
│  │  Service 层       │   │
│  │  ┌─ DatasetSvc   │   │
│  │  ├─ HdfsSvc      │   │
│  │  ├─ FeatureJenkins│  │
│  │  ├─ ScheduleSvc   │   │
│  │  └─ KBSvc        │   │
│  └────────┬─────────┘   │
│           ▼              │
│  ┌─────────────────────┐ │
│  │ MySQL │ HDFS │ Jenkins│ │
│  │       │ Redis │ Milvus │ │
│  └─────────────────────┘ │
└─────────────────────────┘
```

### 2.2 技术栈

| 层级 | 技术 |
|------|------|
| 前端框架 | Vue 3 + TypeScript + Ant Design Vue |
| 状态管理 | 组件局部 ref/reactive + provide/inject（无全局 Store） |
| HTTP 客户端 | Vben Admin 封装的 RequestClient，双重解包模式 |
| 后端框架 | Spring Boot 3 (Java 17) |
| ORM | MyBatis（注解模式，map-underscore-to-camel-case=false） |
| 数据库 | MySQL 8.x |
| 分布式存储 | Hadoop HDFS 3.3.4 |
| CI/CD | Jenkins Pipeline（Scripted Pipeline） |
| 缓存 | Spring Data Redis（条件加载） |
| 构建工具 | Maven |

---

## 三、数据模型

### 3.1 数据集 (Dataset)

**表名**: `xnet_mlops_dpp_dataset`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(36) | 唯一标识，格式 `DS-XXXXXXXX` |
| `userId` | VARCHAR(36) | 创建者用户ID |
| `dataset_file` | VARCHAR(255) | 数据集名称/文件名 |
| `type` | VARCHAR(50) | 数据类型值 |
| `type_label` | VARCHAR(100) | 数据类型显示标签 |
| `zone` | VARCHAR(50) | 数据区域值 |
| `zone_label` | VARCHAR(100) | 数据区域显示标签 |
| `encryption` | TINYINT | 是否加密 |
| `subdata_area` | VARCHAR(255) | 子数据区域 |
| `bucket_name` | VARCHAR(255) | 存储桶名称 |
| `bucket_identifier` | VARCHAR(255) | 存储桶唯一标识符 |
| `tenant_uid` | VARCHAR(36) | 租户UID |
| `dept_uid` | VARCHAR(36) | 部门UID |
| `team_uid` | VARCHAR(36) | 团队UID |
| `team_name` | VARCHAR(255) | 团队名称 |
| `description` | VARCHAR(500) | 描述 |
| `level` | TINYINT | 组织层级 (0-3) |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

**HDFS 存储路径**: `/datasets/{bucket_identifier}/{uid}/{文件名}`

### 3.2 特征工程 (FeatureEngineering)

**表名**: `xnet_mlops_dpp_feature_engineering`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(64) | 唯一标识，格式 `FE-XXXXXXXX` |
| `name` | VARCHAR | 任务名称，唯一，正则校验 |
| `description` | VARCHAR | 任务描述 |
| `datasource_id` | BIGINT | 外部数据源引用ID |
| `datasource_name` | VARCHAR | 数据源显示名称 |
| `database` | VARCHAR | 数据库名 |
| `table_name` | VARCHAR | 源表名 |
| `selected_columns` | TEXT | JSON 数组：已选列名列表 |
| `transform_config` | TEXT | JSON 转换配置 |
| `zone` | VARCHAR | 数据区域 |
| `bucket_uid` | VARCHAR | 存储桶UID |
| `bucket_name` | VARCHAR | 存储桶名称 |
| `sub_data_area` | VARCHAR | 子数据区域 |
| `data_type` | VARCHAR | 数据类型 |
| `encryption` | TINYINT | 是否加密 |
| `push_to_dataset` | TINYINT | 是否推送到数据集 |
| `target_dataset_id` | BIGINT | 目标数据集ID |
| `target_dataset_name` | VARCHAR | 目标数据集名称 |
| `output_path` | VARCHAR | 自定义输出路径 |
| `operator_id` | BIGINT | 特征算子ID |
| `operator_code` | VARCHAR | 算子代码 (如 `to_csv`) |
| `operator_name` | VARCHAR | 算子名称 |
| `output_format` | VARCHAR | 输出格式: csv/parquet/tfrecord/txt/json |
| `feature_config` | TEXT | JSON：每列特征配置 |
| `operator_params` | TEXT | JSON：全局算子参数 |
| `image_uid` | VARCHAR(64) | Docker 镜像 UID |
| `image_name` | VARCHAR(128) | 镜像名称 |
| `image_tag` | VARCHAR(64) | 镜像标签 |
| `harbor_url` | VARCHAR(256) | Harbor 仓库地址 |
| `harbor_credentials_id` | VARCHAR(64) | Harbor Jenkins 凭证ID |
| `schedule_config` | TEXT | JSON：调度配置 |
| `schedule_active` | TINYINT(1) | 是否启用调度 |
| `notification_config` | TEXT | JSON：通知配置 |
| `notification_enabled` | TINYINT(1) | 是否启用通知 |
| `job_uid` | VARCHAR(256) | Jenkins 任务路径 |
| `last_build_status` | VARCHAR(32) | 最后构建状态 |
| `status` | VARCHAR | draft/processing/completed/failed/stopped |
| `created_by` | VARCHAR | 创建者（来自 X-User-Id 请求头） |
| `team_uid` | VARCHAR | 团队UID |
| `team_name` | VARCHAR | 团队名称 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

### 3.3 特征算子 (FeatureOperator)

**表名**: `xnet_mlops_dpp_feature_operator`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(64) UNIQUE | UUID |
| `name` | VARCHAR(100) | 算子显示名称 |
| `code` | VARCHAR(50) UNIQUE | 算子代码标识 |
| `description` | TEXT | 描述 |
| `category` | VARCHAR(50) | 分类: `format_conversion` / `feature_transform` / `data_cleaning` |
| `output_formats` | TEXT | JSON 数组，支持的输出格式 |
| `feature_columns` | TEXT | JSON 数组，定义每列配置的 Schema |
| `parameter_schema` | TEXT | JSON Schema，算子级参数配置 |
| `sort_order` | INT | 排序权重 |
| `enabled` | TINYINT(1) | 是否启用 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

**内置算子（8个）**：

| 代码 | 名称 | 分类 | 输出格式 |
|------|------|------|---------|
| `to_csv` | CSV 转换 | format_conversion | csv |
| `to_parquet` | Parquet 转换 | format_conversion | parquet |
| `to_tfrecord` | TFRecord 转换 | format_conversion | tfrecord |
| `to_txt` | TXT 转换 | format_conversion | txt |
| `to_json` | JSON 转换 | format_conversion | json |
| `normalize` | 特征标准化 | feature_transform | csv/parquet/json |
| `one_hot_encode` | One-Hot 编码 | feature_transform | csv/parquet/tfrecord |
| `data_cleaning` | 数据清洗 | data_cleaning | csv/parquet/json |

### 3.4 特征任务执行记录 (FeatureTaskInfo)

**表名**: `xnet_mlops_dpp_feature_task_info`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(64) UNIQUE | 格式 `FTI-XXXXXXXX` |
| `task_uid` | VARCHAR(64) | 关联的 FeatureEngineering.uid |
| `job_uid` | VARCHAR(256) | Jenkins 任务路径 |
| `job_status` | VARCHAR(32) | 执行状态 |
| `job_content` | TEXT | 序列化的 JenkinsBuildStatus JSON |
| `start_at` | DATETIME | 开始时间 |
| `end_at` | DATETIME | 结束时间 |
| `schedule_active` | TINYINT(1) | 0=手动执行, 1=调度执行 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

### 3.5 知识库 (KnowledgeBase)

**表名**: `xnet_mlops_dpp_knowledge_base`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(64) UNIQUE | UUID |
| `name` | VARCHAR(200) | 知识库名称 |
| `description` | TEXT | 描述 |
| `icon` | VARCHAR(100) | 图标 |
| `embedding_model_id` | BIGINT | 嵌入模型 FK |
| `embedding_provider` | VARCHAR(50) | 默认 'openai' |
| `embedding_model` | VARCHAR(100) | 默认 'text-embedding-ada-002' |
| `embedding_dimension` | INT | 默认 1536 |
| `vector_db_type` | VARCHAR(50) | 默认 'milvus' |
| `vector_collection` | VARCHAR(200) | 向量集合名 |
| `vector_index_type` | VARCHAR(50) | 默认 'HNSW' |
| `chunk_strategy` | VARCHAR(50) | fixed/recursive/semantic |
| `chunk_size` | INT | 默认 500 |
| `chunk_overlap` | INT | 默认 50 |
| `retrieval_method` | VARCHAR(50) | semantic/keyword/hybrid |
| `top_k` | INT | 默认 5 |
| `score_threshold` | DECIMAL(5,4) | 默认 0.5 |
| `rerank_enabled` | TINYINT(1) | 是否启用重排序 |
| `rerank_model` | VARCHAR(100) | 重排序模型 |
| `rerank_top_k` | INT | 默认 3 |
| `status` | VARCHAR(20) | active/indexing/error/archived |
| `doc_count` | INT | 文档数 |
| `chunk_count` | INT | 分块数 |
| `total_tokens` | BIGINT | 总 Token 数 |
| `total_size_bytes` | BIGINT | 总大小 |
| `visibility` | VARCHAR(20) | private/team/public |

### 3.6 知识库文档 (KBDocument)

**表名**: `xnet_mlops_dpp_kb_document`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(64) UNIQUE | UUID |
| `kb_id` | BIGINT FK | 所属知识库 |
| `name` | VARCHAR(500) | 文档名 |
| `original_name` | VARCHAR(500) | 原始文件名 |
| `type` | VARCHAR(50) | pdf/docx/txt/md/html/csv |
| `file_size` | BIGINT | 文件大小(字节) |
| `status` | VARCHAR(20) | pending/processing/completed/failed |
| `process_progress` | INT | 处理进度 0-100 |
| `chunk_count` | INT | 分块数 |
| `token_count` | INT | Token 数 |
| `source_type` | VARCHAR(50) | upload/url/sync |

### 3.7 知识库分块 (KBChunk)

**表名**: `xnet_mlops_dpp_kb_chunk`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `uid` | VARCHAR(64) UNIQUE | UUID |
| `doc_id` | BIGINT FK | 所属文档 |
| `kb_id` | BIGINT FK | 所属知识库 |
| `content` | TEXT | 分块文本内容 |
| `position` | INT | 在文档中的位置 |
| `embedding_id` | VARCHAR(200) | 向量数据库引用ID |
| `token_count` | INT | Token 数 |
| `parent_chunk_id` | BIGINT | 父分块（层级结构） |
| `chunk_level` | INT | 0=叶子, 1=父, 2=祖父 |
| `keywords` | TEXT | 关键词（逗号分隔） |
| `summary` | TEXT | 摘要 |

### 3.8 嵌入模型 (EmbeddingModel)

**表名**: `xnet_mlops_dpp_embedding_model`

预置 6 个模型：OpenAI Ada-002、Embedding-3-Small、Embedding-3-Large、BGE-Large-ZH、BGE-M3、Text2Vec-Large。

---

## 四、API 接口详情

### 4.1 统一响应格式

```json
{ "code": 0, "message": "success", "data": <payload>, "error": "null" }
```

错误码：`400`（验证错误）、`401`（认证失败）、`404`（未找到）、`500`（服务器错误）。

### 4.2 数据集管理 API

| 方法 | 路径 | 说明 | 请求参数 | 响应 |
|------|------|------|---------|------|
| POST | `/api/dpp/datasets-create` | 创建数据集 | Body: Dataset JSON + `tempFilePath` | Dataset |
| GET | `/api/dpp/datasets` | 获取所有数据集 | 无 | Dataset[] |
| GET | `/api/dpp/datasets/{id}` | 获取数据集详情 | Path: id | Dataset |
| PUT | `/api/dpp/datasets/{id}` | 更新数据集 | Body: Dataset JSON | Dataset |
| DELETE | `/api/dpp/datasets/{id}` | 删除数据集 | Path: id | - |
| POST | `/api/dpp/upload` | 上传文件到 HDFS 临时目录 | FormData: file | 临时文件路径 |

**创建数据集流程**：
1. 前端先上传文件到 HDFS `/temp/{uuid}_{filename}.temp`
2. 提交创建请求，携带 `tempFilePath`
3. 后端生成 UID (`DS-XXXXXXXX`)，持久化到 MySQL
4. 将临时文件移动到 `/datasets/{bucket_identifier}/{uid}/{filename}`
5. 如果是 ZIP 文件，自动解压

### 4.3 数据集文件管理 API

| 方法 | 路径 | 说明 | 请求参数 |
|------|------|------|---------|
| GET | `/api/dpp/datasets/{id}/files` | 列出目录 | Query: `path` (默认 `/`) |
| POST | `/api/dpp/datasets/{id}/upload` | 上传文件到数据集 | FormData: `file`, `path` |
| GET | `/api/dpp/datasets/{id}/download` | 下载文件 | Query: `filePath` |
| DELETE | `/api/dpp/datasets/{id}/delete` | 删除文件/目录 | Body: `{ "path": "..." }` |
| POST | `/api/dpp/datasets/{id}/batchDelete` | 批量删除 | Body: `{ "paths": [...] }` |
| POST | `/api/dpp/datasets/{id}/mkdir` | 创建目录 | Body: `{ "path", "folderName" }` |

### 4.4 特征工程 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dpp/feature-engineering` | 获取所有任务 |
| GET | `/api/dpp/feature-engineering/team/{teamUid}` | 按团队筛选 |
| GET | `/api/dpp/feature-engineering/{id}` | 获取单个任务 |
| POST | `/api/dpp/feature-engineering` | 创建任务 |
| PUT | `/api/dpp/feature-engineering/{id}` | 更新任务 |
| DELETE | `/api/dpp/feature-engineering/{id}` | 删除任务 |
| POST | `/api/dpp/feature-engineering/{id}/execute` | 立即执行 |
| POST | `/api/dpp/feature-engineering/{id}/stop` | 停止执行 |
| GET | `/api/dpp/feature-engineering/{id}/status` | 查询构建状态 |
| GET | `/api/dpp/feature-engineering/{id}/history` | 查询执行历史 |
| POST | `/api/dpp/feature-engineering/{id}/schedule` | 启用定时调度 |
| POST | `/api/dpp/feature-engineering/{id}/stop-schedule` | 停止调度 |

**创建任务验证规则**：
- 需要 `X-User-Id` 请求头（401）
- `name` 不能为空（400）
- `name` 必须匹配 `^[a-zA-Z][a-zA-Z0-9_-]*$`（400）
- `name` 长度 ≤ 50 字符（400）
- `name` 全局唯一（400）

**执行流程**：
1. 构建 `FeaturePipelineParams` DTO
2. 生成 Jenkins 任务路径: `xnet-mlops-dpp/feature-engineering/{name}`
3. 如任务不存在，加载 XML 模板替换参数后创建 Jenkins Pipeline 任务
4. 触发 Jenkins 构建
5. 后台线程轮询构建状态（5秒间隔，最长30分钟）
6. 状态保存到 Redis（24小时TTL）和 MySQL

### 4.5 特征算子 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dpp/feature-operators` | 获取所有已启用算子 |
| GET | `/api/dpp/feature-operators/all` | 获取所有算子（含禁用） |
| GET | `/api/dpp/feature-operators/category/{category}` | 按分类获取 |
| GET | `/api/dpp/feature-operators/{id}` | 按 ID 获取 |
| GET | `/api/dpp/feature-operators/code/{code}` | 按代码获取 |
| POST | `/api/dpp/feature-operators` | 创建算子 |
| PUT | `/api/dpp/feature-operators/{id}` | 更新算子 |
| PATCH | `/api/dpp/feature-operators/{id}/enabled` | 切换启用状态 |
| DELETE | `/api/dpp/feature-operators/{id}` | 删除算子 |
| POST | `/api/dpp/feature-operators/init-defaults` | 初始化8个默认算子 |

### 4.6 知识库 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dpp/knowledge-bases` | 获取所有知识库 |
| GET | `/api/dpp/knowledge-bases/{id}` | 获取知识库详情 |
| POST | `/api/dpp/knowledge-bases` | 创建知识库 |
| PUT | `/api/dpp/knowledge-bases/{id}` | 更新知识库 |
| DELETE | `/api/dpp/knowledge-bases/{id}` | 删除知识库（级联删除文档+分块） |
| POST | `/api/dpp/knowledge-bases/{id}/rebuild` | 重建索引 |
| GET | `/api/dpp/knowledge-bases/{kbId}/documents` | 获取知识库文档 |
| POST | `/api/dpp/knowledge-bases/{kbId}/documents` | 上传文档（multipart） |
| DELETE | `/api/dpp/knowledge-bases/{kbId}/documents/{docId}` | 删除文档 |
| POST | `/api/dpp/knowledge-bases/{kbId}/documents/{docId}/reindex` | 重建文档索引 |
| GET | `/api/dpp/knowledge-bases/{kbId}/documents/{docId}/chunks` | 获取文档分块 |
| POST | `/api/dpp/knowledge-bases/{kbId}/retrieve` | 知识检索 |
| POST | `/api/dpp/knowledge-bases/{kbId}/retrieve/test` | 检索测试 |

### 4.7 嵌入模型 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dpp/embedding-models` | 获取所有活跃模型 |
| POST | `/api/dpp/embedding-models` | 创建模型 |
| PUT | `/api/dpp/embedding-models/{id}` | 更新模型 |
| DELETE | `/api/dpp/embedding-models/{id}` | 删除模型 |
| POST | `/api/dpp/embedding-models/{id}/test` | 测试模型 |

---

## 五、核心业务流程

### 5.1 数据集创建完整流程

```
用户操作                          前端逻辑                           后端逻辑
─────────                        ────────                          ────────
打开创建页面     ──────→    并行加载:
                              GET /smp/dataset-config (类型/区域选项)
                              GET /smp/bucket (存储桶列表)
                              inject organizationTree (组织树)

填写表单         ──────→    实时验证:
  - 数据集名称                 正则 /^[a-zA-Z][a-zA-Z0-9_-]*$/
  - 数据类型                   必选
  - 数据区域                   必选
  - 组织(租户/部门/团队)        级联选择，必须选到团队
  - 存储桶                     Modal 选择，按租户过滤
  - 上传文件                   拖拽上传区

点击提交         ──────→    1. 上传文件:
                              ≤500MB: POST /dpp/upload
                              >500MB: 分块上传(100MB/块)
                                init → chunk × N → complete
                              → 获得 tempFilePath

                           2. 创建数据集:             ──────→  生成 UID DS-XXXXXXXX
                              POST /dpp/datasets-create          持久化到 MySQL
                              携带 tempFilePath                  HDFS 移动文件:
                              Header: X-Tenant-Id                /temp/ → /datasets/{bucket}/{uid}/
                                                                ZIP 自动解压

                           3. 跳转列表页:
                              ?expanded=true&newDatasetId=X
```

### 5.2 特征工程执行完整流程

```
用户操作                          前端逻辑                           后端/Jenkins
─────────                        ────────                          ────────────
配置特征工程:
  选择数据源      ──────→    GET /smp/datasource/enabled
  选择数据库      ──────→    GET /smp/datasource/{id}/databases
  选择表          ──────→    GET /smp/datasource/{id}/tables
  选择列          ──────→    GET /smp/datasource/{id}/columns
                              → 列出字段，勾选需要的列
  选择算子        ──────→    GET /dpp/feature-operators
                              → 动态渲染每列配置和全局参数
  选择Docker镜像  ──────→    GET /smp/dockerfile
  配置输出                   推送到数据集 or 指定路径
  配置调度                   daily/weekly/hourly/monthly/cron/interval
  保存            ──────→    POST /dpp/feature-engineering

点击执行         ──────→    POST /dpp/feature-engineering/{id}/execute
                                                                ──────→ 构建 FeaturePipelineParams
                                                                       生成 Jenkins Job 路径
                                                                       加载 XML 模板 + 参数替换
                                                                       创建 Jenkins Pipeline Job
                                                                       触发 Jenkins 构建
                                                                       保存 QUEUED 状态到 Redis
                                                                       启动后台轮询线程

                           前端 5秒轮询:
                           GET /dpp/feature-engineering/{id}/status
                                                                ──────→ Jenkins Pipeline 执行:
                                                                       1. Check Schedule Status
                                                                       2. Pull Data (从数据源)
                                                                       3. Build Feature Task (生成Python脚本)
                                                                       4. Build Environment (Docker pull)
                                                                       5. Feature Processing (Docker/本地执行)
                                                                       6. Data Push (推送到 HDFS)
                                                                       7. Data Cleanup

完成                        → 状态变为 completed/failed
                              停止轮询
```

### 5.3 Jenkins Pipeline 阶段详解

Jenkins Pipeline（Scripted Pipeline）包含以下 7 个阶段：

1. **Check Schedule Status** — 检测是否由定时器触发，若 SCHEDULE_ENABLED != true 则中止
2. **Pull Data** — 从数据源拉取数据到临时目录（实际实现为占位符）
3. **Build Feature Task** — 生成 `feature_process.py` Python 脚本
4. **Build Environment** — 如配置了 Docker 镜像，从 Harbor 拉取
5. **Feature Processing** — 在 Docker 容器或本地执行 Python 脚本
6. **Data Push** — 推送到 HDFS（数据集路径 or 自定义路径）
7. **Data Cleanup** — 清理临时目录，移除 Docker 镜像

**Pipeline 参数列表**：
`TASK_NAME`, `TASK_UID`, `DATASOURCE_NAME`, `DATABASE`, `TABLE_NAME`, `SELECTED_COLUMNS`, `OPERATOR_CODE`, `OPERATOR_NAME`, `OUTPUT_FORMAT`, `FEATURE_CONFIG`, `OPERATOR_PARAMS`, `PUSH_TO_DATASET`, `TARGET_DATASET_NAME`, `OUTPUT_PATH`, `ENCRYPTION`, `ZONE`, `BUCKET_UID`, `BUCKET_NAME`, `DOCKER_IMAGE_NAME`, `DOCKER_IMAGE_TAG`, `HARBOR_URL`, `HARBOR_CREDENTIALS_ID`, `SCHEDULE_ENABLED`, `CRON_EXPRESSION`

### 5.4 调度配置与 Cron 转换

`ScheduleService` 将前端调度配置 JSON 转换为标准 Cron 表达式：

| 类型 | 前端配置 | Cron 格式 |
|------|---------|-----------|
| `daily` | dailyTime: "09:30" | `30 9 * * *` |
| `weekly` | weeklyDays: ["monday","friday"], weeklyTime: "10:00" | `0 10 * * 1,5` |
| `hourly` | hourlyMinute: "15" | `15 * * * *` |
| `monthly` | dailyTime: "08:00" | `0 8 1 * *` (每月1号) |
| `interval` | intervalDuration: 2, intervalUnit: "hours" | `0 */2 * * *` |
| `cron` | cronExpression: "0 0 * * *" | 直接使用 |

### 5.5 知识库 RAG 流程

```
创建知识库 → 配置嵌入模型/向量库/分块策略/检索方法
     ↓
上传文档 → 创建 KBDocument 记录 (status: pending)
     ↓
[TODO] 文档处理 Pipeline:
     解析文档 → 文本分块 → 生成嵌入向量 → 存入 Milvus
     ↓
检索测试 → POST /retrieve/test
     → [TODO] 查询向量化 → 向量检索 → 重排序 → 返回结果
```

---

## 六、前端页面结构

### 6.1 路由结构

```
/DPP                                    (DPP 模块根路由)
├── /DPP/Datatask/index                 数据任务列表（Mock 数据）
│   └── /DPP/Datatask/task              新增数据任务（表单 Stub）
├── /DPP/dataset/index                  数据集列表 ★
│   ├── /DPP/dataset/datafileCreate     新增数据集 ★
│   ├── /DPP/dataset/datafileModify     编辑数据集 ★
│   └── /DPP/dataset/datafileManager    HDFS 文件管理 ★
├── /DPP/feature-engineering/index      特征工程列表 ★
│   ├── /DPP/feature-engineering/create 新建特征工程 ★
│   └── /DPP/feature-engineering/edit/:id 编辑特征工程 ★
└── /DPP/KnowledgeBase                  知识库列表 ★
    ├── /DPP/KnowledgeBase/documents    文档管理 ★
    └── /DPP/KnowledgeBase/retrieval-test 检索测试 ★
```

★ = 已连接后端 API

### 6.2 前端关键组件

| 组件文件 | 功能 | 核心 API 调用 |
|---------|------|-------------|
| `dataset/index.vue` | 数据集列表、搜索、删除 | GET/DELETE /dpp/datasets |
| `dataset/datafileCreate.vue` | 创建数据集(含分块上传) | POST /dpp/upload, POST /dpp/datasets-create |
| `dataset/datafileModify.vue` | 编辑数据集(仅描述) | GET/PUT /dpp/datasets/{id} |
| `dataset/datafileManager.vue` | HDFS 文件浏览器 | GET/POST/DELETE /dpp/datasets/{id}/files\|upload\|download\|delete\|mkdir |
| `FeatureEngineering/index.vue` | 特征工程列表、执行、轮询 | CRUD + execute/stop/status/history |
| `FeatureEngineering/FeatureCreate.vue` | 两步向导式表单(2470行) | 级联加载数据源→库→表→列 |
| `KnowledgeBase/index.vue` | 知识库列表 | CRUD /dpp/knowledge-bases |
| `KnowledgeBase/documents.vue` | 文档管理 | documents CRUD + reindex |
| `KnowledgeBase/components/DocumentUpload.vue` | 文档上传 Modal | POST multipart |
| `KnowledgeBase/components/ChunkViewer.vue` | 分块查看 Drawer | GET chunks |
| `KnowledgeBase/retrieval-test.vue` | 检索测试 | POST /retrieve/test |

### 6.3 前端 HTTP 客户端

```typescript
// DPP 后端请求客户端
dppRequestClient = createRequestClient(dppApiURL, { responseReturn: 'data' })

// SMP 后端请求客户端（配置数据、数据源元数据等）
smpRequestClient = createRequestClient(smpApiURL, { responseReturn: 'data' })
```

**双重解包规则**：`defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'` = 自动提取 `response.data.data`。部分函数使用 `responseReturn: 'body'` 获取完整响应体手动处理。

### 6.4 前端依赖注入 (provide/inject)

`basic.vue` 布局组件通过 `provide` 向所有子页面注入：
- `organizationTree` — 组织架构树（租户→部门→团队）
- `selectedOrganization` — 当前选中的组织上下文
- `currentUserInfo` — 当前登录用户信息

### 6.5 前端状态管理模式

- **无全局 Store**：每个页面使用 `ref()/reactive()` 管理本地状态
- **路由参数传递**：`?id=X&name=Y` 在页面间传递标识
- **localStorage**：检索测试的历史查询记录
- **轮询机制**：特征工程列表对 processing 状态的任务进行 5 秒轮询

---

## 七、跨模块依赖

### 7.1 DPP 依赖 SMP 的接口

| SMP API | DPP 使用场景 |
|---------|------------|
| GET `/smp/dataset-config` | 加载数据类型和区域下拉选项 |
| GET `/smp/bucket` | 加载存储桶列表 |
| GET `/smp/datasource/enabled` | 加载数据源列表 |
| GET `/smp/datasource/{id}/databases` | 级联获取数据库 |
| GET `/smp/datasource/{id}/tables` | 级联获取表 |
| GET `/smp/datasource/{id}/columns` | 级联获取列 |
| GET `/smp/datasource/{id}/preview` | 预览数据 |
| GET `/smp/dockerfile` | 加载 Docker 镜像列表 |

### 7.2 DPP 到 MTP 的跳转

数据任务列表中，点击数据集名称会跳转到 MTP 模块：`/MTP/train/task?id=X`

---

## 八、配置与部署

### 8.1 application.properties 关键配置

```properties
server.port=8182
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/XnetMLops
hdfs.path=hdfs://127.0.0.1:8020
hdfs.user=atguigu
jenkins.url=http://127.0.0.1:8080
jenkins.username=atguigu
jenkins.api-token=${JENKINS_API_TOKEN}
spring.servlet.multipart.max-file-size=100GB
mybatis.configuration.map-underscore-to-camel-case=false
```

### 8.2 K8s 部署配置

- **镜像**: `127.0.0.1/xnet-mlops/xnet-mlops-dpp-service:${BUILD_NUMBER}`
- **资源限制**: requests 500m/512Mi, limits 50CPU/16482Mi
- **Service**: ClusterIP, 端口 8182
- **Ingress**: nginx, 路径 `/dpp`

### 8.3 外部系统连接

| 系统 | 连接方式 | 用途 |
|------|---------|------|
| MySQL | JDBC 127.0.0.1:3306 | 所有元数据持久化 |
| HDFS | WebHDFS 127.0.0.1:8020 | 数据集文件存储 |
| Jenkins | REST API 127.0.0.1:8080 | 特征工程 Pipeline 执行和调度 |
| Redis | 条件配置 | Jenkins 构建状态缓存 (24h TTL) |
| Harbor | 127.0.0.1 | 服务部署的 Docker 镜像仓库 |
| Milvus | 按知识库配置 (TODO) | RAG 向量数据库 |

---

## 九、文件清单

### 9.1 后端文件 (Java)

**Controllers (6)**:
- `DatasetController.java` — 数据集 CRUD
- `DatasetManagerController.java` — HDFS 文件管理
- `FeatureEngineeringController.java` — 特征工程全生命周期
- `FeatureOperatorController.java` — 特征算子管理
- `KnowledgeBaseController.java` — 知识库/文档/分块/检索/嵌入模型
- `UploadController.java` — 文件上传到 HDFS 临时目录

**Services (12)**:
- `DatasetService.java` (接口) / `DatasetServiceImpl.java`
- `DatasetManagerService.java` — HDFS 目录操作
- `HdfsService.java` (接口) / `HdfsServiceImpl.java` — HDFS 核心操作(上传/移动/解压)
- `ChunkedUploadService.java` — 大文件分块上传
- `FeatureJenkinsService.java` — Jenkins 即时执行
- `FeatureJenkinsScheduleService.java` — Jenkins 定时调度
- `FeatureTaskInfoService.java` — 执行记录管理
- `ScheduleService.java` — Cron 表达式转换
- `KnowledgeBaseService.java` — 知识库业务逻辑
- `UploadService.java` — HDFS 上传(带重试)

**Entities (13)**:
- `Dataset.java`, `FeatureEngineering.java`, `FeatureOperator.java`, `FeatureTaskInfo.java`
- `FeaturePipelineParams.java` (DTO), `HdfsFile.java` (DTO), `JenkinsBuildStatus.java` (Serializable)
- `KnowledgeBase.java`, `KBDocument.java`, `KBChunk.java`, `EmbeddingModel.java`
- `RetrievalLog.java`, `ScheduleConfig.java` (DTO)

**Mappers (8)**:
- `DatasetMapper.java`, `FeatureEngineeringMapper.java`, `FeatureOperatorMapper.java`
- `FeatureTaskInfoMapper.java`, `KnowledgeBaseMapper.java`, `KBDocumentMapper.java`
- `KBChunkMapper.java`, `EmbeddingModelMapper.java`

**Config/Utils (4)**:
- `RedisConfig.java`, `HadoopUtil.java`, `JobNotFoundException.java`, `MlopsDppServiceApplication.java`

### 9.2 前端文件 (Vue/TypeScript)

**DPP 视图 (16 文件)**:
- `DPP/Datatask/index.vue`, `DPP/Datatask/datataskCreate.vue`
- `DPP/dataset/index.vue`, `DPP/dataset/datafileCreate.vue`, `DPP/dataset/datafileModify.vue`, `DPP/dataset/datafileManager.vue`
- `DPP/FeatureEngineering/index.vue`, `DPP/FeatureEngineering/FeatureCreate.vue`
- `DPP/KnowledgeBase/index.vue`, `DPP/KnowledgeBase/documents.vue`, `DPP/KnowledgeBase/retrieval-test.vue`
- `DPP/KnowledgeBase/components/DocumentUpload.vue`, `DPP/KnowledgeBase/components/ChunkViewer.vue`
- `DPP/KnowledgeBase/api.ts`, `DPP/KnowledgeBase/types.ts`
- `DPP/table-data.ts`

**API 文件 (12 文件, 位于 SMP/api/)**:
- `dataset.ts`, `datasetConfig.ts`, `bucketConfig.ts`, `datasetManager.ts`
- `featureEngineering.ts`, `featureOperator.ts`, `featureOperatorConfig.ts`
- `datasource.ts`, `dockerFileManager.ts`, `types.ts`

**路由**: `router/routes/modules/DPP.ts`

---

## 十、开发注意事项

### 10.1 MyBatis 映射

DPP 服务设置 `mybatis.configuration.map-underscore-to-camel-case=false`，意味着 DB 字段名和 Java 属性名需要 **手动映射**（`@Results` 注解或在 SQL 中使用别名）。

### 10.2 HDFS 路径规范

- 临时上传: `/temp/{uuid}_{filename}.temp`
- 数据集存储: `/datasets/{bucket_identifier}/{uid}/{filename}`
- ZIP 文件解压后: `/datasets/{bucket_identifier}/{uid}/{filename_without_zip}/`

### 10.3 前端双重解包

`defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'` 会产生双重解包。在 API 函数中需要兼容处理：

```typescript
const res = await dppRequestClient.get<any[]>('/datasets');
const data = Array.isArray(res) ? res : (res as any).data || [];
```

### 10.4 特征工程 JSON 字段

以下字段在数据库中以 JSON 字符串存储，前端提交时需要 `JSON.stringify()`，读取时需要 `JSON.parse()`：
- `selectedColumns` — 列名数组
- `featureConfig` — 每列特征配置
- `operatorParams` — 算子全局参数
- `scheduleConfig` — 调度配置
- `notificationConfig` — 通知配置

### 10.5 Jenkins 构建状态追踪

构建状态通过以下机制追踪：
1. 触发构建后保存到 Redis（key: `jenkins:build:{jobPath}`, TTL: 24h）
2. 后台线程每 5 秒轮询 Jenkins API
3. 状态更新同步写入 Redis + MySQL (FeatureTaskInfo)
4. 前端通过 GET `/feature-engineering/{id}/status` 轮询（先查 Redis，回退查 Jenkins API）
5. 状态流转: `QUEUED → IN_PROGRESS → SUCCESS/FAILURE/ABORTED/TIMEOUT`
