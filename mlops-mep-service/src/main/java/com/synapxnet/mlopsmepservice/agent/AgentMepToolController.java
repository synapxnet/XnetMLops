package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露 MEP 部署证据、推理探针、受控回滚和动作查询接口。
 */
@RestController
public class AgentMepToolController {

    private final DeploymentEvidenceService deploymentEvidenceService;
    private final InferenceProbeService probeService;
    private final DeploymentActionService actionService;

    /**
     * 创建 MEP Agent 工具 Controller。
     *
     * @param deploymentEvidenceService 部署证据服务
     * @param probeService 推理探针服务
     * @param actionService 受控部署动作服务
     */
    public AgentMepToolController(
            DeploymentEvidenceService deploymentEvidenceService,
            InferenceProbeService probeService,
            DeploymentActionService actionService) {
        this.deploymentEvidenceService = deploymentEvidenceService;
        this.probeService = probeService;
        this.actionService = actionService;
    }

    /**
     * 获取部署、模型契约、修订和 Runtime 实际状态证据。
     */
    @PostMapping("/api/agent/v1/tools/mlops.deployment.get:invoke")
    public AgentContract.ToolResponse<MepAgentDtos.DeploymentEvidence> deployment(
            @RequestBody AgentContract.ToolRequest<MepAgentDtos.DeploymentGetArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "mlops.deployment.get";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        if (body.arguments() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        MepAgentDtos.DeploymentEvidence evidence = deploymentEvidenceService.get(
                body.arguments().deploymentUid(), Boolean.TRUE.equals(body.arguments().includeRevisions()));
        return AgentContract.success(
                evidence, context, "XnetMLops/mep", evidence.resourceVersion(), startedNanos);
    }

    /**
     * 对授权 Fixture 执行契约检查和真实模型端点探针。
     */
    @PostMapping("/api/agent/v1/tools/mlops.inference.probe:invoke")
    public AgentContract.ToolResponse<MepAgentDtos.InferenceProbeResult> probe(
            @RequestBody AgentContract.ToolRequest<MepAgentDtos.InferenceProbeArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "mlops.inference.probe";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        MepAgentDtos.InferenceProbeResult result = probeService.probe(body.arguments(), context);
        return AgentContract.success(result, context, "XnetMLops/mep-probe", result.resultDigest(), startedNanos);
    }

    /**
     * 在审批、幂等和资源版本校验通过后受理高风险回滚。
     */
    @PostMapping("/api/agent/v1/tools/mlops.deployment.rollback:invoke")
    public AgentContract.ToolResponse<MepAgentDtos.RollbackAcceptance> rollback(
            @RequestBody AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "mlops.deployment.rollback";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        DeploymentActionService.RollbackOutcome outcome = actionService.submit(body, context);
        return AgentContract.successWithReceipt(
                outcome.acceptance(), context, "XnetMLops/mep-action", body.expectedResourceVersion(),
                startedNanos, outcome.auditReceipt());
    }

    /**
     * 按 Workspace 查询持久化动作状态，供前端刷新和 MCP Resource 使用。
     *
     * @param actionId 动作 UID
     * @param servletRequest 当前 HTTP 请求
     * @return 动作状态视图
     */
    @GetMapping("/api/agent/v1/actions/{actionId}")
    public MepAgentDtos.DeploymentActionView action(
            @PathVariable String actionId,
            HttpServletRequest servletRequest) {
        Object value = servletRequest.getAttribute(AgentContract.CONTEXT_ATTRIBUTE);
        if (!(value instanceof AgentContract.RequestContext context)) {
            throw new AgentContractException(401, "UNAUTHENTICATED", "缺少已验证的 Agent 请求上下文");
        }
        return actionService.getAction(actionId, context.workspaceId());
    }
}
