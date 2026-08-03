package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import com.synapxnet.mlopsmepservice.agent.provider.DeploymentRuntimeProvider;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证回滚的审批、资源版本、dry-run、幂等和队列副作用边界。
 */
class DeploymentActionServiceTest {

    private final AgentMepMapper mapper = mock(AgentMepMapper.class);
    private final DeploymentEvidenceService evidenceService = mock(DeploymentEvidenceService.class);
    private final InferenceProbeService probeService = mock(InferenceProbeService.class);
    private final ApprovalVerifier approvalVerifier = mock(ApprovalVerifier.class);
    private final DeploymentRuntimeProvider provider = mock(DeploymentRuntimeProvider.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);
    private DeploymentActionService service;
    private ModelDeployment deployment;
    private DeploymentRevision revision;

    /** 创建资源版本 42、当前修订 18 和目标修订 17 的稳定上下文。 */
    @BeforeEach
    void setUp() {
        deployment = new ModelDeployment();
        deployment.setUid("deploy_risk_prod");
        deployment.setActiveRevision(18L);
        deployment.setResourceVersion(42L);
        revision = new DeploymentRevision();
        revision.setDeploymentUid(deployment.getUid());
        revision.setRevisionNumber(17L);
        when(evidenceService.requireDeployment(deployment.getUid())).thenReturn(deployment);
        when(mapper.findRevision(deployment.getUid(), 17L)).thenReturn(revision);
        when(provider.supports(deployment)).thenReturn(true);
        when(approvalVerifier.verify(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(new ApprovalVerifier.ApprovalDecision(
                        "apr_goai_demo_approved", "approver", "operator"));
        service = new DeploymentActionService(
                mapper, evidenceService, probeService, approvalVerifier,
                List.of(provider), executor, new ObjectMapper());
    }

    /** dry-run 只返回执行计划，不创建动作、回执或 Runtime 副作用。 */
    @Test
    void dryRunHasNoPersistentOrRuntimeSideEffect() {
        DeploymentActionService.RollbackOutcome outcome = service.submit(request(true), context());

        assertNull(outcome.acceptance().actionId());
        assertEquals("DRY_RUN", outcome.acceptance().status());
        assertNull(outcome.auditReceipt());
        verify(provider).validateRevision(deployment, revision);
        verify(mapper, never()).insertAction(any());
        verify(mapper, never()).updateAction(any());
        verify(executor, never()).execute(any());
        verify(provider, never()).applyRevision(any(), any(), anyString());
    }

    /** 顺序重复调用必须返回同一动作且只排队一次。 */
    @Test
    void repeatedIdempotencyKeyQueuesOnlyOnce() {
        AtomicReference<DeploymentAction> saved = new AtomicReference<>();
        when(mapper.findActionByIdempotency("ws_goai_demo", "ROLLBACK", "idem_rollback_001"))
                .thenAnswer(invocation -> saved.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).insertAction(any());
        when(mapper.findActionByUid(anyString())).thenAnswer(invocation -> saved.get());

        DeploymentActionService.RollbackOutcome first = service.submit(request(false), context());
        DeploymentActionService.RollbackOutcome second = service.submit(request(false), context());

        assertEquals(first.acceptance().actionId(), second.acceptance().actionId());
        verify(mapper, times(1)).insertAction(any());
        verify(executor, times(1)).execute(any());
    }

    /** 相同幂等键对应不同摘要必须在排队前返回冲突。 */
    @Test
    void rejectsIdempotencyDigestConflict() {
        DeploymentAction existing = new DeploymentAction();
        existing.setRequestDigest("different-digest");
        when(mapper.findActionByIdempotency("ws_goai_demo", "ROLLBACK", "idem_rollback_001"))
                .thenReturn(existing);

        AgentContractException error = assertThrows(
                AgentContractException.class,
                () -> service.submit(request(false), context()));

        assertEquals("IDEMPOTENCY_CONFLICT", error.getCode());
        verify(executor, never()).execute(any());
    }

    /** 资源版本变化必须在审批和 Runtime 校验前拒绝。 */
    @Test
    void rejectsResourceVersionConflictWithoutSideEffect() {
        deployment.setResourceVersion(43L);

        AgentContractException error = assertThrows(
                AgentContractException.class,
                () -> service.submit(request(false), context()));

        assertEquals("RESOURCE_VERSION_CONFLICT", error.getCode());
        verify(approvalVerifier, never()).verify(anyString(), any(), anyString(), anyString(), any());
        verify(provider, never()).validateRevision(any(), any());
        verify(executor, never()).execute(any());
    }

    /** 实际回滚必须使用目标修订 17 的契约执行探针，而不是仍活动的修订 18。 */
    @Test
    void executionProbesAgainstTargetRevisionContract() {
        AtomicReference<DeploymentAction> saved = new AtomicReference<>();
        ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
        DeploymentRevision previous = new DeploymentRevision();
        previous.setDeploymentUid(deployment.getUid());
        previous.setRevisionNumber(18L);
        MepAgentDtos.InferenceProbeResult probe = mock(MepAgentDtos.InferenceProbeResult.class);
        when(mapper.findActionByIdempotency("ws_goai_demo", "ROLLBACK", "idem_rollback_001"))
                .thenAnswer(invocation -> saved.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).insertAction(any());
        when(mapper.findActionByUid(anyString())).thenAnswer(invocation -> saved.get());
        when(mapper.findRevision(deployment.getUid(), 18L)).thenReturn(previous);
        when(provider.waitUntilReady(any(), any())).thenReturn(
                new DeploymentRuntimeProvider.RuntimeInspection(true, true, "healthy", "v17"));
        when(probeService.probeAgainstRevision(any(), any(), same(revision))).thenReturn(probe);
        when(probe.contractStatus()).thenReturn(MepAgentDtos.ContractStatus.MATCHED);
        when(probe.errorRate()).thenReturn(BigDecimal.ZERO);
        when(probe.p95Ms()).thenReturn(new BigDecimal("100"));
        when(mapper.activateRevision(anyString(), any(Long.class), same(revision))).thenReturn(1);

        service.submit(request(false), context());
        verify(executor).execute(queued.capture());
        queued.getValue().run();

        verify(probeService).probeAgainstRevision(any(), any(), same(revision));
        verify(provider, never()).restore(any(), any(), anyString());
    }

    /** 创建强类型公共回滚请求。 */
    private AgentContract.ToolRequest<MepAgentDtos.RollbackArguments> request(boolean dryRun) {
        return new AgentContract.ToolRequest<>(
                "req_rollback_001",
                "mlops.deployment.rollback",
                new MepAgentDtos.RollbackArguments(
                        deployment.getUid(), 17L,
                        new MepAgentDtos.VerificationPolicy(
                                new BigDecimal("0.02"), new BigDecimal("800"))),
                "apr_goai_demo_approved",
                "42",
                "模型输入契约维度不匹配，回滚到已验证修订",
                dryRun);
    }

    /** 创建已验证的 Workspace、Incident、Trace 与幂等上下文。 */
    private AgentContract.RequestContext context() {
        return new AgentContract.RequestContext(
                "ws_goai_demo", "inc_model_contract_001", "trace_model_contract_001",
                "mlops.deployment.rollback", "idem_rollback_001", "operator", "req_rollback_001");
    }
}
