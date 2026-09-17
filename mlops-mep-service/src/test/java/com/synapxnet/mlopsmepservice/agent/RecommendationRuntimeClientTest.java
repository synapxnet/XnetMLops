/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContractException;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证推荐运行时客户端的固定目标、模型身份和响应契约。
 */
class RecommendationRuntimeClientTest {

    /** 生产构造器必须明确注册为 Spring 注入入口。 */
    @Test
    void marksProductionConstructorForSpringInjection() throws NoSuchMethodException {
        assertTrue(RecommendationRuntimeClient.class.getConstructor(
                String.class,
                String.class,
                String.class,
                ObjectMapper.class).isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class));
    }

    /** 客户端只接管固定推荐部署和固定脱敏探针。 */
    @Test
    void supportsOnlyRegisteredRecommendationDeploymentAndDataset() {
        RecommendationRuntimeClient client = client("dcn_1");

        assertTrue(client.supports("deploy_recommendation_prod"));
        assertTrue(client.supportsDataset("staging://goai/recommendation-dcn-v1/probe"));
        assertEquals(false, client.supports("deploy_risk_prod"));
        assertEquals(false, client.supportsDataset("https://example.com/private.json"));
    }

    /** 健康状态和推荐结果一致时返回真实模型摘要与候选计数。 */
    @Test
    void validatesHealthAndRecommendationResponse() {
        RecommendationRuntimeClient client = client("dcn_1");

        Map<String, Object> result = client.probe(8);

        assertEquals(true, result.get("passed"));
        assertEquals("dcn_1", result.get("algorithmId"));
        assertEquals("recommendation-dcn-demo-v1", result.get("productVersion"));
        assertEquals(8, result.get("candidateCount"));
        assertTrue(String.valueOf(result.get("resultDigest")).matches("[a-f0-9]{64}"));
    }

    /** 连续监控使用固定请求并返回可核验的 P99 与成功率。 */
    @Test
    void calculatesLiveMonitoringSummary() {
        RecommendationRuntimeClient client = client("dcn_1");

        Map<String, Object> result = client.monitor(10);

        assertEquals(10, result.get("requestCount"));
        assertEquals(10, result.get("successCount"));
        assertEquals(0, result.get("errorCount"));
        assertEquals(100.0, result.get("successRate"));
        assertEquals("MATCHED", result.get("contractStatus"));
        assertTrue(((Number) result.get("p99ResponseTimeMs")).longValue() >= 0L);
    }

    /** 任一候选的算法版本不一致时必须失败关闭。 */
    @Test
    void rejectsMismatchedRecommendationAlgorithm() {
        RecommendationRuntimeClient client = client("dcn_legacy");

        assertThrows(AgentContractException.class, () -> client.probe(8));
    }

    /**
     * 创建返回固定健康和推荐响应的 HTTP 客户端替身。
     *
     * @param responseAlgorithmId 推荐响应中的算法版本
     * @return 可测试的推荐运行时客户端
     */
    @SuppressWarnings("unchecked")
    private RecommendationRuntimeClient client(String responseAlgorithmId) {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> healthResponse = mock(HttpResponse.class);
        HttpResponse<String> recommendationResponse = mock(HttpResponse.class);
        when(healthResponse.statusCode()).thenReturn(200);
        when(healthResponse.body()).thenReturn("""
                {"status":"ready","model_type":"DCN","product_version":"recommendation-dcn-demo-v1",\
                "model_digest_sha256":"be0cc4b7845294b61439469a345944aabea9c0878b94f3ea850230d72d697915",\
                "approval_id":"APR-MEP-PROMOTE-20260828-001","algorithm_id":"dcn_1","candidate_count":8}
                """);
        when(recommendationResponse.statusCode()).thenReturn(200);
        when(recommendationResponse.body()).thenReturn(recommendations(responseAlgorithmId));
        try {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenAnswer(invocation -> {
                        HttpRequest request = invocation.getArgument(0);
                        return request.uri().getPath().endsWith("/health") ? healthResponse : recommendationResponse;
                    });
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return new RecommendationRuntimeClient(
                "http://124.223.86.54:8888",
                "dcn_1",
                "recommendation-dcn-demo-v1",
                new ObjectMapper(),
                httpClient);
    }

    /**
     * 构建八条固定推荐响应。
     *
     * @param algorithmId 响应算法版本
     * @return JSON 数组
     */
    private String recommendations(String algorithmId) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < 8; index++) {
            if (index > 0) value.append(',');
            value.append("{\"item_id\":\"")
                    .append(425236995759276169L - index)
                    .append("\",\"data_type\":\"article\",\"algorithm_id\":\"")
                    .append(algorithmId)
                    .append("\"}");
        }
        return value.append(']').toString();
    }
}
