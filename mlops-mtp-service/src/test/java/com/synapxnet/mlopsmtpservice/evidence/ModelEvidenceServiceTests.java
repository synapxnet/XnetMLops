/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型证据范围与数据真实性测试。 / Scope and data-validity tests for model evidence.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-13
 * Version: 1.0.0 | Security Level: INTERNAL | Maintainer: maoyo
 * Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmtpservice.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsmtpservice.controller.ModelEvidenceController;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingMapper;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingRun;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingWriteAuthorizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 用隔离 Mapper 检查边界，不连接数据库或执行模型。 / Check boundaries with isolated mappers without databases or model execution. */
class ModelEvidenceServiceTests {
    /** 验证字段白名单和跨租户记录不会泄露。 / Verify allowlisted fields and cross-tenant exclusion. */
    @Test
    void projectsOnlySafeTenantRecords() throws Exception {
        RecommendationTrainingRun run = run("tenant-a", "{\"test_metrics\":{\"auc\":0.91},\"row_counts\":{\"test\":23}}");
        run.setApprovalId("private-approval");
        run.setArtifactReference("private-path");
        ModelEvidenceService.Workspace evidence = service(List.of(run, run("tenant-b", "{}"))).read("tenant-a");
        assertEquals(1, evidence.runs().size());
        assertEquals(0.91, evidence.runs().get(0).metrics().get("auc"));
        assertEquals(23L, evidence.runs().get(0).sampleCounts().get("test"));
        String json = new ObjectMapper().writeValueAsString(evidence);
        assertFalse(json.contains("private-approval"));
        assertFalse(json.contains("private-path"));
        assertFalse(json.contains("tenant-b"));
        assertEquals("descriptive_only", evidence.comparisonLevel());
        assertEquals("native", evidence.sourceOrigin());
        assertNull(evidence.executionMode());
        assertNotNull(evidence.requestId());
        assertEquals("unknown", evidence.freshness().status());
    }

    /** 非法指标、计数和摘要不能伪造质量通过。 / Invalid metrics, counts and digests cannot imply quality approval. */
    @Test
    void rejectsOutOfRangeAndMalformedEvidence() {
        RecommendationTrainingRun run = run("tenant-a", "{\"test_metrics\":{\"auc\":1.2,\"f1\":\"0.9\",\"recall\":0.7},\"row_counts\":{\"train\":-1,\"test\":2.5}}");
        run.setModelDigestSha256("invalid");
        ModelEvidenceService.RunEvidence evidence = service(List.of(run)).read("tenant-a").runs().get(0);
        assertEquals(java.util.Map.of("recall", 0.7), evidence.metrics());
        assertTrue(evidence.sampleCounts().isEmpty());
        assertNull(evidence.modelDigestSha256());
        assertEquals("partial", evidence.metricsCoverage());
    }

    /** 损坏 JSON 和未知状态保留为未知，不能伪造成功。 / Preserve corrupt JSON and unknown statuses instead of inventing success. */
    @Test
    void handlesCorruptRecordsWithoutSuccessFallback() {
        RecommendationTrainingRun run = run("tenant-a", "{bad json");
        run.setStatus("promoted");
        ModelEvidenceService.RunEvidence evidence = service(List.of(run)).read("tenant-a").runs().get(0);
        assertEquals("unknown", evidence.status());
        assertEquals("unavailable", evidence.metricsCoverage());
        assertTrue(evidence.metrics().isEmpty());
    }

    /** 空数据库保持空列表，部署与物理能力不能假装已连接。 / Preserve empty data and unavailable runtime capabilities. */
    @Test
    void doesNotSeedEmptyWorkspace() {
        ModelEvidenceService.Workspace evidence = service(List.of()).read("tenant-a");
        assertTrue(evidence.runs().isEmpty());
        assertEquals("empty", evidence.availability());
        assertEquals(1, evidence.capabilities().stream().filter(ModelEvidenceService.Capability::available).count());
    }

    /** 身份校验失败时不得访问任何证据记录。 / Never query evidence when identity verification fails. */
    @Test
    void authorizesBeforeReadingRecords() {
        ModelEvidenceService evidence = mock(ModelEvidenceService.class);
        RecommendationTrainingWriteAuthorizer authorizer = mock(RecommendationTrainingWriteAuthorizer.class);
        doThrow(new IllegalStateException("denied")).when(authorizer).authorize("invalid", "user", "tenant-a");
        ModelEvidenceController controller = new ModelEvidenceController(evidence, authorizer);
        assertThrows(IllegalStateException.class, () -> controller.read("invalid", "tenant-a", "user"));
        verifyNoInteractions(evidence);
    }

    /** 创建不依赖真实数据源的测试服务。 / Create a test service without a live data source. */
    private ModelEvidenceService service(List<RecommendationTrainingRun> runs) {
        RecommendationTrainingMapper mapper = mock(RecommendationTrainingMapper.class);
        when(mapper.findRecentByTenant("tenant-a")).thenReturn(runs);
        return new ModelEvidenceService(mapper, new ObjectMapper());
    }

    /** 创建隔离训练记录。 / Create an isolated training record. */
    private RecommendationTrainingRun run(String tenantUid, String metrics) {
        RecommendationTrainingRun run = new RecommendationTrainingRun();
        run.setRunUid("run-one");
        run.setTenantUid(tenantUid);
        run.setProductVersion("product-v1");
        run.setStatus("succeeded");
        run.setMetricsJson(metrics);
        run.setModelDigestSha256("a".repeat(64));
        return run;
    }
}
