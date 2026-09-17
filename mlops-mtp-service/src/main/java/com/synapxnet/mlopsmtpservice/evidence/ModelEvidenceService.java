/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型证据只读投影。 / Read-only projection of recorded model evidence.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-13
 * Version: 1.0.0 | Security Level: INTERNAL | Maintainer: maoyo
 * Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmtpservice.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingMapper;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingRun;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 白名单读取现有训练记录，不执行训练或查询模拟部署。 / Read allowlisted records without training or synthetic deployment calls. */
@Service
public class ModelEvidenceService {
    private static final List<String> METRIC_KEYS = List.of("auc", "accuracy", "precision", "recall", "f1");
    private static final Set<String> KNOWN_STATUSES = Set.of("running", "succeeded", "failed");
    private static final int MAX_METRICS_LENGTH = 1_000_000;
    private final RecommendationTrainingMapper mapper;
    private final ObjectMapper objectMapper;

    /** 注入只读查询和 JSON 解析器。 / Inject record queries and the JSON parser. */
    public ModelEvidenceService(RecommendationTrainingMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** 返回已验证租户的最近记录与明确能力边界。 / Return recent records and capability boundaries for the verified tenant. */
    public Workspace read(String tenantUid) {
        List<RunEvidence> runs = new ArrayList<>();
        for (RecommendationTrainingRun run : mapper.findRecentByTenant(tenantUid)) {
            if (run != null && tenantUid.equals(run.getTenantUid()) && runs.size() < 100) {
                runs.add(project(run));
            }
        }
        return new Workspace("1.0.0", java.util.UUID.randomUUID().toString(),
                new Scope("tenant", tenantUid), Instant.now().toString(),
                "recommendation-training-records", List.copyOf(runs), List.of(
                new Capability("training-records", true, "RECORDED_RUNS_AVAILABLE"),
                new Capability("model-registry", false, "ARTIFACT_REGISTRY_NOT_CONNECTED"),
                new Capability("release-runtime", false, "VERIFIED_RUNTIME_NOT_CONNECTED"),
                new Capability("evaluation-gate", false, "VERSIONED_EVALUATION_NOT_CONNECTED"),
                new Capability("physical-evaluation", false, "PHYSICAL_EVALUATION_NOT_CONNECTED")),
                "descriptive_only", 100, "mlops", "native", runs.isEmpty() ? "empty" : "available", null,
                List.of("EXECUTION_MODE_NOT_RECORDED", "LEGACY_DIGEST_EXCLUDES_FEATURE_VALUES", "TENANT_SCOPE_ONLY"),
                new Freshness("unknown", null));
    }

    /** 排除身份、审批、路径和配置，仅保留可解释的模型证据。 / Exclude identity, approvals, paths and configuration from evidence. */
    private RunEvidence project(RecommendationTrainingRun run) {
        JsonNode parsed = parseMetrics(run.getMetricsJson());
        Map<String, Double> metrics = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        if (parsed != null) {
            for (String key : METRIC_KEYS) {
                JsonNode value = parsed.path("test_metrics").path(key);
                if (value.isNumber() && Double.isFinite(value.asDouble())
                        && value.asDouble() >= 0 && value.asDouble() <= 1) {
                    metrics.put(key, value.asDouble());
                }
            }
            for (String key : List.of("train", "validation", "test")) {
                JsonNode value = parsed.path("row_counts").path(key);
                if (value.isIntegralNumber() && value.canConvertToLong()
                        && value.asLong() >= 0 && value.asLong() <= 9_007_199_254_740_991L) {
                    counts.put(key, value.asLong());
                }
            }
        }
        String availability = metrics.isEmpty() ? "unavailable"
                : metrics.size() == METRIC_KEYS.size() ? "available" : "partial";
        String status = KNOWN_STATUSES.contains(run.getStatus() == null ? "" : run.getStatus())
                ? run.getStatus() : "unknown";
        return new RunEvidence(text(run.getRunUid()), text(run.getProductVersion()), status,
                timestamp(run.getStartedAt()), timestamp(run.getCompletedAt()),
                digest(run.getModelDigestSha256()), digest(run.getSchemaDigestSha256()),
                digest(run.getArtifactDigestSha256()), Map.copyOf(metrics), Map.copyOf(counts),
                availability, "descriptive_only");
    }

    /** 安全解析有大小边界的指标对象，损坏数据按不可用处理。 / Parse bounded metric objects and treat corrupt records as unavailable. */
    private JsonNode parseMetrics(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_METRICS_LENGTH) return null;
        try {
            JsonNode parsed = objectMapper.readTree(value);
            return parsed != null && parsed.isObject() ? parsed : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 输出有界文本，避免记录值撑大页面。 / Bound record text to prevent oversized presentation. */
    private String text(String value) {
        return value == null ? "" : value.substring(0, Math.min(value.length(), 128));
    }

    /** 只公开有效 SHA-256；缺失摘要不表示制品完整。 / Expose valid SHA-256 values without implying missing artifact integrity. */
    private String digest(String value) {
        return value != null && value.matches("(?i)[0-9a-f]{64}") ? value.toLowerCase(java.util.Locale.ROOT) : null;
    }

    /** 使用 UTC 时间且保留缺失值。 / Use UTC timestamps while preserving missing values. */
    private String timestamp(Date value) {
        return value == null ? null : Instant.ofEpochMilli(value.getTime()).toString();
    }

    /** 当前已核验范围。 / Currently verified scope. */
    public record Scope(String kind, String tenantUid) { }

    /** 真实能力状态。 / Actual capability availability. */
    public record Capability(String id, boolean available, String reason) { }

    /** 不含敏感原始载荷的运行记录。 / Run evidence without sensitive raw payloads. */
    public record RunEvidence(String runUid, String productVersion, String status,
                              String startedAt, String completedAt, String modelDigestSha256,
                              String schemaDigestSha256, String legacyArtifactDigestSha256,
                              Map<String, Double> metrics, Map<String, Long> sampleCounts,
                              String metricsCoverage, String comparisonLevel) { }

    /** 历史来源的新鲜度不具备可信期限。 / Historical source freshness has no verified age policy. */
    public record Freshness(String status, Long maxAgeSeconds) { }

    /** 只读工作区响应。 / Read-only workspace response. */
    public record Workspace(String schemaVersion, String requestId, Scope scope, String capturedAt, String source,
                            List<RunEvidence> runs, List<Capability> capabilities, String comparisonLevel,
                            int recordLimit, String sourcePlatform, String sourceOrigin, String availability,
                            String executionMode, List<String> limitations, Freshness freshness) { }
}
