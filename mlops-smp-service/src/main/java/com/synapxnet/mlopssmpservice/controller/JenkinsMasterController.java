package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.JenkinsMaster;
import com.synapxnet.mlopssmpservice.entity.JenkinsMasterDeployConfig;
import com.synapxnet.mlopssmpservice.service.JenkinsMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jenkins Master 控制器
 * 提供Master节点的CRUD、部署、状态管理等API
 */
@RestController
@RequestMapping("/api/smp/jenkins-masters")
public class JenkinsMasterController {

    private final JenkinsMasterService jenkinsMasterService;

    @Autowired
    public JenkinsMasterController(JenkinsMasterService jenkinsMasterService) {
        this.jenkinsMasterService = jenkinsMasterService;
    }

    // ==================== CRUD 操作 ====================

    /**
     * 获取所有Master节点
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMasters() {
        List<JenkinsMaster> masters = jenkinsMasterService.getAllMasters();
        return ResponseEntity.ok(buildResponse(0, "ok", masters));
    }

    /**
     * 根据ID获取Master
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMasterById(@PathVariable Long id) {
        try {
            JenkinsMaster master = jenkinsMasterService.getMasterById(id);
            return ResponseEntity.ok(buildResponse(0, "ok", master));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 根据UID获取Master
     */
    @GetMapping("/uid/{uid}")
    public ResponseEntity<Map<String, Object>> getMasterByUid(@PathVariable String uid) {
        try {
            JenkinsMaster master = jenkinsMasterService.getMasterByUid(uid);
            return ResponseEntity.ok(buildResponse(0, "ok", master));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 根据状态获取Master列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getMastersByStatus(@PathVariable String status) {
        List<JenkinsMaster> masters = jenkinsMasterService.getMastersByStatus(status);
        return ResponseEntity.ok(buildResponse(0, "ok", masters));
    }

    /**
     * 获取已部署的Master列表
     */
    @GetMapping("/deployed")
    public ResponseEntity<Map<String, Object>> getDeployedMasters() {
        List<JenkinsMaster> masters = jenkinsMasterService.getDeployedMasters();
        return ResponseEntity.ok(buildResponse(0, "ok", masters));
    }

    /**
     * 创建Master配置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMaster(
            @RequestBody JenkinsMaster master,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            master.setCreated_by(userId);
            master.setUpdated_by(userId);
            JenkinsMaster created = jenkinsMasterService.createMaster(master);
            return ResponseEntity.ok(buildResponse(0, "创建成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 更新Master配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMaster(
            @PathVariable Long id,
            @RequestBody JenkinsMaster master,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            master.setUpdated_by(userId);
            JenkinsMaster updated = jenkinsMasterService.updateMaster(id, master);
            return ResponseEntity.ok(buildResponse(0, "更新成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 删除Master
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMaster(@PathVariable Long id) {
        try {
            jenkinsMasterService.deleteMaster(id);
            return ResponseEntity.ok(buildResponse(0, "删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    // ==================== 连接测试 ====================

    /**
     * 测试SSH连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody JenkinsMaster master) {
        Map<String, Object> result = jenkinsMasterService.testConnection(master);
        return ResponseEntity.ok(buildResponse(0, "ok", result));
    }

    // ==================== 部署操作 ====================

    /**
     * 部署Jenkins Master
     */
    @PostMapping("/{id}/deploy")
    public ResponseEntity<Map<String, Object>> deployMaster(
            @PathVariable Long id,
            @RequestBody JenkinsMasterDeployConfig config) {
        try {
            Map<String, Object> result = jenkinsMasterService.deployMaster(id, config);
            return ResponseEntity.ok(buildResponse(0, "部署任务已启动", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 预览部署脚本
     */
    @PostMapping("/preview-script")
    public ResponseEntity<Map<String, Object>> previewScript(
            @RequestParam(defaultValue = "linux") String osType,
            @RequestBody JenkinsMasterDeployConfig config) {
        try {
            String script = jenkinsMasterService.getDeployScript(osType, config);
            Map<String, Object> data = new HashMap<>();
            data.put("script", script);
            data.put("osType", osType);
            return ResponseEntity.ok(buildResponse(0, "ok", data));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    // ==================== 状态管理 ====================

    /**
     * 检查Master状态
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsMasterService.checkMasterStatus(id);
            return ResponseEntity.ok(buildResponse(0, "ok", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 启动Jenkins
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startJenkins(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsMasterService.startJenkins(id);
            return ResponseEntity.ok(buildResponse(0, "启动命令已发送", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 停止Jenkins
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopJenkins(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsMasterService.stopJenkins(id);
            return ResponseEntity.ok(buildResponse(0, "停止命令已发送", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 重启Jenkins
     */
    @PostMapping("/{id}/restart")
    public ResponseEntity<Map<String, Object>> restartJenkins(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsMasterService.restartJenkins(id);
            return ResponseEntity.ok(buildResponse(0, "重启命令已发送", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    /**
     * 获取初始密码
     */
    @GetMapping("/{id}/initial-password")
    public ResponseEntity<Map<String, Object>> getInitialPassword(@PathVariable Long id) {
        try {
            String password = jenkinsMasterService.getInitialPassword(id);
            Map<String, Object> data = new HashMap<>();
            data.put("password", password);
            return ResponseEntity.ok(buildResponse(0, "ok", data));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    // ==================== 凭证管理 ====================

    /**
     * 配置凭证
     */
    @PostMapping("/{id}/credentials")
    public ResponseEntity<Map<String, Object>> configureCredentials(
            @PathVariable Long id,
            @RequestBody JenkinsMasterDeployConfig config) {
        try {
            Map<String, Object> result = jenkinsMasterService.configureCredentials(id, config);
            return ResponseEntity.ok(buildResponse(0, "凭证配置完成", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    // ==================== Node管理 ====================

    /**
     * 在Master上创建Node配置并获取Secret
     */
    @PostMapping("/{id}/create-node")
    public ResponseEntity<Map<String, Object>> createNodeOnMaster(
            @PathVariable Long id,
            @RequestParam String nodeName,
            @RequestParam String workDir,
            @RequestParam(required = false) String labels) {
        try {
            Map<String, Object> result = jenkinsMasterService.createNodeOnMaster(id, nodeName, workDir, labels);
            // 根据 service 返回的 success 字段判断是否成功
            boolean success = result.get("success") != null && (Boolean) result.get("success");
            String message = result.get("message") != null ? (String) result.get("message") : (success ? "Node创建成功" : "Node创建失败");
            return ResponseEntity.ok(buildResponse(success ? 0 : 1, message, result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, "Node创建异常: " + e.getMessage(), null));
        }
    }

    /**
     * 获取Node的Secret
     */
    @GetMapping("/{id}/node-secret/{nodeName}")
    public ResponseEntity<Map<String, Object>> getNodeSecret(
            @PathVariable Long id,
            @PathVariable String nodeName) {
        try {
            String secret = jenkinsMasterService.getNodeSecret(id, nodeName);
            Map<String, Object> data = new HashMap<>();
            data.put("nodeName", nodeName);
            data.put("secret", secret);
            return ResponseEntity.ok(buildResponse(0, "ok", data));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    // ==================== 卸载操作 ====================

    /**
     * 卸载Jenkins
     */
    @PostMapping("/{id}/uninstall")
    public ResponseEntity<Map<String, Object>> uninstallJenkins(@PathVariable Long id) {
        try {
            Map<String, Object> result = jenkinsMasterService.uninstallJenkins(id);
            return ResponseEntity.ok(buildResponse(0, "卸载完成", result));
        } catch (Exception e) {
            return ResponseEntity.ok(buildResponse(1, e.getMessage(), null));
        }
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildResponse(int code, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", data);
        response.put("error", code == 0 ? null : message);
        return response;
    }
}
