package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.JenkinsNode;
import com.synapxnet.mlopssmpservice.entity.JenkinsNodeDeployConfig;
import com.synapxnet.mlopssmpservice.service.JenkinsNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smp/jenkins-nodes")
public class JenkinsNodeController {

    private final JenkinsNodeService jenkinsNodeService;

    @Autowired
    public JenkinsNodeController(JenkinsNodeService jenkinsNodeService) {
        this.jenkinsNodeService = jenkinsNodeService;
    }

    /**
     * 获取所有节点
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllNodes() {
        try {
            List<JenkinsNode> nodes = jenkinsNodeService.getAllNodes();
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", nodes,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取节点列表失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 根据ID获取节点
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNodeById(@PathVariable Long id) {
        try {
            JenkinsNode node = jenkinsNodeService.getNodeById(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", node,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 根据UID获取节点
     */
    @GetMapping("/uid/{uid}")
    public ResponseEntity<Map<String, Object>> getNodeByUid(@PathVariable String uid) {
        try {
            JenkinsNode node = jenkinsNodeService.getNodeByUid(uid);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", node,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 根据状态获取节点
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getNodesByStatus(@PathVariable String status) {
        try {
            List<JenkinsNode> nodes = jenkinsNodeService.getNodesByStatus(status);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", nodes,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取节点列表失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 创建节点
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNode(
            @RequestBody JenkinsNode node,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            node.setCreated_by(userId);
            JenkinsNode created = jenkinsNodeService.createNode(node);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "创建成功",
                    "data", created,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "创建失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 更新节点
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNode(
            @PathVariable Long id,
            @RequestBody JenkinsNode node,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            node.setUpdated_by(userId);
            JenkinsNode updated = jenkinsNodeService.updateNode(id, node);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", updated,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "更新失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 删除节点
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNode(@PathVariable Long id) {
        try {
            jenkinsNodeService.deleteNode(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "删除失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 测试SSH连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody JenkinsNode node) {
        try {
            Map<String, Object> result = jenkinsNodeService.testConnection(node);
            return ResponseEntity.ok(Map.of(
                    "code", (Boolean) result.get("success") ? 0 : 500,
                    "message", result.get("message"),
                    "data", result,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "测试连接失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 部署节点
     */
    @PostMapping("/{id}/deploy")
    public ResponseEntity<Map<String, Object>> deployNode(
            @PathVariable Long id,
            @RequestBody JenkinsNodeDeployConfig config) {
        try {
            Map<String, Object> result = jenkinsNodeService.deployNode(id, config);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", result.get("message"),
                    "data", result,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "部署失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取部署脚本预览
     */
    @PostMapping("/preview-script")
    public ResponseEntity<Map<String, Object>> previewScript(
            @RequestParam String osType,
            @RequestBody JenkinsNodeDeployConfig config) {
        try {
            String script = jenkinsNodeService.getDeployScript(osType, config);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", Map.of("script", script),
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取脚本失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 检查节点状态
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> checkNodeStatus(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsNodeService.checkNodeStatus(id);
            return ResponseEntity.ok(Map.of(
                    "code", (Boolean) result.get("success") ? 0 : 500,
                    "message", result.getOrDefault("message", "ok"),
                    "data", result,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "检查状态失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 停止Agent
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopAgent(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsNodeService.stopAgent(id);
            return ResponseEntity.ok(Map.of(
                    "code", (Boolean) result.get("success") ? 0 : 500,
                    "message", result.get("message"),
                    "data", result,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "停止Agent失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 启动Agent
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startAgent(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsNodeService.startAgent(id);
            return ResponseEntity.ok(Map.of(
                    "code", (Boolean) result.get("success") ? 0 : 500,
                    "message", result.get("message"),
                    "data", result,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "启动Agent失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 卸载Agent
     */
    @PostMapping("/{id}/uninstall")
    public ResponseEntity<Map<String, Object>> uninstallAgent(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsNodeService.uninstallAgent(id);
            return ResponseEntity.ok(Map.of(
                    "code", (Boolean) result.get("success") ? 0 : 500,
                    "message", result.get("message"),
                    "data", result,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "卸载Agent失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }
}
