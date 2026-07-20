package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.mapper.ModelDeploymentMapper;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        deployAsync(deployment.getId());

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

        deploymentMapper.updateStatus(id, "deploying", null, deployment.getEndpoint());

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                String containerId = "container_" + UUID.randomUUID().toString().substring(0, 12);
                deploymentMapper.updateStatus(id, "running", containerId, deployment.getEndpoint());
                log.info("部署 {} 启动成功", deployment.getName());
            } catch (Exception e) {
                log.error("启动部署失败: {}", e.getMessage());
                deploymentMapper.updateStatus(id, "failed", null, null);
            }
        }).start();
    }

    public void stop(Long id) {
        deploymentMapper.updateStatus(id, "stopped", null, null);
    }

    public void restart(Long id) {
        stop(id);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start(id);
    }

    public void scale(Long id, Integer replicas) {
        deploymentMapper.updateReplicas(id, replicas);
    }

    public List<Map<String, Object>> getLogs(Long id, Integer limit, String since) {
        List<Map<String, Object>> logs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String[] messages = {
            "Container started successfully",
            "Model loaded into memory",
            "HTTP server listening on port 8080",
            "Health check passed",
            "Processing request from client",
            "Request completed in 125ms"
        };

        for (int i = 0; i < Math.min(limit, 20); i++) {
            logs.add(Map.of(
                "id", i + 1,
                "deployment_uid", id.toString(),
                "level", i % 5 == 0 ? "warn" : "info",
                "message", messages[i % messages.length],
                "timestamp", LocalDateTime.now().minusMinutes(i).format(formatter)
            ));
        }

        return logs;
    }

    public List<Map<String, Object>> getMetrics(Long id, String start, String end) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        Random random = new Random();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < 10; i++) {
            metrics.add(Map.of(
                "cpu_usage", random.nextInt(50) + 10,
                "memory_usage", random.nextInt(60) + 20,
                "request_count", random.nextInt(1000) + 100,
                "error_count", random.nextInt(10),
                "avg_response_time", random.nextInt(200) + 50,
                "timestamp", LocalDateTime.now().minusMinutes(i * 5).format(formatter)
            ));
        }

        return metrics;
    }

    private void deployAsync(Long id) {
        new Thread(() -> {
            try {
                ModelDeployment deployment = deploymentMapper.findById(id);
                if (deployment == null) return;

                deploymentMapper.updateStatus(id, "deploying", null, deployment.getEndpoint());

                Thread.sleep(5000);

                String containerId = "container_" + UUID.randomUUID().toString().substring(0, 12);
                deploymentMapper.updateStatus(id, "running", containerId, deployment.getEndpoint());

                log.info("部署 {} 完成", deployment.getName());
            } catch (Exception e) {
                log.error("部署失败: {}", e.getMessage());
                deploymentMapper.updateStatus(id, "failed", null, null);
            }
        }).start();
    }
}
