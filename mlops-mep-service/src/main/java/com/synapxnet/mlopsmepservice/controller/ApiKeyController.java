package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.ApiKey;
import com.synapxnet.mlopsmepservice.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mep/api-keys")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllApiKeys() {
        List<ApiKey> apiKeys = apiKeyService.findAll();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", apiKeys
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApiKeyById(@PathVariable Long id) {
        ApiKey apiKey = apiKeyService.findById(id);
        if (apiKey == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 404);
            resp.put("message", "API Key不存在");
            resp.put("data", null);
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", apiKey
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = apiKeyService.create(params);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "创建成功",
                "data", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateApiKey(@PathVariable Long id, @RequestBody ApiKey apiKey) {
        try {
            apiKey.setId(id);
            ApiKey updated = apiKeyService.update(apiKey);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "更新成功",
                "data", updated
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteApiKey(@PathVariable Long id) {
        try {
            ApiKey existing = apiKeyService.findById(id);
            if (existing == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 404);
                resp.put("message", "API Key不存在");
                resp.put("data", null);
                return ResponseEntity.ok(resp);
            }
            apiKeyService.delete(id);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("message", "删除成功");
            resp.put("data", null);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("message", "删除失败: " + e.getMessage());
            resp.put("data", null);
            return ResponseEntity.ok(resp);
        }
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateApiKey(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String userKey = body != null ? body.get("api_key") : null;
        Map<String, Object> result = apiKeyService.regenerate(id, userKey);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", userKey != null ? "密钥已更新" : "重新生成成功",
            "data", result
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String status = params.get("status");
        apiKeyService.updateStatus(id, status);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "状态更新成功");
        resp.put("data", null);
        return ResponseEntity.ok(resp);
    }
}
