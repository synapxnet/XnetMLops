package com.synapxnet.mlopsmepservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MTP模型服务 - 从MTP训练平台获取输出模型
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MTPModelService {

    private final WebClient.Builder webClientBuilder;

    @Value("${mtp.service.url:http://localhost:8183}")
    private String mtpServiceUrl;

    /**
     * 获取MTP训练平台的输出模型列表
     * 从MTP服务获取已完成训练的任务，提取其输出模型信息
     */
    public List<Map<String, Object>> getOutputModels() {
        try {
            WebClient client = webClientBuilder.baseUrl(mtpServiceUrl).build();

            // 调用MTP服务获取训练任务列表
            Map<String, Object> response = client.get()
                    .uri("/api/mtp/tasks")
                    .header("X-Tenant-Uid", "default")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("从MTP服务获取任务列表失败: {}", response);
                return new ArrayList<>();
            }

            Object data = response.get("data");
            if (!(data instanceof List)) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) data;

            // 转换为输出模型格式
            List<Map<String, Object>> models = new ArrayList<>();
            for (Map<String, Object> task : tasks) {
                Map<String, Object> model = convertTaskToModel(task);
                if (model != null) {
                    models.add(model);
                }
            }

            return models;
        } catch (Exception e) {
            log.error("获取MTP输出模型失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 将训练任务转换为模型信息
     */
    private Map<String, Object> convertTaskToModel(Map<String, Object> task) {
        try {
            String uid = (String) task.get("uid");
            String taskName = (String) task.get("task_name");
            String algorithmName = (String) task.get("algorithm_name");
            String algorithmVersion = (String) task.get("algorithm_version");
            String outputConfig = (String) task.get("output_config");
            Object createdAt = task.get("created_at");

            return Map.of(
                    "uid", uid != null ? uid : "",
                    "name", taskName != null ? taskName : "",
                    "algorithm_name", algorithmName != null ? algorithmName : "",
                    "algorithm_version", algorithmVersion != null ? algorithmVersion : "1.0",
                    "output_config", outputConfig != null ? outputConfig : "",
                    "source", "mtp",
                    "created_at", createdAt != null ? createdAt : ""
            );
        } catch (Exception e) {
            log.warn("转换任务到模型失败: {}", e.getMessage());
            return null;
        }
    }
}
