package com.synapxnet.mlopsmtpservice.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class RecommendationTrainingService {

    private static final Pattern AUDIT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{6,128}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,128}$");
    private static final List<String> EXPORT_COLUMNS = List.of(
            "product_version",
            "event_key",
            "user_key",
            "item_key",
            "behavior_type",
            "user_type",
            "user_sex",
            "user_manufacturer_type",
            "user_source",
            "item_type",
            "item_category_key",
            "item_tag_set_key",
            "item_duration_seconds",
            "behavior_duration_ms",
            "read_percent",
            "like_status",
            "label",
            "event_date",
            "dataset_split"
    );

    private final RecommendationTrainingMapper trainingMapper;
    private final ObjectMapper objectMapper;
    private final String recommendationJdbcUrl;
    private final String recommendationUsername;
    private final String recommendationPassword;
    private final String pythonExecutable;
    private final Path trainingScript;
    private final Path outputRoot;
    private final int timeoutSeconds;
    private final int epochs;

    /** 注入训练审计、DataOps 只读连接与 CPU 训练运行时配置。 */
    public RecommendationTrainingService(
            RecommendationTrainingMapper trainingMapper,
            ObjectMapper objectMapper,
            @Value("${xnet.recommendation.jdbc-url}") String recommendationJdbcUrl,
            @Value("${xnet.recommendation.username}") String recommendationUsername,
            @Value("${xnet.recommendation.password}") String recommendationPassword,
            @Value("${xnet.recommendation-training.python}") String pythonExecutable,
            @Value("${xnet.recommendation-training.script}") String trainingScript,
            @Value("${xnet.recommendation-training.output-root}") String outputRoot,
            @Value("${xnet.recommendation-training.timeout-seconds}") int timeoutSeconds,
            @Value("${xnet.recommendation-training.epochs}") int epochs
    ) {
        this.trainingMapper = trainingMapper;
        this.objectMapper = objectMapper;
        this.recommendationJdbcUrl = recommendationJdbcUrl;
        this.recommendationUsername = recommendationUsername;
        this.recommendationPassword = recommendationPassword;
        this.pythonExecutable = pythonExecutable;
        this.trainingScript = Path.of(trainingScript).toAbsolutePath().normalize();
        this.outputRoot = Path.of(outputRoot).toAbsolutePath().normalize();
        this.timeoutSeconds = timeoutSeconds;
        this.epochs = epochs;
    }

    /** 执行租户隔离、发布校验、数据导出、DCN 训练和审计写入闭环。 */
    public RecommendationTrainingRun runTraining(
            RecommendationTrainingRequest request,
            String tenantUid,
            String userId,
            String approvalId,
            String idempotencyKey
    ) {
        validateRequest(request, tenantUid, userId, approvalId, idempotencyKey);
        RecommendationTrainingRun replay = trainingMapper.findByIdempotencyKey(idempotencyKey);
        if (replay != null) {
            return validateReplay(replay, request, tenantUid, userId, approvalId);
        }
        RecommendationDatasetReference dataset = trainingMapper.findDataset(request.datasetId(), tenantUid);
        validateDataset(dataset);
        PublishedProductContract publishedProduct = readPublishedProduct(dataset.getProductVersion());
        validatePublishedContract(dataset, publishedProduct);

        RecommendationTrainingRun run = createRunningRecord(
                request,
                tenantUid,
                userId,
                approvalId,
                idempotencyKey,
                dataset
        );
        trainingMapper.insertRun(run);
        try {
            Path runDirectory = resolveRunDirectory(run.getRunUid());
            Path csvPath = exportTrainingProduct(runDirectory, publishedProduct);
            JsonNode metrics = executeTrainer(runDirectory, csvPath, publishedProduct);
            run.setModelDigestSha256(metrics.path("model_sha256").asText());
            run.setMetricsJson(objectMapper.writeValueAsString(metrics));
            run.setCompletedAt(new Date());
            if (trainingMapper.completeRun(run) != 1) {
                throw new IllegalStateException("训练成功状态写入失败");
            }
            return trainingMapper.findByRunUid(run.getRunUid());
        } catch (Exception exception) {
            run.setErrorSummary(truncate(exception.getMessage(), 1_000));
            run.setCompletedAt(new Date());
            trainingMapper.failRun(run);
            throw new IllegalStateException("推荐 DCN 训练失败: " + run.getErrorSummary(), exception);
        }
    }

    /** 按租户读取最近 100 条推荐训练运行。 */
    public List<RecommendationTrainingRun> listRuns(String tenantUid) {
        validateIdentity("X-Tenant-Uid", tenantUid);
        return trainingMapper.findRecentByTenant(tenantUid);
    }

    /** 校验训练请求及可信身份和审计头。 */
    private void validateRequest(
            RecommendationTrainingRequest request,
            String tenantUid,
            String userId,
            String approvalId,
            String idempotencyKey
    ) {
        if (request == null || request.datasetId() <= 0) {
            throw new IllegalArgumentException("datasetId 必须是正整数");
        }
        validateIdentity("X-Tenant-Uid", tenantUid);
        validateIdentity("X-User-Id", userId);
        validateAuditId("X-Approval-Id", approvalId);
        validateAuditId("Idempotency-Key", idempotencyKey);
    }

    /** 校验可信身份头的格式。 */
    private void validateIdentity(String name, String value) {
        if (value == null || !AUDIT_ID_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(name + " 缺失或格式不合法");
        }
    }

    /** 校验审批号和幂等键的格式。 */
    private void validateAuditId(String name, String value) {
        if (value == null || !AUDIT_ID_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(name + " 缺失或格式不合法");
        }
    }

    /** 校验幂等重放参数必须与原始请求完全一致。 */
    private RecommendationTrainingRun validateReplay(
            RecommendationTrainingRun replay,
            RecommendationTrainingRequest request,
            String tenantUid,
            String userId,
            String approvalId
    ) {
        if (!Long.valueOf(request.datasetId()).equals(replay.getDatasetId())
                || !tenantUid.equals(replay.getTenantUid())
                || !userId.equals(replay.getUserId())
                || !approvalId.equals(replay.getApprovalId())) {
            throw new IllegalArgumentException("幂等键已被不同训练请求使用");
        }
        if (!"succeeded".equals(replay.getStatus())) {
            throw new IllegalStateException("相同幂等训练仍在运行或此前失败");
        }
        return replay;
    }

    /** 校验数据集来自 DataOps 且处于可训练状态。 */
    private void validateDataset(RecommendationDatasetReference dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("当前租户无权访问该数据集");
        }
        if (!"XnetDataOps".equals(dataset.getSourcePlatform())) {
            throw new IllegalArgumentException("推荐训练只接受 XnetDataOps 数据产品");
        }
        if (!"ready".equals(dataset.getImportStatus())) {
            throw new IllegalArgumentException("数据集尚未完成导入验证");
        }
        if (!VERSION_PATTERN.matcher(dataset.getProductVersion()).matches()) {
            throw new IllegalArgumentException("数据产品版本格式不合法");
        }
    }

    /** 从 PostgreSQL 注册表读取已发布数据产品契约。 */
    private PublishedProductContract readPublishedProduct(String productVersion) {
        String sql = """
                SELECT product_version, schema_digest_sha256, artifact_digest_sha256, row_count
                FROM recommendation_curated.data_product_registry
                WHERE product_version=? AND status='published'
                """;
        try (Connection connection = openRecommendationConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("DataOps 数据产品未发布或已下线");
                }
                return new PublishedProductContract(
                        resultSet.getString("product_version"),
                        resultSet.getString("schema_digest_sha256"),
                        resultSet.getString("artifact_digest_sha256"),
                        resultSet.getLong("row_count")
                );
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("读取 DataOps 发布契约失败", exception);
        }
    }

    /** 比较 MLOps 导入快照与 DataOps 当前发布契约。 */
    private void validatePublishedContract(
            RecommendationDatasetReference dataset,
            PublishedProductContract product
    ) {
        if (!product.schemaDigestSha256().equals(dataset.getSchemaDigestSha256())) {
            throw new IllegalStateException("MLOps Schema 摘要与 DataOps 发布记录不一致");
        }
        if (!product.artifactDigestSha256().equals(dataset.getArtifactDigestSha256())) {
            throw new IllegalStateException("MLOps 制品摘要与 DataOps 发布记录不一致");
        }
    }

    /** 创建运行中的推荐训练审计实体。 */
    private RecommendationTrainingRun createRunningRecord(
            RecommendationTrainingRequest request,
            String tenantUid,
            String userId,
            String approvalId,
            String idempotencyKey,
            RecommendationDatasetReference dataset
    ) {
        RecommendationTrainingRun run = new RecommendationTrainingRun();
        run.setRunUid("REC-" + UUID.randomUUID().toString().replace("-", ""));
        run.setDatasetId(request.datasetId());
        run.setTenantUid(tenantUid);
        run.setUserId(userId);
        run.setProductVersion(dataset.getProductVersion());
        run.setApprovalId(approvalId);
        run.setIdempotencyKey(idempotencyKey);
        run.setStatus("running");
        run.setSchemaDigestSha256(dataset.getSchemaDigestSha256());
        run.setArtifactDigestSha256(dataset.getArtifactDigestSha256());
        run.setArtifactReference("mlops://recommendation-runs/" + run.getRunUid());
        run.setStartedAt(new Date());
        return run;
    }

    /** 将运行标识解析为受 outputRoot 约束的物理目录。 */
    private Path resolveRunDirectory(String runUid) throws Exception {
        Path runDirectory = outputRoot.resolve(runUid).normalize();
        if (!runDirectory.startsWith(outputRoot)) {
            throw new IllegalStateException("训练输出路径越界");
        }
        Files.createDirectories(runDirectory);
        return runDirectory;
    }

    /** 从 DataOps 已发布层导出 UTF-8 CSV 训练制品。 */
    private Path exportTrainingProduct(
            Path runDirectory,
            PublishedProductContract product
    ) throws Exception {
        Path csvPath = runDirectory.resolve("training.csv");
        String selectedColumns = String.join(", ", EXPORT_COLUMNS);
        String sql = "SELECT " + selectedColumns + " FROM recommendation_curated.dcn_training " +
                "WHERE product_version=? ORDER BY event_key";
        long exportedRows = 0;
        try (Connection connection = openRecommendationConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = Files.newBufferedWriter(
                     csvPath,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING
             )) {
            statement.setString(1, product.productVersion());
            statement.setFetchSize(1_000);
            writer.write(String.join(",", EXPORT_COLUMNS));
            writer.newLine();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    List<String> values = new ArrayList<>(EXPORT_COLUMNS.size());
                    for (String column : EXPORT_COLUMNS) {
                        values.add(escapeCsv(resultSet.getString(column)));
                    }
                    writer.write(String.join(",", values));
                    writer.newLine();
                    exportedRows++;
                }
            }
        }
        if (exportedRows != product.rowCount()) {
            throw new IllegalStateException(
                    "导出记录数与 DataOps 注册表不一致: " + exportedRows + "/" + product.rowCount()
            );
        }
        return csvPath;
    }

    /** 按 RFC 4180 规则转义单个 CSV 字段。 */
    private String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",")
                || safeValue.contains("\"")
                || safeValue.contains("\n")
                || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    /** 启动固定脚本和固定参数的 CPU 训练子进程并读取指标。 */
    private JsonNode executeTrainer(
            Path runDirectory,
            Path csvPath,
            PublishedProductContract product
    ) throws Exception {
        if (!Files.isRegularFile(trainingScript)) {
            throw new IllegalStateException("推荐训练脚本不存在: " + trainingScript);
        }
        Path modelDirectory = runDirectory.resolve("model");
        Path logPath = runDirectory.resolve("training.log");
        Files.createDirectories(modelDirectory);
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                trainingScript.toString(),
                "--input-csv", csvPath.toString(),
                "--output-dir", modelDirectory.toString(),
                "--product-version", product.productVersion(),
                "--expected-schema-digest", product.schemaDigestSha256(),
                "--expected-artifact-digest", product.artifactDigestSha256(),
                "--epochs", String.valueOf(epochs),
                "--batch-size", "1024",
                "--cpu-threads", "4"
        );
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logPath.toFile());
        processBuilder.environment().put("CUDA_VISIBLE_DEVICES", "");
        processBuilder.environment().put("TORCH_DEVICE_BACKEND_AUTOLOAD", "0");
        Process process = processBuilder.start();
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            throw new IllegalStateException("训练超过超时限制: " + Duration.ofSeconds(timeoutSeconds));
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("训练进程退出码为 " + process.exitValue() + ": " + readLogTail(logPath));
        }
        Path metricsPath = modelDirectory.resolve("metrics.json");
        if (!Files.isRegularFile(metricsPath)) {
            throw new IllegalStateException("训练完成但缺少 metrics.json");
        }
        return objectMapper.readTree(metricsPath.toFile());
    }

    /** 读取受限长度的训练日志尾部用于错误定位。 */
    private String readLogTail(Path logPath) throws Exception {
        String content = Files.exists(logPath) ? Files.readString(logPath, StandardCharsets.UTF_8) : "无日志";
        return content.length() <= 4_000 ? content : content.substring(content.length() - 4_000);
    }

    /** 打开 DataOps 推荐数据产品的只读 PostgreSQL 连接。 */
    private Connection openRecommendationConnection() throws Exception {
        if (recommendationPassword == null || recommendationPassword.isBlank()) {
            throw new IllegalStateException("RECOMMENDATION_MLOPS_DB_PASSWORD 未配置");
        }
        return DriverManager.getConnection(
                recommendationJdbcUrl,
                recommendationUsername,
                recommendationPassword
        );
    }

    /** 截断失败摘要以限制审计字段大小。 */
    private String truncate(String value, int maxLength) {
        String safeValue = value == null ? "unknown" : value;
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }
}
