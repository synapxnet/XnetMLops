package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.LLMService;
import com.synapxnet.mlopsmepservice.service.LLMServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mep/llm-services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LLMServiceController {

    private final LLMServiceService llmServiceService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllServices() {
        List<LLMService> services = llmServiceService.findAll();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", services
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getServiceById(@PathVariable Long id) {
        LLMService service = llmServiceService.findById(id);
        if (service == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "服务不存在",
                "data", (Object) null
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", service
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createService(@RequestBody LLMService service) {
        LLMService created = llmServiceService.create(service);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "创建成功",
            "data", created
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateService(@PathVariable Long id, @RequestBody LLMService service) {
        service.setId(id);
        LLMService updated = llmServiceService.update(service);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteService(@PathVariable Long id) {
        llmServiceService.delete(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "删除成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startService(@PathVariable Long id) {
        llmServiceService.start(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "启动成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopService(@PathVariable Long id) {
        llmServiceService.stop(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "停止成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> params) {
        String endpoint = params.get("endpoint");
        String apiKey = params.get("api_key");
        String type = params.get("type");

        Map<String, Object> result = llmServiceService.testConnection(endpoint, apiKey, type);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", result
        ));
    }
}
