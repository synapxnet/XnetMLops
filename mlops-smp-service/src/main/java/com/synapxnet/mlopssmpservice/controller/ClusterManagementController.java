package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.service.ClusterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一集群管理控制器
 * 提供跨集群类型的统一查询和管理接口
 * 支持 Hadoop、Jenkins，预留 Redis/MySQL/Spark 扩展
 */
@RestController
@RequestMapping("/api/cluster")
@CrossOrigin(origins = "*")
public class ClusterManagementController {

    @Autowired
    private ClusterManagementService clusterManagementService;

    /**
     * 获取指定类型集群的拓扑数据
     * @param clusterType 集群类型: hadoop, jenkins, redis, mysql, spark
     */
    @GetMapping("/{clusterType}/topology")
    public Map<String, Object> getClusterTopology(@PathVariable String clusterType) {
        try {
            Map<String, Object> topology = clusterManagementService.getClusterTopology(clusterType);
            return buildSuccessResponse(topology);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "获取集群拓扑失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有集群类型的概览统计
     */
    @GetMapping("/overview")
    public Map<String, Object> getAllClustersOverview() {
        try {
            Map<String, Object> overview = clusterManagementService.getAllClustersOverview();
            return buildSuccessResponse(overview);
        } catch (Exception e) {
            return buildErrorResponse(500, "获取集群概览失败: " + e.getMessage());
        }
    }

    /**
     * 同步Hosts配置到集群所有节点
     * @param request 包含 clusterType 和 masterId
     */
    @PostMapping("/sync-hosts")
    public Map<String, Object> syncHostsToCluster(@RequestBody Map<String, Object> request) {
        try {
            String clusterType = (String) request.get("clusterType");
            Long masterId = Long.valueOf(request.get("masterId").toString());

            if (clusterType == null || masterId == null) {
                return buildErrorResponse(400, "缺少必要参数: clusterType 或 masterId");
            }

            Map<String, Object> result = clusterManagementService.syncHostsToCluster(clusterType, masterId);
            return buildSuccessResponse(result);
        } catch (NumberFormatException e) {
            return buildErrorResponse(400, "masterId 格式错误");
        } catch (Exception e) {
            return buildErrorResponse(500, "Hosts同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取集群健康状态
     * @param clusterType 集群类型
     * @param masterId Master节点ID
     */
    @GetMapping("/{clusterType}/{masterId}/health")
    public Map<String, Object> getClusterHealth(
            @PathVariable String clusterType,
            @PathVariable Long masterId) {
        try {
            Map<String, Object> health = clusterManagementService.getClusterHealth(clusterType, masterId);
            return buildSuccessResponse(health);
        } catch (Exception e) {
            return buildErrorResponse(500, "获取集群健康状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的集群类型列表
     */
    @GetMapping("/types")
    public Map<String, Object> getSupportedClusterTypes() {
        List<Map<String, Object>> types = List.of(
            Map.of("type", "hadoop", "name", "Hadoop", "icon", "logos:hadoop", "color", "#FFB347", "enabled", true),
            Map.of("type", "jenkins", "name", "Jenkins", "icon", "logos:jenkins", "color", "#D33833", "enabled", true),
            Map.of("type", "redis", "name", "Redis", "icon", "logos:redis", "color", "#DC382D", "enabled", false),
            Map.of("type", "mysql", "name", "MySQL", "icon", "logos:mysql", "color", "#00758F", "enabled", false),
            Map.of("type", "spark", "name", "Spark", "icon", "logos:apache-spark", "color", "#E25A1C", "enabled", false)
        );
        return buildSuccessResponse(types);
    }

    private Map<String, Object> buildSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> buildErrorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}
