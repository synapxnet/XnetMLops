package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.service.DeployNodeService;
import com.synapxnet.mlopsmepservice.service.DeploymentRuntimeUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/mep/nodes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeployNodeController {

    private final DeployNodeService deployNodeService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllNodes() {
        List<DeployNode> nodes = deployNodeService.findAll();
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", nodes
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNodeById(@PathVariable Long id) {
        DeployNode node = deployNodeService.findById(id);
        if (node == null) {
            return ResponseEntity.ok(envelope(
                "code", 404,
                "message", "节点不存在",
                "data", (Object) null
            ));
        }
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", node
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNode(@RequestBody DeployNode node) {
        DeployNode created = deployNodeService.create(node);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "创建成功",
            "data", created
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNode(@PathVariable Long id, @RequestBody DeployNode node) {
        if (deployNodeService.findById(id) == null) return ResponseEntity.ok(envelope("code", 404, "message", "节点不存在", "data", null));
        node.setId(id);
        DeployNode updated = deployNodeService.update(node);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNode(@PathVariable Long id) {
        if (deployNodeService.findById(id) == null) return ResponseEntity.ok(envelope("code", 404, "message", "节点不存在", "data", null));
        deployNodeService.delete(id);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "删除成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> params) {
        String ipAddress = (String) params.get("ip_address");
        Integer port = (Integer) params.get("port");

        Map<String, Object> result = deployNodeService.testConnection(ipAddress, port);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", result
        ));
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<Map<String, Object>> refreshStatus(@PathVariable Long id) {
        DeployNode node = deployNodeService.refreshStatus(id);
        if (node == null) return ResponseEntity.ok(envelope("code", 404, "message", "节点不存在", "data", null));
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "刷新成功",
            "data", node
        ));
    }

    @GetMapping("/{id}/resources")
    public ResponseEntity<Map<String, Object>> getNodeResources(@PathVariable Long id) {
        Map<String, Object> resources = deployNodeService.getResources(id);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", resources
        ));
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<Map<String, Object>> setMaintenance(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        Boolean maintenance = params.get("maintenance");
        if (maintenance == null) return ResponseEntity.badRequest().body(envelope("code", 400, "message", "请指定维护状态", "data", null));
        if (deployNodeService.findById(id) == null) return ResponseEntity.ok(envelope("code", 404, "message", "节点不存在", "data", null));
        deployNodeService.setMaintenance(id, maintenance);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", maintenance ? "已进入维护模式" : "已退出维护模式",
            "data", (Object) null
        ));
    }

    /** 保留空 data 的响应契约。 Preserve nullable data in the response contract. */
    private static Map<String, Object> envelope(Object... entries) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) body.put((String) entries[i], entries[i + 1]);
        return body;
    }

    /** 未连接资源采集器时明确返回不可用。 Report unavailable resource collection explicitly. */
    @ExceptionHandler(DeploymentRuntimeUnavailableException.class)
    public ResponseEntity<Map<String, Object>> runtimeUnavailable(DeploymentRuntimeUnavailableException error) {
        return ResponseEntity.status(503).body(envelope("code", 503, "message", error.getMessage(), "data", null));
    }
}
