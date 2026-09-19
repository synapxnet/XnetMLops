/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 治理重启、幂等和外部动作故障测试 / Governance restart, idempotency and external-action fault tests.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-18 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 覆盖持久化边界而不是复制领域实现。 / Exercise durability boundaries instead of mirroring domain implementation. */
class CompetitionModelCheckpointTest {
    private static final String RISK = "deploy_risk_prod";
    private static final String QUANT = "deploy_quant_ashare_research";
    private final ObjectMapper mapper = new ObjectMapper();
    @TempDir Path temporary;

    /** 重启读取最新版本并精确回放幂等结果，不再读取初始导出。 / Restore latest versions and replay idempotently without rereading the bootstrap. */
    @Test void restoresLatestAndReplaysExactly() throws Exception {
        Path bootstrap = bootstrap(); String digest = sha(bootstrap); Path directory = directory("state");
        var first = service(mock(QuantitativeRuntimeClient.class));
        initialize(first, directory, bootstrap, digest);
        var request = fallback("42", "first", false);
        var before = first.applyFallback(request, context("first"));
        first.closeCheckpoint();
        Files.writeString(bootstrap, "changed old bootstrap is intentionally ignored");
        var second = service(mock(QuantitativeRuntimeClient.class));
        initialize(second, directory, bootstrap, digest);
        assertEquals(true, second.deploymentEvidence(context("read"), RISK).get("fallbackFeatureActive"));
        assertEquals("43", versions(second).get(RISK + "/feature-set"));
        assertEquals(before.data(), second.applyFallback(request, context("first")).data());
        assertEquals("43", versions(second).get(RISK + "/feature-set"));
        second.closeCheckpoint();
    }

    /** 演练及其回放跨重启保留，真实状态不被推进。 / Preserve rehearsals and replay across restart without advancing live state. */
    @Test void persistsDryRunAndDoesNotCreatePending() throws Exception {
        Path bootstrap = bootstrap(); String digest = sha(bootstrap); Path directory = directory("state");
        var first = service(mock(QuantitativeRuntimeClient.class)); initialize(first, directory, bootstrap, digest);
        var request = fallback("42", "dry", true);
        var response = first.applyFallback(request, context("dry")); first.closeCheckpoint();
        var second = service(mock(QuantitativeRuntimeClient.class)); initialize(second, directory, bootstrap, digest);
        assertEquals(response.data(), second.applyFallback(request, context("dry")).data());
        assertEquals(false, second.deploymentEvidence(context("read"), RISK).get("fallbackFeatureActive"));
        assertEquals("42", versions(second).get(RISK + "/feature-set"));
        assertTrue(payload(directory).get("pendingExecution").isNull()); second.closeCheckpoint();
    }

    /** 写前日志落盘失败时外部运行时绝不被调用。 / Never call the external runtime when the write-ahead journal cannot become durable. */
    @Test void persistenceFailureBeforeActionPreventsExternalCall() throws Exception {
        Path bootstrap = bootstrap(); Path directory = directory("state");
        var client = runtime(); var service = service(client); AtomicBoolean fail = new AtomicBoolean();
        service.initializeCheckpoint(directory, bootstrap, sha(bootstrap), (path, phase) -> {
            if (fail.get() && phase.equals("before-replace")) throw new IOException("injected disk fault");
        });
        fail.set(true);
        assertThrows(AgentContractException.class, () -> service.startTrainingSearch(training(), context("train")));
        verify(client, never()).train(any(), any(), anyLong(), anyInt());
        assertThrows(AgentContractException.class, () -> service.deploymentEvidence(context("read"), RISK));
        service.closeCheckpoint();
    }

    /** 外部调用异常后不清 pending，重启不会重放一个不确定的动作。 / Retain pending after external failure so restart cannot replay an uncertain action. */
    @Test void failedExternalActionRetainsJournalAndBlocksRestart() throws Exception {
        Path bootstrap = bootstrap(); String digest = sha(bootstrap); Path directory = directory("state");
        var client = runtime(); when(client.train(any(), any(), anyLong(), anyInt())).thenThrow(new IllegalStateException("transport lost after dispatch"));
        var service = service(client); initialize(service, directory, bootstrap, digest);
        assertThrows(IllegalStateException.class, () -> service.startTrainingSearch(training(), context("train")));
        assertEquals("train", payload(directory).path("pendingExecution").path("idempotencyKey").asText());
        verify(client, times(1)).train(any(), any(), anyLong(), anyInt()); service.closeCheckpoint();
        var next = service(runtime());
        assertThrows(IllegalStateException.class, () -> initialize(next, directory, bootstrap, digest));
        assertEquals("train", payload(directory).path("pendingExecution").path("idempotencyKey").asText());
    }

    /** 外部动作完成但结果落盘前故障时保留 pending，不假设动作未执行。 / Preserve pending if the action completes but its result cannot be committed. */
    @Test void resultCommitFailureRetainsPendingAndBlocksReplay() throws Exception {
        Path bootstrap = bootstrap(); String digest = sha(bootstrap); Path directory = directory("state");
        var client = runtime(); AtomicBoolean invoked = new AtomicBoolean();
        when(client.train(any(), any(), anyLong(), anyInt())).thenAnswer(call -> {
            invoked.set(true); return Map.of("passed", true, "modelSha256", "model", "dataset", "dataset", "usageBoundary", "RESEARCH_ONLY");
        });
        var service = service(client);
        service.initializeCheckpoint(directory, bootstrap, digest, (path, phase) -> {
            if (invoked.get() && phase.equals("before-replace")) throw new IOException("result persistence failed");
        });
        assertThrows(AgentContractException.class, () -> service.startTrainingSearch(training(), context("train")));
        assertTrue(invoked.get()); assertFalse(payload(directory).get("pendingExecution").isNull());
        assertThrows(AgentContractException.class, () -> service.startTrainingSearch(training(), context("train")));
        verify(client, times(1)).train(any(), any(), anyLong(), anyInt()); service.closeCheckpoint();
        assertThrows(IllegalStateException.class, () -> initialize(service(runtime()), directory, bootstrap, digest));
    }

    /** 过期资源版本在外部动作和 pending 之前拒绝，后续正确请求仍能执行。 / Reject stale versions before pending or external action while allowing a later correct request. */
    @Test void staleVersionDoesNotPoisonHealthyCheckpoint() throws Exception {
        Path bootstrap = bootstrap(); Path directory = directory("state"); var service = service(mock(QuantitativeRuntimeClient.class));
        initialize(service, directory, bootstrap, sha(bootstrap));
        assertThrows(AgentContractException.class, () -> service.applyFallback(fallback("41", "stale", false), context("stale")));
        assertTrue(payload(directory).get("pendingExecution").isNull());
        service.applyFallback(fallback("42", "valid", false), context("valid"));
        assertEquals("43", versions(service).get(RISK + "/feature-set")); service.closeCheckpoint();
    }

    /** 文件丢失后拒绝回退旧 bootstrap，进程内篡改也立即隔离。 / Reject stale-bootstrap fallback after file loss and quarantine in-process tampering. */
    @Test void missingOrTamperedCheckpointNeverFallsBack() throws Exception {
        Path bootstrap = bootstrap(); String digest = sha(bootstrap); Path directory = directory("state");
        var service = service(mock(QuantitativeRuntimeClient.class)); initialize(service, directory, bootstrap, digest);
        Files.writeString(directory.resolve("checkpoint.json"), "{}");
        assertThrows(AgentContractException.class, () -> service.deploymentEvidence(context("read"), RISK)); service.closeCheckpoint();
        Files.delete(directory.resolve("checkpoint.json"));
        assertThrows(IllegalStateException.class, () -> initialize(service(mock(QuantitativeRuntimeClient.class)), directory, bootstrap, digest));
    }

    /** 首次导入不能接受错误来源、摘要或未绑定资源。 / Reject incorrect bootstrap provenance, digest or unbound resources. */
    @Test void rejectsWrongBootstrapProvenanceAndDigest() throws Exception {
        Path bootstrap = bootstrap();
        assertThrows(IllegalStateException.class, () -> initialize(service(mock(QuantitativeRuntimeClient.class)), directory("digest"), bootstrap, "0".repeat(64)));
        ObjectNode changed = (ObjectNode) mapper.readTree(bootstrap.toFile()); changed.put("sourceJarSha256", "0".repeat(64));
        Files.write(bootstrap, mapper.writeValueAsBytes(changed));
        assertThrows(IllegalStateException.class, () -> initialize(service(mock(QuantitativeRuntimeClient.class)), directory("source"), bootstrap, sha(bootstrap)));
    }

    /** 新查询初始化的规范领域和版本可在重启后恢复。 / Restore canonical domains and versions initialized by a new read. */
    @Test void newQuantitativeReadPersistsMatchingDomain() throws Exception {
        Path bootstrap = bootstrap(); String digest = sha(bootstrap); Path directory = directory("state");
        var client = runtime(); when(client.deployment()).thenReturn(Map.of("activeRevision", 1));
        when(client.health()).thenReturn(Map.of("status", "ready", "datasetRows", 120, "usageBoundary", "RESEARCH_ONLY"));
        var first = service(client); initialize(first, directory, bootstrap, digest);
        var context = new AgentContract.RequestContext("new-ws", "incident", "trace", "tool", "read", "reader", "request");
        first.deploymentEvidence(context, QUANT); first.closeCheckpoint();
        var second = service(client); initialize(second, directory, bootstrap, digest);
        assertEquals("42", second.deploymentEvidence(context, QUANT).get("resourceVersion")); second.closeCheckpoint();
    }

    /** 创建受控同步替身，Windows 不模拟支持目录 fsync。 / Explicitly use a sync substitute on Windows without claiming production fsync support. */
    private void initialize(CompetitionModelLifecycleService service, Path directory, Path bootstrap, String digest) {
        service.initializeCheckpoint(directory, bootstrap, digest, (path, phase) -> { });
    }

    /** 创建独立持久目录。 / Create an isolated persistent directory. */
    private Path directory(String name) throws IOException { return Files.createDirectory(temporary.resolve(name)).toRealPath(); }

    /** 读取原子文件的当前 payload。 / Read the current payload from the atomic checkpoint file. */
    private com.fasterxml.jackson.databind.JsonNode payload(Path directory) throws IOException { return mapper.readTree(directory.resolve("checkpoint.json").toFile()).required("payload"); }

    /** 创建完整且可验证的当前源导出。 / Create a complete, verifiable current-source export. */
    private Path bootstrap() throws Exception {
        var tracker = new GovernedResourceVersionTracker();
        var domains = new java.util.LinkedHashMap<String, Object>();
        for (String deployment : List.of(RISK, QUANT)) {
            for (String resource : CompetitionModelResourceBinding.forDeployment(deployment).resourceIds()) tracker.initializeResource("ws", resource, 42);
            domains.put("ws:" + deployment, new CompetitionModelLifecycleService.LifecycleSnapshot(17, 19, 42, 100, false, false, false, false, false, false, false, false));
        }
        Map<String, Object> export = Map.of("schema", "openxnet.governed-state-export.v1", "platform", "mlops",
                "controller", CompetitionModelLifecycleService.class.getName(), "domainField", "incidentStates", "readOnly", true,
                "sourceJarSha256", MepGovernedStateCheckpoint.SOURCE_JAR, "exportedAt", "2026-09-18T01:47:17Z", "tracker", tracker.snapshot(), "domainState", domains);
        Path path = temporary.resolve("bootstrap.json"); Files.write(path, mapper.writeValueAsBytes(export)); return path;
    }

    /** 计算源文件原字节摘要。 / Hash the original source bytes. */
    private String sha(Path path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }

    /** 构造可跟踪真实外部动作次数的替身。 / Create a test double that tracks external dispatches. */
    private QuantitativeRuntimeClient runtime() { var client = mock(QuantitativeRuntimeClient.class); when(client.supports(QUANT)).thenReturn(true); return client; }

    /** 审批替身仅验证持久化边界，审批契约由独立测试覆盖。 / Approval test double isolates durability; separate tests cover the approval contract. */
    private CompetitionModelLifecycleService service(QuantitativeRuntimeClient runtime) {
        var verifier = mock(GovernedApprovalVerifier.class);
        when(verifier.verify(any(), any())).thenReturn(new GovernedApprovalVerifier.ApprovalDecision("approval", "approver", "executor", "arguments"));
        return new CompetitionModelLifecycleService(verifier, runtime, mock(RecommendationRuntimeClient.class));
    }

    /** 创建请求范围。 / Create request scope. */
    private AgentContract.RequestContext context(String key) { return new AgentContract.RequestContext("ws", "incident", "trace", "tool", key, "executor", "request"); }

    /** 读取当前规范资源版本。 / Read current canonical resource versions. */
    private Map<?, ?> versions(CompetitionModelLifecycleService service) { return (Map<?, ?>) service.deploymentEvidence(context("read"), RISK).get("resourceVersions"); }

    /** 构造备用特征请求。 / Build a fallback-feature request. */
    private AgentContract.ToolRequest<CompetitionModelLifecycleService.FallbackApplyArguments> fallback(String version, String key, boolean dry) {
        return new AgentContract.ToolRequest<>("request", "mlops.feature.fallback.apply", new CompetitionModelLifecycleService.FallbackApplyArguments(RISK, "fallback", "repair"),
                "approval", "plan", "digest", key, RISK + "/feature-set", 19L, version, "arguments", false, "test", key, dry);
    }

    /** 构造会触发外部量化运行时的真实写请求。 / Build a write request that dispatches to the external quantitative runtime. */
    private AgentContract.ToolRequest<CompetitionModelLifecycleService.TrainingSearchArguments> training() {
        return new AgentContract.ToolRequest<>("request", "mlops.training.search.start", new CompetitionModelLifecycleService.TrainingSearchArguments(QUANT, "dataset", "experiment_quant_2", 2L, 12, List.of("model"), "RETRAIN"),
                "approval", "plan", "digest", "train", "experiment_quant_2", 2L, "42", "arguments", false, "test", "train", false);
    }
}
