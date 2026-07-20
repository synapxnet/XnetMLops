package com.synapxnet.mlopsxaaservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 外部服务调用客户端
 * 用于调用DPP、MTP、MEP服务的API
 */
@Service
public class ExternalServiceClient {

    private final WebClient webClient;

    @Value("${xaa.services.dpp-url:http://localhost:8081}")
    private String dppServiceUrl;

    @Value("${xaa.services.mtp-url:http://localhost:8082}")
    private String mtpServiceUrl;

    @Value("${xaa.services.mep-url:http://localhost:8083}")
    private String mepServiceUrl;

    public ExternalServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 调用DPP服务
     */
    public Map<String, Object> callDppService(String path, Map<String, Object> body) {
        return callService(dppServiceUrl + path, "POST", body);
    }

    /**
     * 调用MTP服务
     */
    public Map<String, Object> callMtpService(String path, Map<String, Object> body) {
        return callService(mtpServiceUrl + path, "POST", body);
    }

    /**
     * 调用MEP服务
     */
    public Map<String, Object> callMepService(String path, Map<String, Object> body) {
        return callService(mepServiceUrl + path, "POST", body);
    }

    /**
     * 执行HTTP请求
     */
    public Map<String, Object> executeHttpRequest(String url, String method, Map<String, Object> body) {
        return callService(url, method, body);
    }

    /**
     * 通用服务调用方法
     */
    private Map<String, Object> callService(String url, String method, Map<String, Object> body) {
        try {
            WebClient.RequestBodySpec request;

            switch (method.toUpperCase()) {
                case "GET":
                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                case "POST":
                    return webClient.post()
                            .uri(url)
                            .bodyValue(body != null ? body : new HashMap<>())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                case "PUT":
                    return webClient.put()
                            .uri(url)
                            .bodyValue(body != null ? body : new HashMap<>())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                case "DELETE":
                    return webClient.delete()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                default:
                    throw new RuntimeException("不支持的HTTP方法: " + method);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 获取DPP数据集列表
     */
    public Map<String, Object> getDppDatasets() {
        return callService(dppServiceUrl + "/api/dpp/datasets", "GET", null);
    }

    /**
     * 获取DPP特征工程列表
     */
    public Map<String, Object> getDppFeatures() {
        return callService(dppServiceUrl + "/api/dpp/features", "GET", null);
    }

    /**
     * 获取MTP算法列表
     */
    public Map<String, Object> getMtpAlgorithms() {
        return callService(mtpServiceUrl + "/api/mtp/algorithms", "GET", null);
    }

    /**
     * 获取MTP训练任务列表
     */
    public Map<String, Object> getMtpTrainTasks() {
        return callService(mtpServiceUrl + "/api/mtp/train-tasks", "GET", null);
    }

    /**
     * 获取MEP部署列表
     */
    public Map<String, Object> getMepDeployments() {
        return callService(mepServiceUrl + "/api/mep/deployments", "GET", null);
    }

    /**
     * 获取MEP服务列表
     */
    public Map<String, Object> getMepServices() {
        return callService(mepServiceUrl + "/api/mep/llm-services", "GET", null);
    }
}
