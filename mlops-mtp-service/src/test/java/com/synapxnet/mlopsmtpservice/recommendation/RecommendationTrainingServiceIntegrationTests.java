package com.synapxnet.mlopsmtpservice.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
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

    /** 验证真实 DataOps 发布版本可以由 MTP 完成训练并幂等返回模型证据。 */
    @Test
    void trainsPublishedProductAndReplaysIdempotently() throws Exception {
        String password = System.getenv("RECOMMENDATION_TEST_DB_PASSWORD");
        assumeTrue(password != null && !password.isBlank(), "未配置本地 PostgreSQL 集成测试密码");
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path trainingScript = resolveTrainingScript(workingDirectory);
        InMemoryTrainingMapper mapper = new InMemoryTrainingMapper();
        RecommendationTrainingService service = new RecommendationTrainingService(
                mapper,
                new ObjectMapper(),
                "jdbc:postgresql://127.0.0.1:55432/recommendation",
                "postgres",
                password,
                "D:\\anaconda\\python.exe",
                trainingScript.toString(),
                outputRoot.toString(),
                180,
                1
        );
        RecommendationTrainingRequest request = new RecommendationTrainingRequest(101L);

        RecommendationTrainingRun first = service.runTraining(
                request,
                "tenant-demo",
                "17870171303",
                "APR-MTP-INTEGRATION-001",
                "IDEM-MTP-INTEGRATION-001"
        );
        RecommendationTrainingRun replay = service.runTraining(
                request,
                "tenant-demo",
                "17870171303",
                "APR-MTP-INTEGRATION-001",
                "IDEM-MTP-INTEGRATION-001"
        );

        assertEquals("succeeded", first.getStatus());
        assertEquals(first.getRunUid(), replay.getRunUid());
        assertNotNull(first.getModelDigestSha256());
        assertEquals(64, first.getModelDigestSha256().length());
        JsonNode metrics = new ObjectMapper().readTree(first.getMetricsJson());
        assertTrue(metrics.path("test_metrics").path("auc").asDouble() > 0.80);
        assertTrue(first.getArtifactReference().startsWith("mlops://recommendation-runs/"));
    }

    /** 根据 Maven 当前工作目录解析模块内训练脚本。 */
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

        private final RecommendationDatasetReference dataset = new RecommendationDatasetReference(
                101L,
                "tenant-demo",
                "XnetDataOps",
                "recommendation-dcn-integration-v2",
                "0fc718caea3ea647b96ee806cbf3882f059967a5a1494675e36f54128a6f5054",
                "b6053fc2e5957223dec58243eabc9d3624d4bbdde2e58405bef0d87c71ebffdb",
                "ready"
        );
        private final Map<String, RecommendationTrainingRun> runsByIdempotency = new HashMap<>();
        private final Map<String, RecommendationTrainingRun> runsByUid = new HashMap<>();

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
