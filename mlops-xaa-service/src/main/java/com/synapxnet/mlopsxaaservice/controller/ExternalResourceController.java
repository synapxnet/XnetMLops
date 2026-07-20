package com.synapxnet.mlopsxaaservice.controller;

import com.synapxnet.mlopsxaaservice.service.ExternalServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 外部资源控制器
 * 用于获取DPP/MTP/MEP的资源列表，供工作流节点配置使用
 */
@RestController
@RequestMapping("/api/xaa/resources")
@CrossOrigin(origins = "*")
public class ExternalResourceController {

    private final ExternalServiceClient externalServiceClient;

    @Autowired
    public ExternalResourceController(ExternalServiceClient externalServiceClient) {
        this.externalServiceClient = externalServiceClient;
    }

    private static class ResponseUtils {
        static ResponseEntity<Map<String, Object>> success(Object data) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", data,
                    "error", "null"
            ));
        }

        static ResponseEntity<Map<String, Object>> error(int code, String message) {
            return ResponseEntity.ok(Map.of(
                    "code", code,
                    "message", message,
                    "data", "null",
                    "error", message
            ));
        }
    }

    /**
     * 获取DPP数据集列表
     */
    @GetMapping("/dpp/datasets")
    public ResponseEntity<Map<String, Object>> getDppDatasets() {
        try {
            Map<String, Object> result = externalServiceClient.getDppDatasets();
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取DPP数据集失败: " + e.getMessage());
        }
    }

    /**
     * 获取DPP特征工程列表
     */
    @GetMapping("/dpp/features")
    public ResponseEntity<Map<String, Object>> getDppFeatures() {
        try {
            Map<String, Object> result = externalServiceClient.getDppFeatures();
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取DPP特征工程失败: " + e.getMessage());
        }
    }

    /**
     * 获取MTP算法列表
     */
    @GetMapping("/mtp/algorithms")
    public ResponseEntity<Map<String, Object>> getMtpAlgorithms() {
        try {
            Map<String, Object> result = externalServiceClient.getMtpAlgorithms();
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取MTP算法失败: " + e.getMessage());
        }
    }

    /**
     * 获取MTP训练任务列表
     */
    @GetMapping("/mtp/train-tasks")
    public ResponseEntity<Map<String, Object>> getMtpTrainTasks() {
        try {
            Map<String, Object> result = externalServiceClient.getMtpTrainTasks();
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取MTP训练任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取MEP部署列表
     */
    @GetMapping("/mep/deployments")
    public ResponseEntity<Map<String, Object>> getMepDeployments() {
        try {
            Map<String, Object> result = externalServiceClient.getMepDeployments();
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取MEP部署失败: " + e.getMessage());
        }
    }

    /**
     * 获取MEP服务列表
     */
    @GetMapping("/mep/services")
    public ResponseEntity<Map<String, Object>> getMepServices() {
        try {
            Map<String, Object> result = externalServiceClient.getMepServices();
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取MEP服务失败: " + e.getMessage());
        }
    }
}
