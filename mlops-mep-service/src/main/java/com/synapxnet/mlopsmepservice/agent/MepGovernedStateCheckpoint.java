/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型治理检查点与外部动作日志 / Model governance checkpoint and external-action journal.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-18 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/** 固定来源的首次导入与后续严格检查点恢复。 / Pin the first bootstrap and strictly restore subsequent checkpoints. */
final class MepGovernedStateCheckpoint implements AutoCloseable {
    static final String SOURCE_JAR = "c1b8c3f44f09e97638fba0e5160ff1f44368aa08f624760960c7033451fbc607";
    private final ObjectMapper mapper = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    private final GovernedStateCheckpointStore store;
    private final boolean initialized;
    private final JsonNode initial;

    /** 生产使用真实目录 fsync，任何不支持均拒绝启动。 / Use real directory fsync in production and reject unsupported storage. */
    MepGovernedStateCheckpoint(Path directory, Path bootstrap, String digest) throws Exception {
        this(directory, bootstrap, digest, null);
    }

    /** 离线测试显式注入目录同步，不改变生产默认。 / Inject directory synchronization explicitly for offline tests only. */
    MepGovernedStateCheckpoint(Path directory, Path bootstrap, String digest,
            GovernedStateCheckpointStore.Durability durability) throws Exception {
        store = durability == null ? new GovernedStateCheckpointStore(directory, digest)
                : new GovernedStateCheckpointStore(directory, digest, durability);
        try {
            initialized = store.initialized();
            initial = initialized ? store.load() : readBootstrap(bootstrap, digest);
            if (!"openxnet.mlops-state-checkpoint.v1".equals(initial.path("schema").asText())
                    || initial.size() != 4 || !initial.path("tracker").isObject()
                    || !initial.path("domainState").isObject() || !initial.has("pendingExecution")) {
                throw new IOException("MEP_CHECKPOINT_SCHEMA_INVALID");
            }
            if (!initial.path("pendingExecution").isNull()) throw new IOException("MEP_PENDING_EXECUTION_REQUIRES_RECONCILIATION");
        } catch (Exception failure) {
            store.close();
            throw failure;
        }
    }

    /** 仅接受指定当前进程导出，不接受旧 consumed 迁移。 / Accept only the pinned current-process export, never the old consumed migration. */
    private JsonNode readBootstrap(Path source, String expectedDigest) throws Exception {
        if (!source.isAbsolute() || !source.normalize().equals(source)
                || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) || Files.size(source) > 32 * 1024 * 1024) {
            throw new IOException("MEP_BOOTSTRAP_FILE_INVALID");
        }
        for (Path cursor = source; cursor != null; cursor = cursor.getParent()) {
            if (Files.isSymbolicLink(cursor)) throw new IOException("MEP_BOOTSTRAP_SYMLINK");
        }
        byte[] bytes = Files.readAllBytes(source);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!expectedDigest.equals(digest)) throw new IOException("MEP_BOOTSTRAP_DIGEST_MISMATCH");
        JsonNode export = mapper.readTree(bytes);
        if (!"openxnet.governed-state-export.v1".equals(export.path("schema").asText())
                || !"mlops".equals(export.path("platform").asText())
                || !CompetitionModelLifecycleService.class.getName().equals(export.path("controller").asText())
                || !"incidentStates".equals(export.path("domainField").asText())
                || !export.path("readOnly").isBoolean() || !export.path("readOnly").booleanValue()
                || !SOURCE_JAR.equals(export.path("sourceJarSha256").asText())
                || !export.path("tracker").isObject() || !export.path("domainState").isObject()) {
            throw new IOException("MEP_BOOTSTRAP_PROVENANCE_INVALID");
        }
        Instant.parse(export.path("exportedAt").asText());
        ObjectNode result = mapper.createObjectNode();
        result.put("schema", "openxnet.mlops-state-checkpoint.v1");
        result.set("tracker", export.required("tracker"));
        result.set("domainState", export.required("domainState"));
        result.putNull("pendingExecution");
        return result;
    }

    /** 返回有界且已验来源的初始内容供领域校验。 / Return bounded, provenance-checked initial content for domain validation. */
    JsonNode initialState() { return initial.deepCopy(); }

    /** 领域全部校验成功后才首次落盘。 / Persist the first checkpoint only after complete domain validation. */
    void acceptValidatedInitialState(JsonNode current) throws IOException {
        if (!initialized) store.initialize(current);
        else store.verifyCurrent();
    }

    /** 在每次治理读取和动作之前验证持久文件未被替换。 / Verify persisted files before each governed read or action. */
    void verifyCurrent() throws IOException { store.verifyCurrent(); }

    /** 完整提交状态和 pending 日志，以同一次原子替换持久化。 / Atomically commit the complete state and pending journal together. */
    void commit(JsonNode current) throws IOException { store.commit(current); }

    /** 严格解析记录，保留零值、空字段和重复键拒绝规则。 / Strictly decode records, rejecting missing fields and duplicate keys. */
    <T> T decode(JsonNode value, Class<T> type) throws IOException { return mapper.treeToValue(value, type); }

    /** 关闭并释放目录独占权。 / Close and release exclusive directory ownership. */
    @Override public void close() throws IOException { store.close(); }
}
