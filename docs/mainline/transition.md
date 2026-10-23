<!-- Copyright (C) 2026 Synapxnet. All rights reserved. Proprietary and Confidential.
Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0 | Security Level: INTERNAL -->
# MLOps 主线迁移设计

以最终交付源码为基线，保留驻场 Agent、智能助手、Skill 导入、模型证据、审批、资源版本、幂等、回滚、独立验证和模型工程能力。补入 9 月 19 日驻场知识与能力工作台。

清理范围为默认比赛账号、固定验证码、比赛组织种子、比赛专用演练入口和固定沙盘数据路由。历史协议 Java 包名不作破坏性改名。旧提交与原始代码已在 workspace backups/mainline-transition-20261023/mlops 归档，不改线上数据库和已发布比赛包。

正式登录保留手机号验证码交互，改为企业短信网关 + Redis 一次性验证码。三平台统一使用 XNET_SMS_GATEWAY_URL / XNET_SMS_GATEWAY_TOKEN，网关接收 POST JSON {phone, code, purpose: login, expiresInSeconds: 300}，通过 Bearer 验证并返回 {accepted: true}。未配置不发送、不接受固定验证码。验证码五分钟有效，每分钟限发一次，最多五次失败尝试，成功验证原子消费。各平台 Redis 命名空间隔离。

比赛沙盘固定数值生成、测试审批及旧 Demo 读取分支由 `openxnet.rehearsal.enabled=false` 退出默认生产配置；保留实现用于明确测试 profile 的回归测试。通用 Agent 工具、模型部署、跨域恢复证据页和治理契约仍可用，不将模拟数值作为生产证据。

验证包括旧验证码拒绝、合法验证码消费、重复消费拒绝、缺失网关提示、种子扫描、前端构建及保留能力路由检查。部署后的历史数据应按精确比赛对象标识先备份后定向清理，不能删除整个模型/审批/审计表。

## 运行配置与存量数据

真实量化与推荐执行器以 `QUANTITATIVE_DEPLOYMENT_UID` / `RECOMMENDATION_DEPLOYMENT_UID` 指向本平台登记部署，地址、令牌、算法/产品版本和探针内容均由部署环境注入，默认空值不会访问比赛服务器。`OPENXNET_MODEL_RESOURCE_BINDINGS` 为 JSON 数组，每条明确给出 `deploymentUid`、`candidateRevision`、`kind`（quant/recommendation/risk）及 `resourceIds`。非演练基线必须由实际运行时返回 `activeRevision` 与正整数 `resourceVersion`（子资源另需逐项 `resourceVersions`）；缺失时明确不可用，不补入固定比赛版本。

跨域特征恢复执行器仍可用；`OPENXNET_FEATURE_DRIFT_RESOURCE_IDS` 显式列出可代理的本平台资源。证据来源仅接收 `(PRODUCTION, false)` 或 `(REAL_CPU_SYNTHETIC_STAGING, true)`，并继续校验 Workspace、事件、Trace、审批摘要与执行回执。真实运行时令牌文件须由部署配置挂载。

已有数据库不会自动删除。`archive-competition-identities.sql` 默认只预览，多重匹配原始比赛操作员身份后可显式执行归档与撤权，保留审计主体；不会按手机号误伤正式账号或删除模型、任务、证据和历史审计。执行前须另做数据库备份。历史演练状态、训练产物与证据须按已核实来源单独归档，本次未操作线上数据库。

Java 包名 `com.synapxnet.goai.contract`、已发布 SQL 迁移编号和驻场契约版本为历史兼容标识，保留以免破坏现有调用与数据库迁移校验；它们不创建比赛数据或专用入口。


## 组织权限迁移与网关边界

新建数据库包含 `data_access_enabled`，默认 `0`。已有数据库先备份，再在目标数据库执行 `mlops-login/src/main/resources/sql/organization_data_access_migration.sql`；迁移可重复执行，且不会自动给现有成员授权。管理员应按已核实的租户、部门、团队和成员关系显式授予数据权限。

业务接口由 Nginx 的 `auth_request /_organization_access` 校验登录状态和实际组织数据权限。Docker Compose 仅将网页网关公开，后端端口默认绑定本机回环地址，容器间通过内部网络通信。不得把 8181–8186 端口直接发布到公网或绕过组织鉴权网关；Kubernetes 部署同样应只公开网关，并用网络策略限制后端服务的调用方。驻场 Agent 专用工具继续使用独立委派令牌与审批校验。
