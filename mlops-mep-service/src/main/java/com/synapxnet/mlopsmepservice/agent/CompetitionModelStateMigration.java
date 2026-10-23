/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型状态定向迁移 / Scoped model state migration.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-15
 * Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/** 启动期间执行一次性受限迁移，失败时阻止服务启动。 / Apply one-shot scoped migration during startup and refuse startup on failure. */
@Component
@Lazy(false)
final class CompetitionModelStateMigration {
    /** 保持旧构造器默认严格拒绝消费后的重启。 / Keep the legacy constructor strict after consumed migration. */
    CompetitionModelStateMigration(CompetitionModelLifecycleService lifecycle, ObjectMapper mapper,
            String migrationFile, String expectedSha256) {
        this(lifecycle, mapper, migrationFile, expectedSha256, false);
    }

    /** 仅从显式匹配摘要的旧部署导出恢复。 / Restore only from an explicit, digest-matched export of the verified old deployment. */
    CompetitionModelStateMigration(CompetitionModelLifecycleService lifecycle, ObjectMapper mapper,
            String migrationFile, String expectedSha256, boolean realRuntimeEnabled) {
        this(lifecycle, mapper, migrationFile, expectedSha256, realRuntimeEnabled, "", "", "");
    }

    /** 新持久检查点有独立来源和绑定，不重新消费历史迁移。 / New durable checkpoints have independent provenance and never reconsume historical migration. */
    @org.springframework.beans.factory.annotation.Autowired
    CompetitionModelStateMigration(CompetitionModelLifecycleService lifecycle, ObjectMapper mapper,
            @Value("${goai.resource-state-migration-file:}") String migrationFile,
            @Value("${goai.resource-state-migration-sha256:}") String expectedSha256,
            @Value("${OPENXNET_FEATURE_DRIFT_ENABLED:false}") boolean realRuntimeEnabled,
            @Value("${goai.resource-state-checkpoint-directory:}") String checkpointDirectory,
            @Value("${goai.resource-state-checkpoint-bootstrap-file:}") String checkpointBootstrapFile,
            @Value("${goai.resource-state-checkpoint-bootstrap-sha256:}") String checkpointBootstrapSha256) {
        if (!checkpointDirectory.isBlank() || !checkpointBootstrapFile.isBlank() || !checkpointBootstrapSha256.isBlank()) {
            if (checkpointDirectory.isBlank() || checkpointBootstrapFile.isBlank()
                    || !checkpointBootstrapSha256.matches("[a-f0-9]{64}"))
                throw new IllegalStateException("MEP checkpoint configuration requires directory, bootstrap file and digest.");
            lifecycle.initializeCheckpoint(Path.of(checkpointDirectory), Path.of(checkpointBootstrapFile), checkpointBootstrapSha256);
            return;
        }
        if ((migrationFile == null || migrationFile.isBlank()) && (expectedSha256 == null || expectedSha256.isBlank())) return;
        try {
            if (migrationFile == null || migrationFile.isBlank() || expectedSha256 == null || !expectedSha256.matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("An explicit migration file and SHA-256 are both required.");
            }
            Path source = Path.of(migrationFile);
            if (!source.isAbsolute() || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) || Files.size(source) > 4 * 1024 * 1024) {
                throw new IllegalArgumentException("Migration file is invalid.");
            }
            for (Path cursor = source; cursor != null; cursor = cursor.getParent()) {
                if (Files.isSymbolicLink(cursor)) throw new IllegalArgumentException("Migration path cannot contain links.");
            }
            Path consumed = source.resolveSibling(source.getFileName() + ".consumed");
            byte[] bytes = Files.readAllBytes(source);
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!expectedSha256.equals(actual)) throw new IllegalArgumentException("Migration digest does not match.");
            ObjectMapper strict = mapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            JsonNode root = strict.readTree(bytes);
            if (!"openxnet.governed-state-export.v1".equals(root.path("schema").asText())
                    || !"mlops".equals(root.path("platform").asText())
                    || !CompetitionModelLifecycleService.class.getName().equals(root.path("controller").asText())
                    || !"incidentStates".equals(root.path("domainField").asText()) || !root.path("readOnly").asBoolean()
                    || !"8424484858047ad50271c79a2c138ee8861ec1a01d8c44513d2a3e3cfeddcca2".equals(root.path("sourceJarSha256").asText())
                    || !root.path("domainState").isObject() || !root.path("domainBindings").isObject()) {
                throw new IllegalArgumentException("Migration schema, platform or source artifact does not match.");
            }
            Instant.parse(root.path("exportedAt").asText());
            if (Files.exists(consumed, LinkOption.NOFOLLOW_LINKS)) {
                if (!realRuntimeEnabled || !Files.isRegularFile(consumed, LinkOption.NOFOLLOW_LINKS)
                        || Files.size(consumed) > 66 || !actual.equals(Files.readString(consumed, StandardCharsets.UTF_8).trim())) {
                    throw new IllegalStateException("Consumed migration cannot provide current governed state.");
                }
                lifecycle.quarantineUnavailableLegacyState();
                return;
            }
            lifecycle.restoreGovernedState(strict.treeToValue(root.path("tracker"), GovernedResourceVersionTracker.StateSnapshot.class),
                    strict.convertValue(root.path("domainState"), new TypeReference<Map<String, CompetitionModelLifecycleService.LifecycleSnapshot>>() { }),
                    strict.convertValue(root.path("domainBindings"), new TypeReference<Map<String, CompetitionModelLifecycleService.LifecycleBinding>>() { }));
            Files.writeString(consumed, actual + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (Exception error) {
            throw new IllegalStateException("Governed MLOps state migration refused; service startup is stopped.", error);
        }
    }
}
