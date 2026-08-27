package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通过后端隔离网络调用量化训练和模拟盘运行时。
 */
@Component
public class QuantitativeRuntimeClient {

    private static final String DEPLOYMENT_UID = "deploy_quant_ashare_research";

    private final URI baseUri;
    private final String serviceToken;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建量化运行时客户端并固定连接超时。
     *
     * @param runtimeUrl 内部运行时基地址
     * @param serviceToken 内部服务令牌
     * @param objectMapper JSON 序列化器
     */
    @Autowired
    public QuantitativeRuntimeClient(
            @Value("${xnet.quantitative.runtime-url}") String runtimeUrl,
            @Value("${xnet.quantitative.runtime-token}") String serviceToken,
            ObjectMapper objectMapper) {
        this(
                runtimeUrl,
                serviceToken,
                objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    /**
     * 创建可注入 HTTP 客户端的量化运行时客户端，供测试复用。
     *
     * @param runtimeUrl 内部运行时基地址
     * @param serviceToken 内部服务令牌
     * @param objectMapper JSON 序列化器
     * @param httpClient Java HTTP 客户端
     */
    QuantitativeRuntimeClient(
            String runtimeUrl,
            String serviceToken,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.baseUri = validateBaseUri(runtimeUrl);
        this.serviceToken = serviceToken == null ? "" : serviceToken.trim();
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * 判断部署标识是否属于真实 A 股研究运行时。
     *
     * @param deploymentUid 模型部署标识
     * @return 属于量化研究部署时返回 true
     */
    public boolean supports(String deploymentUid) {
        return DEPLOYMENT_UID.equals(deploymentUid);
    }

    /** 返回量化运行时和 DataOps 数据产品健康证据。 */
    public Map<String, Object> health() {
        return get("/health");
    }

    /** 返回冻结旧基线的真实评估指标。 */
    public Map<String, Object> baselineMetrics() {
        return get("/metrics?stage=baseline");
    }

    /** 返回最近一次受审批候选训练的真实指标。 */
    public Map<String, Object> candidateMetrics() {
        return get("/metrics?stage=candidate");
    }

    /** 返回模拟盘活动修订、候选修订、流量和摘要。 */
    public Map<String, Object> deployment() {
        return get("/deployment");
    }

    /**
     * 启动受审批候选训练并返回实际试验和质量门结果。
     *
     * @param idempotencyKey 幂等键
     * @param approvalId 审批号
     * @param targetRevision 目标修订
     * @param trialCount 受限试验数量
     * @return 真实训练指标
     */
    public Map<String, Object> train(
            String idempotencyKey,
            String approvalId,
            long targetRevision,
            int trialCount) {
        return post("/train", Map.of(
                "idempotencyKey", idempotencyKey,
                "approvalId", approvalId,
                "targetRevision", targetRevision,
                "trialCount", trialCount));
    }

    /**
     * 应用不超过 50% 的模拟盘候选灰度。
     *
     * @param approvalId 审批号
     * @param targetRevision 候选修订
     * @param trafficPercent 模拟信号流比例
     * @return 持久化部署状态
     */
    public Map<String, Object> canary(String approvalId, long targetRevision, int trafficPercent) {
        return post("/canary", Map.of(
                "approvalId", approvalId,
                "targetRevision", targetRevision,
                "trafficPercent", trafficPercent));
    }

    /**
     * 将已完成灰度的候选提升到全部模拟信号流。
     *
     * @param approvalId 审批号
     * @param targetRevision 候选修订
     * @return 持久化部署状态
     */
    public Map<String, Object> promote(String approvalId, long targetRevision) {
        return post("/promote", Map.of("approvalId", approvalId, "targetRevision", targetRevision));
    }

    /**
     * 将模拟信号流回滚到已审批旧修订。
     *
     * @param approvalId 审批号
     * @param targetRevision 旧修订
     * @return 持久化部署状态
     */
    public Map<String, Object> rollback(String approvalId, long targetRevision) {
        return post("/rollback", Map.of("approvalId", approvalId, "targetRevision", targetRevision));
    }

    /**
     * 对当前活动修订执行真实测试集合推理探针。
     *
     * @param sampleLimit 最大测试样本数
     * @return 推理错误率、延迟和模型摘要证据
     */
    public Map<String, Object> probe(int sampleLimit) {
        return post("/probe", Map.of("sampleLimit", sampleLimit));
    }

    /**
     * 发起已认证 GET 请求并解析 JSON 对象。
     *
     * @param path 相对接口路径
     * @return 结构化响应
     */
    private Map<String, Object> get(String path) {
        HttpRequest request = request(path).GET().build();
        return send(request);
    }

    /**
     * 发起已认证 POST 请求并解析 JSON 对象。
     *
     * @param path 相对接口路径
     * @param payload JSON 请求对象
     * @return 结构化响应
     */
    private Map<String, Object> post(String path, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = request(path)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return send(request);
        } catch (AgentContractException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable("量化运行时请求序列化失败", exception);
        }
    }

    /**
     * 创建带服务令牌和固定超时的 HTTP 请求构建器。
     *
     * @param path 相对接口路径
     * @return HTTP 请求构建器
     */
    private HttpRequest.Builder request(String path) {
        if (serviceToken.length() < 32) {
            throw new AgentContractException(503, "RUNTIME_NOT_CONFIGURED", "量化运行时服务令牌未配置");
        }
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(Duration.ofSeconds(120))
                .header("X-Quant-Runtime-Token", serviceToken)
                .header("Accept", "application/json");
    }

    /**
     * 发送 HTTP 请求并把非成功响应转换为可审计领域错误。
     *
     * @param request 已构造请求
     * @return 结构化响应对象
     */
    private Map<String, Object> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            Map<String, Object> payload = objectMapper.readValue(
                    response.body(),
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                Object error = payload.getOrDefault("error", "量化运行时拒绝请求");
                throw new AgentContractException(
                        response.statusCode() == 400 || response.statusCode() == 412 ? 412 : 502,
                        "QUANTITATIVE_RUNTIME_REJECTED",
                        String.valueOf(error));
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        } catch (AgentContractException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("量化运行时调用被中断", exception);
        } catch (Exception exception) {
            throw unavailable("量化运行时不可用", exception);
        }
    }

    /**
     * 校验运行时基地址仅使用内部 HTTP 或 HTTPS 协议。
     *
     * @param runtimeUrl 待校验基地址
     * @return 规范化 URI
     */
    private URI validateBaseUri(String runtimeUrl) {
        URI value = URI.create(runtimeUrl == null ? "" : runtimeUrl.trim());
        if (!("http".equalsIgnoreCase(value.getScheme()) || "https".equalsIgnoreCase(value.getScheme()))
                || value.getHost() == null) {
            throw new IllegalArgumentException("量化运行时地址无效");
        }
        String normalized = value.toString().endsWith("/") ? value.toString() : value + "/";
        return URI.create(normalized);
    }

    /**
     * 创建不泄露 URL、令牌和响应正文的运行时不可用错误。
     *
     * @param message 固定错误摘要
     * @param cause 内部异常原因
     * @return 结构化 Agent 工具错误
     */
    private AgentContractException unavailable(String message, Exception cause) {
        return new AgentContractException(502, "QUANTITATIVE_RUNTIME_UNAVAILABLE", message);
    }
}
