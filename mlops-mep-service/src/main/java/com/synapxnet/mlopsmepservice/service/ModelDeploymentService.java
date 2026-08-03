package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.entity.DeploymentLog;
import com.synapxnet.mlopsmepservice.entity.ServiceMetric;
import com.synapxnet.mlopsmepservice.mapper.ModelDeploymentMapper;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelDeploymentService {

    private final ModelDeploymentMapper deploymentMapper;
    private final DeployNodeMapper nodeMapper;

    public List<ModelDeployment> findAll() {
        return deploymentMapper.findAll();
    }

    public ModelDeployment findById(Long id) {
        return deploymentMapper.findById(id);
    }

    public ModelDeployment create(ModelDeployment deployment) {
        deployment.setUid(UUID.randomUUID().toString());
        deployment.setStatus("pending");
        deployment.setCreatedAt(LocalDateTime.now());

        DeployNode node = nodeMapper.findByUid(deployment.getNodeUid());
        if (node != null) {
            String endpoint = String.format("http://%s:%d", node.getIpAddress(), deployment.getPort());
            deployment.setEndpoint(endpoint);
        }

        deploymentMapper.insert(deployment);

        return deployment;
    }

    public ModelDeployment update(ModelDeployment deployment) {
        deployment.setUpdatedAt(LocalDateTime.now());
        deploymentMapper.update(deployment);
        return deploymentMapper.findById(deployment.getId());
    }

    public void delete(Long id) {
        ModelDeployment deployment = deploymentMapper.findById(id);
        if (deployment != null && "running".equals(deployment.getStatus())) {
            stop(id);
        }
        deploymentMapper.deleteById(id);
    }

    public void start(Long id) {
        ModelDeployment deployment = deploymentMapper.findById(id);
        if (deployment == null) return;

        deploymentMapper.updateStatus(id, "pending", deployment.getContainerId(), deployment.getEndpoint());
        log.warn("部署 {} 已进入待执行状态；页面接口不再模拟 Runtime 成功", deployment.getName());
    }

    public void stop(Long id) {
        deploymentMapper.updateStatus(id, "stopped", null, null);
    }

    public void restart(Long id) {
        stop(id);
        start(id);
    }

    public void scale(Long id, Integer replicas) {
        deploymentMapper.updateReplicas(id, replicas);
    }

    public List<Map<String, Object>> getLogs(Long id, Integer limit, String since) {
        ModelDeployment deployment = deploymentMapper.findById(id);
        if (deployment == null) return List.of();
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 1000));
        return deploymentMapper.findLogs(deployment.getUid(), safeLimit).stream()
                .map(this::logView).toList();
    }

    public List<Map<String, Object>> getMetrics(Long id, String start, String end) {
        ModelDeployment deployment = deploymentMapper.findById(id);
        if (deployment == null) return List.of();
        return deploymentMapper.findMetrics(deployment.getUid()).stream()
                .map(this::metricView).toList();
    }

    /** 将持久化日志转换为旧页面兼容视图。 */
    private Map<String, Object> logView(DeploymentLog value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", value.getId());
        result.put("deployment_uid", value.getDeploymentUid());
        result.put("level", value.getLevel());
        result.put("message", value.getMessage());
        result.put("timestamp", value.getTimestamp());
        return result;
    }

    /** 将持久化指标转换为旧页面兼容视图。 */
    private Map<String, Object> metricView(ServiceMetric value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cpu_usage", value.getCpuUsage());
        result.put("memory_usage", value.getMemoryUsage());
        result.put("request_count", value.getRequestCount());
        result.put("error_count", value.getErrorCount());
        result.put("avg_response_time", value.getAvgResponseTime());
        result.put("timestamp", value.getTimestamp());
        return result;
    }
}
