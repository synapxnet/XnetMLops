package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.agent.RecommendationRuntimeClient;
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
    private final RecommendationRuntimeClient recommendationRuntimeClient;

    public List<ModelDeployment> findAll() {
        return deploymentMapper.findAll();
    }

    public ModelDeployment findById(Long id) {
        return deploymentMapper.findById(id);
    }

    public ModelDeployment create(ModelDeployment deployment) {
        deployment.setUid(UUID.randomUUID().toString());
        deployment.setStatus("pending");
        deployment.setContainerId(null);
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

    /** 页面入口未绑定受控执行器时明确拒绝。Reject UI mutations without a governed runtime binding. */
    public void start(Long id) {
        throw new DeploymentRuntimeUnavailableException();
    }

    /** 页面入口未绑定受控执行器时明确拒绝。Reject UI mutations without a governed runtime binding. */
    public void stop(Long id) {
        throw new DeploymentRuntimeUnavailableException();
    }

    /** 页面入口未绑定受控执行器时明确拒绝。Reject UI mutations without a governed runtime binding. */
    public void restart(Long id) {
        throw new DeploymentRuntimeUnavailableException();
    }

    /** 页面入口未绑定受控执行器时明确拒绝。Reject UI mutations without a governed runtime binding. */
    public void scale(Long id, Integer replicas) {
        throw new DeploymentRuntimeUnavailableException();
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

    /**
     * 读取部署的实时聚合指标；推荐服务使用固定脱敏探针，其他部署使用最近持久化采样。
     *
     * @param id 部署主键
     * @return 页面可直接展示的指标摘要
     */
    public Map<String, Object> getMetricSummary(Long id) {
        ModelDeployment deployment = deploymentMapper.findById(id);
        if (deployment == null) return Map.of();
        if (isRecommendationDeployment(deployment)) {
            return recommendationRuntimeClient.monitor(10);
        }
        ServiceMetric latest = deploymentMapper.findMetrics(deployment.getUid()).stream()
                .findFirst()
                .orElse(null);
        if (latest == null) return Map.of();
        long requestCount = Optional.ofNullable(latest.getRequestCount()).orElse(0L);
        long errorCount = Optional.ofNullable(latest.getErrorCount()).orElse(0L);
        double successRate = requestCount == 0
                ? 0.0
                : Math.round(((requestCount - errorCount) * 10_000.0) / requestCount) / 100.0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "PERSISTED_SERVICE_METRICS");
        result.put("status", errorCount == 0 ? "HEALTHY" : "DEGRADED");
        result.put("requestCount", requestCount);
        result.put("successCount", Math.max(0L, requestCount - errorCount));
        result.put("errorCount", errorCount);
        result.put("successRate", successRate);
        result.put("averageResponseTimeMs", latest.getAvgResponseTime());
        result.put("cpuUsage", latest.getCpuUsage());
        result.put("memoryUsage", latest.getMemoryUsage());
        result.put("recordedAt", latest.getTimestamp());
        return Collections.unmodifiableMap(result);
    }

    /**
     * 判断部署是否绑定比赛使用的 DCN 推荐服务。
     *
     * @param deployment 部署记录
     * @return 模型或镜像身份包含 recommendation/DCN 时返回 true
     */
    private boolean isRecommendationDeployment(ModelDeployment deployment) {
        String identity = String.join(" ",
                Objects.toString(deployment.getModelUid(), ""),
                Objects.toString(deployment.getModelName(), ""),
                Objects.toString(deployment.getModelVersion(), ""),
                Objects.toString(deployment.getImageName(), ""))
                .toLowerCase(Locale.ROOT);
        return identity.contains("recommendation") || identity.contains("dcn");
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
