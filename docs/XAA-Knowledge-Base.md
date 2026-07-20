# XAA 模块知识库 (Xnet-AI-Agent)

> 本文档为 XAA 模块的完整知识库，供 RAG 智能助手检索使用。
> 生成时间：2026-02-12

---

## 一、模块概述

**XAA** 全称 **Xnet-AI-Agent**，是 XnetMLops 平台的 **AI 智能体服务模块**，端口号 **8186**。XAA 是整个 MLOps 平台的"编排中枢"，通过工作流将 DPP（数据处理）、MTP（模型训练）、MEP（模型部署）的能力串联起来，并提供智能对话助手和技能仓库功能。

### 三大子系统

| 子系统 | 缩写 | 功能描述 |
|--------|------|----------|
| Xnet-Agent-Workflow | **XAW** | AI 工作流编排引擎，支持通过可视化画布将 DPP/MTP/MEP 任务编排为 DAG 流水线 |
| Xnet-Agent-Skill | **XAS** | 元技能仓库，类似应用商店，管理可安装的 AI 技能（PDF处理、数据分析等） |
| 智能助手 | **Assistant** | AI 对话助手系统，支持 OpenClaw Gateway 代理、RAG 知识库、工具调用、SSE 流式对话 |

### 技术栈

- **后端**: Spring Boot 3.4.6 + MyBatis 3.0.4 + MySQL + WebFlux (WebClient) + Java 17 + Lombok
- **前端**: Vue 3 + TypeScript + Ant Design Vue + Vben Admin Pro + Vue Flow（工作流画布）
- **主入口**: `MlopsXaaServiceApplication.java`，启用了 `@EnableAsync` 异步执行支持

---

## 二、系统架构

### 2.1 后端目录结构

```
mlops-xaa-service/src/main/java/com/synapxnet/mlopsxaaservice/
├── MlopsXaaServiceApplication.java          -- 启动类
├── config/
│   └── WebClientConfig.java                 -- WebClient Bean 配置
├── controller/
│   ├── AssistantController.java             -- 智能助手控制器 (19个接口)
│   ├── ExternalResourceController.java      -- 外部资源代理控制器 (6个接口)
│   ├── SkillController.java                 -- 技能管理控制器 (16个接口)
│   ├── WorkflowController.java             -- 工作流管理控制器 (10个接口)
│   └── WorkflowExecutionController.java     -- 工作流执行控制器 (5个接口)
├── entity/
│   ├── Assistant.java                       -- 智能助手实体
│   ├── AssistantConversation.java           -- 助手会话实体
│   ├── AssistantMessage.java                -- 助手消息实体
│   ├── NodeExecution.java                   -- 节点执行记录实体
│   ├── Skill.java                           -- 技能实体
│   ├── SkillCategory.java                   -- 技能分类实体
│   ├── SkillInstallation.java               -- 技能安装记录实体
│   ├── Workflow.java                        -- 工作流实体
│   ├── WorkflowEdge.java                    -- 工作流边实体
│   ├── WorkflowExecution.java               -- 工作流执行记录实体
│   ├── WorkflowNode.java                    -- 工作流节点实体
│   └── enums/
│       ├── ExecutionStatus.java             -- 执行状态枚举 (8种)
│       ├── NodeType.java                    -- 节点类型枚举 (19种)
│       └── WorkflowStatus.java              -- 工作流状态枚举 (3种)
├── mapper/
│   ├── AssistantMapper.java                 -- 注解SQL
│   ├── AssistantConversationMapper.java     -- 注解SQL
│   ├── AssistantMessageMapper.java          -- 注解SQL
│   ├── NodeExecutionMapper.java             -- XML映射
│   ├── SkillCategoryMapper.java             -- XML映射
│   ├── SkillInstallationMapper.java         -- XML映射
│   ├── SkillMapper.java                     -- XML映射
│   ├── WorkflowEdgeMapper.java             -- XML映射
│   ├── WorkflowExecutionMapper.java         -- XML映射
│   ├── WorkflowMapper.java                 -- XML映射
│   └── WorkflowNodeMapper.java             -- XML映射
└── service/
    ├── AssistantService.java                -- 智能助手服务
    ├── ExternalServiceClient.java           -- 外部服务调用客户端
    ├── SkillService.java                    -- 技能服务
    ├── WorkflowExecutionService.java        -- 工作流执行服务 (含异步引擎)
    └── WorkflowService.java                -- 工作流管理服务
```

### 2.2 前端目录结构

```
XnetMLops-web/apps/web-antd/src/views/XAA/
├── api/                                    -- API 层 (4个文件)
│   ├── types.ts                            -- 核心类型定义
│   ├── assistant.ts                        -- 助手 API (18个函数)
│   ├── skill.ts                            -- 技能 API (16个函数)
│   └── workflow.ts                         -- 工作流 API (21个函数, 含6个跨模块)
├── assistant/                              -- 智能助手页面 (4个)
│   ├── index.vue                           -- 助手列表 (卡片网格)
│   ├── create.vue                          -- 创建/编辑助手 (双栏表单)
│   ├── edit.vue                            -- 编辑助手 (复用create.vue)
│   └── chat.vue                            -- 对话界面 (SSE流式)
├── skill/                                  -- 技能管理 (4页面 + 3组件 + 2配置)
│   ├── index.vue                           -- 占位页（功能开发中）
│   ├── types.ts                            -- 技能类型定义
│   ├── create/index.vue                    -- 创建技能表单
│   ├── installed/index.vue                 -- 已安装技能表格
│   └── repository/                         -- 元技能仓库
│       ├── index.vue                       -- 仓库主页
│       ├── constants.ts                    -- 分类/类型常量
│       └── components/
│           ├── CategoryFilter.vue          -- 分类筛选器
│           ├── SkillCard.vue               -- 技能卡片
│           └── SkillDetail.vue             -- 技能详情抽屉
└── workflow/                               -- 工作流管理 (6页面 + 设计器)
    ├── index.vue                           -- 工作流列表
    ├── workflowCreate.vue                  -- 创建工作流
    ├── workflowEdit.vue                    -- 编辑工作流
    ├── workflowDesigner.vue                -- 设计器页面封装
    ├── executionList.vue                   -- 执行记录列表
    ├── executionDetail.vue                 -- 执行详情
    └── designer/                           -- 工作流设计器核心
        ├── WorkflowDesigner.vue            -- 主设计器 (Vue Flow)
        ├── index.ts                        -- 导出入口
        ├── types.ts                        -- 设计器类型定义
        ├── constants.ts                    -- 节点元数据/画布配置
        ├── components/
        │   ├── Toolbar.vue                 -- 顶部工具栏
        │   ├── NodeSelector.vue            -- 左侧节点选择面板
        │   └── NodeConfigPanel.vue         -- 右侧节点配置面板
        └── nodes/
            ├── BaseNode.vue                -- 统一节点渲染组件
            ├── NodePreview.vue             -- 拖拽预览节点
            └── index.ts                    -- 节点类型注册

全局共享组件:
└── src/components/AssistantFloatingWindow/index.vue  -- 智能助手浮窗
```

### 2.3 统计数据

| 维度 | 数量 |
|------|------|
| 后端 Java 文件 | 约 30 个 |
| 前端文件 | 34 个 |
| 实体类 | 11 个 + 3 个枚举 |
| Controller | 5 个 |
| REST API 接口 | **56 个** |
| Service | 5 个 |
| Mapper | 11 个 (3 注解SQL + 8 XML) |
| 数据库表 | **9 张** |
| 前端页面 | 14 个 Vue 页面 + 8 个子组件 |

---

## 三、数据模型

### 3.1 Assistant (智能助手)

表名: `xnet_mlops_xaa_assistant`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | 唯一标识 (ASST-XXXXXXXX) |
| name | String | 助手名称 |
| description | String | 描述 |
| avatar | String | 头像URL |
| openclawInstanceId | Long | 关联的 OpenClaw 实例ID |
| gatewayUrl | String | Gateway HTTP URL |
| gatewayToken | String | Gateway 认证令牌 |
| llmServiceId | Long | 关联的 MEP LLM 服务ID |
| defaultModel | String | 默认模型 |
| systemPrompt | String | 系统提示词 |
| temperature | BigDecimal | 温度参数 (默认0.70) |
| maxTokens | Integer | 最大token数 (默认4096) |
| knowledgeBaseIds | String | 关联的DPP知识库ID列表 (JSON数组) |
| ragEnabled | Boolean | 是否启用RAG (默认false) |
| ragTopK | Integer | RAG返回结果数 (默认5) |
| skillIds | String | 关联的XAA技能ID列表 (JSON数组) |
| toolsEnabled | Boolean | 是否启用工具调用 (默认true) |
| uiConfig | String | UI配置JSON (位置/尺寸/主题色) |
| welcomeMessage | String | 欢迎消息 |
| placeholder | String | 输入框占位符 |
| status | String | active / disabled |
| isDefault | Boolean | 是否为默认助手 |
| totalConversations | Long | 总会话数 |
| totalMessages | Long | 总消息数 |
| lastUsedAt | LocalDateTime | 最后使用时间 |
| tenantUid | String | 租户ID |
| createdBy / updatedBy | String | 创建/更新者 |
| createdAt / updatedAt | LocalDateTime | 创建/更新时间 |

### 3.2 AssistantConversation (助手会话)

表名: `xnet_mlops_xaa_assistant_conversation`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | 唯一标识 (CONV-XXXXXXXX) |
| assistantId | Long | 关联助手ID |
| title | String | 会话标题 (自动从首条消息截取) |
| summary | String | 会话摘要 |
| userId | String | 用户ID |
| userName | String | 用户名 |
| status | String | active / archived / deleted |
| messageCount | Integer | 消息条数 |
| tokenCount | Long | Token 消耗总量 |
| lastMessageAt | LocalDateTime | 最后消息时间 |
| createdAt / updatedAt | LocalDateTime | 创建/更新时间 |

### 3.3 AssistantMessage (助手消息)

表名: `xnet_mlops_xaa_assistant_message`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | 唯一标识 (MSG-XXXXXXXX) |
| conversationId | Long | 关联会话ID |
| role | String | user / assistant / system |
| content | String | 消息内容 |
| tokenCount | Integer | Token消耗 |
| modelUsed | String | 使用的模型 |
| ragSources | String | RAG来源文档 (JSON数组) |
| toolCalls | String | 工具调用记录 (JSON数组) |
| metadata | String | 其他元数据 (JSON) |
| createdAt | LocalDateTime | 创建时间 |

### 3.4 Workflow (工作流)

表名: `xnet_mlops_xaa_workflow`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | UUID |
| name | String | 工作流名称 |
| description | String | 描述 |
| type | String | workflow / chat / pipeline |
| status | String | draft / published / archived |
| graphJson | String | 完整图结构JSON (LONGTEXT) |
| configJson | String | 配置JSON |
| version | Integer | 版本号 (每次更新+1) |
| creatorId / creatorName | String | 创建者 |
| tenantUid / deptUid / teamUid | String | 租户/部门/团队UID |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.5 WorkflowNode (工作流节点)

表名: `xnet_mlops_xaa_workflow_node`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | UUID |
| workflowId | Long | 所属工作流ID (外键级联删除) |
| nodeType | String | 节点类型 (19种) |
| title | String | 节点标题 |
| description | String | 节点描述 |
| configJson | String | 节点配置JSON (任务参数) |
| positionX / positionY | Double | 画布坐标 |
| width / height | Double | 节点尺寸 (默认200x80) |
| sortOrder | Integer | 排序 |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.6 WorkflowEdge (工作流边)

表名: `xnet_mlops_xaa_workflow_edge`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | UUID |
| workflowId | Long | 所属工作流ID (外键级联删除) |
| sourceNodeId | Long | 源节点ID |
| sourceHandle | String | 源节点输出端口 |
| targetNodeId | Long | 目标节点ID |
| targetHandle | String | 目标节点输入端口 |
| edgeType | String | default / success / fail |
| conditionJson | String | 条件表达式JSON (条件分支用) |
| sortOrder | Integer | 排序 |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.7 WorkflowExecution (工作流执行记录)

表名: `xnet_mlops_xaa_workflow_execution`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | UUID |
| workflowId | Long | 所属工作流ID (外键级联删除) |
| status | String | scheduled/running/succeeded/failed/stopped/paused |
| inputsJson | String | 输入参数JSON |
| outputsJson | String | 输出结果JSON |
| errorMessage | String | 错误信息 |
| startedAt / finishedAt | Date | 开始/结束时间 |
| elapsedTime | Long | 执行耗时(毫秒) |
| triggeredBy | String | 触发者ID |
| triggerType | String | manual / scheduled / api |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.8 NodeExecution (节点执行记录)

表名: `xnet_mlops_xaa_node_execution`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | UUID |
| executionId | Long | 所属工作流执行ID (外键级联删除) |
| workflowId | Long | 所属工作流ID |
| nodeId | Long | 节点ID |
| nodeType | String | 节点类型 |
| nodeTitle | String | 节点标题 |
| status | String | pending/running/succeeded/failed/skipped/stopped |
| inputsJson / outputsJson | String | 输入/输出JSON |
| metadataJson | String | 执行元数据JSON (token消耗/外部任务ID等) |
| errorMessage | String | 错误信息 |
| startedAt / finishedAt | Date | 开始/结束时间 |
| elapsedTime | Long | 执行耗时(毫秒) |
| retryCount | Integer | 重试次数 |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.9 Skill (技能)

表名: `xnet_mlops_xaa_skill`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| uid | String | 唯一标识 (SKILL-XXXXXXXX) |
| name | String | 技能名称 |
| description | String | 描述 |
| type | String | tool / prompt / chain / workflow |
| status | String | draft / published / archived |
| category | String | 8种分类 (document/creative/development/data/enterprise/ai-ml/utilities/custom) |
| tags | String | 标签 (逗号分隔) |
| contentMd | String | SKILL.md 内容 (LONGTEXT) |
| configJson | String | 配置JSON |
| icon | String | 图标URL |
| hasScripts | Boolean | 是否包含脚本 |
| hasReferences | Boolean | 是否包含参考文档 |
| hasAssets | Boolean | 是否包含资源文件 |
| installCount | Integer | 安装次数 |
| sourceUrl | String | 来源URL |
| isOfficial | Boolean | 是否官方技能 |
| version | Integer | 版本号 |
| creatorId | String | 创建者ID |
| tenantUid | String | 租户UID |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.10 SkillCategory (技能分类)

表名: `xnet_mlops_xaa_skill_category`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| key | String | 分类标识 (如: document, creative) |
| name | String | 中文名 |
| nameEn | String | 英文名 |
| description | String | 描述 |
| icon | String | 图标名称 |
| color | String | 主题颜色 |
| sortOrder | Integer | 排序 |
| createdAt / updatedAt | Date | 创建/更新时间 |

### 3.11 SkillInstallation (技能安装记录)

表名: `xnet_mlops_xaa_skill_installation`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| skillId | Long | 技能ID |
| tenantUid | String | 租户UID |
| installedBy | String | 安装者ID |
| installedAt | Date | 安装时间 |
| configOverride | String | 配置覆盖JSON |
| status | String | active / disabled |

---

## 四、数据库表

### 4.1 全部表清单

| 表名 | 说明 | SQL文件 | 外键 |
|------|------|---------|------|
| xnet_mlops_xaa_workflow | 工作流主表 | xaa_tables.sql | — |
| xnet_mlops_xaa_workflow_node | 工作流节点表 | xaa_tables.sql | workflow_id → workflow (CASCADE) |
| xnet_mlops_xaa_workflow_edge | 工作流边表 | xaa_tables.sql | workflow_id → workflow (CASCADE) |
| xnet_mlops_xaa_workflow_execution | 工作流执行记录表 | xaa_tables.sql | workflow_id → workflow (CASCADE) |
| xnet_mlops_xaa_node_execution | 节点执行记录表 | xaa_tables.sql | execution_id → workflow_execution (CASCADE) |
| xnet_mlops_xaa_skill | 技能表 | xaa_tables.sql + xaa_skill_update.sql | — |
| xnet_mlops_xaa_skill_category | 技能分类表 | xaa_skill_update.sql | — |
| xnet_mlops_xaa_skill_installation | 技能安装记录表 | xaa_skill_update.sql | — |
| xnet_mlops_xaa_assistant | 智能助手表 | Mapper注解推导 | — |
| xnet_mlops_xaa_assistant_conversation | 助手会话表 | Mapper注解推导 | — |
| xnet_mlops_xaa_assistant_message | 助手消息表 | Mapper注解推导 | — |

### 4.2 初始化数据

`xaa_skill_update.sql` 初始化了：
- **8 个技能分类**: document(文档处理)、creative(创意写作)、development(开发工具)、data(数据分析)、enterprise(企业应用)、ai-ml(AI/ML)、utilities(实用工具)、custom(自定义)
- **8 个官方示例技能**: PDF处理、Word处理、Excel处理、PPT处理、前端设计、MCP服务构建、Web应用测试、数据分析

### 4.3 SQL 亮点

- **SkillMapper.search**: 支持 `keyword` 模糊搜索 (name/description/tags LIKE) + `category` 精确过滤，带动态 `<if>` 条件
- **SkillMapper.selectAll**: 排序 `is_official DESC, install_count DESC, created_at DESC`（官方优先、安装量优先）
- **SkillMapper.decrementInstallCount**: `GREATEST(0, install_count - 1)` 防止负数
- **WorkflowMapper.countByName**: 支持 `excludeId` 排除自身，用于更新时的名称唯一性校验
- **WorkflowNodeMapper.batchInsert / WorkflowEdgeMapper.batchInsert**: `<foreach>` 批量插入
- **外键级联删除**: 工作流删除时自动级联删除节点、边、执行记录

---

## 五、后端 API 接口

### 5.1 AssistantController (`/api/xaa/assistants`) — 19 个接口

| HTTP方法 | 路径 | 功能 | 参数 |
|----------|------|------|------|
| GET | `/api/xaa/assistants` | 获取所有助手 | — |
| GET | `/api/xaa/assistants/active` | 获取所有激活的助手 | — |
| GET | `/api/xaa/assistants/default` | 获取默认助手 | — |
| GET | `/api/xaa/assistants/{id}` | 获取助手详情 | id |
| GET | `/api/xaa/assistants/{id}/config` | 获取助手配置(浮窗用) | id |
| POST | `/api/xaa/assistants` | 创建助手 | @RequestBody Assistant |
| PUT | `/api/xaa/assistants/{id}` | 更新助手 | id + @RequestBody Assistant |
| DELETE | `/api/xaa/assistants/{id}` | 删除助手 | id |
| POST | `/api/xaa/assistants/{id}/status` | 启用/禁用助手 | id + {status} |
| POST | `/api/xaa/assistants/{id}/set-default` | 设为默认助手 | id |
| GET | `/api/xaa/assistants/{id}/conversations` | 获取助手的会话列表 | id |
| POST | `/api/xaa/assistants/{id}/conversations` | 创建新会话 | id + {userId, userName} |
| GET | `/api/xaa/assistants/conversations/{convId}` | 获取会话详情 | convId |
| POST | `/api/xaa/assistants/conversations/{convId}/archive` | 归档会话 | convId |
| DELETE | `/api/xaa/assistants/conversations/{convId}` | 删除会话 | convId |
| GET | `/api/xaa/assistants/conversations/{convId}/messages` | 获取会话消息 | convId, limit=50 |
| POST | `/api/xaa/assistants/conversations/{convId}/messages` | 添加消息 | convId + {role, content, tokenCount, modelUsed, ragSources, toolCalls} |
| GET | `/api/xaa/assistants/user/{userId}/conversations` | 获取用户的所有会话 | userId |
| POST | `/api/xaa/assistants/{id}/chat/completions` | **SSE流式对话代理** | id + OpenAI格式请求体 |

### 5.2 WorkflowController (`/api/xaa`) — 10 个接口

| HTTP方法 | 路径 | 功能 | 参数 |
|----------|------|------|------|
| POST | `/api/xaa/workflows` | 创建工作流 | @RequestBody Workflow |
| GET | `/api/xaa/workflows` | 获取工作流列表 | status? |
| GET | `/api/xaa/workflows/{id}` | 获取工作流详情 | id |
| PUT | `/api/xaa/workflows/{id}` | 更新工作流 | id + @RequestBody Workflow |
| POST | `/api/xaa/workflows/{id}/publish` | 发布工作流 | id |
| POST | `/api/xaa/workflows/{id}/archive` | 归档工作流 | id |
| DELETE | `/api/xaa/workflows/{id}` | 删除工作流(级联) | id |
| GET | `/api/xaa/workflows/{id}/nodes` | 获取工作流节点 | id |
| GET | `/api/xaa/workflows/{id}/edges` | 获取工作流边 | id |
| POST | `/api/xaa/workflows/{id}/graph` | 保存工作流图(节点+边) | id + {nodes, edges} |

### 5.3 WorkflowExecutionController (`/api/xaa`) — 5 个接口

| HTTP方法 | 路径 | 功能 | 参数 |
|----------|------|------|------|
| POST | `/api/xaa/workflows/{workflowId}/execute` | 执行工作流 | workflowId + {inputs?, triggeredBy?, triggerType?} |
| GET | `/api/xaa/executions/{id}` | 获取执行记录详情 | id |
| GET | `/api/xaa/workflows/{workflowId}/executions` | 获取工作流的所有执行记录 | workflowId |
| GET | `/api/xaa/executions/{executionId}/nodes` | 获取节点执行详情 | executionId |
| POST | `/api/xaa/executions/{id}/stop` | 停止执行 | id |

### 5.4 SkillController (`/api/xaa`) — 16 个接口

| HTTP方法 | 路径 | 功能 | 参数 |
|----------|------|------|------|
| POST | `/api/xaa/skills` | 创建技能 | @RequestBody Skill |
| GET | `/api/xaa/skills` | 获取技能列表 | status?, type?, category? |
| GET | `/api/xaa/skills/repository` | 获取仓库技能(已发布) | category? |
| GET | `/api/xaa/skills/search` | 搜索技能 | keyword?, category? |
| GET | `/api/xaa/skills/{id}` | 获取技能详情 | id |
| PUT | `/api/xaa/skills/{id}` | 更新技能 | id + @RequestBody Skill |
| POST | `/api/xaa/skills/{id}/publish` | 发布技能 | id |
| POST | `/api/xaa/skills/{id}/archive` | 归档技能 | id |
| DELETE | `/api/xaa/skills/{id}` | 删除技能 | id |
| GET | `/api/xaa/skill-categories` | 获取所有技能分类 | — |
| GET | `/api/xaa/skill-categories/{key}` | 根据Key获取分类 | key |
| POST | `/api/xaa/skills/{id}/install` | 安装技能 | id + X-Tenant-UID + X-User-ID |
| POST | `/api/xaa/skills/{id}/uninstall` | 卸载技能 | id + X-Tenant-UID |
| GET | `/api/xaa/skills/{id}/installed` | 检查是否已安装 | id + X-Tenant-UID |
| GET | `/api/xaa/skills/installed` | 获取已安装技能列表 | X-Tenant-UID |
| GET | `/api/xaa/skill-installations` | 获取安装记录 | X-Tenant-UID |

### 5.5 ExternalResourceController (`/api/xaa/resources`) — 6 个接口

| HTTP方法 | 路径 | 功能 | 代理到 |
|----------|------|------|--------|
| GET | `/api/xaa/resources/dpp/datasets` | 获取DPP数据集列表 | DPP: `/api/dpp/datasets` |
| GET | `/api/xaa/resources/dpp/features` | 获取DPP特征工程列表 | DPP: `/api/dpp/features` |
| GET | `/api/xaa/resources/mtp/algorithms` | 获取MTP算法列表 | MTP: `/api/mtp/algorithms` |
| GET | `/api/xaa/resources/mtp/train-tasks` | 获取MTP训练任务列表 | MTP: `/api/mtp/train-tasks` |
| GET | `/api/xaa/resources/mep/deployments` | 获取MEP部署列表 | MEP: `/api/mep/deployments` |
| GET | `/api/xaa/resources/mep/services` | 获取MEP服务列表 | MEP: `/api/mep/llm-services` |

---

## 六、Service 层

### 6.1 AssistantService — 智能助手服务

| 方法 | 功能 |
|------|------|
| `findAll()` | 查询所有助手 |
| `findActive()` | 查询所有激活的助手 |
| `findById(Long id)` | 按ID查询 |
| `findByUid(String uid)` | 按UID查询 |
| `findDefault()` | 查询默认助手 |
| `create(Assistant)` | 创建助手，自动生成UID(ASST-)、设置默认值(温度0.70、maxTokens 4096、默认UI配置) |
| `update(Assistant)` | 更新助手 |
| `delete(Long id)` | **@Transactional** 删除助手及所有关联会话和消息 |
| `updateStatus(Long id, String status)` | 更新助手状态 |
| `setDefault(Long id)` | 设置默认助手(先清除旧默认) |
| `createConversation(assistantId, userId, userName)` | 创建会话(UID=CONV-)，自增助手会话计数 |
| `addMessage(convId, role, content, ...)` | 添加消息(UID=MSG-)，更新会话和助手统计，自动从首条用户消息截取标题(<=30字) |
| `getRecentMessages(convId, limit)` | 获取最近N条消息(反转为正序) |
| `getAssistantConfig(Long id)` | 解析助手配置(JSON解析uiConfig、skillIds、knowledgeBaseIds) |

### 6.2 WorkflowService — 工作流管理服务

| 方法 | 功能 |
|------|------|
| `createWorkflow(Workflow)` | **@Transactional** 创建，检查名称唯一性 |
| `updateWorkflow(Long id, Workflow)` | **@Transactional** 更新，检查名称冲突，版本号+1 |
| `publishWorkflow(Long id)` | **@Transactional** 发布工作流(状态→published) |
| `archiveWorkflow(Long id)` | **@Transactional** 归档工作流(状态→archived) |
| `deleteWorkflow(Long id)` | **@Transactional** 级联删除(边→节点→工作流) |
| `saveWorkflowGraph(id, nodes, edges)` | **@Transactional** 保存图(先删旧、再批量插入) |

### 6.3 WorkflowExecutionService — 工作流执行引擎（核心）

| 方法 | 功能 |
|------|------|
| `startExecution(workflowId, inputs, triggeredBy, triggerType)` | **@Transactional** 启动执行，创建记录，触发异步 |
| `executeWorkflowAsync(executionId)` | **@Async** 异步执行工作流图 |
| `executeGraph(execution, nodes, edges)` | 构建 DAG，从 start 节点递归执行 |
| `executeNode(execution, node, context, nodeMap, edgeMap)` | 执行单节点，创建 NodeExecution 记录 |
| `executeNodeByType(node, context)` | **核心路由**: 按 nodeType 分发到不同执行器 |
| `executeDppTask/executeDppDataset/executeDppFeature` | 调用 DPP 服务 |
| `executeMtpTrain/executeMtpAlgorithm` | 调用 MTP 服务 |
| `executeMepDeploy/executeMepService` | 调用 MEP 服务 |
| `executeHttpRequest(config, context)` | 通用 HTTP 请求执行 |
| `executeCode(config, context)` | 代码执行 (**TODO: 预留**) |
| `executeVariableAssigner(config, context)` | 变量赋值 |
| `stopExecution(Long executionId)` | **@Transactional** 停止执行 |

### 6.4 SkillService — 技能服务

| 方法 | 功能 |
|------|------|
| `createSkill(Skill)` | **@Transactional** 创建(UID=SKILL-) |
| `searchSkills(keyword, category)` | 关键词+分类搜索 |
| `publishSkill(id)` / `archiveSkill(id)` | 发布/归档技能 |
| `installSkill(skillId, tenantUid, userId)` | **@Transactional** 安装(检查重复、已发布、创建记录、增加计数) |
| `uninstallSkill(skillId, tenantUid)` | **@Transactional** 卸载(删除记录、减少计数) |
| `getInstalledSkills(tenantUid)` | 获取租户已安装技能 |

### 6.5 ExternalServiceClient — 跨模块调用客户端

使用 Spring WebFlux WebClient 进行 HTTP 调用（`.block()` 同步阻塞）：

| 方法 | 目标 |
|------|------|
| `callDppService(path, body)` | DPP (POST) |
| `callMtpService(path, body)` | MTP (POST) |
| `callMepService(path, body)` | MEP (POST) |
| `getDppDatasets()` / `getDppFeatures()` | DPP (GET) |
| `getMtpAlgorithms()` / `getMtpTrainTasks()` | MTP (GET) |
| `getMepDeployments()` / `getMepServices()` | MEP (GET) |
| `executeHttpRequest(url, method, body)` | 通用 HTTP (GET/POST/PUT/DELETE) |

---

## 七、Mapper/DAO 层

### 7.1 SQL 方式分布

| Mapper | SQL方式 | 说明 |
|--------|---------|------|
| AssistantMapper | 注解SQL | `@Select/@Insert/@Update/@Delete` |
| AssistantConversationMapper | 注解SQL | 同上 |
| AssistantMessageMapper | 注解SQL | 同上 |
| WorkflowMapper | XML | `WorkflowMapper.xml` |
| WorkflowNodeMapper | XML | 支持 `batchInsert` |
| WorkflowEdgeMapper | XML | 支持 `batchInsert` |
| WorkflowExecutionMapper | XML | 支持查询 running 状态 |
| NodeExecutionMapper | XML | 支持 `batchInsert` |
| SkillMapper | XML | 支持模糊搜索 + 分类过滤 |
| SkillCategoryMapper | XML | 按 sortOrder 排序 |
| SkillInstallationMapper | XML | 联合唯一键 (skill_id + tenant_uid) |

---

## 八、前端路由与页面

### 8.1 路由结构

顶层路由 `/XAA`，菜单图标 `carbon:machine-learning-model`，排序 `order: 5`。

**侧边栏菜单**（4个可见项）：
```
Xnet智能体(XAA)
  ├── 工作流管理     /XAA/workflow/index
  ├── 元技能仓库     /XAA/skill/repository
  ├── 已安装技能     /XAA/skill/installed
  └── 智能助手       /XAA/assistant/index
```

**隐藏路由**（菜单不可见）：
| 路径 | 组件 | 说明 |
|------|------|------|
| `/XAA/workflow/create` | workflowCreate.vue | 创建工作流 |
| `/XAA/workflow/edit` | workflowEdit.vue | 编辑工作流 (query: id) |
| `/XAA/workflow/designer` | workflowDesigner.vue | 工作流设计器 (query: id) |
| `/XAA/workflow/executions` | executionList.vue | 执行记录 (query: workflowId) |
| `/XAA/workflow/execution-detail` | executionDetail.vue | 执行详情 (query: id) |
| `/XAA/skill/create` | create/index.vue | 创建技能 |
| `/XAA/assistant/create` | create.vue | 创建助手 |
| `/XAA/assistant/edit` | edit.vue | 编辑助手 (query: id) |
| `/XAA/assistant/chat` | chat.vue | 对话界面 (query: id) |

### 8.2 请求客户端

XAA 使用独立的 `xaaRequestClient`：
- 开发环境: `/xaa`（Vite 代理）
- 生产环境: `http://127.0.0.1:8186/api`
- 双解包模式: `defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'`
- 防御性写法: `Array.isArray(res) ? res : (res as any).data || []`

---

## 九、前端 API 层

### 9.1 助手 API (`api/assistant.ts`) — 18 个函数

| 函数名 | HTTP | URL | 说明 |
|--------|------|-----|------|
| fetchAssistantList | GET | /assistants | 获取所有助手 |
| fetchActiveAssistants | GET | /assistants/active | 获取激活的助手 |
| fetchDefaultAssistant | GET | /assistants/default | 获取默认助手 |
| fetchAssistant | GET | /assistants/{id} | 获取助手详情 |
| fetchAssistantConfig | GET | /assistants/{id}/config | 获取助手配置(浮窗用) |
| createAssistant | POST | /assistants | 创建助手 |
| updateAssistant | PUT | /assistants/{id} | 更新助手 |
| deleteAssistant | DELETE | /assistants/{id} | 删除助手 |
| updateAssistantStatus | POST | /assistants/{id}/status | 更新状态 |
| setDefaultAssistant | POST | /assistants/{id}/set-default | 设为默认 |
| fetchConversations | GET | /assistants/{id}/conversations | 获取会话列表 |
| createConversation | POST | /assistants/{id}/conversations | 创建会话 |
| fetchConversation | GET | /assistants/conversations/{convId} | 获取会话详情 |
| archiveConversation | POST | /assistants/conversations/{convId}/archive | 归档会话 |
| deleteConversation | DELETE | /assistants/conversations/{convId} | 删除会话 |
| fetchUserConversations | GET | /assistants/user/{userId}/conversations | 获取用户会话 |
| fetchMessages | GET | /assistants/conversations/{convId}/messages | 获取消息 |
| addMessage | POST | /assistants/conversations/{convId}/messages | 添加消息 |

### 9.2 技能 API (`api/skill.ts`) — 16 个函数

| 函数名 | HTTP | URL | 说明 |
|--------|------|-----|------|
| fetchSkillList | GET | /skills | 获取技能列表 |
| fetchRepositorySkills | GET | /skills/repository | 获取仓库技能(已发布) |
| searchSkills | GET | /skills/search | 搜索技能 |
| fetchSkillById | GET | /skills/{id} | 获取技能详情 |
| createSkill | POST | /skills | 创建技能 |
| updateSkill | PUT | /skills/{id} | 更新技能 |
| publishSkill | POST | /skills/{id}/publish | 发布技能 |
| archiveSkill | POST | /skills/{id}/archive | 归档技能 |
| deleteSkill | DELETE | /skills/{id} | 删除技能 |
| fetchSkillCategories | GET | /skill-categories | 获取所有分类 |
| fetchSkillCategoryByKey | GET | /skill-categories/{key} | 按Key获取分类 |
| installSkill | POST | /skills/{id}/install | 安装技能 |
| uninstallSkill | POST | /skills/{id}/uninstall | 卸载技能 |
| checkSkillInstalled | GET | /skills/{id}/installed | 检查是否已安装 |
| fetchInstalledSkills | GET | /skills/installed | 获取已安装技能 |
| fetchSkillInstallations | GET | /skill-installations | 获取安装记录 |

### 9.3 工作流 API (`api/workflow.ts`) — 21 个函数

| 函数名 | HTTP | URL | 说明 |
|--------|------|-----|------|
| fetchWorkflowList | GET | /workflows | 获取工作流列表 |
| fetchWorkflowById | GET | /workflows/{id} | 获取工作流详情 |
| createWorkflow | POST | /workflows | 创建工作流 |
| updateWorkflow | PUT | /workflows/{id} | 更新工作流 |
| deleteWorkflow | DELETE | /workflows/{id} | 删除工作流 |
| publishWorkflow | POST | /workflows/{id}/publish | 发布工作流 |
| archiveWorkflow | POST | /workflows/{id}/archive | 归档工作流 |
| fetchWorkflowNodes | GET | /workflows/{id}/nodes | 获取节点 |
| fetchWorkflowEdges | GET | /workflows/{id}/edges | 获取边 |
| saveWorkflowGraph | POST | /workflows/{id}/graph | 保存图 |
| executeWorkflow | POST | /workflows/{id}/execute | 执行工作流 |
| fetchExecutionById | GET | /executions/{id} | 获取执行详情 |
| fetchWorkflowExecutions | GET | /workflows/{id}/executions | 获取执行记录 |
| fetchNodeExecutions | GET | /executions/{id}/nodes | 获取节点执行记录 |
| stopExecution | POST | /executions/{id}/stop | 停止执行 |
| **fetchDppDatasets** | GET | /resources/dpp/datasets | **跨模块**: DPP数据集 |
| **fetchDppFeatures** | GET | /resources/dpp/features | **跨模块**: DPP特征工程 |
| **fetchMtpAlgorithms** | GET | /resources/mtp/algorithms | **跨模块**: MTP算法 |
| **fetchMtpTrainTasks** | GET | /resources/mtp/train-tasks | **跨模块**: MTP训练任务 |
| **fetchMepDeployments** | GET | /resources/mep/deployments | **跨模块**: MEP部署 |
| **fetchMepServices** | GET | /resources/mep/services | **跨模块**: MEP服务 |

---

## 十、核心业务流程

### 10.1 SSE 流式对话流程

```
前端 chat.vue / AssistantFloatingWindow
  │
  ├── 1. 用户输入消息 → sendMessage()
  ├── 2. 添加用户消息到 UI → addMessage() 保存到后端
  ├── 3. sendToGateway()
  │     └── fetch('/xaa/assistants/{id}/chat/completions', POST, stream:true)
  │         请求体兼容 OpenAI Chat Completions API:
  │         { model: 'openclaw', messages: [...最近20条], stream: true, user }
  │
  ├── 4. 后端 AssistantController.chatCompletions()
  │     ├── 获取助手配置
  │     ├── 解析 Gateway Token (优先自身 → 查 MEP OpenClaw 实例 → fallback UID)
  │     ├── 构建请求 → java.net.http.HttpClient
  │     └── SSE 流式转发: 读取 Gateway 响应 → 直接写入 HttpServletResponse
  │
  └── 5. 前端接收 SSE 流
        ├── ReadableStream → TextDecoder → 按 \n 分割
        ├── 解析 data: {JSON} → choices[0].delta.content
        ├── 追加到 streamingContent (实时渲染)
        └── 流完成 → saveAssistantMessage() 保存到后端
```

### 10.2 工作流执行流程

```
前端 workflowDesigner.vue → executeWorkflow()
  │
  ├── 1. POST /api/xaa/workflows/{id}/execute
  │     └── WorkflowExecutionService.startExecution()
  │         ├── 创建 WorkflowExecution 记录 (status=SCHEDULED)
  │         └── 触发 @Async executeWorkflowAsync()
  │
  ├── 2. 异步执行引擎
  │     ├── executeGraph() — 构建 DAG
  │     │   ├── 构建 nodeMap: Map<Long, WorkflowNode>
  │     │   ├── 构建 edgeMap: Map<Long, List<WorkflowEdge>>
  │     │   ├── 找到 start 节点
  │     │   └── 递归执行 executeNode()
  │     │
  │     └── executeNode() — 执行单个节点
  │         ├── 创建 NodeExecution 记录 (status=RUNNING)
  │         ├── executeNodeByType() — 按类型分发
  │         │   ├── DPP_DATASET → callDppService("/api/dpp/datasets/{id}")
  │         │   ├── DPP_FEATURE → callDppService("/api/dpp/features/{id}/execute")
  │         │   ├── DPP_TASK → callDppService("/api/dpp/tasks/{id}/execute")
  │         │   ├── MTP_TRAIN → callMtpService("/api/mtp/train-tasks/{id}/start")
  │         │   ├── MTP_ALGORITHM → callMtpService("/api/mtp/algorithms/{id}")
  │         │   ├── MEP_DEPLOY → callMepService("/api/mep/deployments/{id}/deploy")
  │         │   ├── MEP_SERVICE → callMepService("/api/mep/services/{id}")
  │         │   ├── HTTP_REQUEST → executeHttpRequest()
  │         │   ├── CODE → executeCode() (TODO)
  │         │   └── VARIABLE_ASSIGNER → executeVariableAssigner()
  │         ├── 更新 NodeExecution (status=SUCCEEDED/FAILED)
  │         └── 根据出边递归执行下一节点
  │
  └── 3. 完成
        └── 更新 WorkflowExecution (status=SUCCEEDED/FAILED)
```

### 10.3 技能安装/卸载流程

```
安装:
  前端 SkillCard → installSkill(id)
    → POST /api/xaa/skills/{id}/install (X-Tenant-UID, X-User-ID)
    → SkillService.installSkill()
      ├── 检查技能是否存在且已发布
      ├── 检查是否已安装 (skill_id + tenant_uid 联合唯一)
      ├── 创建 SkillInstallation 记录
      └── 技能 install_count + 1

卸载:
  前端 installed/index.vue → uninstallSkill(id)
    → POST /api/xaa/skills/{id}/uninstall (X-Tenant-UID)
    → SkillService.uninstallSkill()
      ├── 删除 SkillInstallation 记录
      └── 技能 install_count - 1 (GREATEST(0, count-1) 防负)
```

### 10.4 工作流设计器操作流程

```
1. 创建工作流 → 自动跳转设计器页面
2. 设计器加载:
   ├── 并行获取 nodes + edges
   ├── 后端格式 → 前端格式 (positionX/Y → position:{x,y}, configJson → config对象)
   └── 渲染 WorkflowDesigner 组件

3. 编辑:
   ├── 左侧面板拖拽/点击添加节点
   ├── 连线: Vue Flow 原生连接
   ├── 右侧面板配置节点参数
   ├── 撤销/重做: undo/redo (最多50步)
   └── 快捷键: Delete/Ctrl+Z/Ctrl+Y/H/V

4. 保存:
   ├── 前端格式 → 后端格式
   └── POST /workflows/{id}/graph (节点+边)

5. 运行:
   ├── POST /workflows/{id}/execute
   └── 跳转执行详情页
```

---

## 十一、工作流设计器详解

### 11.1 节点类型体系 (19种)

| 分类 | 节点类型 | 标题 | 颜色 | 图标 |
|------|----------|------|------|------|
| 基础 | start | 开始 | #52c41a (绿) | play-circle |
| 基础 | end | 结束 | #ff4d4f (红) | stop-circle |
| 数据处理(DPP) | dpp-dataset | 数据集 | #1890ff (蓝) | database |
| 数据处理(DPP) | dpp-feature | 特征工程 | #1890ff (蓝) | experiment |
| 数据处理(DPP) | dpp-task | 数据处理任务 | #1890ff (蓝) | thunderbolt |
| 模型训练(MTP) | mtp-algorithm | 算法选择 | #722ed1 (紫) | robot |
| 模型训练(MTP) | mtp-train | 模型训练 | #722ed1 (紫) | rocket |
| 模型训练(MTP) | mtp-output | 模型输出 | #722ed1 (紫) | crown |
| 模型部署(MEP) | mep-deploy | 模型部署 | #fa8c16 (橙) | cloud-upload |
| 模型部署(MEP) | mep-service | 推理服务 | #fa8c16 (橙) | api |
| 逻辑控制 | if-else | 条件分支 | #13c2c2 (青) | branches |
| 逻辑控制 | loop | 循环 | #13c2c2 (青) | sync |
| 逻辑控制 | parallel | 并行执行 | #13c2c2 (青) | apartment |
| 工具 | http-request | HTTP请求 | #eb2f96 (粉) | global |
| 工具 | code | 代码执行 | #eb2f96 (粉) | code |
| 工具 | variable-assigner | 变量赋值 | #eb2f96 (粉) | edit |
| 工具 | template-transform | 模板转换 | #eb2f96 (粉) | file-text |
| 工具 | wait | 等待 | #eb2f96 (粉) | hourglass |
| 工具 | human-input | 人工输入 | — | — |

### 11.2 设计器三栏布局

```
┌──────────┬───────────────────────────────────┬──────────┐
│          │         Toolbar (48px)             │          │
│ NodeSel  ├───────────────────────────────────┤ NodeCfg  │
│ ector    │                                   │ Panel    │
│ (280px)  │     Vue Flow Canvas               │ (320px)  │
│          │     (MiniMap + Controls)           │          │
│ 搜索     │                                   │ 节点配置  │
│ 6个分类  │     拖拽/连线/缩放                  │ 按类型动态 │
│ 可拖拽   │     snapGrid: [15,15]              │          │
│          │     zoom: 0.25-2                   │          │
└──────────┴───────────────────────────────────┴──────────┘
```

### 11.3 NodeConfigPanel 动态配置

| 节点类型 | 配置字段 |
|----------|----------|
| start / end | 无配置 |
| dpp-dataset | 数据集选择(Select) |
| dpp-feature | 特征工程选择(Select) |
| mtp-algorithm | 算法选择(Select) |
| mtp-train | 训练任务名称 + 超参数JSON |
| mep-deploy | 部署名称 + 副本数 + CPU + 内存 |
| if-else | 条件分支管理(动态添加分支和条件) |
| http-request | 请求方法 + URL + 请求头JSON + 请求体 + 超时 |
| code | 编程语言(Python/JS) + 代码编辑器 |
| wait | 等待时间 + 时间单位 |

### 11.4 快捷键

| 快捷键 | 功能 |
|--------|------|
| Delete | 删除选中节点/边 |
| Ctrl+Z | 撤销 |
| Ctrl+Y | 重做 |
| H | 手形拖拽模式 |
| V | 指针选择模式 |
| Ctrl+1 | 适应画布 |

---

## 十二、跨模块依赖

### 12.1 依赖关系图

```
XAA (AI智能体 — 编排中枢)
  │
  ├──→ DPP (数据处理平台)
  │     ├── GET /api/dpp/datasets — 获取数据集列表
  │     ├── GET /api/dpp/features — 获取特征工程列表
  │     ├── GET/POST /api/dpp/datasets/{id} — 操作数据集
  │     ├── POST /api/dpp/features/{id}/execute — 执行特征工程
  │     └── POST /api/dpp/tasks/{id}/execute — 执行数据处理任务
  │
  ├──→ MTP (模型训练平台)
  │     ├── GET /api/mtp/algorithms — 获取算法列表
  │     ├── GET /api/mtp/train-tasks — 获取训练任务列表
  │     ├── GET /api/mtp/algorithms/{id} — 获取算法详情
  │     └── POST /api/mtp/train-tasks/{id}/start — 启动训练
  │
  └──→ MEP (模型部署平台)
        ├── GET /api/mep/deployments — 获取部署列表
        ├── GET /api/mep/llm-services — 获取LLM服务列表
        ├── POST /api/mep/deployments/{id}/deploy — 触发部署
        ├── GET /api/mep/services/{id} — 获取服务详情
        └── GET /mep/openclaw/instances/{id} — 获取OpenClaw实例(Gateway Token)
```

### 12.2 前端跨模块调用

- **助手创建页** (`create.vue`): 通过 `mepRequestClient.get('/openclaw/instances')` 获取 MEP 的 OpenClaw 实例列表
- **工作流 API** (`workflow.ts`): 6 个 `/resources/*` 代理函数通过 XAA 后端中转获取 DPP/MTP/MEP 资源

### 12.3 全局浮窗集成

`AssistantFloatingWindow` 组件嵌入 `layouts/basic.vue`，在所有页面显示。该浮窗直接导入 `views/XAA/api/types` 和 `views/XAA/api/assistant`，是 XAA 模块对全平台的入口。

---

## 十三、前端类型定义

### 13.1 核心类型 (`api/types.ts`)

**工作流相关:**
- `Workflow` — id, uid, name, description, type, status, graphJson, configJson, version, 租户/部门/团队UID
- `WorkflowNode` — nodeType, title, configJson, positionX/Y, sortOrder
- `WorkflowEdge` — sourceNodeId, targetNodeId, sourceHandle, targetHandle, edgeType, conditionJson
- `WorkflowExecution` — status, inputsJson, outputsJson, errorMessage, startedAt, finishedAt, elapsedTime, triggerType
- `NodeExecution` — nodeType, nodeTitle, status, metadataJson, retryCount
- `NodeType` — 19种联合类型
- `ExecutionStatus` — scheduled/pending/running/succeeded/failed/stopped/paused/skipped
- `WorkflowStatus` — draft/published/archived

**智能助手相关:**
- `Assistant` — 基本信息 + OpenClaw配置 + 模型配置 + 知识库配置 + 技能配置 + UI配置 + 状态 + 统计
- `AssistantUIConfig` — position, size, theme, primaryColor, borderRadius, showAvatar
- `AssistantConversation` — assistantId, title, summary, status, messageCount, tokenCount
- `AssistantMessage` — conversationId, role, content, tokenCount, modelUsed, ragSources, toolCalls

### 13.2 技能类型 (`skill/types.ts`)

- `SkillType` = `'tool' | 'prompt' | 'chain' | 'workflow'`
- `SkillStatus` = `'draft' | 'published' | 'archived'`
- `SkillCategoryKey` = 8种分类键
- `Skill` — 扩展版(category, tags, contentMd, hasScripts, installCount, isOfficial)
- `SkillCategory` — id, key, name, nameEn, description, icon, color, sortOrder
- `SkillInstallation` — skillId, tenantUid, installedBy, configOverride, status

### 13.3 设计器类型 (`designer/types.ts`)

**枚举:**
- `NodeType` — 18种节点枚举
- `NodeClassification` — 6种分类 (Basic/DataProcess/ModelTrain/ModelDeploy/Logic/Tool)
- `ExecutionStatus` — 6种状态枚举
- `ControlMode` — Pointer / Hand

**接口:**
- `NodeMetadata` — type, classification, title, description, icon, color, min/maxInputs, min/maxOutputs
- `NodeData` — type, title, config, inputs/outputs, selected, running, status
- 各模块节点配置: `DppDatasetConfig`, `DppFeatureConfig`, `MtpTrainConfig`, `MepDeployConfig`
- 工具节点配置: `HttpRequestConfig`, `CodeConfig`, `WaitConfig`
- `IfElseConfig` — conditions/cases + else 分支
- `WorkflowGraph`, `WorkflowDesignerState`, `NodePanelConfig`

---

## 十四、配置与枚举

### 14.1 application.yml

```yaml
server:
  port: 8186

spring:
  application:
    name: mlops-xaa-service
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/XnetMLops?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: "${DB_PASSWORD}"

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.synapxnet.mlopsxaaservice.entity
  configuration:
    map-underscore-to-camel-case: true

# 跨模块服务端点
xaa:
  services:
    dpp-url: http://localhost:8081
    mtp-url: http://localhost:8082
    mep-url: http://127.0.0.1:8184
```

### 14.2 枚举类

**ExecutionStatus (8种):**
| 枚举值 | 标签 | 终态 |
|--------|------|------|
| SCHEDULED | 已调度 | 否 |
| PENDING | 等待中 | 否 |
| RUNNING | 运行中 | 否 |
| SUCCEEDED | 成功 | 是 |
| FAILED | 失败 | 是 |
| STOPPED | 已停止 | 是 |
| PAUSED | 暂停中 | 否 |
| SKIPPED | 已跳过 | 是 |

**NodeType (19种):**
- 基础: START, END
- DPP: DPP_DATASET, DPP_FEATURE, DPP_TASK
- MTP: MTP_ALGORITHM, MTP_TRAIN, MTP_OUTPUT
- MEP: MEP_DEPLOY, MEP_SERVICE
- 流程控制: IF_ELSE, LOOP, PARALLEL
- 工具: HTTP_REQUEST, CODE, VARIABLE_ASSIGNER, TEMPLATE_TRANSFORM, WAIT, HUMAN_INPUT

**WorkflowStatus (3种):** DRAFT, PUBLISHED, ARCHIVED

### 14.3 UID 生成策略

| 实体 | UID 格式 |
|------|----------|
| Assistant | `ASST-XXXXXXXX` (UUID前8位大写) |
| Conversation | `CONV-XXXXXXXX` |
| Message | `MSG-XXXXXXXX` |
| Skill | `SKILL-XXXXXXXX` |
| Workflow | 完整 UUID |
| WorkflowExecution | 完整 UUID |
| NodeExecution | 完整 UUID |
| WorkflowNode | 完整 UUID |
| WorkflowEdge | 完整 UUID |

### 14.4 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "error": null
}
```

---

## 十五、特殊设计模式

### 15.1 异步工作流执行引擎

- `@Async` + Spring 线程池异步执行
- 入口 `startExecution()` 创建执行记录后立即返回
- 递归 DFS 图遍历，每个节点创建独立的 `NodeExecution` 记录
- `executeNodeByType()` switch-case 路由到不同执行器
- 节点失败时整个 Execution 标记为 failed

### 15.2 SSE 流式对话代理

- 使用 `HttpServletResponse` 直接写原始字节流，避免 Spring SSE 框架二次包装
- `java.net.http.HttpClient` 进行原始 SSE 流转发
- Token 解析优先级: 助手自身 gatewayToken → MEP 查 OpenClaw 实例 → fallback UID
- 超时: 连接 30s，请求 5min

### 15.3 工作流设计器 (参考 Dify)

- 基于 `@vue-flow/core` 库实现
- 统一节点渲染: 所有 18 种节点通过 `BaseNode.vue` 渲染
- 拖拽添加: `application/xaa-node` DataTransfer
- undo/redo 系统 (最多50步)
- IfElse 节点多输出 Handle (每分支一个 + ELSE)

### 15.4 多租户技能安装

- `X-Tenant-UID` 请求头传递租户标识
- `(skill_id, tenant_uid)` 联合唯一键
- 安装计数使用 `GREATEST(0, count-1)` 防负

### 15.5 可拖拽浮窗

- `AssistantFloatingWindow` 嵌入全局布局 `basic.vue`
- 60px圆形按钮，支持拖拽 (5px阈值防误触)
- 展开 380x500px 聊天窗口，支持全屏
- `Teleport` 到 body，脱离组件树
- 自动加载默认助手配置

### 15.6 前端双解包防御

由于 `defaultResponseInterceptor({dataField:'data'})` + `responseReturn:'data'` 双重解包，API 函数普遍使用:
```typescript
Array.isArray(res) ? res : (res as any).data || []
```

### 15.7 版本控制

Workflow 和 Skill 都有 `version` 字段，每次 `update` 时自动 +1。

### 15.8 状态管理

XAA 模块没有独立的 Pinia store。所有状态在组件内部通过 `ref`/`reactive` 管理，跨页面数据传递通过路由 query 参数（如 `?id=123`）。
