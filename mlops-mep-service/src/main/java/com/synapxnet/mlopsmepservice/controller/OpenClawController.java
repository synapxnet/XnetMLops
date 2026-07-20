package com.synapxnet.mlopsmepservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsmepservice.entity.OpenClawInstance;
import com.synapxnet.mlopsmepservice.service.ApiKeyService;
import com.synapxnet.mlopsmepservice.service.OpenClawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenClaw实例管理控制器
 * 提供OpenClaw个人助手的一键部署和管理功能
 */
@RestController
@RequestMapping("/mep/openclaw")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OpenClawController {

    private final OpenClawService openClawService;
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有OpenClaw实例
     */
    @GetMapping("/instances")
    public ResponseEntity<Map<String, Object>> getAllInstances() {
        List<OpenClawInstance> instances = openClawService.findAll();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", instances
        ));
    }

    /**
     * 获取实例详情
     */
    @GetMapping("/instances/{id}")
    public ResponseEntity<Map<String, Object>> getInstanceById(@PathVariable Long id) {
        OpenClawInstance instance = openClawService.findById(id);
        if (instance == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "实例不存在"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", instance
        ));
    }

    /**
     * 创建新实例
     */
    @PostMapping("/instances")
    public ResponseEntity<Map<String, Object>> createInstance(@RequestBody Map<String, Object> params) {
        OpenClawInstance instance = new OpenClawInstance();
        instance.setName((String) params.get("name"));
        instance.setDescription((String) params.get("description"));
        instance.setDeployMode((String) params.getOrDefault("deployMode", "docker"));
        instance.setWorkstationId(params.get("workstationId") != null ? ((Number) params.get("workstationId")).longValue() : null);
        instance.setGatewayHost((String) params.getOrDefault("gatewayHost", "localhost"));
        instance.setGatewayPort(params.get("gatewayPort") != null ? ((Number) params.get("gatewayPort")).intValue() : 18789);
        instance.setGatewayToken((String) params.get("gatewayToken"));
        instance.setDefaultModel((String) params.get("defaultModel"));
        // fallbackModels: 前端传入数组，序列化为JSON字符串存储
        if (params.get("fallbackModels") != null) {
            try {
                instance.setFallbackModels(objectMapper.writeValueAsString(params.get("fallbackModels")));
            } catch (Exception e) {
                instance.setFallbackModels(null);
            }
        }
        instance.setSubagentModel((String) params.get("subagentModel"));
        instance.setLlmServiceId(params.get("llmServiceId") != null ? ((Number) params.get("llmServiceId")).longValue() : null);
        instance.setEnabledSkills((String) params.get("enabledSkills"));
        instance.setChannelsConfig((String) params.get("channelsConfig"));

        // API密钥：支持直接输入或引用托管密钥
        if (params.get("apiKeyRefId") != null) {
            instance.setApiKeyRefId(((Number) params.get("apiKeyRefId")).longValue());
        }
        String plainApiKey = (String) params.get("apiKey");
        if (plainApiKey != null && !plainApiKey.isEmpty()) {
            instance.setApiKeyId(openClawService.encryptApiKey(plainApiKey));
        }

        OpenClawInstance created = openClawService.create(instance);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "创建成功",
            "data", created
        ));
    }

    /**
     * 更新实例配置
     */
    @PutMapping("/instances/{id}")
    public ResponseEntity<Map<String, Object>> updateInstance(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        OpenClawInstance instance = openClawService.findById(id);
        if (instance == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "实例不存在"
            ));
        }

        if (params.containsKey("name")) instance.setName((String) params.get("name"));
        if (params.containsKey("description")) instance.setDescription((String) params.get("description"));
        if (params.containsKey("deployMode")) instance.setDeployMode((String) params.get("deployMode"));
        if (params.containsKey("workstationId")) instance.setWorkstationId(params.get("workstationId") != null ? ((Number) params.get("workstationId")).longValue() : null);
        if (params.containsKey("gatewayHost")) instance.setGatewayHost((String) params.get("gatewayHost"));
        if (params.containsKey("gatewayPort")) instance.setGatewayPort(((Number) params.get("gatewayPort")).intValue());
        if (params.containsKey("gatewayToken")) instance.setGatewayToken((String) params.get("gatewayToken"));
        if (params.containsKey("defaultModel")) instance.setDefaultModel((String) params.get("defaultModel"));
        if (params.containsKey("fallbackModels")) {
            if (params.get("fallbackModels") != null) {
                try {
                    instance.setFallbackModels(objectMapper.writeValueAsString(params.get("fallbackModels")));
                } catch (Exception e) {
                    instance.setFallbackModels(null);
                }
            } else {
                instance.setFallbackModels(null);
            }
        }
        if (params.containsKey("subagentModel")) instance.setSubagentModel((String) params.get("subagentModel"));
        if (params.containsKey("llmServiceId")) instance.setLlmServiceId(params.get("llmServiceId") != null ? ((Number) params.get("llmServiceId")).longValue() : null);
        if (params.containsKey("enabledSkills")) instance.setEnabledSkills((String) params.get("enabledSkills"));
        if (params.containsKey("channelsConfig")) instance.setChannelsConfig((String) params.get("channelsConfig"));

        // API密钥：支持直接输入或引用托管密钥
        if (params.containsKey("apiKeyRefId")) {
            instance.setApiKeyRefId(params.get("apiKeyRefId") != null ? ((Number) params.get("apiKeyRefId")).longValue() : null);
        }
        String plainApiKey = (String) params.get("apiKey");
        if (plainApiKey != null && !plainApiKey.isEmpty()) {
            instance.setApiKeyId(openClawService.encryptApiKey(plainApiKey));
        }

        OpenClawInstance updated = openClawService.update(instance);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    /**
     * 删除实例
     */
    @DeleteMapping("/instances/{id}")
    public ResponseEntity<Map<String, Object>> deleteInstance(@PathVariable Long id) {
        openClawService.delete(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "删除成功"
        ));
    }

    /**
     * 启动实例
     */
    @PostMapping("/instances/{id}/start")
    public ResponseEntity<Map<String, Object>> startInstance(@PathVariable Long id) {
        Map<String, Object> result = openClawService.start(id);
        int code = (Boolean) result.get("success") ? 0 : 500;
        return ResponseEntity.ok(Map.of(
            "code", code,
            "message", result.get("message"),
            "data", result
        ));
    }

    /**
     * 停止实例
     */
    @PostMapping("/instances/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopInstance(@PathVariable Long id) {
        Map<String, Object> result = openClawService.stop(id);
        int code = (Boolean) result.get("success") ? 0 : 500;
        return ResponseEntity.ok(Map.of(
            "code", code,
            "message", result.get("message"),
            "data", result
        ));
    }

    /**
     * 重启实例
     */
    @PostMapping("/instances/{id}/restart")
    public ResponseEntity<Map<String, Object>> restartInstance(@PathVariable Long id) {
        Map<String, Object> result = openClawService.restart(id);
        int code = (Boolean) result.get("success") ? 0 : 500;
        return ResponseEntity.ok(Map.of(
            "code", code,
            "message", result.get("message"),
            "data", result
        ));
    }

    /**
     * 获取实例状态
     */
    @GetMapping("/instances/{id}/status")
    public ResponseEntity<Map<String, Object>> getInstanceStatus(@PathVariable Long id) {
        Map<String, Object> result = openClawService.getStatus(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", result.get("data")
        ));
    }

    /**
     * 获取实例日志
     */
    @GetMapping("/instances/{id}/logs")
    public ResponseEntity<Map<String, Object>> getInstanceLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") Integer lines) {
        Map<String, Object> result = openClawService.getLogs(id, lines);
        int code = (Boolean) result.get("success") ? 0 : 500;
        Object message = result.containsKey("message") ? result.get("message") : "success";
        Object logs = result.getOrDefault("logs", "");
        return ResponseEntity.ok(Map.of(
            "code", code,
            "message", message,
            "data", logs
        ));
    }

    /**
     * 测试Gateway连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> params) {
        String host = (String) params.get("host");
        Integer port = params.get("port") != null ? ((Number) params.get("port")).intValue() : 18789;
        String token = (String) params.get("token");

        Map<String, Object> result = openClawService.testConnection(host, port, token);
        int code = (Boolean) result.get("success") ? 0 : 500;
        return ResponseEntity.ok(Map.of(
            "code", code,
            "message", result.get("message"),
            "data", result
        ));
    }

    /**
     * 获取可用的部署模式
     */
    @GetMapping("/deploy-modes")
    public ResponseEntity<Map<String, Object>> getDeployModes() {
        List<Map<String, Object>> modes = List.of(
            Map.of("value", "docker", "label", "Docker容器", "description", "使用Docker容器部署，推荐生产环境"),
            Map.of("value", "npm", "label", "NPM全局安装", "description", "使用npm/pnpm全局安装，适合开发环境"),
            Map.of("value", "source", "label", "源码构建", "description", "从源码构建运行，适合高级用户")
        );
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", modes
        ));
    }

    /**
     * 获取支持的模型列表
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getSupportedModels() {
        List<Map<String, Object>> models = List.of(
            // Anthropic
            Map.of("value", "anthropic/claude-opus-4-6", "label", "Claude Opus 4.6", "provider", "anthropic"),
            Map.of("value", "anthropic/claude-sonnet-4-5", "label", "Claude Sonnet 4.5", "provider", "anthropic"),
            // OpenAI
            Map.of("value", "openai/gpt-5.2", "label", "GPT-5.2", "provider", "openai"),
            Map.of("value", "openai/gpt-5-mini", "label", "GPT-5 Mini", "provider", "openai"),
            // Google
            Map.of("value", "google/gemini-3-pro-preview", "label", "Gemini 3 Pro", "provider", "google"),
            Map.of("value", "google/gemini-3-flash-preview", "label", "Gemini 3 Flash", "provider", "google"),
            // DeepSeek
            Map.of("value", "deepseek/deepseek-chat", "label", "DeepSeek Chat", "provider", "deepseek"),
            Map.of("value", "deepseek/deepseek-reasoner", "label", "DeepSeek Reasoner", "provider", "deepseek"),
            Map.of("value", "deepseek/deepseek-coder", "label", "DeepSeek Coder", "provider", "deepseek")
        );
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", models
        ));
    }

    /**
     * 生成Gateway访问令牌并保存到API密钥管理
     */
    @PostMapping("/generate-token")
    public ResponseEntity<Map<String, Object>> generateGatewayToken(@RequestBody Map<String, Object> params) {
        String instanceName = (String) params.getOrDefault("instanceName", "OpenClaw");

        // 生成唯一密钥名称：如已存在则追加序号
        String baseName = instanceName + " - Gateway令牌";
        String keyName = baseName;
        int seq = 1;
        while (apiKeyService.findByName(keyName) != null) {
            keyName = baseName + " (" + seq + ")";
            seq++;
        }

        // 使用ApiKeyService创建密钥，provider设为"custom"
        Map<String, Object> apiKeyParams = new HashMap<>();
        apiKeyParams.put("name", keyName);
        apiKeyParams.put("provider", "custom");
        apiKeyParams.put("description", "OpenClaw Gateway 访问令牌（自动生成）");

        Map<String, Object> result = apiKeyService.create(apiKeyParams);
        String plainKey = (String) result.get("plain_key");

        Map<String, Object> response = new HashMap<>();
        response.put("token", plainKey);
        response.put("apiKeyId", result.get("id"));
        response.put("keyMasked", result.get("key_masked"));

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "令牌生成成功",
            "data", response
        ));
    }
}
