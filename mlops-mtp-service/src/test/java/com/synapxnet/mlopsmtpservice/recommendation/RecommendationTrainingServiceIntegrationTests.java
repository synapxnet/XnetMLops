/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 已发布数据产品训练集成测试 / Published data-product training integration test.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmtpservice.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RecommendationTrainingServiceIntegrationTests {

    @TempDir
    Path outputRoot;

    /** 用真实发布数据训练并验证幂等回读。 Train a real published product and verify idempotent replay. */
    @Test
    void trainsPublishedProductAndReplaysIdempotently() throws Exception {
        String password = System.getenv("RECOMMENDATION_TEST_DB_PASSWORD");
        assumeTrue(password != null && !password.isBlank(), "未配置本地 PostgreSQL 集成测试密码");
        String databaseUrl = environment("RECOMMENDATION_TEST_DB_URL", "jdbc:postgresql://127.0.0.1:55432/recommendation");
        String databaseUser = environment("RECOMMENDATION_TEST_DB_USERNAME", "postgres");
        String python = environment("RECOMMENDATION_TEST_PYTHON", "python");
        String productVersion = environment("RECOMMENDATION_TEST_PRODUCT_VERSION", "recommendation-dcn-integration-v2");
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path trainingScript = resolveTrainingScript(workingDirectory);
        InMemoryTrainingMapper mapper = new InMemoryTrainingMapper(
                readPublishedTestDataset(databaseUrl, databaseUser, password, productVersion));
        RecommendationTrainingService service = new RecommendationTrainingService(
                mapper,
                new ObjectMapper(),
                databaseUrl,
                databaseUser,
                password,
                python,
                trainingScript.toString(),
                outputRoot.toString(),
                300,
                Integer.parseInt(environment("RECOMMENDATION_TEST_EPOCHS", "4"))
        );
        RecommendationTrainingRequest request = new RecommendationTrainingRequest(1L);

        RecommendationTrainingRun first = service.runTraining(
                request,
                "tenant-integration",
                "integration-operator",
                "APR-MTP-INTEGRATION-001",
                "IDEM-MTP-INTEGRATION-001"
        );
        RecommendationTrainingRun replay = service.runTraining(
                request,
                "tenant-integration",
                "integration-operator",
                "APR-MTP-INTEGRATION-001",
                "IDEM-MTP-INTEGRATION-001"
        );

        assertEquals("succeeded", first.getStatus());
        assertEquals(first.getRunUid(), replay.getRunUid());
        assertNotNull(first.getModelDigestSha256());
        assertEquals(64, first.getModelDigestSha256().length());
        JsonNode metrics = new ObjectMapper().readTree(first.getMetricsJson());
        double auc = metrics.path("test_metrics").path("auc").asDouble();
        assertTrue(auc > 0.80, () -> "Expected held-out AUC > 0.80; actual=" + auc
                + "; metrics=" + first.getMetricsJson());
        assertTrue(first.getArtifactReference().startsWith("mlops://recommendation-runs/"));
    }

    /** 读取可移植测试配置。 Read portable integration-test configuration. */
    private String environment(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /** 从真实发布注册表读取摘要，不固化环境专属版本。 Read digests from the actual published registry. */
    private RecommendationDatasetReference readPublishedTestDataset(
            String url, String username, String password, String productVersion) throws Exception {
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.prepareStatement(
                     "SELECT schema_digest_sha256, artifact_digest_sha256 FROM recommendation_curated.data_product_registry "
                             + "WHERE product_version=? AND status='published'")) {
            statement.setString(1, productVersion);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next(), "The configured integration product must be published by DataOps");
                return new RecommendationDatasetReference(1L, "tenant-integration", "XnetDataOps",
                        productVersion, rows.getString("schema_digest_sha256"),
                        rows.getString("artifact_digest_sha256"), "ready");
            }
        }
    }

    /** 解析模块内训练脚本。 Resolve the training script relative to the Maven working directory. */
    private Path resolveTrainingScript(Path workingDirectory) {
        Path moduleRelative = workingDirectory.resolve("recommendation-training/train_dcn.py").normalize();
        if (java.nio.file.Files.isRegularFile(moduleRelative)) {
            return moduleRelative;
        }
        return workingDirectory
                .resolve("mlops-mtp-service/recommendation-training/train_dcn.py")
                .normalize();
    }

    /** 用内存状态模拟 MyBatis Mapper，同时保留真实训练和 PostgreSQL 访问。 */
    private static final class InMemoryTrainingMapper implements RecommendationTrainingMapper {

        private final RecommendationDatasetReference dataset;
        private final Map<String, RecommendationTrainingRun> runsByIdempotency = new HashMap<>();
        private final Map<String, RecommendationTrainingRun> runsByUid = new HashMap<>();

        /** 仅隔离记录持久化，数据与训练来自真实运行。 Isolate record storage while keeping real data and training. */
        private InMemoryTrainingMapper(RecommendationDatasetReference dataset) {
            this.dataset = dataset;
        }

        /** 按固定租户和数据集主键返回测试引用。 */
        @Override
        public RecommendationDatasetReference findDataset(long datasetId, String tenantUid) {
            return dataset.getDatasetId() == datasetId && dataset.getTenantUid().equals(tenantUid)
                    ? dataset
                    : null;
        }

        /** 保存运行中的训练记录。 */
        @Override
        public void insertRun(RecommendationTrainingRun run) {
            runsByIdempotency.put(run.getIdempotencyKey(), run);
            runsByUid.put(run.getRunUid(), run);
        }

        /** 按幂等键读取训练记录。 */
        @Override
        public RecommendationTrainingRun findByIdempotencyKey(String idempotencyKey) {
            return runsByIdempotency.get(idempotencyKey);
        }

        /** 按运行标识读取训练记录。 */
        @Override
        public RecommendationTrainingRun findByRunUid(String runUid) {
            return runsByUid.get(runUid);
        }

        /** 按租户返回内存中的训练记录。 */
        @Override
        public List<RecommendationTrainingRun> findRecentByTenant(String tenantUid) {
            List<RecommendationTrainingRun> result = new ArrayList<>();
            for (RecommendationTrainingRun run : runsByUid.values()) {
                if (tenantUid.equals(run.getTenantUid())) {
                    result.add(run);
                }
            }
            return result;
        }

        /** 将内存训练记录标记为成功。 */
        @Override
        public int completeRun(RecommendationTrainingRun run) {
            run.setStatus("succeeded");
            return 1;
        }

        /** 将内存训练记录标记为失败。 */
        @Override
        public int failRun(RecommendationTrainingRun run) {
            run.setStatus("failed");
            return 1;
        }
    }
}
