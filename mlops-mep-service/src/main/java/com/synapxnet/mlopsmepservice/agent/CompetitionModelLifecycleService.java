package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护比赛部署的模型迭代、备用特征、灰度和发布状态，并生成可独立验证的 Live 证据。
 */
@Service
public class CompetitionModelLifecycleService {

    private final GovernedApprovalVerifier approvalVerifier;
    private final Map<String, LifecycleState> incidentStates = new ConcurrentHashMap<>();
    private final GovernedResourceVersionTracker versionTracker = new GovernedResourceVersionTracker();

    /**
     * 创建比赛模型生命周期服务。
     *
     * @param approvalVerifier 通用计划级审批验证器
     */
    public CompetitionModelLifecycleService(GovernedApprovalVerifier approvalVerifier) {
        this.approvalVerifier = approvalVerifier;
    }

    /**
     * 判断部署是否属于比赛沙盘。
     */
    public boolean supports(String deploymentUid) {
        return deploymentUid != null && (deploymentUid.startsWith("deploy_recommendation_")
                || deploymentUid.startsWith("deploy_quant_") || deploymentUid.startsWith("deploy_risk_"));
    }

    /**
     * 返回比赛部署、修订和模型输入契约证据。
     */
    public Map<String, Object> deploymentEvidence(
            AgentContract.RequestContext context,
            String deploymentUid) {
        requireDeployment(deploymentUid);
        LifecycleState state = state(context, deploymentUid);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deploymentUid", deploymentUid);
        data.put("name", deploymentUid.replace("deploy_", ""));
        data.put("status", "RUNNING");
        data.put("activeRevision", state.activeRevision());
        data.put("resourceVersion", String.valueOf(state.resourceVersion()));
        data.put("modelVersion", "revision-" + state.activeRevision());
        data.put("inputContractStatus", state.contractHealthy() ? "MATCHED" : "MISMATCHED");
        data.put("trafficPercent", state.trafficPercent());
        data.put("fallbackFeatureActive", state.fallbackFeatureActive());
        data.put("ready", true);
        return Map.copyOf(data);
    }

    /**
     * 执行比赛部署推理探针并返回契约、错误率和延迟证据。
     */
    public Map<String, Object> probe(
            AgentContract.RequestContext context,
            MepAgentDtos.InferenceProbeArguments arguments) {
        requireDeployment(arguments == null ? null : arguments.deploymentUid());
        LifecycleState state = state(context, arguments.deploymentUid());
        boolean forcedVerificationFailure = "fixture://goai/verification-failure-v1"
                .equals(arguments.testDatasetRef());
        boolean passed = state.contractHealthy() && !forcedVerificationFailure;
        return Map.of(
                "probeUid", "probe-" + context.requestId(),
                "deploymentUid", arguments.deploymentUid(),
                "revisionNumber", state.activeRevision(),
                "sampleCount", arguments.sampleLimit() == null ? 100 : arguments.sampleLimit(),
                "passed", passed,
                "errorRate", passed ? 0.002 : 0.184,
                "p95Ms", passed ? 72 : 430,
                "contractStatus", passed ? "MATCHED" : "MISMATCHED",
                "resultDigest", state.digestLabel());
    }

    /**
     * 返回量化模型的因子贡献、市场漂移和候选效果归因。
     */
    public Map<String, Object> attribution(
            AgentContract.RequestContext context,
            AttributionArguments arguments) {
        requireText(arguments == null ? null : arguments.reportUid(), "reportUid");
        requireDeployment(arguments == null ? null : arguments.deploymentUid());
        LifecycleState state = state(context, arguments.deploymentUid());
        boolean recovered = state.promoted();
        return Map.of(
                "deploymentUid", arguments.deploymentUid(),
                "reportUid", arguments.reportUid(),
                "informationCoefficient", recovered ? 0.035 : 0.01,
                "informationCoefficientThreshold", 0.03,
                "sharpeImprovement", recovered ? 0.06 : 0.0,
                "maximumDrawdownIncrease", recovered ? -0.01 : 0.02,
                "marketRegime", "MOMENTUM",
                "degradedFactors", List.of("value_pe", "value_pb", "dividend_yield"),
                "recommendedFactors", List.of("momentum_20d", "momentum_60d"),
                "passed", recovered);
    }

    /**
     * 发布候选特征流水线。
     */
    public AgentContract.ToolResponse<Map<String, Object>> publishFeaturePipeline(
            AgentContract.ToolRequest<FeaturePipelineArguments> body,
            AgentContract.RequestContext context) {
        FeaturePipelineArguments arguments = requireFeaturePipeline(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            state.setFeaturePipelinePublished(true);
            return Map.of("pipelineUid", arguments.pipelineUid(), "datasetUid", arguments.datasetUid(),
                    "addedFeatures", arguments.addedFeatures(), "removedFeatures", arguments.removedFeatures());
        });
    }

    /**
     * 启动固定搜索空间的并行训练或重训练。
     */
    public AgentContract.ToolResponse<Map<String, Object>> startTrainingSearch(
            AgentContract.ToolRequest<TrainingSearchArguments> body,
            AgentContract.RequestContext context) {
        TrainingSearchArguments arguments = requireTraining(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            state.setTargetRevision(arguments.targetRevision());
            state.setTrainingCompleted(true);
            return Map.of(
                    "experimentUid", arguments.experimentUid(),
                    "targetRevision", arguments.targetRevision(),
                    "trialCount", arguments.trialCount(),
                    "architectures", arguments.architectures(),
                    "mode", arguments.mode(),
                    "status", "SUCCEEDED");
        });
    }

    /**
     * 执行候选模型质量门评估，不修改部署。
     */
    public Map<String, Object> evaluate(
            AgentContract.RequestContext context,
            EvaluationArguments arguments) {
        requireEvaluation(arguments);
        LifecycleState state = state(context, arguments.deploymentUid());
        boolean passed = state.trainingCompleted() && state.targetRevision() == arguments.targetRevision();
        state.setEvaluationPassed(passed);
        return Map.of(
                "experimentUid", arguments.experimentUid(),
                "targetRevision", arguments.targetRevision(),
                "passed", passed,
                "status", passed ? "PASSED" : "FAILED",
                "informationCoefficient", passed ? 0.035 : 0.01,
                "sharpeImprovement", passed ? 0.06 : 0.0,
                "maximumDrawdownIncrease", passed ? -0.01 : 0.02,
                "adversarialValidationPassed", passed);
    }

    /**
     * 登记通过质量门的候选模型和模型卡。
     */
    public AgentContract.ToolResponse<Map<String, Object>> registerModel(
            AgentContract.ToolRequest<ModelRegisterArguments> body,
            AgentContract.RequestContext context) {
        ModelRegisterArguments arguments = requireRegister(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            if (!state.evaluationPassed()) {
                throw new AgentContractException(412, "QUALITY_GATE_FAILED", "候选模型尚未通过评估");
            }
            state.setRegistered(true);
            return Map.of("modelCardUid", arguments.modelCardUid(), "targetRevision", arguments.targetRevision(),
                    "registryStatus", "REGISTERED");
        });
    }

    /**
     * 启用已经验证的备用特征集以即时止损。
     */
    public AgentContract.ToolResponse<Map<String, Object>> applyFallback(
            AgentContract.ToolRequest<FallbackApplyArguments> body,
            AgentContract.RequestContext context) {
        FallbackApplyArguments arguments = requireFallbackApply(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            state.setFallbackFeatureActive(true);
            state.setContractHealthy(true);
            return Map.of("featureSetUid", arguments.featureSetUid(), "status", "APPLIED",
                    "reasonCode", arguments.reasonCode());
        });
    }

    /**
     * 在新修订全量稳定后退出备用特征集。
     */
    public AgentContract.ToolResponse<Map<String, Object>> removeFallback(
            AgentContract.ToolRequest<FallbackRemoveArguments> body,
            AgentContract.RequestContext context) {
        FallbackRemoveArguments arguments = requireFallbackRemove(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            if (!state.promoted()) {
                throw new AgentContractException(412, "PRECONDITION_FAILED", "候选修订尚未全量提升");
            }
            state.setFallbackFeatureActive(false);
            state.setContractHealthy(true);
            return Map.of("featureSetUid", arguments.featureSetUid(), "targetRevision", arguments.targetRevision(),
                    "status", "REMOVED");
        });
    }

    /**
     * 将候选修订发布到受控灰度流量。
     */
    public AgentContract.ToolResponse<Map<String, Object>> applyCanary(
            AgentContract.ToolRequest<CanaryArguments> body,
            AgentContract.RequestContext context) {
        CanaryArguments arguments = requireCanary(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            if (!state.registered()) {
                throw new AgentContractException(412, "PRECONDITION_FAILED", "候选模型尚未登记");
            }
            state.setCanaryPassed(true);
            state.setTrafficPercent(arguments.trafficPercent());
            return Map.of("targetRevision", arguments.targetRevision(), "trafficPercent", arguments.trafficPercent(),
                    "environment", arguments.environment(), "observationMinutes", arguments.observationMinutes(),
                    "status", "SUCCEEDED");
        });
    }

    /**
     * 将通过灰度门的候选修订提升到目标流量。
     */
    public AgentContract.ToolResponse<Map<String, Object>> promote(
            AgentContract.ToolRequest<PromoteArguments> body,
            AgentContract.RequestContext context) {
        PromoteArguments arguments = requirePromote(body.arguments());
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            if (!state.canaryPassed()) {
                throw new AgentContractException(412, "PRECONDITION_FAILED", "候选修订尚未通过灰度门");
            }
            state.setActiveRevision(arguments.targetRevision());
            state.setTargetRevision(arguments.targetRevision());
            state.setTrafficPercent(arguments.trafficPercent());
            state.setPromoted(arguments.trafficPercent() == 100);
            state.setContractHealthy(true);
            return Map.of("targetRevision", arguments.targetRevision(), "trafficPercent", arguments.trafficPercent(),
                    "environment", arguments.environment(), "status", "SUCCEEDED");
        });
    }

    /**
     * 返回训练、评估、登记、灰度、流量和回滚就绪状态。
     */
    public Map<String, Object> releaseValidation(
            AgentContract.RequestContext context,
            ReleaseValidationArguments arguments) {
        requireRelease(arguments);
        LifecycleState state = state(context, arguments.deploymentUid());
        boolean passed = state.trainingCompleted() && state.evaluationPassed() && state.registered()
                && state.canaryPassed() && state.promoted()
                && state.activeRevision() == arguments.targetRevision();
        return Map.of(
                "deploymentUid", arguments.deploymentUid(),
                "targetRevision", arguments.targetRevision(),
                "passed", passed,
                "status", passed ? "PASSED" : "FAILED",
                "trainingCompleted", state.trainingCompleted(),
                "evaluationPassed", state.evaluationPassed(),
                "modelRegistered", state.registered(),
                "canaryPassed", state.canaryPassed(),
                "trafficPercent", state.trafficPercent(),
                "rollbackReady", true);
    }

    /**
     * 对比赛部署执行预先审批的补偿回滚。
     */
    public AgentContract.ToolResponse<Map<String, Object>> rollback(
            AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> body,
            AgentContract.RequestContext context) {
        MepAgentDtos.RollbackArguments arguments = body.arguments();
        requireDeployment(arguments == null ? null : arguments.deploymentUid());
        if (arguments.targetRevision() == null || arguments.targetRevision() < 1) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "targetRevision 无效");
        }
        return executeWrite(body, context, () -> {
            LifecycleState state = state(context, arguments.deploymentUid());
            state.setActiveRevision(arguments.targetRevision());
            state.setTrafficPercent(100);
            state.setPromoted(false);
            state.setContractHealthy(true);
            return Map.of("deploymentUid", arguments.deploymentUid(),
                    "targetRevision", arguments.targetRevision(), "stage", "ROLLED_BACK");
        });
    }

    /**
     * 执行一个受审批模型写步骤并生成不可歧义平台回执。
     */
    private AgentContract.ToolResponse<Map<String, Object>> executeWrite(
            AgentContract.ToolRequest<?> body,
            AgentContract.RequestContext context,
            Mutation mutation) {
        long startedNanos = System.nanoTime();
        GovernedApprovalVerifier.ApprovalDecision decision = approvalVerifier.verify(body, context);
        GovernedResourceVersionTracker.Execution execution = versionTracker.execute(
                context, body, mutation::apply);
        Map<String, Object> domain = execution.data();
        long beforeVersion = execution.beforeVersion();
        long afterVersion = execution.afterVersion();
        if (!Boolean.TRUE.equals(body.dryRun()) && !execution.replayed()) {
            state(context, deploymentUid(body.arguments())).setResourceVersion(afterVersion);
        }
        String idempotencyKey = context.workspaceId() + ":" + context.incidentId()
                + ":" + context.idempotencyKey();
        String actionId = "mlops-" + UUID.nameUUIDFromBytes(
                idempotencyKey.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> data = new LinkedHashMap<>(domain);
        data.put("actionId", actionId);
        data.put("status", Boolean.TRUE.equals(body.dryRun()) ? "DRY_RUN" : "SUCCEEDED");
        data.put("stepId", body.stepId());
        data.put("planDigest", body.planDigest());
        AgentContract.AuditReceipt receipt = new AgentContract.AuditReceipt(
                "receipt-" + actionId, context.requestId(), context.workspaceId(), context.incidentId(),
                context.traceId(), context.toolName(), context.actorId(), decision.approverId(),
                body.approvalId(), decision.argumentsDigest(), actionId, String.valueOf(data.get("status")),
                String.valueOf(beforeVersion), String.valueOf(afterVersion), Instant.now(), Instant.now(), List.of());
        return AgentContract.successWithReceipt(
                Map.copyOf(data), context, "XnetMLOps/model-lifecycle", String.valueOf(afterVersion),
                startedNanos, receipt);
    }

    /**
     * 从已知强类型参数读取部署标识。
     */
    private String deploymentUid(Object arguments) {
        if (arguments instanceof FeaturePipelineArguments value) return value.deploymentUid();
        if (arguments instanceof TrainingSearchArguments value) return value.deploymentUid();
        if (arguments instanceof ModelRegisterArguments value) return value.deploymentUid();
        if (arguments instanceof FallbackApplyArguments value) return value.deploymentUid();
        if (arguments instanceof FallbackRemoveArguments value) return value.deploymentUid();
        if (arguments instanceof CanaryArguments value) return value.deploymentUid();
        if (arguments instanceof PromoteArguments value) return value.deploymentUid();
        if (arguments instanceof MepAgentDtos.RollbackArguments value) return value.deploymentUid();
        return "unknown";
    }

    /**
     * 读取或创建按 Workspace、Incident 隔离的生命周期状态。
     */
    private LifecycleState state(AgentContract.RequestContext context, String deploymentUid) {
        String key = context.workspaceId() + ":" + context.incidentId();
        return incidentStates.computeIfAbsent(key, ignored -> new LifecycleState(deploymentUid));
    }

    /** 校验部署标识属于比赛沙盘。 */
    private void requireDeployment(String deploymentUid) {
        requireText(deploymentUid, "deploymentUid");
        if (!supports(deploymentUid)) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "比赛部署不存在");
        }
    }

    /** 校验必填有界文本。 */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 512 || value.indexOf('\0') >= 0) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", field + " 无效");
        }
    }

    /** 校验特征流水线参数。 */
    private FeaturePipelineArguments requireFeaturePipeline(FeaturePipelineArguments value) {
        if (value == null || value.addedFeatures() == null || value.removedFeatures() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "特征流水线参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.datasetUid(), "datasetUid");
        requireText(value.pipelineUid(), "pipelineUid");
        return value;
    }

    /** 校验训练搜索参数。 */
    private TrainingSearchArguments requireTraining(TrainingSearchArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1
                || value.trialCount() == null || value.trialCount() < 1 || value.trialCount() > 100
                || value.architectures() == null || value.architectures().isEmpty()) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "训练搜索参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.datasetUid(), "datasetUid");
        requireText(value.experimentUid(), "experimentUid");
        requireText(value.mode(), "mode");
        return value;
    }

    /** 校验候选评估参数。 */
    private void requireEvaluation(EvaluationArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1
                || value.minimumSharpeImprovement() == null || value.maximumDrawdownIncrease() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "模型评估参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.experimentUid(), "experimentUid");
        requireText(value.testDatasetRef(), "testDatasetRef");
    }

    /** 校验模型登记参数。 */
    private ModelRegisterArguments requireRegister(ModelRegisterArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "模型登记参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.experimentUid(), "experimentUid");
        requireText(value.modelCardUid(), "modelCardUid");
        return value;
    }

    /** 校验备用特征启用参数。 */
    private FallbackApplyArguments requireFallbackApply(FallbackApplyArguments value) {
        if (value == null) throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        requireDeployment(value.deploymentUid());
        requireText(value.featureSetUid(), "featureSetUid");
        requireText(value.reasonCode(), "reasonCode");
        return value;
    }

    /** 校验备用特征退出参数。 */
    private FallbackRemoveArguments requireFallbackRemove(FallbackRemoveArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "备用特征退出参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.featureSetUid(), "featureSetUid");
        return value;
    }

    /** 校验灰度发布参数。 */
    private CanaryArguments requireCanary(CanaryArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1
                || value.trafficPercent() == null || value.trafficPercent() < 1 || value.trafficPercent() > 50
                || value.observationMinutes() == null || value.observationMinutes() < 1) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "灰度发布参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.environment(), "environment");
        return value;
    }

    /** 校验全量提升参数。 */
    private PromoteArguments requirePromote(PromoteArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1
                || value.trafficPercent() == null || value.trafficPercent() < 1 || value.trafficPercent() > 100) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "流量提升参数无效");
        }
        requireDeployment(value.deploymentUid());
        requireText(value.environment(), "environment");
        return value;
    }

    /** 校验发布验证参数。 */
    private void requireRelease(ReleaseValidationArguments value) {
        if (value == null || value.targetRevision() == null || value.targetRevision() < 1) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "发布验证参数无效");
        }
        requireDeployment(value.deploymentUid());
    }

    /** 表示审批通过后的状态修改函数。 */
    @FunctionalInterface
    private interface Mutation { Map<String, Object> apply(); }

    /** 保存单个事件的模型生命周期状态。 */
    private static final class LifecycleState {
        private long activeRevision;
        private long targetRevision;
        private long resourceVersion = 42;
        private int trafficPercent = 100;
        private boolean featurePipelinePublished;
        private boolean trainingCompleted;
        private boolean evaluationPassed;
        private boolean registered;
        private boolean canaryPassed;
        private boolean promoted;
        private boolean fallbackFeatureActive;
        private boolean contractHealthy;

        /** 根据部署标识创建场景基线。 */
        private LifecycleState(String deploymentUid) {
            activeRevision = deploymentUid != null && deploymentUid.contains("recommendation") ? 6 : 18;
            targetRevision = activeRevision;
            contractHealthy = deploymentUid == null || !deploymentUid.contains("risk");
        }
        /** 返回当前修订。 */ private long activeRevision() { return activeRevision; }
        /** 更新当前修订。 */ private void setActiveRevision(long value) { activeRevision = value; }
        /** 返回目标修订。 */ private long targetRevision() { return targetRevision; }
        /** 更新目标修订。 */ private void setTargetRevision(long value) { targetRevision = value; }
        /** 返回资源版本。 */ private long resourceVersion() { return resourceVersion; }
        /** 更新资源版本。 */ private void setResourceVersion(long value) { resourceVersion = value; }
        /** 返回流量比例。 */ private int trafficPercent() { return trafficPercent; }
        /** 更新流量比例。 */ private void setTrafficPercent(int value) { trafficPercent = value; }
        /** 更新特征流水线状态。 */ private void setFeaturePipelinePublished(boolean value) { featurePipelinePublished = value; }
        /** 返回训练状态。 */ private boolean trainingCompleted() { return trainingCompleted; }
        /** 更新训练状态。 */ private void setTrainingCompleted(boolean value) { trainingCompleted = value; }
        /** 返回评估状态。 */ private boolean evaluationPassed() { return evaluationPassed; }
        /** 更新评估状态。 */ private void setEvaluationPassed(boolean value) { evaluationPassed = value; }
        /** 返回登记状态。 */ private boolean registered() { return registered; }
        /** 更新登记状态。 */ private void setRegistered(boolean value) { registered = value; }
        /** 返回灰度状态。 */ private boolean canaryPassed() { return canaryPassed; }
        /** 更新灰度状态。 */ private void setCanaryPassed(boolean value) { canaryPassed = value; }
        /** 返回全量提升状态。 */ private boolean promoted() { return promoted; }
        /** 更新全量提升状态。 */ private void setPromoted(boolean value) { promoted = value; }
        /** 返回备用特征状态。 */ private boolean fallbackFeatureActive() { return fallbackFeatureActive; }
        /** 更新备用特征状态。 */ private void setFallbackFeatureActive(boolean value) { fallbackFeatureActive = value; }
        /** 返回输入契约健康状态。 */ private boolean contractHealthy() { return contractHealthy; }
        /** 更新输入契约健康状态。 */ private void setContractHealthy(boolean value) { contractHealthy = value; }
        /** 返回可审计状态摘要标签。 */ private String digestLabel() { return "lifecycle-r" + activeRevision + "-v" + resourceVersion; }
    }

    /** 模型归因参数。 */
    public record AttributionArguments(String deploymentUid, String reportUid) { }
    /** 特征流水线参数。 */
    public record FeaturePipelineArguments(String deploymentUid, String datasetUid, String pipelineUid, List<String> addedFeatures, List<String> removedFeatures) { }
    /** 训练搜索参数。 */
    public record TrainingSearchArguments(String deploymentUid, String datasetUid, String experimentUid, Long targetRevision, Integer trialCount, List<String> architectures, String mode) { }
    /** 模型评估参数。 */
    public record EvaluationArguments(String deploymentUid, String experimentUid, Long targetRevision, String testDatasetRef, Double minimumSharpeImprovement, Double maximumDrawdownIncrease) { }
    /** 模型登记参数。 */
    public record ModelRegisterArguments(String deploymentUid, String experimentUid, Long targetRevision, String modelCardUid) { }
    /** 备用特征启用参数。 */
    public record FallbackApplyArguments(String deploymentUid, String featureSetUid, String reasonCode) { }
    /** 备用特征退出参数。 */
    public record FallbackRemoveArguments(String deploymentUid, String featureSetUid, Long targetRevision) { }
    /** 灰度发布参数。 */
    public record CanaryArguments(String deploymentUid, Long targetRevision, Integer trafficPercent, String environment, Integer observationMinutes) { }
    /** 流量提升参数。 */
    public record PromoteArguments(String deploymentUid, Long targetRevision, Integer trafficPercent, String environment) { }
    /** 发布验证参数。 */
    public record ReleaseValidationArguments(String deploymentUid, Long targetRevision) { }
}
