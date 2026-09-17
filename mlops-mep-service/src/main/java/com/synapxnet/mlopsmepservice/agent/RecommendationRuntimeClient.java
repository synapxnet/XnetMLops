/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过固定部署地址调用 DCN 推荐运行时，并把健康和推理响应转换为可审计证据。
 */
@Component
public class RecommendationRuntimeClient {

    private static final String DEPLOYMENT_UID = "deploy_recommendation_prod";
    private static final String PROBE_DATASET_REF = "staging://goai/recommendation-dcn-v1/probe";
    private static final int EXPECTED_CANDIDATE_COUNT = 8;

    private final URI baseUri;
    private final String expectedAlgorithmId;
    private final String expectedProductVersion;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建推荐运行时客户端并固定连接超时与预期模型身份。
     *
     * @param runtimeUrl 推荐服务基地址
     * @param expectedAlgorithmId 预期算法版本
     * @param expectedProductVersion 预期模型产品版本
     * @param objectMapper JSON 序列化器
     */
    @Autowired
    public RecommendationRuntimeClient(
            @Value("${xnet.recommendation.runtime-url}") String runtimeUrl,
            @Value("${xnet.recommendation.expected-algorithm-id}") String expectedAlgorithmId,
            @Value("${xnet.recommendation.expected-product-version}") String expectedProductVersion,
            ObjectMapper objectMapper) {
        this(
                runtimeUrl,
                expectedAlgorithmId,
                expectedProductVersion,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build());
    }

    /**
     * 创建可注入 HTTP 客户端的推荐运行时客户端，供测试复用。
     *
     * @param runtimeUrl 推荐服务基地址
     * @param expectedAlgorithmId 预期算法版本
     * @param expectedProductVersion 预期模型产品版本
     * @param objectMapper JSON 序列化器
     * @param httpClient Java HTTP 客户端
     */
    RecommendationRuntimeClient(
            String runtimeUrl,
            String expectedAlgorithmId,
            String expectedProductVersion,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.baseUri = validateBaseUri(runtimeUrl);
        this.expectedAlgorithmId = requireConfiguredText(expectedAlgorithmId, "推荐算法版本未配置");
        this.expectedProductVersion = requireConfiguredText(expectedProductVersion, "推荐产品版本未配置");
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * 判断部署标识是否属于真实推荐运行时。
     *
     * @param deploymentUid 模型部署标识
     * @return 属于固定推荐部署时返回 true
     */
    public boolean supports(String deploymentUid) {
        return DEPLOYMENT_UID.equals(deploymentUid);
    }

    /**
     * 判断探针数据集是否为固定脱敏推荐请求。
     *
     * @param datasetRef 数据集引用
     * @return 引用匹配时返回 true
     */
    public boolean supportsDataset(String datasetRef) {
        return PROBE_DATASET_REF.equals(datasetRef);
    }

    /**
     * 读取推荐运行时健康状态并校验模型身份。
     *
     * @return 已校验的健康证据
     */
    public Map<String, Object> health() {
        Map<String, Object> payload = getObject("health");
        requireEquals(payload, "status", "ready", "推荐运行时未就绪");
        requireEquals(payload, "algorithm_id", expectedAlgorithmId, "推荐算法版本不一致");
        requireEquals(payload, "product_version", expectedProductVersion, "推荐产品版本不一致");
        requireSha256(payload.get("model_digest_sha256"), "推荐模型摘要无效");
        requirePositiveInteger(payload.get("candidate_count"), EXPECTED_CANDIDATE_COUNT, "推荐候选数量不足");
        return Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    /**
     * 调用真实推荐接口并校验每个候选的算法版本、类型和标识。
     *
     * @param sampleLimit 探针最多验证的候选数量
     * @return 不包含原始用户请求的聚合证据
     */
    public Map<String, Object> probe(int sampleLimit) {
        int requestedSize = Math.max(1, Math.min(EXPECTED_CANDIDATE_COUNT, sampleLimit));
        Map<String, Object> health = health();
        Map<String, Object> requestBody = fixedProbeRequest(requestedSize);
        long startedNanos = System.nanoTime();
        List<Map<String, Object>> items = postList("recommend", requestBody);
        long latencyMs = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
        validateRecommendations(items, requestedSize);
        String responseDigest = digest(items);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", true);
        result.put("sampleCount", items.size());
        result.put("successCount", items.size());
        result.put("errorCount", 0);
        result.put("errorRate", 0.0);
        result.put("p95Ms", latencyMs);
        result.put("contractStatus", "MATCHED");
        result.put("algorithmId", expectedAlgorithmId);
        result.put("productVersion", expectedProductVersion);
        result.put("candidateCount", items.size());
        result.put("modelDigestSha256", health.get("model_digest_sha256"));
        result.put("approvalId", health.get("approval_id"));
        result.put("resultDigest", responseDigest);
        return Collections.unmodifiableMap(result);
    }

    /**
     * 连续执行固定脱敏请求并计算部署详情页需要的实时服务指标。
     *
     * @param requestCount 监控窗口内的请求次数
     * @return 不包含原始请求和推荐结果的聚合指标
     */
    public Map<String, Object> monitor(int requestCount) {
        int boundedRequestCount = Math.max(1, Math.min(50, requestCount));
        Map<String, Object> health = health();
        List<Long> latencies = new ArrayList<>(boundedRequestCount);
        int successCount = 0;
        int errorCount = 0;
        int candidateCount = 0;
        for (int index = 0; index < boundedRequestCount; index++) {
            long startedNanos = System.nanoTime();
            try {
                List<Map<String, Object>> items = postList(
                        "recommend",
                        fixedProbeRequest(EXPECTED_CANDIDATE_COUNT));
                validateRecommendations(items, EXPECTED_CANDIDATE_COUNT);
                candidateCount = items.size();
                successCount++;
            } catch (AgentContractException exception) {
                errorCount++;
            } finally {
                latencies.add(Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L));
            }
        }
        double successRate = Math.round((successCount * 10_000.0) / boundedRequestCount) / 100.0;
        double averageLatency = Math.round(latencies.stream().mapToLong(Long::longValue).average().orElse(0.0) * 100.0) / 100.0;
        boolean contractMatched = errorCount == 0 && candidateCount == EXPECTED_CANDIDATE_COUNT;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "LIVE_RECOMMENDATION_PROBE");
        result.put("status", contractMatched ? "HEALTHY" : "DEGRADED");
        result.put("requestCount", boundedRequestCount);
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("successRate", successRate);
        result.put("averageResponseTimeMs", averageLatency);
        result.put("p95ResponseTimeMs", percentile(latencies, 95));
        result.put("p99ResponseTimeMs", percentile(latencies, 99));
        result.put("candidateCount", candidateCount);
        result.put("contractStatus", contractMatched ? "MATCHED" : "DEGRADED");
        result.put("algorithmId", expectedAlgorithmId);
        result.put("productVersion", expectedProductVersion);
        result.put("modelDigestSha256", health.get("model_digest_sha256"));
        result.put("recordedAt", Instant.now().toString());
        return Collections.unmodifiableMap(result);
    }

    /**
     * 计算有界延迟样本的最近秩百分位值。
     *
     * @param values 延迟样本
     * @param percentile 百分位整数
     * @return 对应百分位的毫秒值
     */
    private long percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0L;
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = Math.max(0, (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1);
        return sorted.get(Math.min(index, sorted.size() - 1));
    }

    /**
     * 构建固定脱敏推荐请求，禁止调用方注入用户数据或任意参数。
     *
     * @param size 返回候选数量
     * @return 固定请求对象
     */
    private Map<String, Object> fixedProbeRequest(int size) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user_id", "430051532588974080");
        payload.put("source", "ios");
        payload.put("udid", "3bfd7f1d32724d32b73305d3c1e754cf");
        payload.put("data_type", "article");
        payload.put("size", size);
        payload.put("request_time", 1785403850937L);
        payload.put("recommend_scene", "all");
        payload.put("nav_id", "720001000000000022");
        payload.put("biz_ids", List.of());
        payload.put("current", 0);
        return Collections.unmodifiableMap(payload);
    }

    /**
     * 校验推荐响应数量和逐项契约。
     *
     * @param items 推荐候选列表
     * @param expectedCount 预期候选数量
     */
    private void validateRecommendations(List<Map<String, Object>> items, int expectedCount) {
        if (items.size() != expectedCount) {
            throw rejected("推荐响应候选数量不符合部署契约");
        }
        for (Map<String, Object> item : items) {
            Object itemId = item.get("item_id");
            if (!(itemId instanceof String value) || !value.matches("[0-9]{8,32}")) {
                throw rejected("推荐响应包含无效内容标识");
            }
            requireEquals(item, "data_type", "article", "推荐响应数据类型不一致");
            requireEquals(item, "algorithm_id", expectedAlgorithmId, "推荐响应算法版本不一致");
        }
    }

    /**
     * 发起健康检查请求并读取 JSON 对象。
     *
     * @param path 相对路径
     * @return JSON 对象
     */
    private Map<String, Object> getObject(String path) {
        HttpRequest request = request(path).GET().build();
        return sendObject(request);
    }

    /**
     * 发起推荐请求并读取 JSON 数组。
     *
     * @param path 相对路径
     * @param payload 固定请求对象
     * @return 推荐候选数组
     */
    private List<Map<String, Object>> postList(String path, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = request(path)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = send(request);
            List<Map<String, Object>> value = objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<Map<String, Object>>>() { });
            return List.copyOf(value);
        } catch (AgentContractException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable("推荐响应无法解析", exception);
        }
    }

    /**
     * 构建固定目标和超时的 HTTP 请求。
     *
     * @param path 相对路径
     * @return HTTP 请求构建器
     */
    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
    }

    /**
     * 发送 HTTP 请求并拒绝重定向、非成功状态和超大响应。
     *
     * @param request 已构造请求
     * @return UTF-8 HTTP 响应
     */
    private HttpResponse<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw rejected("推荐运行时拒绝请求");
            }
            if (response.body().getBytes(StandardCharsets.UTF_8).length > 256 * 1024) {
                throw rejected("推荐运行时响应超过大小限制");
            }
            return response;
        } catch (AgentContractException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("推荐运行时调用被中断", exception);
        } catch (Exception exception) {
            throw unavailable("推荐运行时不可用", exception);
        }
    }

    /**
     * 发送请求并解析 JSON 对象。
     *
     * @param request 已构造请求
     * @return JSON 对象
     */
    private Map<String, Object> sendObject(HttpRequest request) {
        try {
            HttpResponse<String> response = send(request);
            Map<String, Object> payload = objectMapper.readValue(
                    response.body(),
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            return Collections.unmodifiableMap(payload);
        } catch (AgentContractException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable("推荐健康响应无法解析", exception);
        }
    }

    /**
     * 校验固定运行时基地址，禁止查询、片段、用户信息和非 8000-9000 端口。
     *
     * @param runtimeUrl 待校验地址
     * @return 规范化 URI
     */
    private URI validateBaseUri(String runtimeUrl) {
        URI value = URI.create(runtimeUrl == null ? "" : runtimeUrl.trim());
        int port = value.getPort();
        if (!("http".equalsIgnoreCase(value.getScheme()) || "https".equalsIgnoreCase(value.getScheme()))
                || value.getHost() == null || value.getUserInfo() != null || value.getQuery() != null
                || value.getFragment() != null || port < 8000 || port > 9000) {
            throw new IllegalArgumentException("推荐运行时地址无效");
        }
        String normalized = value.toString().endsWith("/") ? value.toString() : value + "/";
        return URI.create(normalized);
    }

    /**
     * 校验配置文本非空且长度受限。
     *
     * @param value 配置值
     * @param message 错误摘要
     * @return 规范化文本
     */
    private String requireConfiguredText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 128) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    /**
     * 校验 JSON 字段等于预期文本。
     *
     * @param payload JSON 对象
     * @param field 字段名
     * @param expected 预期值
     * @param message 错误摘要
     */
    private void requireEquals(Map<String, Object> payload, String field, String expected, String message) {
        if (!expected.equals(payload.get(field))) {
            throw rejected(message);
        }
    }

    /**
     * 校验候选数量为不小于门槛的整数。
     *
     * @param value 待校验值
     * @param minimum 最小值
     * @param message 错误摘要
     */
    private void requirePositiveInteger(Object value, int minimum, String message) {
        if (!(value instanceof Number number) || number.intValue() < minimum) {
            throw rejected(message);
        }
    }

    /**
     * 校验模型摘要为 SHA-256 十六进制文本。
     *
     * @param value 待校验值
     * @param message 错误摘要
     */
    private void requireSha256(Object value, String message) {
        if (!(value instanceof String text) || !text.matches("[a-f0-9]{64}")) {
            throw rejected(message);
        }
    }

    /**
     * 计算不包含用户请求的推荐响应摘要。
     *
     * @param items 推荐候选列表
     * @return SHA-256 十六进制文本
     */
    private String digest(List<Map<String, Object>> items) {
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(items);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialized));
        } catch (Exception exception) {
            throw unavailable("推荐响应摘要生成失败", exception);
        }
    }

    /**
     * 创建不暴露地址和响应正文的契约拒绝错误。
     *
     * @param message 固定错误摘要
     * @return 结构化工具错误
     */
    private AgentContractException rejected(String message) {
        return new AgentContractException(412, "RECOMMENDATION_RUNTIME_REJECTED", message);
    }

    /**
     * 创建不暴露地址和内部异常的运行时不可用错误。
     *
     * @param message 固定错误摘要
     * @param cause 内部异常原因
     * @return 结构化工具错误
     */
    private AgentContractException unavailable(String message, Exception cause) {
        return new AgentContractException(502, "RECOMMENDATION_RUNTIME_UNAVAILABLE", message);
    }
}
