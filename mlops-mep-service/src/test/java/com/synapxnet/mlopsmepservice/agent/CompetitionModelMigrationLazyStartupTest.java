/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 延迟初始化下的迁移启动验证 / Migration startup verification with lazy initialization.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-15
 * Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** 验证真实 Boot 全局延迟初始化不能推迟迁移门禁。 / Verify that actual Boot global lazy initialization cannot defer the migration gate. */
class CompetitionModelMigrationLazyStartupTest {
    private static final String DEPLOYMENT = "deploy_risk_prod";
    @TempDir Path temporary;

    /** 不请求 Bean 或业务读取就应完成迁移，并保留三个 tracker 集合。 / Complete migration before Bean requests or business reads while preserving all three tracker collections. */
    @Test void consumesAndRestoresBeforeFirstRequestWithGlobalLazyEnabled() throws Exception {
        MigrationInput input = migrationInput();
        try (ConfigurableApplicationContext context = start(input.file(), input.sha256())) {
            assertEquals("true", context.getEnvironment().getProperty("spring.main.lazy-initialization"));
            assertTrue(Files.isRegularFile(temporary.resolve("migration.json.consumed")));
            assertEquals(input.sha256(), Files.readString(temporary.resolve("migration.json.consumed")).trim());
            assertFalse(context.getBeanFactory().containsSingleton("unusedStartupMarker"));
            Object service = context.getBeanFactory().getSingleton("lifecycleService");
            assertNotNull(service, "Migration must already have instantiated its state owner.");
            var tracker = (GovernedResourceVersionTracker) field(service, "versionTracker");
            assertEquals(input.tracker(), tracker.snapshot());
            assertFalse(tracker.snapshot().liveVersions().isEmpty());
            assertFalse(tracker.snapshot().rehearsalVersions().isEmpty());
            assertEquals(2, tracker.snapshot().executions().size());
            Map<?, ?> domains = (Map<?, ?>) field(service, "incidentStates");
            assertEquals(1, domains.size());
            Object restored = domains.get("ws:" + DEPLOYMENT);
            assertNotNull(restored);
            assertEquals(19L, field(restored, "activeRevision"));
            assertEquals(80L, field(restored, "resourceVersion"));
            assertEquals(true, field(restored, "fallbackFeatureActive"));
            assertEquals(input.sha256(), digest(Files.readAllBytes(input.file())));
        }
    }

    /** 错误摘要必须在启动时失败，不能等首次请求再发现。 / A wrong digest must fail during startup instead of waiting for the first request. */
    @Test void rejectsWrongMigrationDigestDuringLazyApplicationStartup() throws Exception {
        MigrationInput input = migrationInput();
        assertThrows(RuntimeException.class, () -> {
            try (ConfigurableApplicationContext unexpected = start(input.file(), "0".repeat(64))) {
                fail("The application must not finish startup with an invalid migration digest.");
            }
        });
        assertFalse(Files.exists(temporary.resolve("migration.json.consumed")));
        assertEquals(input.sha256(), digest(Files.readAllBytes(input.file())));
    }

    /** 已消费重启保留普通服务，旧状态不重放也不归零后开放。 / Consumed restart retains native service without replaying or reopening reset legacy state. */
    @Test void consumedMigrationQuarantinesEveryLegacyGovernedEntry() throws Exception {
        MigrationInput input = migrationInput();
        try (var initial = start(input.file(), input.sha256())) { }
        byte[] marker = Files.readAllBytes(temporary.resolve("migration.json.consumed"));
        var service = MigrationOnlyConfiguration.createLifecycle();
        new CompetitionModelStateMigration(service, new ObjectMapper(), input.file().toString(), input.sha256(), true);
        var ctx = new AgentContract.RequestContext("ws", "new", "trace", "mlops.deployment.get", "id", "executor", "req");
        assertEquals("STATE_UNAVAILABLE", assertThrows(com.synapxnet.goai.contract.AgentContractException.class, () -> service.deploymentEvidence(ctx, DEPLOYMENT)).getCode());
        var body = new AgentContract.ToolRequest<>("req", "mlops.feature.fallback.apply", new CompetitionModelLifecycleService.FallbackApplyArguments(DEPLOYMENT, "feature_set_risk_fallback_v1", "UPSTREAM_SDK_CONTRACT_DRIFT"), "apr", "plan", "a".repeat(64), "step", DEPLOYMENT + "/feature-set", 18L, "42", "b".repeat(64), false, "test", "id", false);
        assertEquals("STATE_UNAVAILABLE", assertThrows(com.synapxnet.goai.contract.AgentContractException.class, () -> service.applyFallback(body, ctx)).getCode());
        assertTrue(((GovernedResourceVersionTracker) field(service, "versionTracker")).snapshot().liveVersions().isEmpty());
        assertTrue(((Map<?,?>) field(service, "incidentStates")).isEmpty());
        assertArrayEquals(marker, Files.readAllBytes(temporary.resolve("migration.json.consumed")));
        assertEquals(input.sha256(), digest(Files.readAllBytes(input.file())));
        assertThrows(IllegalStateException.class, () -> new CompetitionModelStateMigration(MigrationOnlyConfiguration.createLifecycle(), new ObjectMapper(), input.file().toString(), input.sha256()));
    }

    /** 真实模式仍严格拒绝消费标记篡改。 / Real mode still strictly rejects consumed-marker tampering. */
    @Test void consumedMarkerTamperingRemainsStartupFatal() throws Exception {
        MigrationInput input = migrationInput();
        Files.writeString(temporary.resolve("migration.json.consumed"), "0".repeat(64) + "\n");
        assertThrows(IllegalStateException.class, () -> new CompetitionModelStateMigration(MigrationOnlyConfiguration.createLifecycle(), new ObjectMapper(), input.file().toString(), input.sha256(), true));
    }

    /** 仅启动迁移所需配置，不启动服务器、数据库或真实运行时客户端。 / Start only migration configuration without a server, database or real runtime clients. */
    private ConfigurableApplicationContext start(Path file, String sha256) {
        SpringApplication application = new SpringApplication(MigrationOnlyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        return application.run("--spring.main.lazy-initialization=true",
                "--spring.config.location=optional:classpath:/isolated-migration-test.properties",
                "--logging.level.root=OFF",
                "--goai.resource-state-migration-file=" + file,
                "--goai.resource-state-migration-sha256=" + sha256);
    }

    /** 构造有真实版本分叉与幂等历史的独立迁移输入。 / Build an isolated migration input with divergent versions and idempotency history. */
    private MigrationInput migrationInput() throws Exception {
        var tracker = new GovernedResourceVersionTracker();
        tracker.initializeResource("ws", DEPLOYMENT, 52);
        tracker.initializeResource("ws", DEPLOYMENT + "/traffic", 70);
        recordExecution(tracker, "live", "70", false);
        recordExecution(tracker, "dry", "71", true);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schema", "openxnet.governed-state-export.v1");
        document.put("platform", "mlops");
        document.put("controller", CompetitionModelLifecycleService.class.getName());
        document.put("sourceJarSha256", "8424484858047ad50271c79a2c138ee8861ec1a01d8c44513d2a3e3cfeddcca2");
        document.put("exportedAt", "2026-09-15T00:00:00Z");
        document.put("readOnly", true);
        document.put("domainField", "incidentStates");
        document.put("tracker", tracker.snapshot());
        document.put("domainState", Map.of("ws:old", new CompetitionModelLifecycleService.LifecycleSnapshot(
                19, 19, 80, 100, false, true, true, true, true, true, true, true)));
        document.put("domainBindings", Map.of("ws:old", new CompetitionModelLifecycleService.LifecycleBinding("ws", "old", DEPLOYMENT)));
        byte[] bytes = new ObjectMapper().writeValueAsBytes(document);
        Path file = temporary.resolve("migration.json");
        Files.write(file, bytes);
        return new MigrationInput(file, digest(bytes), tracker.snapshot());
    }

    /** 使用真实 tracker 生成待保全的写入或演练回执。 / Generate a retained write or rehearsal receipt using the actual tracker. */
    private void recordExecution(GovernedResourceVersionTracker tracker, String key, String version, boolean dryRun) {
        var body = new AgentContract.ToolRequest<>("request-" + key, "mlops.deployment.promote", Map.of("deploymentUid", DEPLOYMENT),
                "approval", "plan", "plan-digest", "promote", DEPLOYMENT + "/traffic", 19L, version,
                "arguments", false, "isolated-migration-test", key, dryRun);
        var context = new AgentContract.RequestContext("ws", "old", "trace", body.toolName(), key, "executor", body.requestId());
        tracker.execute(context, body, () -> Map.of("deploymentUid", DEPLOYMENT, "retainedProof", key));
    }

    /** 读取明确测试对象字段，不触发 Bean 创建或业务调用。 / Read explicit test object fields without creating Beans or invoking business operations. */
    private Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        assertTrue(field.trySetAccessible());
        return field.get(target);
    }

    /** 计算迁移输入原字节摘要。 / Hash the original migration input bytes. */
    private String digest(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    /** 保存本地迁移输入及预期原 tracker。 / Retain local migration input and the expected original tracker. */
    private record MigrationInput(Path file, String sha256, GovernedResourceVersionTracker.StateSnapshot tracker) { }

    /** 配置真实 Spring 迁移门禁，全部外部依赖为不联网替身。 / Configure the actual Spring migration gate with network-free external doubles. */
    @Configuration(proxyBeanMethods = false)
    @Import(CompetitionModelStateMigration.class)
    static class MigrationOnlyConfiguration {
        /** 提供迁移文档解析器。 / Provide the migration document parser. */
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }

        /** 创建无联网依赖的真实状态所有者。 / Construct the real state owner with network-free dependencies. */
        static CompetitionModelLifecycleService createLifecycle() {
            return new CompetitionModelLifecycleService(mock(GovernedApprovalVerifier.class), mock(QuantitativeRuntimeClient.class), mock(RecommendationRuntimeClient.class));
        }

        /** 提供实际状态所有者，禁止触发外部审批或平台请求。 / Provide the actual state owner without external approval or platform requests. */
        @Bean CompetitionModelLifecycleService lifecycleService() {
            return new CompetitionModelLifecycleService(mock(GovernedApprovalVerifier.class), mock(QuantitativeRuntimeClient.class), mock(RecommendationRuntimeClient.class));
        }

        /** 未相关 Bean 若被提前创建则直接使测试失败。 / Fail if an unrelated Bean is created eagerly. */
        @Bean Object unusedStartupMarker() { throw new AssertionError("Unrelated beans must remain lazy."); }
    }
}
