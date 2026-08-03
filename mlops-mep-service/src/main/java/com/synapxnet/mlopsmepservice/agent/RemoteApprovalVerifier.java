package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;

/**
 * 调用 OpenXnet Approval Introspection，并在不可用时失败关闭。
 */
@Component
@Profile("!test")
public class RemoteApprovalVerifier implements ApprovalVerifier {

    private final WebClient client;
    private final String serviceToken;

    /**
     * 创建远程审批验证器。
     *
     * @param builder WebClient Builder
     * @param baseUrl OpenXnet 控制面地址
     * @param serviceToken 环境注入的内部服务令牌
     */
    public RemoteApprovalVerifier(
            WebClient.Builder builder,
            @Value("${openxnet.approval.base-url:http://127.0.0.1:3456}") String baseUrl,
            @Value("${openxnet.approval.service-token:}") String serviceToken) {
        this.client = builder.baseUrl(baseUrl).codecs(configurer ->
                configurer.defaultCodecs().maxInMemorySize(128 * 1024)).build();
        this.serviceToken = serviceToken;
    }

    /**
     * 调用审批内省并严格匹配 Workspace、Incident、工具、资源、摘要和资源版本。
     */
    @Override
    public ApprovalDecision verify(
            String approvalId,
            MepAgentDtos.RollbackArguments arguments,
            String requestDigest,
            String expectedResourceVersion,
            AgentContract.RequestContext context) {
        if (approvalId == null || approvalId.isBlank()) {
            throw new AgentContractException(403, "APPROVAL_REQUIRED", "回滚需要有效审批");
        }
        if (serviceToken == null || serviceToken.length() < 32) {
            throw new AgentContractException(503, "UPSTREAM_UNAVAILABLE", "审批服务身份未配置");
        }
        try {
            IntrospectionResponse response = client.post()
                    .uri("/api/v1/approvals/{approvalId}/introspect", approvalId)
                    .headers(headers -> headers.setBearerAuth(serviceToken))
                    .bodyValue(new IntrospectionRequest(
                            context.workspaceId(), context.incidentId(), "mlops.deployment.rollback",
                            arguments.deploymentUid(), arguments.targetRevision(), requestDigest,
                            expectedResourceVersion, context.actorId()))
                    .retrieve().bodyToMono(IntrospectionResponse.class).block(Duration.ofSeconds(2));
            requireValid(response, arguments, requestDigest, expectedResourceVersion, context);
            return new ApprovalDecision(approvalId, response.approverId(), context.actorId());
        } catch (AgentContractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AgentContractException(
                    503, "UPSTREAM_UNAVAILABLE", "审批服务暂时不可用，回滚已拒绝", true, java.util.Map.of());
        }
    }

    /**
     * 对审批内省结果执行失败关闭验证。
     */
    private void requireValid(
            IntrospectionResponse response,
            MepAgentDtos.RollbackArguments arguments,
            String requestDigest,
            String expectedResourceVersion,
            AgentContract.RequestContext context) {
        boolean valid = response != null && response.active() && "APPROVED".equals(response.status())
                && response.expiresAt() != null && response.expiresAt().isAfter(Instant.now())
                && context.workspaceId().equals(response.workspaceId())
                && context.incidentId().equals(response.incidentId())
                && "mlops.deployment.rollback".equals(response.toolName())
                && arguments.deploymentUid().equals(response.deploymentUid())
                && arguments.targetRevision().equals(response.targetRevision())
                && requestDigest.equals(response.requestDigest())
                && expectedResourceVersion.equals(response.expectedResourceVersion())
                && response.approverId() != null
                && !response.approverId().equals(context.actorId());
        if (!valid) {
            throw new AgentContractException(403, "APPROVAL_INVALID", "审批已失效、范围不匹配或不满足职责分离");
        }
    }

    /** 表示发送给审批服务的内省上下文。 */
    private record IntrospectionRequest(
            String workspaceId,
            String incidentId,
            String toolName,
            String deploymentUid,
            Long targetRevision,
            String requestDigest,
            String expectedResourceVersion,
            String executorId) {
    }

    /** 表示审批服务的强类型内省结果。 */
    private record IntrospectionResponse(
            boolean active,
            String status,
            Instant expiresAt,
            String workspaceId,
            String incidentId,
            String toolName,
            String deploymentUid,
            Long targetRevision,
            String requestDigest,
            String expectedResourceVersion,
            String approverId) {
    }
}
