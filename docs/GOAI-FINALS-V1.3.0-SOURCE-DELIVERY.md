<!-- Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly forbidden to copy,
distribute, or use without explicit authorization.
源码交付与版本说明 / Source delivery and version notes.
Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com -->

# XnetMLOps GOAI 决赛 V1.3.0 源码交付

## 版本与范围

产品与 Maven 模块统一为 1.3.0。原有契约字段、第三方依赖和迁移格式版本保持兼容。

本次交付组合已上线优化工作树与受控执行修复：模型证据/模型制品读取、OpenXnet Skill 候选查询、工作流图契约、数据集配置、DPP/MTP 有界内存 HDFS 下载，以及 MEP Agent 契约、审批绑定、资源版本和迁移校验。

MEP 页面启停/扩缩操作在未绑定受控执行器时明确返回不可用，不得伪造执行成功。受控工具入口继续要求委派身份、审批、幂等键和资源版本。日志与指标读取持久化事实，缺失事实返回空记录；连接在线不等于运行时已验证。

## 构建

需要 JDK 17、Maven 3.9 或以上。仓库根运行 `mvn -B -ntp -DskipTests package`；Dockerfile 使用各模块 `target/*-1.3.0.jar`。完整业务联调需要另行提供数据库、Redis、HDFS 及模型运行环境，本轮源码提交不重部署这些服务。

数据库结构来自 `XnetMLops.sql` 与模块 SQL；生产升级须先备份并审阅差异，不执行清库初始化。MEP 新安装先导入基础表，再审阅执行 `mlops-mep-service/src/main/resources/sql/V20260917_01__goai_governance.sql`；已有环境先比对结构后应用。该脚本依据 2026-09-17 只读 `SHOW CREATE TABLE` 的四张实际治理表与部署表三列整理，移除了自增计数，不包含业务行。它保留索引/唯一约束、只补缺失列，但尚未在隔离 MySQL 上执行迁移演练；不能把结构核对当成迁移验收。MySQL DDL 会自动提交，应先备份并在隔离环境试跑。模型制品新增表见 `mlops-mtp-service/src/main/resources/sql/model_artifact_table.sql`。

## 配置与运行边界

复制 `.env.example` 到本机受限 `.env` 后按目标环境填写。数据库口令、JWT 签名密钥、服务委派密钥和平台凭据不进入源码。数据库/HDFS/运行时地址均由环境配置，不能直接复用开发默认值到生产。

DPP/MTP/SMP 显式传递 `HDFS_HOST`、`HDFS_PORT`、`HDFS_USER`。若 HDFS 公告独立 DataNode 主机名，部署者还需配置可达的网络与主机名映射；仅 NameNode 可连不代表文件下载可用。Jenkins 令牌允许空值以支持只读页面初始化，但空值不授予构建能力。既有生产隧道私钥、主机指纹和存储数据不随源码公开。

通用 Compose 是独立环境入口，服务名与生产环境可能不同；不得用其直接覆盖当前线上 Compose。生产 Nginx 必须保留模型证据、Skill 读取、驻场 Agent 及受控执行路由。服务重建必须保留 HDFS 配置、持久化挂载、委派密钥和审批配置。迁移 consumed 标记不得删除以重放历史状态。

驻场 Agent 是独立平台服务，源码固定依赖 [OpenXnet c841ef841da8477fc312e27cd390aecac8ed2d7e](https://github.com/synapxnet/OpenXnet/tree/c841ef841da8477fc312e27cd390aecac8ed2d7e/services/platform-resident-agent)。本仓库 `deploy/resident-agent-v1.3.0.yaml` 是部署契约，不能当作已配置模型与生产凭据的运行实例。

## 验证与限制

本轮新组合已通过八个 Maven reactor 项目的完整打包与 118 项隔离单元测试（27 个测试类，零失败、零错误、零跳过）。覆盖委派鉴权、自动发现注册、审批摘要、版本/迁移、执行边界、独立探针、HDFS 下载、证据读取、工作流与 Skill 导入权限。单测明确排除需要真实数据库/训练环境的 Application/Integration 测试。历史线上发布记录不是此次源码组合的再次线上验收。本轮不连接生产数据库、不执行训练或部署动作、不发布新容器。

源码回退通过 Git 提交选择实现；线上回退必须同时核对镜像、JAR、数据库迁移、持久化状态和配置，不能只回退 JAR 名称。
