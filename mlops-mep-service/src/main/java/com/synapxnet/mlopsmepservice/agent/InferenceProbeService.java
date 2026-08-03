package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对授权 Fixture 执行输入契约检查和真实 HTTP 推理请求，并持久化聚合结果。
 */
@Service
public class InferenceProbeService {

    private static final String RISK_DATASET = "fixture://goai/risk-120-v1";
    private final DeploymentEvidenceService deploymentEvidenceService;
    private final AgentMepMapper agentMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final WebClient webClient;

    /**
     * 创建推理探针服务。
     *
     * @param deploymentEvidenceService 部署读取服务
     * @param agentMapper 探针与契约 Mapper
     * @param objectMapper JSON 解析和摘要序列化器
     * @param resourceLoader classpath Fixture 加载器
     * @param webClientBuilder 有连接池和取消能力的 HTTP Client Builder
     */
    public InferenceProbeService(
            DeploymentEvidenceService deploymentEvidenceService,
            AgentMepMapper agentMapper,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            WebClient.Builder webClientBuilder) {
        this.deploymentEvidenceService = deploymentEvidenceService;
        this.agentMapper = agentMapper;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.webClient = webClientBuilder.codecs(configurer ->
                configurer.defaultCodecs().maxInMemorySize(256 * 1024)).build();
    }

    /**
     * 执行有界、授权且可审计的推理探针。
     *
     * @param arguments 探针参数
     * @param context 已验证 Agent 上下文
     * @return 聚合探针结果
     */
    public MepAgentDtos.InferenceProbeResult probe(
            MepAgentDtos.InferenceProbeArguments arguments,
            AgentContract.RequestContext context) {
        ProbeLimits limits = validate(arguments);
        ModelDeployment deployment = deploymentEvidenceService.requireDeployment(arguments.deploymentUid());
        Long activeRevision = deployment.getActiveRevision();
        ModelContract contract = agentMapper.findContract(deployment.getModelUid(), deployment.getModelVersion());
        if (contract == null || activeRevision == null) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "部署缺少活动修订或模型输入契约");
        }
        return executeProbe(arguments, context, limits, deployment, contract, activeRevision);
    }

    /**
     * 按尚未提交为活动版本的目标修订执行真实探针。
     *
     * @param arguments 探针参数
     * @param context 已验证 Agent 上下文
     * @param revision 回滚目标修订
     * @return 使用目标修订契约和修订号生成的聚合结果
     */
    public MepAgentDtos.InferenceProbeResult probeAgainstRevision(
            MepAgentDtos.InferenceProbeArguments arguments,
            AgentContract.RequestContext context,
            DeploymentRevision revision) {
        ProbeLimits limits = validate(arguments);
        ModelDeployment deployment = deploymentEvidenceService.requireDeployment(arguments.deploymentUid());
        if (revision == null || revision.getRevisionNumber() == null
                || revision.getDeploymentUid() == null
                || !deployment.getUid().equals(revision.getDeploymentUid())
                || revision.getModelContractUid() == null || revision.getModelContractUid().isBlank()) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订缺少有效模型输入契约");
        }
        ModelContract contract = agentMapper.findContractByUid(revision.getModelContractUid());
        if (contract == null) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订模型输入契约不存在");
        }
        return executeProbe(arguments, context, limits, deployment, contract, revision.getRevisionNumber());
    }

    /**
     * 使用指定契约和修订号执行探针、计算摘要并持久化结果。
     *
     * @param arguments 探针参数
     * @param context 已验证 Agent 上下文
     * @param limits 已校验探针预算
     * @param deployment 当前部署和真实端点
     * @param contract 本次验证采用的模型契约
     * @param revisionNumber 本次验证采用的修订号
     * @return 持久化后的聚合探针结果
     */
    private MepAgentDtos.InferenceProbeResult executeProbe(
            MepAgentDtos.InferenceProbeArguments arguments,
            AgentContract.RequestContext context,
            ProbeLimits limits,
            ModelDeployment deployment,
            ModelContract contract,
            Long revisionNumber) {
        ProbeDataset dataset = loadDataset(arguments.testDatasetRef(), context.workspaceId());
        List<ProbeSample> samples = dataset.samples().stream().limit(limits.sampleLimit()).toList();
        Instant startedAt = Instant.now();
        ProbeAggregation aggregation = dataset.inputDimension() == contract.getInputDimension()
                ? invokeEndpoint(deployment, samples, limits.timeoutMs())
                : contractMismatch(samples);
        Instant completedAt = Instant.now();
        String digest = digest(deployment, revisionNumber, dataset, aggregation, startedAt, completedAt);
        String probeUid = "probe_" + UUID.randomUUID().toString().replace("-", "").substring(0, 26);
        MepAgentDtos.ContractStatus status = dataset.inputDimension() == contract.getInputDimension()
                ? MepAgentDtos.ContractStatus.MATCHED : MepAgentDtos.ContractStatus.MISMATCHED;
        MepAgentDtos.InferenceProbeResult result = new MepAgentDtos.InferenceProbeResult(
                probeUid, deployment.getUid(), revisionNumber, samples.size(),
                aggregation.successCount(), aggregation.errorCount(), rate(aggregation.errorCount(), samples.size()),
                percentile(aggregation.latenciesMs(), 0.50), percentile(aggregation.latenciesMs(), 0.95),
                dataset.inputDimension(), contract.getInputDimension(), status, aggregation.failures(),
                startedAt, completedAt, digest);
        persist(result, arguments.testDatasetRef(), context);
        return result;
    }

    /**
     * 校验数据集引用、样本数和总超时，禁止任意文件路径与 URL。
     *
     * @param arguments 探针参数
     * @return 规范化限制
     */
    private ProbeLimits validate(MepAgentDtos.InferenceProbeArguments arguments) {
        if (arguments == null || arguments.deploymentUid() == null || arguments.deploymentUid().isBlank()
                || !RISK_DATASET.equals(arguments.testDatasetRef())) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "仅允许已登记的脱敏 Fixture 引用");
        }
        int sampleLimit = arguments.sampleLimit() == null ? 12 : arguments.sampleLimit();
        int timeoutMs = arguments.timeoutMs() == null ? 30_000 : arguments.timeoutMs();
        if (sampleLimit < 1 || sampleLimit > 100 || timeoutMs < 1_000 || timeoutMs > 60_000) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "sampleLimit 或 timeoutMs 超出允许范围");
        }
        return new ProbeLimits(sampleLimit, timeoutMs);
    }

    /**
     * 从 classpath 读取已登记 Fixture 并验证 Workspace 授权。
     *
     * @param datasetRef 数据集引用
     * @param workspaceId Workspace ID
     * @return 脱敏探针数据集
     */
    private ProbeDataset loadDataset(String datasetRef, String workspaceId) {
        try {
            Resource resource = resourceLoader.getResource("classpath:goai/probe/risk-120-v1.json");
            ProbeDataset dataset = objectMapper.readValue(resource.getInputStream(), ProbeDataset.class);
            if (!datasetRef.equals(dataset.datasetRef()) || !dataset.workspaceIds().contains(workspaceId)) {
                throw new AgentContractException(403, "PERMISSION_DENIED", "Workspace 无权使用该探针 Fixture");
            }
            if (dataset.samples().stream().anyMatch(sample -> sample.features().size() != dataset.inputDimension())) {
                throw new AgentContractException(500, "INTERNAL_ERROR", "探针 Fixture 维度不一致");
            }
            return dataset;
        } catch (AgentContractException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "探针 Fixture 无法读取");
        }
    }

    /**
     * 向真实部署端点逐样本发送请求，基于实际耗时和 HTTP 结果分类。
     *
     * @param deployment 当前部署
     * @param samples 授权样本
     * @param timeoutMs 总超时
     * @return 聚合结果
     */
    private ProbeAggregation invokeEndpoint(
            ModelDeployment deployment,
            List<ProbeSample> samples,
            int timeoutMs) {
        long deadline = System.nanoTime() + Duration.ofMillis(timeoutMs).toNanos();
        List<Double> latencies = new ArrayList<>();
        Map<String, FailureCounter> failures = new LinkedHashMap<>();
        int success = 0;
        for (ProbeSample sample : samples) {
            long remainingMs = Math.max(1L, (deadline - System.nanoTime()) / 1_000_000L);
            if (remainingMs <= 1L) {
                addFailure(failures, "PROBE_TIMEOUT", sample.sampleRef());
                continue;
            }
            long start = System.nanoTime();
            try {
                webClient.post().uri(deployment.getEndpoint())
                        .bodyValue(new ProbePayload(sample.sampleRef(), sample.features()))
                        .retrieve().toBodilessEntity()
                        .block(Duration.ofMillis(Math.min(remainingMs, 10_000L)));
                success++;
            } catch (WebClientResponseException exception) {
                addFailure(failures,
                        exception.getStatusCode().is4xxClientError() ? "MODEL_REJECTED_INPUT" : "MODEL_UPSTREAM_ERROR",
                        sample.sampleRef());
            } catch (RuntimeException exception) {
                addFailure(failures, "NETWORK_OR_TIMEOUT", sample.sampleRef());
            } finally {
                latencies.add((System.nanoTime() - start) / 1_000_000.0);
            }
        }
        List<MepAgentDtos.ProbeFailure> failureList = failures.entrySet().stream()
                .map(entry -> new MepAgentDtos.ProbeFailure(
                        entry.getKey(), entry.getValue().count(), entry.getValue().firstSampleRef()))
                .toList();
        return new ProbeAggregation(success, samples.size() - success, latencies, failureList);
    }

    /**
     * 输入维度不匹配时在发送前明确失败，避免向端点提交无效样本。
     *
     * @param samples 探针样本
     * @return 全部归类为 CONTRACT_MISMATCH 的聚合结果
     */
    private ProbeAggregation contractMismatch(List<ProbeSample> samples) {
        String first = samples.isEmpty() ? null : samples.get(0).sampleRef();
        return new ProbeAggregation(
                0, samples.size(), List.of(),
                List.of(new MepAgentDtos.ProbeFailure("CONTRACT_MISMATCH", samples.size(), first)));
    }

    /** 增加失败类别计数并保留首个脱敏样本引用。 */
    private void addFailure(Map<String, FailureCounter> failures, String category, String sampleRef) {
        FailureCounter current = failures.get(category);
        failures.put(category, current == null
                ? new FailureCounter(1, sampleRef)
                : new FailureCounter(current.count() + 1, current.firstSampleRef()));
    }

    /** 计算 0 到 1 错误率。 */
    private BigDecimal rate(int errors, int total) {
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(errors)
                .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }

    /**
     * 使用 nearest-rank 计算真实请求耗时百分位。
     *
     * @param values 实际毫秒耗时
     * @param percentile 百分位 0 到 1
     * @return 毫秒值，无请求时返回 null
     */
    private BigDecimal percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return BigDecimal.valueOf(sorted.get(index)).setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * 计算不含原始样本的探针结果摘要。
     *
     * @param deployment 部署
     * @param revisionNumber 本次验证采用的修订号
     * @param dataset 数据集元信息
     * @param aggregation 聚合结果
     * @param startedAt 开始时间
     * @param completedAt 完成时间
     * @return SHA-256 十六进制摘要
     */
    private String digest(
            ModelDeployment deployment,
            Long revisionNumber,
            ProbeDataset dataset,
            ProbeAggregation aggregation,
            Instant startedAt,
            Instant completedAt) {
        try {
            String value = objectMapper.writeValueAsString(Map.of(
                    "deploymentUid", deployment.getUid(),
                    "revision", revisionNumber,
                    "datasetRef", dataset.datasetRef(),
                    "inputDimension", dataset.inputDimension(),
                    "successCount", aggregation.successCount(),
                    "errorCount", aggregation.errorCount(),
                    "startedAt", startedAt.toString(),
                    "completedAt", completedAt.toString()));
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "无法生成探针摘要");
        }
    }

    /** 将聚合探针结果持久化，不保存 features 或模型输出。 */
    private void persist(
            MepAgentDtos.InferenceProbeResult result,
            String datasetRef,
            AgentContract.RequestContext context) {
        InferenceProbe probe = new InferenceProbe();
        probe.setUid(result.probeUid());
        probe.setDeploymentUid(result.deploymentUid());
        probe.setRevisionNumber(result.revisionNumber());
        probe.setTestDatasetRef(datasetRef);
        probe.setSampleCount(result.sampleCount());
        probe.setSuccessCount(result.successCount());
        probe.setErrorCount(result.errorCount());
        probe.setErrorRate(result.errorRate());
        probe.setP50Ms(result.p50Ms());
        probe.setP95Ms(result.p95Ms());
        probe.setInputDimension(result.observedInputDimension());
        probe.setContractStatus(result.contractStatus().name());
        probe.setResultDigest(result.resultDigest());
        probe.setIncidentId(context.incidentId());
        probe.setTraceId(context.traceId());
        probe.setStartedAt(LocalDateTime.ofInstant(result.startedAt(), ZoneOffset.UTC));
        probe.setCompletedAt(LocalDateTime.ofInstant(result.completedAt(), ZoneOffset.UTC));
        agentMapper.insertProbe(probe);
    }

    /** 表示规范化探针限制。 */
    private record ProbeLimits(int sampleLimit, int timeoutMs) {
    }

    /** 表示 classpath 中登记的脱敏探针数据集。 */
    private record ProbeDataset(
            String datasetRef,
            List<String> workspaceIds,
            int inputDimension,
            List<ProbeSample> samples) {
    }

    /** 表示仅在内存中短暂使用的脱敏探针样本。 */
    private record ProbeSample(String sampleRef, List<Double> features) {
    }

    /** 表示发送到模型端点的请求体。 */
    private record ProbePayload(String sampleRef, List<Double> features) {
    }

    /** 表示失败类别计数。 */
    private record FailureCounter(int count, String firstSampleRef) {
    }

    /** 表示探针执行后的内部聚合。 */
    private record ProbeAggregation(
            int successCount,
            int errorCount,
            List<Double> latenciesMs,
            List<MepAgentDtos.ProbeFailure> failures) {
    }
}
