<div align="center">

**简体中文** | [English](./README.en-US.md) | [日本語](./README.ja-JP.md)

# XnetMLops

**连接数据处理、模型训练、部署服务与智能体编排的开源 MLOps 平台**

[![GOAI release](https://img.shields.io/badge/GOAI_release-1.3.0-1677ff.svg)](https://github.com/synapxnet/XnetMLops/releases/tag/v1.3.0)
[![Java](https://img.shields.io/badge/Java-17-e76f00.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-6db33f.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-2ea44f.svg)](./LICENSE)

[在线体验](https://www.xnetmlops.synapxnet.cn) · [前端仓库 XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) · [OpenXnet 开源社区](https://openxnet.synapxnet.com) · [查看许可](./LICENSE)

</div>

## GOAI v1.3.0 发布与下载

当前默认 `display` 分支保留原展示代码。**GOAI v1.3.0 发布版**及决赛源码请通过下方入口获取；本次 README 更新不代表默认分支业务代码已升级。

**[发布说明](https://github.com/synapxnet/XnetMLops/releases/tag/v1.3.0) · [下载源码 ZIP](https://github.com/synapxnet/XnetMLops/releases/download/v1.3.0/XnetMLops-v1.3.0-c9cae060-source.zip) · [GOAI 源码分支](https://github.com/synapxnet/XnetMLops/tree/GOAI-Competition) · [构建与交付文档](https://github.com/synapxnet/XnetMLops/blob/c9cae0607fc6fba6b76b26d9da4ca1ace0ab13bc/docs/GOAI-FINALS-V1.3.0-SOURCE-DELIVERY.md)**

[配套前端 v1.3.0](https://github.com/synapxnet/XnetMLops-web/releases/tag/v1.3.0) · [OpenXnet v1.3.0](https://github.com/synapxnet/OpenXnet/releases/tag/v1.3.0)

GOAI 版包含模型证据/制品读取、Skill 候选查询、工作流契约和有界内存 HDFS 下载。MEP 受控执行检查委派身份、审批摘要、资源版本和幂等约束；未配置执行器的操作明确返回不可用。

[驻场 Agent 运行时](https://github.com/synapxnet/OpenXnet/tree/c841ef841da8477fc312e27cd390aecac8ed2d7e/services/platform-resident-agent)作为独立平台服务运行，与 OpenXnet AgentTeams 协同，需要配置平台身份、模型服务与委派权限；已有智能助手不代表驻场运行时已经配置完成。

本次源码交付验证：8 个 Maven Reactor 打包成功，118 项隔离单测通过；外部服务 Application/Integration 测试未执行，迁移 SQL 尚未在隔离 MySQL 试跑。 发布版本不代表重新部署线上，也不等于生产认证；环境配置与限制以交付文档为准。

> **下方均为历史界面截图：** 保留作为展示参考，不代表 v1.3.0 最新 UI 验收结果，也不是实时治理执行证据。

![XnetMLops MLOps 分析中心](./docs/images/xnetmlops-analytics-2026.png)

## 界面预览

以下界面由配套前端仓库 [XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) 提供，展示数据来自 `demo/showcase_data.sql`。

### DPP 数据处理

| 数据集管理 | RAG 知识库 |
| --- | --- |
| ![XnetMLops 数据集管理](./docs/images/xnetmlops-dpp-datasets.png) | ![XnetMLops RAG 知识库](./docs/images/xnetmlops-dpp-knowledge-base.png) |

### MTP 模型训练与 MEP 模型部署

| 训练任务 | 模型部署 |
| --- | --- |
| ![XnetMLops 训练任务](./docs/images/xnetmlops-mtp-training.png) | ![XnetMLops 模型部署](./docs/images/xnetmlops-mep-deployments.png) |

### SMP 系统管理与 XAA 智能体

| 工作站资源 | 智能体工作流 |
| --- | --- |
| ![XnetMLops 工作站资源](./docs/images/xnetmlops-smp-workstations.png) | ![XnetMLops 智能体工作流](./docs/images/xnetmlops-xaa-workflows.png) |

| 元技能仓库 | 智能助手 |
| --- | --- |
| ![XnetMLops 元技能仓库](./docs/images/xnetmlops-xaa-skills.png) | ![XnetMLops 智能助手](./docs/images/xnetmlops-xaa-assistants.png) |

### 演示入口与项目信息

| 演示登录 | 关于项目 |
| --- | --- |
| ![XnetMLops 演示登录](./docs/images/xnetmlops-login.png) | ![XnetMLops 关于项目](./docs/images/xnetmlops-about.png) |

## 项目简介

XnetMLops 是由 **SynapXnet 团队**开源的全流程 MLOps 平台，面向机器学习、生成式 AI 与智能体应用，将数据准备、模型训练、模型部署、资源管理和智能体编排连接为可持续迭代的工程闭环。

本仓库是平台后端，与 [XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) 前端仓库共同组成企业级、多租户、前后端分离系统。平台以独立微服务承载 DPP、MTP、MEP、SMP 与 XAA 五个核心业务域，可以对接 Hadoop、Jenkins、对象存储、镜像仓库与模型推理节点。

## 项目优势

- **企业多租户**：通过租户、部门、团队、角色和资源边界支撑多角色协作。
- **前后端分离**：Web 控制台与后端服务独立交付，便于企业集成和二次开发。
- **全流程闭环**：连接数据处理、定时训练、模型部署、推理服务与智能体编排。
- **开放式集成**：可对接 Hadoop、Jenkins、对象存储、Harbor 与模型计算节点。
- **持续更新**：SynapXnet 团队会持续完善训练调度、模型服务、RAG、智能体与文档。

## 核心能力

| 模块 | 服务目录 | 说明 |
| --- | --- | --- |
| DPP 数据处理 | `mlops-dpp-service` | 管理数据集、预处理与特征工程，编排定时数据管道；支持 RAG 知识库与 Jenkins 执行集成 |
| MTP 模型训练 | `mlops-mtp-service` | 管理算法与训练任务，配置数据集、变量和运行参数；支持立即或定时训练，并跟踪训练状态、日志与模型产物 |
| MEP 模型部署 | `mlops-mep-service` | 管理模型部署、LLM 服务、API 密钥、部署节点和运行日志，并支持 OpenClaw 实例管理 |
| SMP 系统管理 | `mlops-smp-service` | 管理租户、部门、团队、数据源、存储桶、工作站以及 Hadoop、Jenkins、Harbor、镜像和算子资源 |
| XAA 智能体 | `mlops-xaa-service` | 创建智能体助手，管理会话与消息，通过可视化工作流、技能和编排能力调用 DPP、MTP 与 MEP |
| Login 认证服务 | `mlops-login` | 提供登录认证和平台访问入口，为多模块协作提供统一身份基础 |

## 典型工作流

```mermaid
flowchart LR
    Data["原始数据"] --> DPP["DPP 数据处理"]
    DPP --> Dataset["数据集 / 特征 / RAG 知识库"]
    Dataset --> MTP["MTP 即时或定时训练"]
    MTP --> Artifact["模型产物"]
    Artifact --> MEP["MEP 模型与 LLM 服务"]
    MEP --> API["推理 API"]
    XAA["XAA 智能体编排"] --> DPP
    XAA --> MTP
    XAA --> MEP
    SMP["SMP 资源与租户"] --> DPP
    SMP --> MTP
    SMP --> MEP
```

后端基于 Java 17、Spring Boot 3.4.6 与 Maven 多模块工程构建，使用 MySQL 和 Redis 保存平台数据，并通过 Hadoop、Jenkins、Harbor 等外部系统执行数据处理、训练和制品交付任务。

## 目录结构

```text
XnetMLops/
├── mlops-dpp-service/    # 数据处理与 RAG 知识库
├── mlops-mtp-service/    # 模型训练
├── mlops-mep-service/    # 模型部署与推理服务
├── mlops-smp-service/    # 租户和基础资源管理
├── mlops-xaa-service/    # 智能体与工作流编排
├── mlops-login/          # 登录认证
├── docs/                 # 专题文档与项目图片
├── nginx/                # 反向代理配置
├── XnetMLops.sql         # 数据库初始化脚本
└── docker-compose.yml    # 容器编排
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Docker 与 Docker Compose
- MySQL 8.x、Redis 7.x
- 按启用模块准备 Hadoop、Jenkins、Harbor 或对象存储

### Maven 构建

```bash
mvn -DskipTests package
```

### 容器启动

复制环境变量模板，配置数据库、Redis、HDFS、Jenkins、`JENKINS_API_TOKEN` 与 `JWT_SECRET`；如需同时启动 Web，请先构建同级的 `XnetMLops-web` 仓库，并确认 `WEB_DIST_PATH` 指向前端产物。

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

生产环境请使用专用服务账号、独立密钥和最小权限策略，不要沿用演示环境配置。

## 在线体验

- 访问地址：<https://www.xnetmlops.synapxnet.cn>
- 演示手机号：`12345678900`
- 演示验证码：`000000`

固定验证码仅用于开源项目展示，不应作为生产环境认证方案。

## SynapXnet 开源生态

XnetMLops 是 SynapXnet 开源体系的 AI 工程平台。更多团队项目、技术方向与社区动态请访问 [OpenXnet](https://openxnet.synapxnet.com)。

## 参与贡献

欢迎通过 Issue 提交缺陷与需求，或通过 Pull Request 贡献新算子、训练适配、部署后端和智能体能力。涉及数据结构或外部服务契约的变更，请同时提供迁移说明。

## 开源许可

本项目基于 [MIT License](./LICENSE) 开源。你可以自由使用、修改和分发本项目，但须保留原始版权与许可声明。
