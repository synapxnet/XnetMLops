package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import com.synapxnet.mlopsmepservice.agent.provider.DeploymentRuntimeProvider;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 集中管理回滚审批、幂等、资源版本、持久化状态机、Runtime 副作用和独立探针验证。
 */
@Service
public class DeploymentActionService {

    private static final String ACTION_TYPE = "ROLLBACK";
    private static final String TOOL_NAME = "mlops.deployment.rollback";
    private static final String VERIFY_DATASET = "fixture://goai/risk-120-v1";
    private final AgentMepMapper agentMapper;
    private final DeploymentEvidenceService deploymentEvidenceService;
    private final InferenceProbeService probeService;
    private final ApprovalVerifier approvalVerifier;
    private final List<DeploymentRuntimeProvider> runtimeProviders;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper canonicalMapper;

    /**
     * 创建部署动作服务。
     *
     * @param agentMapper 动作、修订和乐观锁 Mapper
     * @param deploymentEvidenceService 部署读取服务
     * @param probeService 独立推理探针服务
     * @param approvalVerifier 审批内省端口
     * @param runtimeProviders 真实 Runtime Provider 列表
     * @param taskExecutor 有界持久动作执行器
     * @param objectMapper 应用 Jackson 配置
     */
    public DeploymentActionService(
            AgentMepMapper agentMapper,
            DeploymentEvidenceService deploymentEvidenceService,
            InferenceProbeService probeService,
            ApprovalVerifier approvalVerifier,
            List<DeploymentRuntimeProvider> runtimeProviders,
            @Qualifier("deploymentActionExecutor") TaskExecutor taskExecutor,
            ObjectMapper objectMapper) {
        this.agentMapper = agentMapper;
        this.deploymentEvidenceService = deploymentEvidenceService;
        this.probeService = probeService;
        this.approvalVerifier = approvalVerifier;
        this.runtimeProviders = List.copyOf(runtimeProviders);
        this.taskExecutor = taskExecutor;
        this.canonicalMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * 验证治理字段并幂等受理回滚；实际副作用在持久化动作创建后异步执行。
     *
     * @param request 公共工具请求
     * @param context 已验证上下文
     * @return 受理结果和审计回执
     */
    public RollbackOutcome submit(
            AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> request,
            AgentContract.RequestContext context) {
        MepAgentDtos.RollbackArguments arguments = validateRequest(request);
        ModelDeployment deployment = deploymentEvidenceService.requireDeployment(arguments.deploymentUid());
        String actualVersion = String.valueOf(value(deployment.getResourceVersion()));
        if (!actualVersion.equals(request.expectedResourceVersion())) {
            throw new AgentContractException(
                    409, "RESOURCE_VERSION_CONFLICT", "部署资源版本已变化，请重新取证和审批", false,
                    Map.of("expectedResourceVersion", request.expectedResourceVersion(), "actualResourceVersion", actualVersion));
        }
        DeploymentRevision target = agentMapper.findRevision(arguments.deploymentUid(), arguments.targetRevision());
        if (target == null) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "目标部署修订不存在");
        }
        DeploymentRuntimeProvider provider = requireProvider(deployment);
        String digest = requestDigest(request);
        ApprovalVerifier.ApprovalDecision approval = approvalVerifier.verify(
                request.approvalId(), arguments, digest, request.expectedResourceVersion(), context);
        DeploymentAction existing = agentMapper.findActionByIdempotency(
                context.workspaceId(), ACTION_TYPE, context.idempotencyKey());
        if (existing != null) {
            if (!digest.equals(existing.getRequestDigest())) {
                throw new AgentContractException(409, "IDEMPOTENCY_CONFLICT", "相同幂等键对应不同回滚参数");
            }
            return outcome(existing, approval.approverId());
        }
        provider.validateRevision(deployment, target);
        if (Boolean.TRUE.equals(request.dryRun())) {
            return dryRunOutcome(arguments, request.expectedResourceVersion());
        }
        DeploymentAction action = createAction(request, context, deployment, arguments, digest);
        DeploymentAction persisted = insertIdempotently(action);
        if (persisted != action) {
            return outcome(persisted, approval.approverId());
        }
        try {
            taskExecutor.execute(() -> execute(action.getUid()));
        } catch (RuntimeException exception) {
            transition(action, "FAILED", "PRECHECK", "RATE_LIMITED", "部署动作队列已满", true);
            throw new AgentContractException(429, "RATE_LIMITED", "部署动作队列已满，请稍后重试", true, Map.of());
        }
        return outcome(agentMapper.findActionByUid(action.getUid()), approval.approverId());
    }

    /**
     * 按 Workspace 读取动作状态，页面刷新后可恢复真实进度。
     *
     * @param actionId 动作 UID
     * @param workspaceId 当前 Workspace
     * @return 动作状态视图
     */
    public MepAgentDtos.DeploymentActionView getAction(String actionId, String workspaceId) {
        DeploymentAction action = agentMapper.findActionByUid(actionId);
        if (action == null || !action.getWorkspaceId().equals(workspaceId)) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "部署动作不存在");
        }
        return toView(action);
    }

    /**
     * 应用启动时将上次进程遗留的 RUNNING 动作明确标记失败，避免永久卡住。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedActions() {
        agentMapper.failInterruptedActions();
    }

    /**
     * 执行持久化动作：切换 Runtime、等待就绪、独立探针、乐观锁提交或补偿。
     *
     * @param actionId 动作 UID
     */
    private void execute(String actionId) {
        DeploymentAction action = agentMapper.findActionByUid(actionId);
        ModelDeployment deployment = deploymentEvidenceService.requireDeployment(action.getDeploymentUid());
        DeploymentRevision target = agentMapper.findRevision(action.getDeploymentUid(), action.getTargetRevision());
        DeploymentRevision previous = agentMapper.findRevision(action.getDeploymentUid(), action.getFromRevision());
        DeploymentRuntimeProvider provider = requireProvider(deployment);
        boolean applied = false;
        try {
            transition(action, "RUNNING", "APPLYING", null, null, false);
            provider.applyRevision(deployment, target, action.getUid());
            applied = true;
            transition(action, "RUNNING", "WAITING_READY", null, null, false);
            DeploymentRuntimeProvider.RuntimeInspection ready =
                    provider.waitUntilReady(deployment, Duration.ofSeconds(90));
            if (!ready.ready()) {
                throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订在时限内未就绪");
            }
            transition(action, "RUNNING", "VERIFYING", null, null, false);
            MepAgentDtos.VerificationPolicy policy = readPolicy(action.getVerificationPolicyJson());
            AgentContract.RequestContext probeContext = new AgentContract.RequestContext(
                    action.getWorkspaceId(), action.getIncidentId(), action.getTraceId(),
                    "mlops.inference.probe", "probe-" + action.getIdempotencyKey(), action.getCreatedBy(),
                    action.getRequestId() + "-verify");
            MepAgentDtos.InferenceProbeResult probe = probeService.probe(
                    new MepAgentDtos.InferenceProbeArguments(
                            action.getDeploymentUid(), VERIFY_DATASET, 12, 60_000), probeContext);
            requireProbeThreshold(probe, policy);
            transition(action, "RUNNING", "FINALIZING", null, null, false);
            int updated = agentMapper.activateRevision(
                    action.getDeploymentUid(), Long.parseLong(action.getExpectedResourceVersion()), target);
            if (updated != 1) {
                throw new AgentContractException(409, "RESOURCE_VERSION_CONFLICT", "最终提交时资源版本发生变化");
            }
            transition(action, "SUCCEEDED", "FINALIZING", null, null, true);
        } catch (AgentContractException exception) {
            compensate(provider, deployment, previous, action, applied);
            transition(action, "FAILED", "COMPENSATING", exception.getCode(), exception.getMessage(), true);
        } catch (RuntimeException exception) {
            compensate(provider, deployment, previous, action, applied);
            transition(action, "FAILED", "COMPENSATING", "INTERNAL_ERROR", "回滚执行失败", true);
        }
    }

    /**
     * 验证失败时尝试恢复旧修订；恢复错误只记录为结构化补偿失败。
     */
    private void compensate(
            DeploymentRuntimeProvider provider,
            ModelDeployment deployment,
            DeploymentRevision previous,
            DeploymentAction action,
            boolean applied) {
        if (!applied || previous == null) {
            return;
        }
        try {
            provider.restore(deployment, previous, action.getUid());
        } catch (RuntimeException exception) {
            action.setErrorCode("COMPENSATION_FAILED");
            action.setErrorMessage("目标修订失败且旧修订恢复失败，需要人工处理");
        }
    }

    /**
     * 校验探针契约、错误率和 P95 阈值。
     */
    private void requireProbeThreshold(
            MepAgentDtos.InferenceProbeResult probe,
            MepAgentDtos.VerificationPolicy policy) {
        boolean passed = probe.contractStatus() == MepAgentDtos.ContractStatus.MATCHED
                && probe.errorRate().compareTo(policy.maxErrorRate()) <= 0
                && probe.p95Ms() != null && probe.p95Ms().compareTo(policy.maxP95Ms()) <= 0;
        if (!passed) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订探针未达到审批阈值");
        }
    }

    /**
     * 校验公共治理字段和回滚领域参数。
     */
    private MepAgentDtos.RollbackArguments validateRequest(
            AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> request) {
        MepAgentDtos.RollbackArguments arguments = request.arguments();
        boolean invalid = arguments == null || arguments.deploymentUid() == null || arguments.deploymentUid().isBlank()
                || arguments.targetRevision() == null || arguments.targetRevision() < 1
                || arguments.verificationPolicy() == null
                || request.approvalId() == null || request.approvalId().isBlank()
                || request.expectedResourceVersion() == null || request.expectedResourceVersion().isBlank()
                || request.reason() == null || request.reason().length() < 4 || request.reason().length() > 1000
                || request.dryRun() == null;
        if (invalid) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "回滚参数和四个治理字段均必须有效");
        }
        MepAgentDtos.VerificationPolicy policy = arguments.verificationPolicy();
        if (policy.maxErrorRate() == null || policy.maxP95Ms() == null
                || policy.maxErrorRate().compareTo(BigDecimal.ZERO) < 0
                || policy.maxErrorRate().compareTo(BigDecimal.ONE) > 0
                || policy.maxP95Ms().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "verificationPolicy 阈值无效");
        }
        return arguments;
    }

    /** 选择支持当前部署的真实 Runtime Provider。 */
    private DeploymentRuntimeProvider requireProvider(ModelDeployment deployment) {
        return runtimeProviders.stream().filter(provider -> provider.supports(deployment)).findFirst()
                .orElseThrow(() -> new AgentContractException(
                        412, "PRECONDITION_FAILED", "当前部署模式没有可用 Runtime Provider"));
    }

    /**
     * 生成排序 JSON 的 SHA-256 请求摘要，确保幂等键冲突可检测。
     */
    private String requestDigest(AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> request) {
        try {
            byte[] json = canonicalMapper.writeValueAsBytes(Map.of(
                    "arguments", request.arguments(),
                    "approvalId", request.approvalId(),
                    "expectedResourceVersion", request.expectedResourceVersion(),
                    "reason", request.reason(),
                    "dryRun", request.dryRun()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (Exception exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "无法生成回滚请求摘要");
        }
    }

    /** 根据受理请求创建 PENDING 持久化动作。 */
    private DeploymentAction createAction(
            AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> request,
            AgentContract.RequestContext context,
            ModelDeployment deployment,
            MepAgentDtos.RollbackArguments arguments,
            String digest) {
        DeploymentAction action = new DeploymentAction();
        action.setUid("act_" + UUID.randomUUID().toString().replace("-", "").substring(0, 26));
        action.setDeploymentUid(arguments.deploymentUid());
        action.setActionType(ACTION_TYPE);
        action.setFromRevision(deployment.getActiveRevision());
        action.setTargetRevision(arguments.targetRevision());
        action.setStatus("PENDING");
        action.setStage("PRECHECK");
        action.setRequestId(request.requestId());
        action.setWorkspaceId(context.workspaceId());
        action.setIncidentId(context.incidentId());
        action.setTraceId(context.traceId());
        action.setApprovalId(request.approvalId());
        action.setIdempotencyKey(context.idempotencyKey());
        action.setRequestDigest(digest);
        action.setExpectedResourceVersion(request.expectedResourceVersion());
        action.setVerificationPolicyJson(writePolicy(arguments.verificationPolicy()));
        action.setDryRun(request.dryRun());
        action.setCreatedBy(context.actorId());
        return action;
    }

    /** 在唯一键竞争时重新读取并验证摘要。 */
    private DeploymentAction insertIdempotently(DeploymentAction action) {
        try {
            agentMapper.insertAction(action);
            return action;
        } catch (DataIntegrityViolationException exception) {
            DeploymentAction existing = agentMapper.findActionByIdempotency(
                    action.getWorkspaceId(), ACTION_TYPE, action.getIdempotencyKey());
            if (existing == null || !existing.getRequestDigest().equals(action.getRequestDigest())) {
                throw new AgentContractException(409, "IDEMPOTENCY_CONFLICT", "幂等键并发冲突");
            }
            return existing;
        }
    }

    /**
     * 构造无数据库、Runtime 或审计副作用的预检计划。
     *
     * @param arguments 已校验回滚参数
     * @param resourceVersion 当前已审批资源版本
     * @return 不带 action Resource 和 Audit Receipt 的 dry-run 结果
     */
    private RollbackOutcome dryRunOutcome(
            MepAgentDtos.RollbackArguments arguments,
            String resourceVersion) {
        MepAgentDtos.RollbackAcceptance acceptance = new MepAgentDtos.RollbackAcceptance(
                null, "DRY_RUN", "PRECHECK", true, null,
                List.of(
                        "验证目标修订镜像与部署规格",
                        "切换 Runtime 到修订 " + arguments.targetRevision(),
                        "等待 readiness 并执行独立推理探针",
                        "满足阈值后以资源版本 " + resourceVersion + " 提交"));
        return new RollbackOutcome(acceptance, null);
    }

    /** 更新持久化动作状态机。 */
    private void transition(
            DeploymentAction action,
            String status,
            String stage,
            String errorCode,
            String errorMessage,
            boolean terminal) {
        action.setStatus(status);
        action.setStage(stage);
        action.setErrorCode(errorCode);
        action.setErrorMessage(errorMessage);
        if (action.getStartedAt() == null && !"PENDING".equals(status)) {
            action.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        action.setCompletedAt(terminal ? LocalDateTime.now(ZoneOffset.UTC) : null);
        agentMapper.updateAction(action);
    }

    /** 将受理动作转换为响应和公共审计回执。 */
    private RollbackOutcome outcome(DeploymentAction action, String approverId) {
        MepAgentDtos.RollbackAcceptance acceptance = new MepAgentDtos.RollbackAcceptance(
                action.getUid(), action.getStatus(), action.getStage(), Boolean.TRUE.equals(action.getDryRun()),
                "openxnet://workspaces/" + action.getWorkspaceId() + "/actions/" + action.getUid(),
                List.of("PRECHECK", "APPLYING", "WAITING_READY", "VERIFYING", "FINALIZING"));
        AgentContract.AuditReceipt receipt = new AgentContract.AuditReceipt(
                "receipt_" + action.getUid().substring(4), action.getRequestId(), action.getWorkspaceId(),
                action.getIncidentId(), action.getTraceId(), TOOL_NAME, action.getCreatedBy(), approverId,
                action.getApprovalId(), action.getRequestDigest(), action.getUid(), action.getStatus(),
                action.getExpectedResourceVersion(), null, toInstant(action.getStartedAt()),
                toInstant(action.getCompletedAt()), List.of());
        return new RollbackOutcome(acceptance, receipt);
    }

    /** 将动作 Entity 转换为可轮询状态视图。 */
    private MepAgentDtos.DeploymentActionView toView(DeploymentAction action) {
        return new MepAgentDtos.DeploymentActionView(
                action.getUid(), action.getDeploymentUid(), value(action.getFromRevision()),
                value(action.getTargetRevision()), action.getStatus(), action.getStage(),
                Boolean.TRUE.equals(action.getDryRun()), action.getErrorCode(), action.getErrorMessage(),
                toInstant(action.getStartedAt()), toInstant(action.getCompletedAt()));
    }

    /** 序列化验证阈值。 */
    private String writePolicy(MepAgentDtos.VerificationPolicy policy) {
        try {
            return canonicalMapper.writeValueAsString(policy);
        } catch (JsonProcessingException exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "无法序列化验证阈值");
        }
    }

    /** 反序列化持久化验证阈值。 */
    private MepAgentDtos.VerificationPolicy readPolicy(String value) {
        try {
            return canonicalMapper.readValue(value, MepAgentDtos.VerificationPolicy.class);
        } catch (JsonProcessingException exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "持久化验证阈值无效");
        }
    }

    /** 将可能为空的 Long 转换为稳定 0。 */
    private long value(Long value) {
        return value == null ? 0L : value;
    }

    /** 将数据库 UTC 本地时间转换为 RFC 3339。 */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /** 表示回滚受理结果和审计回执。 */
    public record RollbackOutcome(
            MepAgentDtos.RollbackAcceptance acceptance,
            AgentContract.AuditReceipt auditReceipt) {
    }
}
