package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.service.DeployNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", nodes
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNodeById(@PathVariable Long id) {
        DeployNode node = deployNodeService.findById(id);
        if (node == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "节点不存在",
                "data", (Object) null
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", node
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNode(@RequestBody DeployNode node) {
        DeployNode created = deployNodeService.create(node);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "创建成功",
            "data", created
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNode(@PathVariable Long id, @RequestBody DeployNode node) {
        node.setId(id);
        DeployNode updated = deployNodeService.update(node);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNode(@PathVariable Long id) {
        deployNodeService.delete(id);
        return ResponseEntity.ok(Map.of(
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
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", result
        ));
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<Map<String, Object>> refreshStatus(@PathVariable Long id) {
        DeployNode node = deployNodeService.refreshStatus(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "刷新成功",
            "data", node
        ));
    }

    @GetMapping("/{id}/resources")
    public ResponseEntity<Map<String, Object>> getNodeResources(@PathVariable Long id) {
        Map<String, Object> resources = deployNodeService.getResources(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", resources
        ));
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<Map<String, Object>> setMaintenance(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        Boolean maintenance = params.get("maintenance");
        deployNodeService.setMaintenance(id, maintenance);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", maintenance ? "已进入维护模式" : "已退出维护模式",
            "data", (Object) null
        ));
    }
}
