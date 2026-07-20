package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.HadoopCluster;
import com.synapxnet.mlopssmpservice.entity.HadoopDeployConfig;
import com.synapxnet.mlopssmpservice.service.HadoopClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hadoop 集群控制器
 * 提供集群节点的 CRUD、部署、状态管理等 API
 */
@RestController
@RequestMapping("/api/hadoop")
@CrossOrigin(origins = "*")
public class HadoopClusterController {

    @Autowired
    private HadoopClusterService hadoopClusterService;

    // ==================== CRUD 操作 ====================

    /**
     * 获取所有节点
     */
    @GetMapping
    public Map<String, Object> getAll() {
        try {
            List<HadoopCluster> clusters = hadoopClusterService.getAll();
            return buildResponse(0, "success", clusters);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 获取所有 Master 节点
     */
    @GetMapping("/masters")
    public Map<String, Object> getMasters() {
        try {
            List<HadoopCluster> masters = hadoopClusterService.getMasters();
            return buildResponse(0, "success", masters);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 获取所有 Node 节点
     */
    @GetMapping("/nodes")
    public Map<String, Object> getNodes() {
        try {
            List<HadoopCluster> nodes = hadoopClusterService.getNodes();
            return buildResponse(0, "success", nodes);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 根据 ID 获取节点
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        try {
            Optional<HadoopCluster> cluster = hadoopClusterService.getById(id);
            if (cluster.isPresent()) {
                return buildResponse(0, "success", cluster.get());
            } else {
                return buildResponse(404, "节点不存在", null);
            }
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 根据 UID 获取节点
     */
    @GetMapping("/uid/{uid}")
    public Map<String, Object> getByUid(@PathVariable String uid) {
        try {
            Optional<HadoopCluster> cluster = hadoopClusterService.getByUid(uid);
            if (cluster.isPresent()) {
                return buildResponse(0, "success", cluster.get());
            } else {
                return buildResponse(404, "节点不存在", null);
            }
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 创建节点
     */
    @PostMapping
    public Map<String, Object> create(
            @RequestBody HadoopCluster cluster,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            HadoopCluster created = hadoopClusterService.create(cluster, userId);
            return buildResponse(0, "创建成功", created);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 更新节点
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody HadoopCluster cluster,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            HadoopCluster updated = hadoopClusterService.update(id, cluster, userId);
            return buildResponse(0, "更新成功", updated);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 删除节点
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        try {
            boolean deleted = hadoopClusterService.delete(id);
            if (deleted) {
                return buildResponse(0, "删除成功", null);
            } else {
                return buildResponse(404, "节点不存在", null);
            }
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    // ==================== 部署操作 ====================

    /**
     * 测试 SSH 连接
     */
    @PostMapping("/test-connection")
    public Map<String, Object> testConnection(@RequestBody HadoopCluster cluster) {
        try {
            Map<String, Object> result = hadoopClusterService.testConnection(cluster);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 部署节点
     */
    @PostMapping("/{id}/deploy")
    public Map<String, Object> deploy(
            @PathVariable Long id,
            @RequestBody HadoopDeployConfig config) {
        try {
            Map<String, Object> result = hadoopClusterService.deploy(id, config);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 预览部署脚本
     */
    @PostMapping("/preview-script")
    public Map<String, Object> previewScript(
            @RequestParam(defaultValue = "centos7") String osType,
            @RequestParam(defaultValue = "master") String nodeType,
            @RequestBody HadoopDeployConfig config) {
        try {
            String script = hadoopClusterService.previewScript(osType, nodeType, config);
            return buildResponse(0, "success", script);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    // ==================== 状态操作 ====================

    /**
     * 检查节点状态
     */
    @GetMapping("/{id}/status")
    public Map<String, Object> checkStatus(@PathVariable Long id) {
        try {
            Map<String, Object> result = hadoopClusterService.checkStatus(id);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 启动 Hadoop 服务
     */
    @PostMapping("/{id}/start")
    public Map<String, Object> startServices(@PathVariable Long id) {
        try {
            Map<String, Object> result = hadoopClusterService.startServices(id);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 停止 Hadoop 服务
     */
    @PostMapping("/{id}/stop")
    public Map<String, Object> stopServices(@PathVariable Long id) {
        try {
            Map<String, Object> result = hadoopClusterService.stopServices(id);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 重启 Hadoop 服务
     */
    @PostMapping("/{id}/restart")
    public Map<String, Object> restartServices(@PathVariable Long id) {
        try {
            Map<String, Object> result = hadoopClusterService.restartServices(id);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    // ==================== 集群健康 ====================

    /**
     * 获取集群健康状态
     */
    @GetMapping("/{masterId}/health")
    public Map<String, Object> getClusterHealth(@PathVariable Long masterId) {
        try {
            Map<String, Object> result = hadoopClusterService.getClusterHealth(masterId);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 获取 HDFS 状态
     */
    @GetMapping("/{masterId}/hdfs-status")
    public Map<String, Object> getHdfsStatus(@PathVariable Long masterId) {
        try {
            Map<String, Object> result = hadoopClusterService.getHdfsStatus(masterId);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 获取 YARN 状态
     */
    @GetMapping("/{masterId}/yarn-status")
    public Map<String, Object> getYarnStatus(@PathVariable Long masterId) {
        try {
            Map<String, Object> result = hadoopClusterService.getYarnStatus(masterId);
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    private Map<String, Object> buildResponse(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
