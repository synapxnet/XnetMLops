# model-regression-response

版本：1.0.0
状态：草稿，必须人工审核后发布

## 目标

当模型输入契约与生产数据 Schema 不一致时，使用跨平台证据定位问题，在审批边界内回滚，并通过独立探针与服务健康证据验证结果。

## 依赖工具

- `aiops.alert.get`
- `aiops.service.health`
- `aiops.k8s.workload.get`
- `dataops.quality.report.get`
- `dataops.schema.snapshot.get`
- `dataops.lineage.get`
- `dataops.workflow.instance.get`
- `mlops.deployment.get`
- `mlops.inference.probe`
- `mlops.deployment.rollback`

最小工具契约版本：`1.0.0`

## 执行边界

1. 只读工具用于收集结构化 Evidence，不根据截图下结论。
2. `mlops.deployment.rollback` 必须具备有效审批、幂等键、预期资源版本和原因。
3. 动作受理不等于成功；必须等待动作终态，再运行独立推理探针和服务健康检查。
4. Skill 不保存 Token、端点凭据、样本值或本地绝对路径。

## 输出

输出应引用 Incident、Trace、Evidence、Approval、Action 和 Audit Receipt ID，并明确区分事实、判断、动作和验证结果。
