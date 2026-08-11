# GOAI Competition 1.0.0 后端交接

- 仓库：`synapxnet/XnetMLops`
- 任务：`MLOPS-BE-01`、`MLOPS-BE-02`、`MLOPS-BE-03`
- Worktree：`D:\synapxnet\.codex-build\goai-competition-1.0.0\XnetMLops`
- 分支：`GOAI-Competition`
- 基线提交：`8a66c633ea57602cef717e50a58067fcca8df6bc`
- 结束提交：见 `GOAI-Competition` 分支候选 HEAD
- 产品/契约版本：`1.0.0`

## 交付范围

| 工具 | 风险 | 行为 |
| --- | --- | --- |
| `mlops.deployment.get` | READ | 数据库期望状态、Runtime readiness、修订和模型契约 |
| `mlops.inference.probe` | COSTLY_READ | 读取脱敏 120 维 Fixture 并调用推理端点 |
| `mlops.deployment.rollback` | HIGH_RISK_WRITE | 审批、职责分离、幂等、乐观锁、持久状态机和补偿 |

动作查询路径为 `GET /api/agent/v1/actions/{actionId}`。XAA 新增复盘 Skill 草稿，只能人工审核后发布，不复制审批或编排所有权。

## 迁移与回退

按 `V20260802_11` 至 `V20260802_17` 的顺序执行 `database/migrations/`，最后执行 `V20260802__validate.sql`。Fixture 创建：

- `deploy_risk_prod`：活动 revision 18。
- `contract_risk_v18`：期望输入 128 维。
- `contract_risk_v17`：期望输入 120 维。
- `fixture://goai/risk-120-v1`：脱敏、固定摘要的真实探针输入。

执行前备份。回退前停止动作消费者并导出 Action/Audit/Evidence，然后在维护窗口执行 `R20260802__goai_rollback.sql`。运行中的 Action 不得通过回退脚本伪装成已取消。

## 配置键

- `openxnet.agent.delegation-secret`
- `openxnet.agent.audience`
- `openxnet.approval.base-url`
- `openxnet.approval.service-token`
- Docker Runtime 使用标准 `DOCKER_HOST` 与 TLS 环境配置

`TestApprovalVerifier` 只在 Spring `test` Profile 生效；非测试 Profile 强制使用远程审批内省。

## 回滚调用样例

```bash
curl -X POST 'https://<mlops-mep>/api/agent/v1/tools/mlops.deployment.rollback:invoke' \
  -H 'Authorization: Bearer <short-lived-delegation-token>' \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: <stable-idempotency-key>' \
  -H 'X-OpenXnet-Workspace-Id: ws_goai_demo' \
  -H 'X-OpenXnet-Incident-Id: inc_model_contract_001' \
  -H 'X-OpenXnet-Trace-Id: trace_model_contract_001' \
  -H 'X-OpenXnet-Tool-Name: mlops.deployment.rollback' \
  -d '{"requestId":"req_rollback_001","toolName":"mlops.deployment.rollback","arguments":{"deploymentUid":"deploy_risk_prod","targetRevision":17,"verificationPolicy":{"maxErrorRate":0.02,"maxP95Ms":500}},"approvalId":"<approved-id>","expectedResourceVersion":"<current-version>","reason":"v18 输入维度与生产特征不一致，回滚至已验证的 v17","dryRun":true}'
```

先用 `dryRun=true` 验证计划，再以同一审批范围、最新 `resourceVersion` 和新的幂等键提交真实动作。网络重试必须复用同一幂等键。

## 验证记录

```powershell
mvn -pl mlops-agent-contract -am test
mvn -pl mlops-mep-service,mlops-xaa-service -am test
```

结果：均为 `BUILD SUCCESS`；6 项新增测试通过，覆盖委托令牌、Dry Run 无副作用、相同幂等键只排队一次、摘要冲突和资源版本冲突。仓库原有 POM 存在重复依赖警告，未由本版本引入。

## GOAI Competition 1.1.0 三场景扩展

比赛环境入口为 `https://goai.xnetmlops.synapxnet.online`，当前解析到比赛专用服务器 `150.109.120.15`。域名和 IP 只属于部署环境，代码必须继续通过配置注入端点。

XnetMLops 当前负责 14 个固定工具：部署证据、归因、推理探针、模型迭代、特征流水线、训练搜索、质量评估、模型登记、备用特征启停、灰度发布、流量提升、发布验证和部署回滚。所有写步骤共享计划级审批、参数摘要、资源版本、职责分离、幂等与补偿边界。

`CompetitionModelLifecycleService` 保存按 Workspace + Incident 隔离的比赛生命周期状态；专用 `fixture://goai/verification-failure-v1` 只用于验证失败/补偿验收，不得作为成功 Fixture。真实 HTTPS Live 已验证三条正向场景以及独立验证失败后的部署回滚与备用特征恢复，测试后部署恢复 `v18/18/42`。

本轮定向测试：Agent Contract 7 项、原部署动作 5 项、比赛生命周期 2 项通过。Maven 仍会报告 DPP/MTP 原有重复依赖声明警告，本次没有用跳过编译掩盖该警告。

权威工具契约位于 `contracts/goai-tools.v1.json`，由 `D:\synapxnet\scripts\Sync-GoaiCompetitionContracts.cjs` 从 OpenXnet 注册表生成。不得手工删除新增工具或恢复旧的 10 工具清单。

## 已知限制与后续注意

- 真实回滚要求受控本机 Docker、目标镜像和修订规格均可用；Fixture 不等于生产 Runtime。
- 客户端超时不代表动作失败，必须读取 Action Resource。
- readiness 通过后仍需独立 Probe；Probe 失败不能标记最终成功。
- 前端深链为 `XnetMLops-web` 的 `/agent/incidents/:incidentId/model-evidence`。
