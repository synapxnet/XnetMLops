/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型版本和状态迁移回归 / Model version and state migration regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-15
 * Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 验证独立资源版本、持续领域状态和受限迁移。 / Verify independent resource versions, persistent domain state and scoped migration. */
class CompetitionModelVersionTest {
    private static final String RISK = "deploy_risk_prod";
    private static final String QUANT = "deploy_quant_ashare_research";
    private static final String FALLBACK = "mlops.feature.fallback.apply";
    @TempDir Path temporary;

    /** 完整风险计划的流量与回滚版本必须独立且跨事件保留。 / Traffic and rollback versions stay independent across a complete risk plan and new incidents. */
    @Test void trafficAndRollbackRemainIndependentAcrossIncidents() {
        var service = service();
        riskPlan(service);
        var before = service.deploymentEvidence(context("new", "read"), RISK);
        assertEquals(19L, before.get("activeRevision"));
        assertEquals("42", before.get("resourceVersion"));
        assertEquals(Map.of(RISK, "42", RISK + "/traffic", "44", RISK + "/feature-set", "44", RISK + "/revisions/19", "43", "experiment_risk_19", "43"), before.get("resourceVersions"));
        var request = body("mlops.deployment.rollback", RISK, "42", "rollback", false, new MepAgentDtos.RollbackArguments(RISK, 17L, null));
        var result = service.rollback(request, context("new", "rollback"));
        assertEquals("42", result.auditReceipt().beforeResourceVersion());
        assertEquals("43", result.auditReceipt().afterResourceVersion());
        var after = service.deploymentEvidence(context("another", "read"), RISK);
        assertEquals(17L, after.get("activeRevision"));
        assertEquals("43", after.get("resourceVersion"));
        assertEquals("44", ((Map<?, ?>) after.get("resourceVersions")).get(RISK + "/traffic"));
    }

    /** 演练不会改变当前公开版本和备用特征状态。 / Rehearsals cannot change public versions or fallback state. */
    @Test void dryRunAndReplayPreserveLiveState() {
        var service = service();
        var dry = fallbackBody("42", "dry", true);
        var response = service.applyFallback(dry, context("old", "dry"));
        assertEquals("43", response.meta().resourceVersion());
        assertEquals("42", ((Map<?, ?>) response.data().get("resourceVersions")).get(RISK + "/feature-set"));
        assertEquals(false, service.deploymentEvidence(context("new", "read"), RISK).get("fallbackFeatureActive"));
        var body = fallbackBody("42", "first", false);
        var first = service.applyFallback(body, context("old", "first"));
        var replay = service.applyFallback(body, context("old", "first"));
        assertEquals(first.data(), replay.data());
        assertEquals("43", ((Map<?, ?>) service.deploymentEvidence(context("new", "read"), RISK).get("resourceVersions")).get(RISK + "/feature-set"));
        assertThrows(AgentContractException.class, () -> service.applyFallback(fallbackBody("42", "stale", false), context("new", "stale")));
    }

    /** 同一事件查询另一部署不会串用生命周期对象。 / Different deployments in the same incident cannot share lifecycle objects. */
    @Test void separatesDeploymentsWithinOneIncident() {
        var service = service();
        service.applyFallback(fallbackBody("42", "first", false), context("old", "first"));
        var risk = service.deploymentEvidence(context("old", "read"), RISK);
        var recommendation = service.deploymentEvidence(context("old", "read"), "deploy_recommendation_prod");
        assertEquals(true, risk.get("fallbackFeatureActive"));
        assertEquals(false, recommendation.get("fallbackFeatureActive"));
        assertEquals(6L, recommendation.get("activeRevision"));
        assertEquals(Map.of("deploy_recommendation_prod", "42"), recommendation.get("resourceVersions"));
    }

    /** 固定量化产品保留真实运行时字段并给出修订2资源映射。 / Preserve quantitative runtime fields and expose revision-2 resource mappings. */
    @Test void quantitativeDeploymentPreservesRuntimeEvidence() {
        var quantitative = mock(QuantitativeRuntimeClient.class);
        when(quantitative.supports(QUANT)).thenReturn(true);
        when(quantitative.deployment()).thenReturn(Map.of("activeRevision", 1, "modelDigestSha256", "digest", "stage", "BASELINE"));
        when(quantitative.health()).thenReturn(Map.of("status", "ready", "datasetRows", 2000, "usageBoundary", "RESEARCH_ONLY"));
        var service = new CompetitionModelLifecycleService(approval(), quantitative, mock(RecommendationRuntimeClient.class));
        var result = service.deploymentEvidence(context("old", "read"), QUANT);
        assertEquals("digest", result.get("modelDigestSha256"));
        assertEquals(2000, result.get("datasetRows"));
        assertEquals(true, result.get("ready"));
        assertEquals("42", result.get("resourceVersion"));
        assertEquals(Map.of(QUANT, "42", QUANT + "/traffic", "42", QUANT + "/revisions/2", "42", "pipeline_quant_2", "42", "experiment_quant_2", "42"), result.get("resourceVersions"));
    }

    /** 错误审批目标和未知资源在审批网络调用前拒绝。 / Reject mismatched approval targets and unknown resources before approval network calls. */
    @Test void rejectsMismatchedOrUnknownResources() {
        var verifier = approval();
        var service = new CompetitionModelLifecycleService(verifier, mock(QuantitativeRuntimeClient.class), mock(RecommendationRuntimeClient.class));
        var arguments = new CompetitionModelLifecycleService.FallbackApplyArguments(RISK, "fallback", "repair");
        var wrong = body(FALLBACK, RISK + "/traffic", "42", "wrong", false, arguments);
        assertThrows(AgentContractException.class, () -> service.applyFallback(wrong, context("old", "wrong")));
        var unknownArguments = new CompetitionModelLifecycleService.TrainingSearchArguments(RISK, "dataset", "experiment_risk_999", 999L, 12, List.of("model"), "RETRAIN");
        var unknown = body("mlops.training.search.start", "experiment_risk_999", "42", "unknown", false, unknownArguments);
        assertThrows(AgentContractException.class, () -> service.startTrainingSearch(unknown, context("old", "unknown")));
        verifyNoInteractions(verifier);
    }

    /** Controller 包络使用与领域数据相同的实际主目标版本。 / Controller envelopes use the same actual primary version as domain evidence. */
    @Test void deploymentControllerUsesActualPrimaryVersion() {
        var service = service();
        var rollback = body("mlops.deployment.rollback", RISK, "42", "rollback", false, new MepAgentDtos.RollbackArguments(RISK, 17L, null));
        service.rollback(rollback, context("old", "rollback"));
        var controller = new AgentMepToolController(mock(DeploymentEvidenceService.class), mock(InferenceProbeService.class), mock(DeploymentActionService.class), service);
        var request = body("mlops.deployment.get", null, null, "read", false, new MepAgentDtos.DeploymentGetArguments(RISK, true));
        var servlet = new MockHttpServletRequest();
        servlet.setAttribute(AgentContract.CONTEXT_ATTRIBUTE, new AgentContract.RequestContext("ws", "new", "trace", request.toolName(), "read", "reader", "request"));
        var result = controller.deployment(request, servlet);
        assertEquals("43", result.meta().resourceVersion());
        assertEquals("43", ((Map<?, ?>) result.data()).get("resourceVersion"));
    }

    /** 迁移独立资源版本，忽略旧状态最后写版本作为读取源。 / Restore independent resource versions without using the legacy last-write field as the read source. */
    @Test void restoresRealResourceVersionsAndDomainState() {
        var tracker = new GovernedResourceVersionTracker();
        tracker.initializeResource("ws", RISK, 52);
        tracker.initializeResource("ws", RISK + "/traffic", 70);
        var service = service();
        service.restoreGovernedState(tracker.snapshot(), Map.of("ws:old", legacyState(19)), bindings());
        var result = service.deploymentEvidence(context("new", "read"), RISK);
        assertEquals(19L, result.get("activeRevision"));
        assertEquals("52", result.get("resourceVersion"));
        assertEquals("70", ((Map<?, ?>) result.get("resourceVersions")).get(RISK + "/traffic"));
        assertEquals(true, result.get("fallbackFeatureActive"));
        assertThrows(IllegalStateException.class, () -> service.restoreGovernedState(tracker.snapshot(), Map.of(), Map.of()));
    }

    /** 迁移后重放原治理结果，不重复修改领域。 / Replay the original governed result after migration without repeating mutations. */
    @Test void preservesIdempotencyReplayAfterMigration() {
        var tracker = new GovernedResourceVersionTracker();
        tracker.initializeResource("ws", RISK + "/feature-set", 42);
        var body = fallbackBody("42", "first", false);
        tracker.execute(context("old", "first"), body, () -> Map.of("retainedProof", "original"));
        var service = service();
        service.restoreGovernedState(tracker.snapshot(), Map.of("ws:old", legacyState(19)), bindings());
        var result = service.applyFallback(body, context("old", "first"));
        assertEquals("original", result.data().get("retainedProof"));
        assertEquals("43", result.meta().resourceVersion());
        assertEquals("42", service.deploymentEvidence(context("new", "read"), RISK).get("resourceVersion"));
    }

    /** 不完整和冲突领域绑定整体失败且不污染空目标。 / Incomplete and conflicting bindings fail atomically without contaminating the empty target. */
    @Test void rejectsIncompleteAndConflictingMigration() {
        var empty = new GovernedResourceVersionTracker().snapshot();
        var service = service();
        assertThrows(IllegalArgumentException.class, () -> service.restoreGovernedState(empty, Map.of("ws:old", legacyState(19)), Map.of()));
        var both = new LinkedHashMap<>(bindings());
        both.put("ws:other", new CompetitionModelLifecycleService.LifecycleBinding("ws", "other", RISK));
        assertThrows(IllegalArgumentException.class, () -> service.restoreGovernedState(empty, Map.of("ws:old", legacyState(19), "ws:other", legacyState(18)), both));
        service.restoreGovernedState(empty, Map.of("ws:old", legacyState(19)), bindings());
        assertEquals(19L, service.deploymentEvidence(context("new", "read"), RISK).get("activeRevision"));
    }

    /** 完整迁移文件仅能消费一次，错误摘要不能留下标记。 / Complete migration files are consumed once; a wrong digest leaves no marker. */
    @Test void migrationFileChecksDigestAndOneShotConsumption() throws Exception {
        var mapper = new ObjectMapper();
        var document = new LinkedHashMap<String, Object>();
        document.put("schema", "openxnet.governed-state-export.v1");
        document.put("platform", "mlops");
        document.put("controller", CompetitionModelLifecycleService.class.getName());
        document.put("sourceJarSha256", "8424484858047ad50271c79a2c138ee8861ec1a01d8c44513d2a3e3cfeddcca2");
        document.put("exportedAt", "2026-09-15T00:00:00Z");
        document.put("readOnly", true);
        document.put("domainField", "incidentStates");
        document.put("tracker", new GovernedResourceVersionTracker().snapshot());
        document.put("domainState", Map.of("ws:old", legacyState(19)));
        document.put("domainBindings", bindings());
        byte[] bytes = mapper.writeValueAsBytes(document);
        Path filename = temporary.resolve("migration.json");
        Files.write(filename, bytes);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThrows(IllegalStateException.class, () -> new CompetitionModelStateMigration(service(), mapper, filename.toString(), "0".repeat(64)));
        assertFalse(Files.exists(filename.resolveSibling("migration.json.consumed")));
        var restored = service();
        new CompetitionModelStateMigration(restored, mapper, filename.toString(), digest);
        assertEquals(19L, restored.deploymentEvidence(context("new", "read"), RISK).get("activeRevision"));
        assertThrows(IllegalStateException.class, () -> new CompetitionModelStateMigration(service(), mapper, filename.toString(), digest));
    }

    /** 执行完整风险候选链，包含不推进版本的评估门。 / Execute the complete risk candidate chain, including a gate that does not advance versions. */
    private void riskPlan(CompetitionModelLifecycleService service) {
        service.applyFallback(fallbackBody("42", "fallback", false), context("old", "fallback"));
        var training = body("mlops.training.search.start", "experiment_risk_19", "42", "train", false,
                new CompetitionModelLifecycleService.TrainingSearchArguments(RISK, "dataset", "experiment_risk_19", 19L, 12, List.of("model"), "RETRAIN"));
        service.startTrainingSearch(training, context("old", "train"));
        var evaluation = service.evaluate(context("old", "evaluate"), new CompetitionModelLifecycleService.EvaluationArguments(RISK, "experiment_risk_19", 19L, "staging://dataset", 0.0, 0.0));
        assertEquals("43", evaluation.get("resourceVersion"));
        service.registerModel(body("mlops.model.register", RISK + "/revisions/19", "42", "register", false,
                new CompetitionModelLifecycleService.ModelRegisterArguments(RISK, "experiment_risk_19", 19L, "card")), context("old", "register"));
        service.applyCanary(body("mlops.deployment.canary.apply", RISK + "/traffic", "42", "canary", false,
                new CompetitionModelLifecycleService.CanaryArguments(RISK, 19L, 5, "CANARY", 30)), context("old", "canary"));
        service.promote(body("mlops.deployment.promote", RISK + "/traffic", "43", "promote", false,
                new CompetitionModelLifecycleService.PromoteArguments(RISK, 19L, 100, "CANARY")), context("old", "promote"));
        service.removeFallback(body("mlops.feature.fallback.remove", RISK + "/feature-set", "43", "remove", false,
                new CompetitionModelLifecycleService.FallbackRemoveArguments(RISK, "fallback", 19L)), context("old", "remove"));
    }

    /** 创建完整旧生命周期字段。 / Create a complete legacy lifecycle snapshot. */
    private CompetitionModelLifecycleService.LifecycleSnapshot legacyState(long revision) {
        return new CompetitionModelLifecycleService.LifecycleSnapshot(revision, revision, 80, 100, false, true, true, true, true, true, true, true);
    }

    /** 创建可核对的旧记录绑定。 / Create a verifiable legacy record binding. */
    private Map<String, CompetitionModelLifecycleService.LifecycleBinding> bindings() {
        return Map.of("ws:old", new CompetitionModelLifecycleService.LifecycleBinding("ws", "old", RISK));
    }

    /** 创建无外部请求的服务。 / Create a service without external requests. */
    private CompetitionModelLifecycleService service() {
        return new CompetitionModelLifecycleService(approval(), mock(QuantitativeRuntimeClient.class), mock(RecommendationRuntimeClient.class));
    }

    /** 创建审批通过替身。 / Create an approved-decision test double. */
    private GovernedApprovalVerifier approval() {
        var verifier = mock(GovernedApprovalVerifier.class);
        when(verifier.verify(any(), any())).thenReturn(new GovernedApprovalVerifier.ApprovalDecision("approval", "approver", "executor", "arguments"));
        return verifier;
    }

    /** 创建备用特征治理请求。 / Create a governed fallback request. */
    private AgentContract.ToolRequest<CompetitionModelLifecycleService.FallbackApplyArguments> fallbackBody(String version, String key, boolean dry) {
        return body(FALLBACK, RISK + "/feature-set", version, key, dry, new CompetitionModelLifecycleService.FallbackApplyArguments(RISK, "fallback", "repair"));
    }

    /** 创建已鉴权范围上下文。 / Create an authenticated scope context. */
    private AgentContract.RequestContext context(String incident, String key) {
        return new AgentContract.RequestContext("ws", incident, "trace", "tool", key, "executor", "request");
    }

    /** 创建强类型治理请求。 / Create a typed governed request. */
    private <T> AgentContract.ToolRequest<T> body(String tool, String resource, String version, String key, boolean dry, T arguments) {
        return new AgentContract.ToolRequest<>("request", tool, arguments, "approval", "plan", "digest", key, resource, 19L, version, "arguments", false, "test", key, dry);
    }
}
