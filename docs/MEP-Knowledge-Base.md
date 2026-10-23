# MEP（模型端点平台）模块知识库

> **版本**: 1.0
> **更新日期**: 2026-02-12
> **适用范围**: XnetMLops 平台 MEP 模块前后端完整架构
> **说明**: 本文档为 RAG 知识库源文件，涵盖 MEP 模块的架构、API、数据模型、业务流程等全部细节

---

## 一、模块概述

### 1.1 基本信息

| 属性 | 值 |
|------|-----|
| 模块名称 | MEP (Model Endpoint Platform) 模型端点平台 |
| 后端服务 | `mlops-mep-service` (Spring Boot 3.4.6, Java 17) |
| 服务端口 | 8184 |
| 数据库 | MySQL `XnetMLops` (192.168.1.5:3306) |
| 前端路径 | `XnetMLops-web/apps/web-antd/src/views/MEP/` |
| API 路径 | `XnetMLops-web/apps/web-antd/src/views/MEP/api/` |
| 路由前缀 | `/MEP/` |
| 包基础路径 | `com.synapxnet.mlopsmepservice` |
| 加密密钥 | `openclaw.encryption.key` (默认 `XnetMLops2026Key`) |

### 1.2 五大子系统

MEP 模块由五个核心子系统组成：

| 子系统 | 功能 | 状态 |
|--------|------|------|
| LLM 服务管理 | 外部大模型服务注册、连接测试、启停管理 | 已完成 |
| API 密钥管理 | 密钥生成/导入、AES 加密存储、SHA-256 哈希、用量追踪 | 已完成 |
| 部署节点管理 | 服务器注册、SSH 连通测试、Docker/Nginx 状态、资源监控 | 部分完成（资源监控为 Mock） |
| 模型部署 | 4 步向导创建、Docker 容器部署、Nginx 代理、日志/指标监控 | 部分完成（部署流程为模拟） |
| OpenClaw 实例管理 | AI 编码助手部署（Docker/NPM/Source）、多模型配置、远程 SSH 部署、Gateway 生命周期管理 | 已完成 |

### 1.3 技术栈

**后端:**
- Spring Boot 3.4.6 + MyBatis 3.0.4（注解 SQL，无 XML Mapper）
- MySQL Connector/J（MySQL 驱动）
- Spring WebFlux WebClient（HTTP 调用外部服务）
- JSch 0.1.55（SSH/SFTP 远程服务器操作）
- Docker Java 3.3.4（Docker 客户端，已声明但实际使用 ProcessBuilder）
- Hadoop Client 3.3.4（HDFS 访问，已声明但当前未使用）
- Jackson Databind 2.17.0（JSON 序列化）
- Lombok（`@Data`, `@RequiredArgsConstructor`, `@Slf4j`）

**前端:**
- Vue 3 Composition API + TypeScript
- Ant Design Vue（UI 组件库）
- Vben Admin Pro 框架
- 请求客户端: `mepRequestClient`（基础 URL: `/mep`，开发代理至 `http://192.168.1.156:8184`）
- 响应拦截器: `defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'`（双重解包）

---

## 二、系统架构

### 2.1 后端架构

```
mlops-mep-service/src/main/java/com/synapxnet/mlopsmepservice/
├── MlopsMepServiceApplication.java          # Spring Boot 入口，@MapperScan
├── controller/
│   ├── ApiKeyController.java                # API 密钥 CRUD + 再生 + 状态切换
│   ├── DeployNodeController.java            # 部署节点 CRUD + 测试/刷新/维护
│   ├── LLMServiceController.java            # LLM 服务 CRUD + 启停/测试
│   ├── ModelDeploymentController.java       # 模型部署 CRUD + 生命周期 + 日志/指标
│   ├── MTPModelController.java              # MTP 训练模型代理接口
│   └── OpenClawController.java              # OpenClaw 实例管理（最复杂的控制器）
├── entity/
│   ├── ApiKey.java                          # API 密钥实体（加密、哈希、脱敏）
│   ├── DeploymentLog.java                   # 部署日志实体
│   ├── DeployNode.java                      # 部署节点实体
│   ├── LLMService.java                      # LLM 服务注册实体
│   ├── ModelDeployment.java                 # 模型部署实体（容器化）
│   └── OpenClawInstance.java                # OpenClaw 实例实体（最复杂，~30 字段）
├── mapper/
│   ├── ApiKeyMapper.java                    # xnet_mlops_mep_api_key
│   ├── DeployNodeMapper.java                # xnet_mlops_mep_deploy_node
│   ├── LLMServiceMapper.java               # xnet_mlops_mep_llm_service
│   ├── ModelDeploymentMapper.java           # xnet_mlops_mep_model_deployment
│   └── OpenClawInstanceMapper.java          # xnet_mlops_mep_openclaw_instance
├── service/
│   ├── ApiKeyService.java                   # 密钥业务：生成/哈希/加密/CRUD
│   ├── DeployNodeService.java               # 节点业务：连通测试/状态刷新
│   ├── LLMServiceService.java               # LLM 服务业务：WebClient 连接测试
│   ├── ModelDeploymentService.java          # 部署业务：异步部署/Docker 容器
│   ├── MTPModelService.java                 # 跨模块：从 MTP 获取训练输出模型
│   ├── OpenClawService.java                 # OpenClaw 核心（~700 行，最复杂的服务）
│   └── SshRemoteService.java               # SSH/SFTP 操作，SMP 凭据获取
└── resources/
    ├── application.yml                      # 配置（端口/DB/Docker/SMP/MTP URL）
    ├── scripts/
    │   ├── openclaw-deploy-linux.sh         # 全量部署脚本（9 步）
    │   └── openclaw-restart-linux.sh        # 轻量重启脚本（4 步）
    └── sql/
        ├── mep_tables.sql                   # 核心 MEP 表（6 张）
        └── openclaw_tables.sql              # OpenClaw + XAA 表（4 张）
```

### 2.2 前端架构

```
XnetMLops-web/apps/web-antd/src/views/MEP/
├── api/
│   ├── types.ts                             # TypeScript 类型定义（全部 MEP 实体）
│   ├── apiKey.ts                            # API 密钥接口
│   ├── deployment.ts                        # 模型部署接口
│   ├── llmService.ts                        # LLM 服务接口
│   └── node.ts                              # 部署节点接口
├── llmservice/
│   ├── index.vue                            # LLM 服务列表
│   ├── serviceCreate.vue                    # 新增 LLM 服务
│   └── serviceModify.vue                    # 编辑 LLM 服务
├── apikey/
│   └── index.vue                            # API 密钥管理（含模态框增删改）
├── deployment/
│   ├── index.vue                            # 模型部署列表
│   ├── deployCreate.vue                     # 4 步向导创建部署
│   └── deployDetail.vue                     # 部署详情（日志/指标/缩放）
├── nodes/
│   ├── index.vue                            # 部署节点列表（含模态框增删改）
│   └── nodeCreate.vue                       # 新增节点（独立页面）
└── openclaw/
    ├── index.vue                            # OpenClaw 实例列表（含日志抽屉）
    ├── create.vue                           # 创建/编辑 OpenClaw 实例（最复杂组件）
    └── detail.vue                           # OpenClaw 实例详情

路由配置: router/routes/modules/MEP.ts
请求客户端: api/request.ts (mepRequestClient)
跨模块引用:
  - SMP/api/workstation.ts (getOnlineWorkstations)
  - components/AssistantFloatingWindow/index.vue (OpenClaw 聊天集成)
  - XAA/assistant/create.vue (加载 OpenClaw 实例)
  - XAA/assistant/chat.vue (通过 OpenClaw Gateway SSE 对话)
```

### 2.3 application.yml 配置

```yaml
server.port: 8184
spring.datasource.url: jdbc:mysql://192.168.1.5:3306/XnetMLops
mybatis.mapper-locations: classpath:mapper/*.xml
mybatis.configuration.map-underscore-to-camel-case: true
docker.host: unix:///var/run/docker.sock
mtp.service.url: http://localhost:8183
smp.service.url: http://localhost:8185
# openclaw.encryption.key: XnetMLops2026Key (默认值)
```

---

## 三、数据模型

### 3.1 ApiKey — API 密钥

**表名**: `xnet_mlops_mep_api_key`
**实体类**: `entity/ApiKey.java`

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|----------|------|
| `id` | `Long` | `id` | 主键，自增 |
| `uid` | `String` | `uid` | UUID 唯一标识 |
| `name` | `String` | `name` | 密钥名称（唯一约束） |
| `keyHash` | `String` | `key_hash` | SHA-256 哈希值 |
| `keyMasked` | `String` | `key_masked` | 脱敏显示（如 `sk-abcd****efgh`） |
| `encryptedKey` | `String` | `encrypted_key` | AES 加密的原始密钥 |
| `provider` | `String` | `provider` | 服务商：`ollama`/`openai`/`deepseek`/`custom` |
| `description` | `String` | `description` | 描述 |
| `status` | `String` | `status` | 状态：`active`/`disabled`/`expired` |
| `usageLimit` | `Long` | `usage_limit` | 最大使用次数 |
| `usageCount` | `Long` | `usage_count` | 当前使用次数 |
| `expiresAt` | `LocalDateTime` | `expires_at` | 过期时间 |
| `createdBy` | `String` | `created_by` | 创建者 |
| `createdAt` | `LocalDateTime` | `created_at` | 创建时间 |
| `updatedAt` | `LocalDateTime` | `updated_at` | 更新时间 |

**加密方案**: AES/ECB/PKCS5Padding，密钥通过 `openclaw.encryption.key` 配置，默认 `XnetMLops2026Key`，补齐到 16/24/32 字节。

### 3.2 DeployNode — 部署节点

**表名**: `xnet_mlops_mep_deploy_node`
**实体类**: `entity/DeployNode.java`

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|----------|------|
| `id` | `Long` | `id` | 主键 |
| `uid` | `String` | `uid` | UUID 唯一标识 |
| `name` | `String` | `name` | 节点名称 |
| `ipAddress` | `String` | `ip_address` | IP 地址 |
| `port` | `Integer` | `port` | SSH 端口（默认 22） |
| `status` | `String` | `status` | 状态：`online`/`offline`/`maintenance` |
| `cpuCores` | `Integer` | `cpu_cores` | CPU 核数 |
| `memoryGb` | `Integer` | `memory_gb` | 内存 GB |
| `gpuInfo` | `String` | `gpu_info` | GPU 描述 |
| `dockerVersion` | `String` | `docker_version` | Docker 版本 |
| `nginxStatus` | `String` | `nginx_status` | Nginx 状态：`running`/`stopped` |
| `labels` | `String` | `labels` | JSON 数组标签 |
| `description` | `String` | `description` | 描述 |
| `createdBy` | `String` | `created_by` | 创建者 |
| `createdAt` | `LocalDateTime` | `created_at` | 创建时间 |
| `updatedAt` | `LocalDateTime` | `updated_at` | 更新时间 |

### 3.3 LLMService — 大模型服务

**表名**: `xnet_mlops_mep_llm_service`
**实体类**: `entity/LLMService.java`

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|----------|------|
| `id` | `Long` | `id` | 主键 |
| `uid` | `String` | `uid` | UUID 唯一标识 |
| `name` | `String` | `name` | 服务名称 |
| `type` | `String` | `type` | 类型：`ollama`/`openai`/`deepseek`/`custom` |
| `description` | `String` | `description` | 描述 |
| `endpoint` | `String` | `endpoint` | 服务端点 URL |
| `modelName` | `String` | `model_name` | 模型名称 |
| `apiKey` | `String` | `api_key` | API 密钥 |
| `status` | `String` | `status` | 状态：`running`/`stopped`/`error`/`deploying` |
| `config` | `String` | `config` | JSON 配置（max_tokens, temperature 等） |
| `createdBy` | `String` | `created_by` | 创建者 |
| `updatedBy` | `String` | `updated_by` | 更新者 |
| `createdAt` | `LocalDateTime` | `created_at` | 创建时间 |
| `updatedAt` | `LocalDateTime` | `updated_at` | 更新时间 |

### 3.4 ModelDeployment — 模型部署

**表名**: `xnet_mlops_mep_model_deployment`
**实体类**: `entity/ModelDeployment.java`

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|----------|------|
| `id` | `Long` | `id` | 主键 |
| `uid` | `String` | `uid` | UUID 唯一标识 |
| `name` | `String` | `name` | 部署名称 |
| `modelSource` | `String` | `model_source` | 模型来源：`mtp`（训练平台）或 `llm` |
| `modelUid` | `String` | `model_uid` | 源模型 UID |
| `modelName` | `String` | `model_name` | 模型显示名 |
| `modelVersion` | `String` | `model_version` | 模型版本 |
| `nodeUid` | `String` | `node_uid` | 目标部署节点 UID |
| `nodeName` | `String` | `node_name` | 目标节点名称 |
| `status` | `String` | `status` | 状态：`pending`/`deploying`/`running`/`failed`/`stopped` |
| `containerId` | `String` | `container_id` | Docker 容器 ID |
| `containerName` | `String` | `container_name` | Docker 容器名 |
| `imageName` | `String` | `image_name` | Docker 镜像名 |
| `port` | `Integer` | `port` | 服务端口 |
| `endpoint` | `String` | `endpoint` | 服务端点 URL |
| `replicas` | `Integer` | `replicas` | 副本数 |
| `resourceConfig` | `String` | `resource_config` | JSON 资源配置 |
| `nginxConfig` | `String` | `nginx_config` | JSON Nginx 配置 |
| `healthCheck` | `String` | `health_check` | JSON 健康检查配置 |
| `createdBy` | `String` | `created_by` | 创建者 |
| `createdAt` | `LocalDateTime` | `created_at` | 创建时间 |
| `updatedAt` | `LocalDateTime` | `updated_at` | 更新时间 |

### 3.5 DeploymentLog — 部署日志

**表名**: `xnet_mlops_mep_deployment_log`
**实体类**: `entity/DeploymentLog.java`

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|----------|------|
| `id` | `Long` | `id` | 主键 |
| `deploymentUid` | `String` | `deployment_uid` | 关联部署 UID |
| `level` | `String` | `level` | 日志级别：`info`/`warn`/`error` |
| `message` | `String` | `message` | 日志内容 |
| `timestamp` | `LocalDateTime` | `timestamp` | 时间戳 |

### 3.6 OpenClawInstance — OpenClaw 实例（最复杂实体，~30 字段）

**表名**: `xnet_mlops_mep_openclaw_instance`
**实体类**: `entity/OpenClawInstance.java`

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|----------|------|
| **基本信息** | | | |
| `id` | `Long` | `id` | 主键 |
| `uid` | `String` | `uid` | 格式：`OPENCLAW-XXXXXXXX` |
| `name` | `String` | `name` | 实例名称 |
| `description` | `String` | `description` | 描述 |
| **部署配置** | | | |
| `deployMode` | `String` | `deploy_mode` | 部署模式：`docker`/`npm`/`source` |
| `deployNodeId` | `String` | `deploy_node_id` | 部署节点 ID |
| `workstationId` | `Long` | `workstation_id` | SMP 工作站 ID（null=本地，非 null=远程） |
| `gatewayHost` | `String` | `gateway_host` | Gateway 主机地址（默认 `localhost`） |
| `gatewayPort` | `Integer` | `gateway_port` | Gateway 端口（默认 18789） |
| `gatewayToken` | `String` | `gateway_token` | Gateway 访问令牌 |
| **模型配置** | | | |
| `defaultModel` | `String` | `default_model` | 主模型（如 `anthropic/claude-opus-4-6`） |
| `fallbackModels` | `String` | `fallback_models` | 回退模型 JSON 数组 |
| `subagentModel` | `String` | `subagent_model` | 子代理模型（轻量级） |
| `llmServiceId` | `Long` | `llm_service_id` | 关联 MEP LLM 服务 ID |
| `apiKeyId` | `String` | `api_key_id` | AES 加密的 API 密钥（直接输入） |
| `apiKeyRefId` | `Long` | `api_key_ref_id` | 引用 MEP API 密钥管理的 ID |
| **技能配置** | | | |
| `enabledSkills` | `String` | `enabled_skills` | JSON 数组（启用的技能） |
| `skillsConfig` | `String` | `skills_config` | JSON 对象（技能配置） |
| **渠道配置** | | | |
| `channelsConfig` | `String` | `channels_config` | JSON 对象（渠道配置：Telegram 等） |
| **运行状态** | | | |
| `status` | `String` | `status` | 状态：`stopped`/`starting`/`running`/`error` |
| `containerName` | `String` | `container_name` | Docker 容器名 |
| `processId` | `String` | `process_id` | 操作系统进程 ID |
| `lastError` | `String` | `last_error` | 最后错误信息 |
| **资源路径** | | | |
| `workspacePath` | `String` | `workspace_path` | 工作空间目录 |
| `configPath` | `String` | `config_path` | 配置目录 |
| `logsPath` | `String` | `logs_path` | 日志目录 |
| **统计信息** | | | |
| `totalConversations` | `Long` | `total_conversations` | 总对话数 |
| `totalMessages` | `Long` | `total_messages` | 总消息数 |
| `lastActiveAt` | `LocalDateTime` | `last_active_at` | 最后活跃时间 |
| **元数据** | | | |
| `tenantUid` | `String` | `tenant_uid` | 租户 UID |
| `createdBy` | `String` | `created_by` | 创建者 |
| `updatedBy` | `String` | `updated_by` | 更新者 |
| `createdAt` | `LocalDateTime` | `created_at` | 创建时间 |
| `updatedAt` | `LocalDateTime` | `updated_at` | 更新时间 |

**远程/本地判断规则**: `workstationId != null` 为远程模式（SSH 操作），`workstationId == null` 为本地模式。

---

## 四、数据库表结构

### 4.1 核心 MEP 表（`mep_tables.sql`，6 张表）

| 表名 | 说明 |
|------|------|
| `xnet_mlops_mep_llm_service` | LLM 服务注册表 |
| `xnet_mlops_mep_api_key` | API 密钥管理表（`name` 唯一约束） |
| `xnet_mlops_mep_deploy_node` | 部署目标服务器表 |
| `xnet_mlops_mep_model_deployment` | 容器化模型部署表 |
| `xnet_mlops_mep_deployment_log` | 部署操作日志表 |
| `xnet_mlops_mep_service_metrics` | 服务监控指标表（CPU/内存/请求量） |

### 4.2 OpenClaw + XAA 表（`openclaw_tables.sql`，4 张表）

| 表名 | 说明 |
|------|------|
| `xnet_mlops_mep_openclaw_instance` | OpenClaw 实例管理表 |
| `xnet_mlops_xaa_assistant` | XAA 智能助手表（关联 OpenClaw） |
| `xnet_mlops_xaa_assistant_conversation` | XAA 对话会话表 |
| `xnet_mlops_xaa_assistant_message` | XAA 聊天消息表（user/assistant/system 角色） |

> **注**: XAA 表在 SQL 中定义，但 MEP 模块中尚无对应的 Java 实体/控制器/服务代码。

---

## 五、后端 API 端点

### 5.1 API 密钥管理 — `ApiKeyController`（`/mep/api-keys`）

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `GET` | `/mep/api-keys` | — | `List<ApiKey>` | 查询全部密钥 |
| `GET` | `/mep/api-keys/{id}` | `@PathVariable Long id` | `ApiKey` | 按 ID 查询 |
| `POST` | `/mep/api-keys` | `@RequestBody Map<String,Object>`：`name`, `api_key`(可选), `provider`, `description`, `usage_limit`, `expires_at` | `{id, uid, name, key_masked, plain_key, provider}` | 创建密钥（自动生成或用户提供） |
| `PUT` | `/mep/api-keys/{id}` | `@PathVariable Long id`, `@RequestBody ApiKey` | `ApiKey` | 更新密钥元数据 |
| `DELETE` | `/mep/api-keys/{id}` | `@PathVariable Long id` | — | 删除密钥 |
| `POST` | `/mep/api-keys/{id}/regenerate` | `@PathVariable Long id`, `@RequestBody Map`（可选 `api_key`） | `{plain_key}` | 重新生成密钥 |
| `PUT` | `/mep/api-keys/{id}/status` | `@PathVariable Long id`, `@RequestBody Map` with `status` | — | 更新密钥状态 |

### 5.2 部署节点管理 — `DeployNodeController`（`/mep/nodes`）

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `GET` | `/mep/nodes` | — | `List<DeployNode>` | 查询全部节点 |
| `GET` | `/mep/nodes/{id}` | `@PathVariable Long id` | `DeployNode` | 按 ID 查询 |
| `POST` | `/mep/nodes` | `@RequestBody DeployNode` | `DeployNode` | 创建节点 |
| `PUT` | `/mep/nodes/{id}` | `@PathVariable Long id`, `@RequestBody DeployNode` | `DeployNode` | 更新节点 |
| `DELETE` | `/mep/nodes/{id}` | `@PathVariable Long id` | — | 删除节点 |
| `POST` | `/mep/nodes/test-connection` | `@RequestBody Map`：`ip_address`, `port` | `{success, message, docker_version}` | 测试 SSH 连通性 |
| `POST` | `/mep/nodes/{id}/refresh` | `@PathVariable Long id` | `DeployNode` | 刷新节点状态 |
| `GET` | `/mep/nodes/{id}/resources` | `@PathVariable Long id` | `{cpu_usage, memory_usage, memory_total, disk_usage, disk_total, containers_running}` | 获取资源使用（当前为 Mock） |
| `POST` | `/mep/nodes/{id}/maintenance` | `@PathVariable Long id`, `@RequestBody Map` with `maintenance` | — | 切换维护模式 |

### 5.3 LLM 服务管理 — `LLMServiceController`（`/mep/llm-services`）

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `GET` | `/mep/llm-services` | — | `List<LLMService>` | 查询全部服务 |
| `GET` | `/mep/llm-services/{id}` | `@PathVariable Long id` | `LLMService` | 按 ID 查询 |
| `POST` | `/mep/llm-services` | `@RequestBody LLMService` | `LLMService` | 创建服务 |
| `PUT` | `/mep/llm-services/{id}` | `@PathVariable Long id`, `@RequestBody LLMService` | `LLMService` | 更新服务 |
| `DELETE` | `/mep/llm-services/{id}` | `@PathVariable Long id` | — | 删除服务（自动停止） |
| `POST` | `/mep/llm-services/{id}/start` | `@PathVariable Long id` | — | 启动服务 |
| `POST` | `/mep/llm-services/{id}/stop` | `@PathVariable Long id` | — | 停止服务 |
| `POST` | `/mep/llm-services/test-connection` | `@RequestBody Map`：`endpoint`, `api_key`, `type` | `{success, message}` | 测试 LLM 端点连通性 |

**连接测试逻辑**: 根据 `type` 发送 WebClient GET 请求 — `ollama` → `/api/tags`, `openai`/`deepseek` → `/models`, 其他 → `/health`

### 5.4 模型部署管理 — `ModelDeploymentController`（`/mep/deployments`）

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `GET` | `/mep/deployments` | — | `List<ModelDeployment>` | 查询全部部署 |
| `GET` | `/mep/deployments/{id}` | `@PathVariable Long id` | `ModelDeployment` | 按 ID 查询 |
| `POST` | `/mep/deployments` | `@RequestBody ModelDeployment` | `ModelDeployment` | 创建部署（自动异步启动） |
| `PUT` | `/mep/deployments/{id}` | `@PathVariable Long id`, `@RequestBody ModelDeployment` | `ModelDeployment` | 更新部署 |
| `DELETE` | `/mep/deployments/{id}` | `@PathVariable Long id` | — | 删除（自动停止） |
| `POST` | `/mep/deployments/{id}/start` | `@PathVariable Long id` | — | 启动部署 |
| `POST` | `/mep/deployments/{id}/stop` | `@PathVariable Long id` | — | 停止部署 |
| `POST` | `/mep/deployments/{id}/restart` | `@PathVariable Long id` | — | 重启部署 |
| `POST` | `/mep/deployments/{id}/scale` | `@PathVariable Long id`, `@RequestBody Map` with `replicas` | — | 缩放副本数 |
| `GET` | `/mep/deployments/{id}/logs` | `@PathVariable Long id`, `@RequestParam limit`(默认100), `@RequestParam since` | `List<Map>` | 获取部署日志（Mock） |
| `GET` | `/mep/deployments/{id}/metrics` | `@PathVariable Long id`, `@RequestParam start`, `@RequestParam end` | `List<Map>` | 获取部署指标（Mock） |

### 5.5 MTP 模型代理 — `MTPModelController`（`/mep/mtp-models`）

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `GET` | `/mep/mtp-models` | — | `List<Map>` | 从 MTP 训练平台获取输出模型列表 |

**跨模块调用**: WebClient GET → `${mtp.service.url}/api/mtp/tasks`，Header: `X-Tenant-Uid: default`，过滤已完成任务并转换为模型格式。

### 5.6 OpenClaw 实例管理 — `OpenClawController`（`/mep/openclaw`）

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| `GET` | `/mep/openclaw/instances` | — | `List<OpenClawInstance>` | 查询全部实例 |
| `GET` | `/mep/openclaw/instances/{id}` | `@PathVariable Long id` | `OpenClawInstance` | 按 ID 查询 |
| `POST` | `/mep/openclaw/instances` | `@RequestBody Map<String,Object>`（见下方） | `OpenClawInstance` | 创建实例 |
| `PUT` | `/mep/openclaw/instances/{id}` | `@PathVariable Long id`, `@RequestBody Map` | `OpenClawInstance` | 更新实例配置 |
| `DELETE` | `/mep/openclaw/instances/{id}` | `@PathVariable Long id` | — | 删除实例（自动停止） |
| `POST` | `/mep/openclaw/instances/{id}/start` | `@PathVariable Long id` | `{success, message, containerName/pid}` | 启动实例 |
| `POST` | `/mep/openclaw/instances/{id}/stop` | `@PathVariable Long id` | `{success, message}` | 停止实例 |
| `POST` | `/mep/openclaw/instances/{id}/restart` | `@PathVariable Long id` | `{success, message, pid}` | 重启实例 |
| `GET` | `/mep/openclaw/instances/{id}/status` | `@PathVariable Long id` | `{id, uid, status, gatewayHost, gatewayPort, healthy}` | 获取状态+健康检查 |
| `GET` | `/mep/openclaw/instances/{id}/logs` | `@PathVariable Long id`, `@RequestParam lines`(默认100) | `String`（日志文本） | 获取实例日志 |
| `POST` | `/mep/openclaw/test-connection` | `@RequestBody Map`：`host`, `port`, `token` | `{success, message}` | 测试 Gateway 连通性 |
| `GET` | `/mep/openclaw/deploy-modes` | — | `List<Map>` | 获取可用部署模式列表 |
| `GET` | `/mep/openclaw/models` | — | `List<Map>` | 获取支持的模型列表（Anthropic/OpenAI/Google/DeepSeek） |
| `POST` | `/mep/openclaw/generate-token` | `@RequestBody Map` with `instanceName` | `{token, apiKeyId, keyMasked}` | 生成 Gateway 访问令牌（创建托管 API 密钥） |

**创建实例请求体字段**（Map 手动提取）：
- `name`, `description` — 基本信息
- `deployMode`（默认 "docker"）, `workstationId`（可空）— 部署配置
- `gatewayHost`（默认 "localhost"）, `gatewayPort`（默认 18789）, `gatewayToken` — Gateway 配置
- `defaultModel`, `fallbackModels`（数组→JSON 字符串）, `subagentModel` — 模型配置
- `llmServiceId`, `apiKeyRefId`, `apiKey` — 服务/密钥关联
- `enabledSkills`, `channelsConfig` — 高级配置

**重要模式**: `OpenClawController` 和 `ApiKeyController` 使用 `Map<String, Object>` 接收请求体，手动类型转换提取字段。其他控制器使用类型化实体类。

---

## 六、后端服务层

### 6.1 ApiKeyService — API 密钥业务

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `findAll()` | `List<ApiKey>` | 委托 Mapper |
| `findById(Long)` | `ApiKey` | 委托 Mapper |
| `findByUid(String)` | `ApiKey` | 委托 Mapper |
| `findByName(String)` | `ApiKey` | 唯一性检查 |
| `create(Map<String,Object>)` | `Map` | 名称唯一检查→生成/导入密钥→SHA-256 哈希→脱敏→AES 加密→插入→返回 `{id, uid, name, key_masked, plain_key, provider}` |
| `update(ApiKey)` | `ApiKey` | 名称唯一检查（排除自身）→更新 |
| `delete(Long)` | `void` | 按 ID 删除 |
| `regenerate(Long, String)` | `Map` | 重新生成或替换密钥→更新哈希/脱敏/加密 |
| `decryptApiKeyById(Long)` | `String` | 按 ID 查找→AES 解密（供 OpenClawService 调用） |
| `updateStatus(Long, String)` | `void` | 更新状态字段 |

**密钥生成规则**: `sk-` + 32 字节随机数的 URL-safe Base64 编码
**脱敏规则**: 前 7 字符 + `****` + 后 4 字符

### 6.2 DeployNodeService — 部署节点业务

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `findAll()` | `List<DeployNode>` | 委托 Mapper |
| `findById(Long)` | `DeployNode` | 委托 Mapper |
| `create(DeployNode)` | `DeployNode` | 生成 UUID→初始状态 "offline"→插入→自动刷新状态 |
| `update(DeployNode)` | `DeployNode` | 更新 |
| `delete(Long)` | `void` | 删除 |
| `testConnection(String, Integer)` | `Map` | Socket 连通测试（5 秒超时）→`{success, message, docker_version}` |
| `refreshStatus(Long)` | `DeployNode` | Socket 测试 + Docker 版本 + Nginx 状态→更新数据库 |
| `getResources(Long)` | `Map` | 资源使用（**当前为 Mock 随机数据**） |
| `setMaintenance(Long, Boolean)` | `void` | 切换 "maintenance"/"online" 状态 |

> **注**: `getDockerVersion()` 和 `checkNginxStatus()` 为**桩方法**，分别固定返回 `"24.0.7"` 和 `true`。

### 6.3 LLMServiceService — LLM 服务业务

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `findAll()` | `List<LLMService>` | 委托 Mapper |
| `findById(Long)` | `LLMService` | 委托 Mapper |
| `create(LLMService)` | `LLMService` | 生成 UUID→状态 "stopped"→插入 |
| `update(LLMService)` | `LLMService` | 更新 |
| `delete(Long)` | `void` | 如运行中先停止→删除 |
| `start(Long)` | `void` | 数据库中设置状态为 "running" |
| `stop(Long)` | `void` | 数据库中设置状态为 "stopped" |
| `testConnection(String, String, String)` | `Map` | WebClient GET 到对应健康端点 |

### 6.4 ModelDeploymentService — 模型部署业务

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `findAll()` | `List<ModelDeployment>` | 委托 Mapper |
| `findById(Long)` | `ModelDeployment` | 委托 Mapper |
| `create(ModelDeployment)` | `ModelDeployment` | 生成 UUID→状态 "pending"→构建端点→插入→后台 `deployAsync()` |
| `start(Long)` | `void` | 设 "deploying"→后台线程 sleep 3s→伪容器 ID→设 "running" |
| `stop(Long)` | `void` | 设 "stopped" |
| `restart(Long)` | `void` | stop() → sleep 1s → start() |
| `scale(Long, Integer)` | `void` | 更新副本数 |
| `getLogs(Long, Integer, String)` | `List<Map>` | **Mock 数据**（示例日志） |
| `getMetrics(Long, String, String)` | `List<Map>` | **Mock 数据**（随机指标） |

> **注**: `deployAsync()` 为**模拟部署** — sleep 5s，生成伪容器 ID，无真实 Docker 操作。

### 6.5 MTPModelService — 跨模块模型获取

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `getOutputModels()` | `List<Map>` | WebClient GET → `${mtp.service.url}/api/mtp/tasks`，Header: `X-Tenant-Uid: default`，过滤已完成任务→转换为模型格式 |
| `convertTaskToModel(Map)` | `Map` | 提取 `uid`, `task_name`, `algorithm_name`, `algorithm_version`, `output_config`, `created_at`，添加 `source: "mtp"` |

### 6.6 OpenClawService — 核心服务（~700 行）

**依赖注入**: `OpenClawInstanceMapper`, `LLMServiceService`, `ApiKeyService`, `SshRemoteService`, `WebClient.Builder`, `ObjectMapper`
**运行时状态**: `ConcurrentHashMap<Long, Process> runningProcesses` — 按实例 ID 追踪本地运行进程

#### CRUD 方法

| 方法 | 逻辑 |
|------|------|
| `findAll()` / `findById()` / `findByUid()` | 委托 Mapper |
| `create(OpenClawInstance)` | UID = `OPENCLAW-` + 8 字符大写 UUID；端口默认 18789；**远程分支**（workstationId != null）：从 SMP 获取 SSH 凭据→确定远程 home 目录→SSH 创建远程目录 `{home}/.openclaw/instances/{UID}/{workspace,config,logs}`；**本地分支**：创建本地目录 `~/.openclaw/instances/{UID}/` |
| `update(OpenClawInstance)` | 更新 |
| `delete(Long)` | 如运行中先停止→删除 |

#### 生命周期方法

| 方法 | 逻辑 |
|------|------|
| `start(Long)` | 设状态 "starting"→生成并部署配置→按 `deployMode` 分支：`docker` → `startWithDocker()`, `npm` → `startWithNpm()`, `source` → `startFromSource()` |
| `stop(Long)` | **远程 Docker**: SSH `docker stop/rm`；**远程进程**: 通过 PID 文件 + 存储的 processId kill；**本地进程**: `runningProcesses.remove().destroy()`；**本地 Docker**: ProcessBuilder `docker stop/rm` |
| `restart(Long)` | 远程 source 模式 → `restartQuick()`（轻量）；其他 → `stop()` + `start()` |
| `restartQuick(Long)` | 重新生成配置→加载 `openclaw-restart-linux.sh` 模板→SSH 上传执行→提取 PID |

#### 状态/日志/测试方法

| 方法 | 逻辑 |
|------|------|
| `getStatus(Long)` | 返回实例信息；如运行中执行 HTTP GET `http://{host}:{port}/health` 健康检查 |
| `getLogs(Long, Integer)` | **远程**: SSH `tail -n {lines} {logsPath}/gateway.log`；**本地**: 直接读文件 |
| `testConnection(String, Integer, String)` | WebClient GET → `http://{host}:{port}/health`，Header: `X-OpenClaw-Token` |

#### 配置生成方法（私有）

| 方法 | 逻辑 |
|------|------|
| `buildConfigMap(OpenClawInstance)` | 构建完整 `openclaw.json` 配置（见第九章） |
| `buildModelAlias(String)` | 创建 `{alias: "Provider ModelId"}` 白名单条目 |
| `generateAndDeployConfig(OpenClawInstance)` | 序列化为 JSON；**远程**: SFTP 上传至 `{configPath}/openclaw.json`；**本地**: 写文件 |
| `resolveApiKeyPlaintext(OpenClawInstance)` | 优先级：`apiKeyRefId` → `apiKeyService.decryptApiKeyById()`，回退：`apiKeyId` → `decryptApiKey()` |
| `buildProviderConfig(List<String>)` | 收集所有非内置 provider，生成 `models.providers` 配置 |
| `buildApiKeyEnvExport(OpenClawInstance)` | 生成 API 密钥环境变量导出语句（如 `export ANTHROPIC_API_KEY='...'`） |
| `getProviderFromModel(String)` | 从 `{provider}/{model}` 格式提取 provider |

### 6.7 SshRemoteService — SSH/SFTP 远程操作

| 方法 | 签名 | 逻辑 |
|------|------|------|
| `fetchCredentials(Long workstationId)` | `Map<String,Object>` | WebClient GET → `${smp.service.url}/api/smp/workstations/{id}/credentials`，返回解密的 SSH 凭据 |
| `executeCommand(Map creds, String command)` | `String` | 通过 JSch 连接（密码/私钥认证），执行远程命令，返回 stdout+stderr |
| `executeCommand(Map creds, String command, long timeout)` | `String` | 带超时版本（用于部署脚本） |
| `uploadFile(Map creds, String content, String remotePath)` | `void` | SFTP 上传文件内容到远程路径 |
| `uploadFile(Map creds, byte[] content, String remotePath)` | `void` | SFTP 上传字节数组 |

**JSch 连接配置**: `StrictHostKeyChecking=no`, 连接超时 30s, 命令超时可配

---

## 七、Mapper/DAO 层

所有 Mapper 使用 **MyBatis 注解 SQL**（无 XML 映射文件），表名前缀 `xnet_mlops_mep_`。

### 7.1 ApiKeyMapper（`xnet_mlops_mep_api_key`）

| 方法 | SQL 操作 | 备注 |
|------|----------|------|
| `findAll()` | `SELECT ... ORDER BY created_at DESC` | 显式列列表 + `@Results` |
| `findById(Long)` | `SELECT ... WHERE id` | |
| `findByUid(String)` | `SELECT ... WHERE uid` | |
| `findByName(String)` | `SELECT ... WHERE name` | 唯一性检查 |
| `insert(ApiKey)` | `INSERT INTO ...` | `@Options(useGeneratedKeys=true)` |
| `update(ApiKey)` | `UPDATE ... SET name, description, status, usage_limit, expires_at` | |
| `updateKey(Long, String, String, String)` | `UPDATE ... SET key_hash, key_masked, encrypted_key` | 密钥再生 |
| `updateStatus(Long, String)` | `UPDATE ... SET status` | |
| `incrementUsageCount(Long)` | `UPDATE ... SET usage_count = usage_count + 1` | |
| `deleteById(Long)` | `DELETE ... WHERE id` | |

### 7.2 DeployNodeMapper（`xnet_mlops_mep_deploy_node`）

| 方法 | SQL 操作 |
|------|----------|
| `findAll()` | `SELECT * ORDER BY created_at DESC` |
| `findById(Long)` / `findByUid(String)` | 按 ID/UID 查询 |
| `findOnlineNodes()` | `SELECT * WHERE status = 'online'` |
| `insert(DeployNode)` | `INSERT` + 自增主键 |
| `update(DeployNode)` | `UPDATE name, ip_address, port, cpu_cores, memory_gb, gpu_info, labels, description` |
| `updateStatus(Long, String, String, String)` | `UPDATE status, docker_version, nginx_status` |
| `deleteById(Long)` | `DELETE` |

### 7.3 LLMServiceMapper（`xnet_mlops_mep_llm_service`）

| 方法 | SQL 操作 |
|------|----------|
| `findAll()` | `SELECT * ORDER BY created_at DESC` |
| `findById(Long)` / `findByUid(String)` | 按 ID/UID 查询 |
| `insert(LLMService)` | `INSERT` + 自增主键 |
| `update(LLMService)` | `UPDATE name, description, endpoint, model_name, api_key, config, updated_by` |
| `updateStatus(Long, String)` | `UPDATE status` |
| `deleteById(Long)` | `DELETE` |

### 7.4 ModelDeploymentMapper（`xnet_mlops_mep_model_deployment`）

| 方法 | SQL 操作 |
|------|----------|
| `findAll()` | `SELECT * ORDER BY created_at DESC` |
| `findById(Long)` / `findByUid(String)` | 按 ID/UID 查询 |
| `findByNodeUid(String)` | 按节点 UID 查询 |
| `findByStatus(String)` | 按状态查询 |
| `insert(ModelDeployment)` | `INSERT`（全字段）+ 自增主键 |
| `update(ModelDeployment)` | `UPDATE name, replicas, resource_config, nginx_config, health_check` |
| `updateStatus(Long, String, String, String)` | `UPDATE status, container_id, endpoint` |
| `updateReplicas(Long, Integer)` | `UPDATE replicas` |
| `deleteById(Long)` | `DELETE` |

### 7.5 OpenClawInstanceMapper（`xnet_mlops_mep_openclaw_instance`）

| 方法 | SQL 操作 |
|------|----------|
| `findAll()` | `SELECT * ORDER BY created_at DESC` |
| `findById(Long)` / `findByUid(String)` | 按 ID/UID 查询 |
| `findByStatus(String)` | 按状态查询 |
| `insert(OpenClawInstance)` | `INSERT`（全部部署/模型/技能/渠道/状态/路径/元数据字段） |
| `update(OpenClawInstance)` | `UPDATE name, description, deploy_mode, deploy_node_id, workstation_id, gateway_host, gateway_port, gateway_token, default_model, fallback_models, subagent_model, llm_service_id, api_key_id, api_key_ref_id, enabled_skills, skills_config, channels_config, workspace_path, config_path, logs_path, updated_by` |
| `updateStatus(Long, String)` | `UPDATE status` |
| `updateRuntime(Long, String, String, String, String)` | `UPDATE status, container_name, process_id, last_error` |
| `updateStats(Long, Long, Long, LocalDateTime)` | `UPDATE total_conversations, total_messages, last_active_at` |
| `deleteById(Long)` | `DELETE` |

---

## 八、前端 API 层

### 8.1 HTTP 客户端配置

**文件**: `api/request.ts`

- **客户端**: `mepRequestClient` — `createRequestClient(mepApiURL, { responseReturn: 'data' })`
- **开发环境 URL**: `/mep`（Vite 代理至 `http://192.168.1.156:8184`）
- **生产环境 URL**: `http://192.168.1.156:8184/api`
- **超时**: 600,000ms（10 分钟）
- **响应拦截**: `defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'` — **双重解包模式**
- **认证头**: `Bearer {accessToken}` + `X-User-Id`

### 8.2 LLM 服务 API（`llmService.ts`）

| 函数 | 方法 | URL | 参数 | 返回类型 |
|------|------|-----|------|----------|
| `fetchLLMServiceList()` | GET | `/llm-services` | — | `LLMService[]` |
| `fetchLLMServiceDetail(id)` | GET | `/llm-services/{id}` | `id: number` | `LLMService` |
| `createLLMService(payload)` | POST | `/llm-services` | `Omit<LLMService, ...>` | `LLMService` |
| `updateLLMService(id, payload)` | PUT | `/llm-services/{id}` | `id`, `Partial<LLMService>` | `LLMService` |
| `deleteLLMService(id)` | DELETE | `/llm-services/{id}` | `id` | `void` |
| `startLLMService(id)` | POST | `/llm-services/{id}/start` | `id` | `void` |
| `stopLLMService(id)` | POST | `/llm-services/{id}/stop` | `id` | `void` |
| `testLLMServiceConnection(payload)` | POST | `/llm-services/test-connection` | `{endpoint, api_key?, type}` | `{success, message}` |

### 8.3 API 密钥 API（`apiKey.ts`）

| 函数 | 方法 | URL | 参数 | 返回类型 |
|------|------|-----|------|----------|
| `fetchApiKeyList()` | GET | `/api-keys` | — | `ApiKeyItem[]` |
| `fetchApiKeyDetail(id)` | GET | `/api-keys/{id}` | `id` | `ApiKeyItem` |
| `createApiKey(payload)` | POST | `/api-keys` | `Omit<ApiKeyItem, ...>` | `ApiKeyItem & {plain_key}` |
| `updateApiKey(id, payload)` | PUT | `/api-keys/{id}` | `id`, 部分字段 | `ApiKeyItem` |
| `deleteApiKey(id)` | DELETE | `/api-keys/{id}` | `id` | `void` |
| `regenerateApiKey(id)` | POST | `/api-keys/{id}/regenerate` | `id` | `{plain_key}` |
| `toggleApiKeyStatus(id, status)` | PUT | `/api-keys/{id}/status` | `id`, `status` | `void` |

### 8.4 部署 API（`deployment.ts`）

| 函数 | 方法 | URL | 参数 | 返回类型 |
|------|------|-----|------|----------|
| `fetchDeploymentList()` | GET | `/deployments` | — | `ModelDeployment[]` |
| `fetchDeploymentDetail(id)` | GET | `/deployments/{id}` | `id` | `ModelDeployment` |
| `createDeployment(payload)` | POST | `/deployments` | `Omit<ModelDeployment, ...>` | `ModelDeployment` |
| `updateDeployment(id, payload)` | PUT | `/deployments/{id}` | `id`, `Partial<ModelDeployment>` | `ModelDeployment` |
| `deleteDeployment(id)` | DELETE | `/deployments/{id}` | `id` | `void` |
| `startDeployment(id)` | POST | `/deployments/{id}/start` | `id` | `void` |
| `stopDeployment(id)` | POST | `/deployments/{id}/stop` | `id` | `void` |
| `restartDeployment(id)` | POST | `/deployments/{id}/restart` | `id` | `void` |
| `scaleDeployment(id, replicas)` | POST | `/deployments/{id}/scale` | `id`, `{replicas}` | `void` |
| `fetchDeploymentLogs(id, params?)` | GET | `/deployments/{id}/logs` | `id`, `{limit?, since?}` | `DeploymentLog[]` |
| `fetchDeploymentMetrics(id, params?)` | GET | `/deployments/{id}/metrics` | `id`, `{start?, end?}` | `ServiceMetrics[]` |
| `fetchMTPOutputModels()` | GET | `/mtp-models` | — | `MTPOutputModel[]` |

### 8.5 节点 API（`node.ts`）

| 函数 | 方法 | URL | 参数 | 返回类型 |
|------|------|-----|------|----------|
| `fetchNodeList()` | GET | `/nodes` | — | `DeployNode[]` |
| `fetchNodeDetail(id)` | GET | `/nodes/{id}` | `id` | `DeployNode` |
| `createNode(payload)` | POST | `/nodes` | `Omit<DeployNode, ...>` | `DeployNode` |
| `updateNode(id, payload)` | PUT | `/nodes/{id}` | `id`, `Partial<DeployNode>` | `DeployNode` |
| `deleteNode(id)` | DELETE | `/nodes/{id}` | `id` | `void` |
| `testNodeConnection(payload)` | POST | `/nodes/test-connection` | `{ip_address, port}` | `{success, message, docker_version?}` |
| `refreshNodeStatus(id)` | POST | `/nodes/{id}/refresh` | `id` | `DeployNode` |
| `fetchNodeResources(id)` | GET | `/nodes/{id}/resources` | `id` | `{cpu_usage, memory_usage, ...}` |
| `setNodeMaintenance(id, maintenance)` | POST | `/nodes/{id}/maintenance` | `id`, `{maintenance}` | `void` |

### 8.6 OpenClaw API（内联在组件中，无独立 API 文件）

OpenClaw 端点直接在 Vue 组件中通过 `mepRequestClient` 调用：

| 调用组件 | 方法 | URL | 说明 |
|----------|------|-----|------|
| `openclaw/index.vue` | GET | `/openclaw/instances` | 列表查询 |
| `openclaw/index.vue` | DELETE | `/openclaw/instances/{id}` | 删除实例 |
| `openclaw/index.vue` | POST | `/openclaw/instances/{id}/start\|stop\|restart` | 生命周期操作 |
| `openclaw/index.vue` | GET | `/openclaw/instances/{id}/logs?lines=` | 查看日志 |
| `openclaw/create.vue` | GET | `/openclaw/instances/{id}` | 加载编辑数据 |
| `openclaw/create.vue` | POST/PUT | `/openclaw/instances` | 创建/更新 |
| `openclaw/create.vue` | GET | `/api-keys` | 加载 API 密钥选项 |
| `openclaw/create.vue` | GET | `/llm-services` | 加载 LLM 服务选项 |
| `openclaw/create.vue` | GET | `/openclaw/models` | 加载模型选项 |
| `openclaw/create.vue` | POST | `/openclaw/generate-token` | 生成 Gateway 令牌 |
| `openclaw/create.vue` | POST | `/openclaw/test-connection` | 测试 Gateway 连通性 |
| `openclaw/detail.vue` | GET | `/openclaw/instances/{id}` | 加载详情 |
| `openclaw/detail.vue` | POST | `/openclaw/instances/{id}/start\|stop\|restart` | 生命周期操作 |
| XAA assistant create | GET | `/openclaw/instances` | 选择 OpenClaw 实例 |

---

## 九、OpenClaw 配置生成

### 9.1 openclaw.json 结构

`buildConfigMap()` 方法生成的完整配置：

```json
{
  "gateway": {
    "port": 18789,
    "bind": "lan",
    "auth": { "token": "{gatewayToken 或 UID}" },
    "mode": "local",
    "controlUi": { "allowInsecureAuth": true },
    "http": {
      "endpoints": {
        "chatCompletions": { "enabled": true }
      }
    }
  },
  "agents": {
    "defaults": {
      "model": {
        "primary": "anthropic/claude-opus-4-6",
        "fallbacks": ["openai/gpt-5.2", "deepseek/deepseek-chat"]
      },
      "models": {
        "anthropic/claude-opus-4-6": { "alias": "Anthropic claude-opus-4-6" },
        "openai/gpt-5.2": { "alias": "OpenAI gpt-5.2" },
        "deepseek/deepseek-chat": { "alias": "Deepseek deepseek-chat" }
      },
      "subagents": { "model": "anthropic/claude-sonnet-4-5" }
    }
  },
  "models": {
    "mode": "merge",
    "providers": {
      "deepseek": {
        "baseUrl": "https://api.deepseek.com/v1",
        "apiKey": "${DEEPSEEK_API_KEY}",
        "api": "openai-completions",
        "models": [{ "id": "deepseek-chat", "name": "Deepseek deepseek-chat" }]
      }
    }
  },
  "channels": { "...渠道配置..." }
}
```

### 9.2 关键设计决策

| 配置项 | 说明 |
|--------|------|
| `bind: "lan"` | 绑定局域网，需要认证（token）— 如未提供则自动使用 UID |
| `controlUi.allowInsecureAuth: true` | 允许非 HTTPS 认证（远程局域网访问） |
| `chatCompletions.enabled: true` | 启用 OpenAI 兼容的 HTTP API |
| **内置 Provider** | `anthropic`, `openai`, `google`, `groq` 等只需环境变量 |
| **非内置 Provider** | `deepseek`, `together` 等需要显式 `models.providers` 配置 |
| **设备自动审批** | 通过 `devices/pending.json` 文件实现（不在 openclaw.json 中） |

### 9.3 多模型配置（三级模型）

| 级别 | 字段 | 用途 |
|------|------|------|
| 主模型 | `defaultModel` → `agents.defaults.model.primary` | 默认使用的模型 |
| 回退模型 | `fallbackModels` → `agents.defaults.model.fallbacks` | 主模型失败时按顺序尝试 |
| 子代理模型 | `subagentModel` → `agents.defaults.subagents.model` | 子任务使用的轻量级模型 |

所有模型自动注册到 `agents.defaults.models` 白名单。Provider 从 `{provider}/{model}` 格式自动推断。

### 9.4 API 密钥环境变量

`buildApiKeyEnvExport()` 根据 provider 生成对应的环境变量导出：
- `anthropic` → `export ANTHROPIC_API_KEY='...'`
- `openai` → `export OPENAI_API_KEY='...'`
- `deepseek` → `export DEEPSEEK_API_KEY='...'`
- 其他 → `export {PROVIDER}_API_KEY='...'`

密钥解析优先级：`apiKeyRefId`（托管密钥 ID）优先，`apiKeyId`（直接加密密钥）回退。

---

## 十、Shell 脚本模板

### 10.1 全量部署脚本（`openclaw-deploy-linux.sh`，9 步）

**模板变量**（Java `String.replace()` 替换）：
- `${GATEWAY_PORT}` — 从 `instance.getGatewayPort()`
- `${CONFIG_PATH}` — 从 `instance.getConfigPath()`
- `${LOGS_PATH}` — 从 `instance.getLogsPath()`
- `${SOURCE_PATH}` — 从 `configPath` 推导
- `${API_KEY_ENV}` — 从 `buildApiKeyEnvExport()` 生成

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | 系统检测 | Linux 检测、CPU 架构、内存、磁盘 ≥2GB、端口冲突检查 |
| 2 | 安装基础依赖 | curl, git（apt/dnf/yum 适配） |
| 3 | 安装 Node.js ≥22 | 官方二进制 tarball（x64/arm64/armv7l） |
| 4 | 启用 pnpm | corepack 或 npm 回退 |
| 5 | Git clone OpenClaw | 含镜像回退（ghproxy.net, mirror.ghproxy.com, gitclone.com） |
| 6 | 构建 | `pnpm install` → `pnpm ui:build` → `pnpm build` |
| 7 | 准备配置 | 创建目录、验证 `openclaw.json`、写入 `devices/pending.json` |
| 8 | 启动 Gateway | `nohup pnpm openclaw gateway --port {port}`，写入 PID 文件 |
| 9 | 健康检查 | 60s 超时，接受 HTTP 200 或 403 |

**输出标记**: `OPENCLAW_PID={pid}`（后端解析提取）

### 10.2 轻量重启脚本（`openclaw-restart-linux.sh`，4 步）

跳过步骤 2-6（环境/Node.js/pnpm/git/构建），仅：

| 步骤 | 操作 |
|------|------|
| 1 | Kill 现有进程（PID 文件 + 端口检查） |
| 2 | 准备配置目录，验证 `openclaw.json`，写入 `devices/pending.json` |
| 3 | 启动 Gateway（同全量部署步骤 8） |
| 4 | 健康检查（同全量部署步骤 9） |

---

## 十一、前端页面组件

### 11.1 路由配置（`router/routes/modules/MEP.ts`）

**父路由**: `MEP:manager`，路径 `/MEP`，图标 `ic:baseline-cloud-upload`，菜单标题 "模型部署(MEP)"

| 路由名 | 路径 | 菜单可见 | 标题 |
|--------|------|----------|------|
| `MEP:llmservice` | `/MEP/llmservice/index` | 是 | 大模型服务 |
| `MEP:llmservice:create` | `/MEP/llmservice/create` | 隐藏 | 新增大模型服务 |
| `MEP:llmservice:modify` | `/MEP/llmservice/modify` | 隐藏 | 编辑大模型服务 |
| `MEP:apikey` | `/MEP/apikey/index` | 是 | API密钥管理 |
| `MEP:deployment` | `/MEP/deployment/index` | 是 | 模型部署 |
| `MEP:deployment:create` | `/MEP/deployment/create` | 隐藏 | 新建部署 |
| `MEP:deployment:detail` | `/MEP/deployment/detail` | 隐藏 | 部署详情 |
| `MEP:nodes` | `/MEP/nodes/index` | 是 | 部署节点 |
| `MEP:nodes:create` | `/MEP/nodes/create` | 隐藏 | 新增节点 |
| `MEP:openclaw` | `/MEP/openclaw/index` | 是 | OpenClaw部署 |
| `MEP:openclaw:create` | `/MEP/openclaw/create` | 隐藏 | 创建OpenClaw实例 |
| `MEP:openclaw:detail` | `/MEP/openclaw/detail` | 隐藏 | OpenClaw实例详情 |

**导航层级**: 侧边栏显示 5 个顶级菜单项，创建/编辑/详情页面隐藏，通过 `router.push()` 程序化访问。

### 11.2 LLM 服务列表（`llmservice/index.vue`）

**搜索栏**: 名称（Input）、类型（Select: ollama/openai/deepseek/custom）、状态（Select: running/stopped/error/deploying）
**操作栏**: 新增服务、批量删除（勾选行）、刷新
**表格列**: 服务名称（可点击链接）、类型（彩色 Tag）、模型名称、服务端点（省略）、状态（Tag + 图标，deploying 旋转）、描述、创建时间、操作
**操作列**: 启动/停止切换、编辑、删除（Popconfirm）
**行选择**: 支持批量删除操作

**数据管理**: `serviceList: ref<LLMService[]>([])`，`filteredList: computed()`（客户端过滤），`onMounted → fetchLLMServiceList()`

### 11.3 LLM 服务创建（`llmservice/serviceCreate.vue`）

**表单布局**: Card 标题 "新增大模型服务"，`layout="vertical"`
- 服务名称 + 服务类型（带描述的 Select 选项）
- 服务端点（带"测试连接"按钮，显示绿/红结果）+ 模型名称（预置列表或手动输入）
- API Key（条件显示，ollama 时隐藏）
- 高级配置: 最大 Token 数（1-128000）、Temperature（Slider 0-2）、Top P（Slider 0-1）、超时时间、重试次数

**模型预置**: 每个类型有预设模型列表（ollama: llama2/codellama 等；openai: gpt-4/gpt-4o 等；deepseek: deepseek-chat/deepseek-coder 等）
**端点默认值**: 每个类型有默认端点 URL
**类型切换联动**: 切换类型时自动更新端点、清空模型/密钥、重置测试结果

### 11.4 LLM 服务编辑（`llmservice/serviceModify.vue`）

与创建页面几乎相同，区别：
- 标题: "编辑大模型服务"
- **服务类型禁止修改**（disabled + 提示 "服务类型不支持修改"）
- API Key 占位符: "留空则不修改"
- 提交调用 `updateLLMService()` 而非 `createLLMService()`
- `onMounted` 加载现有数据（API Key 不回显）

### 11.5 API 密钥管理（`apikey/index.vue`）

**搜索栏**: 名称（Input）、服务商（Select）
**表格列**: 名称、API Key（脱敏/可见切换 + 复制按钮）、服务商（彩色 Tag）、状态（Tag: 正常/已禁用/已过期）、使用情况（Progress 进度条）、过期时间、创建时间、操作
**操作列**: 编辑、启用/禁用切换、重新生成（Popconfirm）、删除（Popconfirm）

**模态框**（双用途创建/编辑）:
- 创建模式: 名称、服务商、API Key（密码输入）、使用限制、过期日期、描述
- 编辑模式: 同上但无 API Key 输入、服务商禁用
- **创建成功后**: 显示明文密钥 + 复制按钮 + 警告 "此密钥只显示一次"

**可见性追踪**: `visibleKeys: ref<Set<number>>()` 追踪哪些密钥展开了脱敏值

### 11.6 模型部署列表（`deployment/index.vue`）

**搜索栏**: 名称、模型来源（Select: MTP训练/大模型）、状态（5 种）
**表格列**: 部署名称（可点击 + 图标）、模型来源（Tag）、模型名称、部署节点、状态（Tag + 图标）、服务端点（运行中可点击链接）、副本数、创建时间、操作
**行选择**: 支持批量删除
**可展开行**（3 个 Tab）:
1. 基本信息 — Descriptions 展示全字段
2. 资源配置 — CPU/Memory/GPU 限制
3. Nginx 配置 — upstream/server_name/listen_port/SSL

### 11.7 模型部署创建（`deployment/deployCreate.vue`）— 4 步向导

| 步骤 | 标题 | 内容 |
|------|------|------|
| 0 | 基本配置 | 部署名称、模型来源（Radio: MTP/LLM）、模型选择、版本、部署节点（Select 显示 CPU/内存/GPU）、副本数 |
| 1 | 容器配置 | 容器名、镜像名、服务端口；资源限制（CPU/内存/GPU 数/GPU 内存）；健康检查配置 |
| 2 | 网络配置 | Nginx 代理（upstream/server_name/listen_port/proxy_pass/SSL/自定义指令） |
| 3 | 确认部署 | 4 分区摘要卡片，回顾全部配置，提交 |

**自动联动**: 部署名称变更时自动生成 Nginx upstream_name 和 server_name；选择 MTP 模型时自动填充模型名/版本/容器名
**节点加载**: `onMounted` 加载在线节点列表和 MTP 输出模型列表

### 11.8 模型部署详情（`deployment/deployDetail.vue`）

**左栏（span=16）**: 头部卡片（名称/状态/操作按钮）+ 内容卡片（5 Tab: 基本信息/资源配置/Nginx 配置/健康检查/部署日志）
**右栏（span=8）**: 运行状态（圆形 Progress）、快速操作（+1/-1 副本/刷新）、部署节点信息

**自动刷新**: `onMounted` 启动 5 秒间隔刷新（仅 "deploying" 状态），`onUnmounted` 清理

### 11.9 部署节点列表（`nodes/index.vue`）

**搜索栏**: 名称、状态（online/offline/maintenance）
**表格列**: 节点名称（图标）、IP 地址、状态（Tag: 在线/离线/维护中）、配置（CPU+Memory）、Docker（版本）、Nginx（Badge）、标签（Tag，最多显示 3 个）、创建时间、操作
**操作列**: 编辑、刷新状态、切换维护模式、删除
**可展开行**（3 Tab）: 基本信息、资源使用（4 张卡片: CPU/内存/磁盘/容器数的 Statistic+Progress）、标签

**模态框**: 创建/编辑节点（名称/端口/IP + 测试连接按钮/CPU/内存/GPU/标签/描述）
**资源懒加载**: 展开行时才加载该节点资源数据

### 11.10 节点创建独立页面（`nodes/nodeCreate.vue`）

全页面 Card 表单：基本信息（名称/SSH 端口）→ IP 地址 + 测试连接 → 硬件配置（CPU/内存/GPU）→ 其他（标签 Select mode="tags"/描述）

### 11.11 OpenClaw 实例列表（`openclaw/index.vue`）

**头部卡片**: 标题 "OpenClaw部署管理" + 副标题 + 刷新/新建按钮
**搜索卡片**: 名称、部署模式（Docker/NPM/Source）、状态（running/stopped/starting/error）
**表格列**: 实例名称（可点击）、部署模式（Tag）、Gateway 地址（host:port）、默认模型、状态（Tag + 图标，starting 旋转）、统计（对话/消息数）、创建时间、操作
**操作列**: 启动/停止、重启、查看日志、详情、编辑、删除

**日志抽屉**: 右侧 Drawer（宽 680px），深色主题控制台风格 `<pre>` 日志查看器，5 秒自动刷新
**操作追踪**: `operatingIds: ref<Set<number>>()` 追踪进行中操作的实例 ID
**自动轮询**: 3 秒间隔，检测到 "starting" 状态或进行中操作时激活，无过渡状态时自动停止

**双重解包处理**: `Array.isArray(res) ? res : res.data || []`

### 11.12 OpenClaw 创建/编辑（`openclaw/create.vue`）— 最复杂组件

**两栏布局**:

**左栏（span=12）**:
- 卡片 "基本信息": 实例名称、描述（Textarea）、部署模式（Select: Docker/NPM/Source 带描述）
- 卡片 "Gateway配置": 目标工作站（Select，从 SMP 在线工作站加载，显示地区信息）→ 主机地址（自动填充/禁用）→ 端口（默认 18789）→ 访问令牌（Password + 生成按钮）→ 测试连接（仅编辑模式）

**右栏（span=12）**:
- 卡片 "模型配置":
  - 主模型（Select 单选，可搜索，默认 `anthropic/claude-sonnet-4-5`）
  - 回退模型（Select 多选，排除主模型，描述 "当主模型不可用时按选择顺序依次尝试"）
  - 子代理模型（Select 单选，可清空，描述 "子代理执行简单子任务时使用的模型"）
  - 分割线
  - 关联 LLM 服务（Select）
  - API 密钥（两种方式：从 MEP 密钥管理选择 或 直接输入）
- 卡片 "高级配置": 启用的技能（Textarea JSON 数组）、渠道配置（Textarea JSON 对象）

**数据加载**（`loadOptions()`）: 并行加载 API 密钥、LLM 服务、模型选项
**工作站联动**: 选择工作站时自动填充 `gatewayHost` 为工作站 IP
**令牌生成**: POST `/openclaw/generate-token` → 保存返回的 token/apiKeyId/keyMasked

### 11.13 OpenClaw 详情（`openclaw/detail.vue`）

**左栏（span=16）**: 头部（名称/UID/刷新/启停/编辑按钮）+ 实例信息 Descriptions + 运行日志（深色控制台）
**右栏（span=8）**: 统计信息（对话数/消息数/最后活跃时间）+ 快速操作 + 状态变更 Timeline

**自适应刷新**: "starting" 状态 3 秒间隔，"running" 状态 10 秒间隔

---

## 十二、跨模块集成

### 12.1 MEP → SMP（服务管理平台，端口 8185）

| 调用方 | 端点 | 用途 |
|--------|------|------|
| `SshRemoteService.fetchCredentials(Long)` | `GET /api/smp/workstations/{id}/credentials` | 获取 SSH 凭据（ipAddress, sshPort, sshUser, authType, password, privateKey）|
| 前端 `openclaw/create.vue` | `getOnlineWorkstations()` (SMP API) | 加载在线工作站列表 |

**流程**: MEP 存储 `workstationId` 在 OpenClawInstance 上；每次需要 SSH 访问时（创建目录/部署/启停/日志），从 SMP 获取最新凭据。

### 12.2 MEP → MTP（模型训练平台，端口 8183）

| 调用方 | 端点 | 用途 |
|--------|------|------|
| `MTPModelService.getOutputModels()` | `GET /api/mtp/tasks`（Header: `X-Tenant-Uid: default`） | 获取训练输出模型列表，用于模型部署 |

### 12.3 MEP → OpenClaw Gateway（动态 host:port）

| 操作 | 端点 | 说明 |
|------|------|------|
| 健康检查 | `GET http://{host}:{port}/health` | 实例状态检测 |
| 连通测试 | `GET http://{host}:{port}/health` + `X-OpenClaw-Token` | 带认证的连通测试 |
| 聊天 API | `POST http://{host}:{port}/v1/chat/completions` | OpenAI 兼容接口 |

### 12.4 MEP ← XAA（智能助手模块）

| XAA 组件 | 调用 MEP | 用途 |
|-----------|----------|------|
| `XAA/assistant/create.vue` | `GET /openclaw/instances` | 选择 OpenClaw 实例关联助手 |
| `XAA/assistant/chat.vue` | 通过后端代理 → OpenClaw Gateway | 发送 `model: 'openclaw'` 的聊天消息 |
| `AssistantFloatingWindow` | SSE → `POST /xaa/assistants/{id}/chat/completions` | 流式对话（SSE data: 行解析） |

---

## 十三、前端类型定义

**文件**: `views/MEP/api/types.ts`

### 枚举类型（联合类型）

```typescript
type LLMServiceType = 'ollama' | 'openai' | 'deepseek' | 'custom';
type ServiceStatus = 'running' | 'stopped' | 'error' | 'deploying';
type DeploymentStatus = 'pending' | 'deploying' | 'running' | 'failed' | 'stopped';
```

### 核心接口

```typescript
// LLM 服务
interface LLMService {
  id: number; uid: string; name: string; type: LLMServiceType;
  description: string; endpoint: string; model_name: string;
  api_key?: string; status: ServiceStatus;
  config: LLMServiceConfig;
  created_by: string; updated_by: null | string;
  created_at: string; updated_at: null | string;
}

interface LLMServiceConfig {
  max_tokens?: number; temperature?: number; top_p?: number;
  timeout?: number; retry_count?: number;
  custom_params?: Record<string, any>;
}

// API 密钥
interface ApiKeyItem {
  id: number; uid: string; name: string; key: string; key_masked: string;
  provider: LLMServiceType; description: string;
  status: 'active' | 'disabled' | 'expired';
  usage_limit: number | null; usage_count: number;
  expires_at: null | string; created_by: string;
  created_at: string; updated_at: null | string;
}

// 部署节点
interface DeployNode {
  id: number; uid: string; name: string;
  ipAddress: string; port: number;
  status: 'online' | 'offline' | 'maintenance';
  cpuCores: number; memoryGb: number; gpuInfo: string | null;
  dockerVersion: string; nginxStatus: 'running' | 'stopped';
  labels: string | string[]; description: string;
  createdBy: string; createdAt: string; updatedAt: null | string;
}

// 模型部署
interface ModelDeployment {
  id: number; uid: string; name: string;
  model_source: 'mtp' | 'llm'; model_uid: string;
  model_name: string; model_version: string;
  node_uid: string; node_name: string;
  status: DeploymentStatus;
  container_id: string | null; container_name: string;
  image_name: string; port: number; endpoint: string;
  replicas: number;
  resource_config: DeploymentResourceConfig;
  nginx_config: NginxConfig;
  health_check: HealthCheckConfig;
  created_by: string; created_at: string; updated_at: null | string;
}

interface DeploymentResourceConfig {
  cpu_limit: string; memory_limit: string;
  gpu_count: number; gpu_memory: string;
}

interface NginxConfig {
  upstream_name: string; server_name: string;
  listen_port: number; proxy_pass: string;
  ssl_enabled: boolean; ssl_cert_path?: string;
  ssl_key_path?: string; custom_config?: string;
}

interface HealthCheckConfig {
  enabled: boolean; path: string; interval: number;
  timeout: number; retries: number;
}

// MTP 输出模型（跨模块）
interface MTPOutputModel {
  id: number; uid: string; task_uid: string; task_name: string;
  model_name: string; version: string; output_path: string;
  model_type: string; framework: string;
  metrics: Record<string, number>; created_at: string;
}

// 通用类型
interface ApiResponse<T> { code: number; message: string; data: T; error: null | string; }
interface DeploymentLog { id: number; deployment_uid: string; level: 'info'|'warn'|'error'; message: string; timestamp: string; }
interface ServiceMetrics { cpu_usage: number; memory_usage: number; request_count: number; error_count: number; avg_response_time: number; timestamp: string; }
```

### OpenClaw 实例类型（内联在 `openclaw/index.vue` 中）

```typescript
interface OpenClawInstance {
  id: number; uid: string; name: string; description?: string;
  deployMode: string; gatewayHost?: string; gatewayPort: number;
  defaultModel?: string; status: string;
  containerName?: string; processId?: string;
  totalConversations?: number; totalMessages?: number;
  createdAt: string; updatedAt: string;
}
```

---

## 十四、响应格式与注意事项

### 14.1 后端响应格式

所有控制器返回 `ResponseEntity<Map<String, Object>>`：

```json
{
  "code": 0,        // 0=成功, 400=请求错误, 404=未找到, 500=服务错误
  "message": "success",
  "data": { ... }   // 或 null
}
```

> **不一致性**: 部分响应使用 `Map.of()`（不允许 null 值，省略 data 键），部分使用 `HashMap`（允许 null）。

### 14.2 前端状态管理

**无 Pinia/Vuex Store** — 所有状态为组件局部：
- 每个列表页维护 `ref<Entity[]>([])`
- 搜索过滤通过 `computed()` 实现客户端过滤
- 跨组件通信通过 **Vue Router**（`router.push()` + 路由 query 参数 `?id=123`）
- OpenClaw 列表使用 `operatingIds: ref<Set<number>>()` 轻量状态追踪

### 14.3 重要模式与已知问题

| 模式/问题 | 说明 |
|-----------|------|
| **Map vs Entity 请求体** | `OpenClawController` 和 `ApiKeyController` 使用 `Map<String,Object>`，手动类型转换提取字段；其他控制器使用类型化实体类 |
| **远程/本地分支** | `workstationId != null` 是 `OpenClawService` 全局判断远程/本地执行的模式 |
| **Mock/桩实现** | `DeployNodeService.getDockerVersion()` 固定返回 `"24.0.7"`；`checkNginxStatus()` 固定返回 `true`；`ModelDeploymentService.getLogs()`/`getMetrics()` 返回 Mock 数据；`deployAsync()` 仅 sleep 5s |
| **AES 加密重复** | `ApiKeyService` 和 `OpenClawService` 包含完全相同的 AES 加密/解密/补位实现，共享 `@Value` 配置 |
| **换行符规范化** | 部署脚本上传前将 `\r\n` 转换为 `\n`（Windows → Linux） |
| **XAA 表无代码** | `openclaw_tables.sql` 定义了 XAA 助手/对话/消息表，但 MEP 中无对应 Java 代码 |
| **无 XML Mapper** | 尽管配置了 `mybatis.mapper-locations: classpath:mapper/*.xml`，实际全部使用注解 SQL |
| **OpenClaw API 无独立文件** | 其他子模块有独立 API 文件，OpenClaw 端点直接在 Vue 组件中内联调用 |
| **双重解包** | 前端需要处理 `defaultResponseInterceptor` 和 `responseReturn:'data'` 的双重解包，使用 `Array.isArray(res) ? res : res.data \|\| []` |
| **OpenClaw 内置 Provider** | 仅 `anthropic` 和 `openai` 为内置支持；deepseek/google/together/groq 等需要 `models.providers` 配置 |
| **OpenClaw Gateway HTTP 版本** | 要求 HTTP/1.1（Java HttpClient 默认 HTTP/2 → 需设置 `.version(HTTP_1_1)`） |
