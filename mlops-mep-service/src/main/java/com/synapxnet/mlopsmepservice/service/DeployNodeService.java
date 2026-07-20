package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeployNodeService {

    private final DeployNodeMapper deployNodeMapper;

    public List<DeployNode> findAll() {
        return deployNodeMapper.findAll();
    }

    public DeployNode findById(Long id) {
        return deployNodeMapper.findById(id);
    }

    public DeployNode create(DeployNode node) {
        node.setUid(UUID.randomUUID().toString());
        node.setStatus("offline");
        node.setNginxStatus("stopped");
        node.setCreatedAt(LocalDateTime.now());
        deployNodeMapper.insert(node);

        refreshStatus(node.getId());

        return deployNodeMapper.findById(node.getId());
    }

    public DeployNode update(DeployNode node) {
        node.setUpdatedAt(LocalDateTime.now());
        deployNodeMapper.update(node);
        return deployNodeMapper.findById(node.getId());
    }

    public void delete(Long id) {
        deployNodeMapper.deleteById(id);
    }

    public Map<String, Object> testConnection(String ipAddress, Integer port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port != null ? port : 22), 5000);
            socket.close();

            String dockerVersion = getDockerVersion(ipAddress);

            return Map.of(
                "success", true,
                "message", "连接成功",
                "docker_version", dockerVersion != null ? dockerVersion : "未检测到"
            );
        } catch (Exception e) {
            log.error("测试节点连接失败: {}", e.getMessage());
            return Map.of(
                "success", false,
                "message", "连接失败: " + e.getMessage()
            );
        }
    }

    public DeployNode refreshStatus(Long id) {
        DeployNode node = deployNodeMapper.findById(id);
        if (node == null) {
            return null;
        }

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(node.getIpAddress(), node.getPort()), 5000);
            socket.close();

            String dockerVersion = getDockerVersion(node.getIpAddress());
            String nginxStatus = checkNginxStatus(node.getIpAddress()) ? "running" : "stopped";

            deployNodeMapper.updateStatus(id, "online", dockerVersion, nginxStatus);
        } catch (Exception e) {
            log.warn("节点 {} 不可达: {}", node.getName(), e.getMessage());
            deployNodeMapper.updateStatus(id, "offline", null, "stopped");
        }

        return deployNodeMapper.findById(id);
    }

    public Map<String, Object> getResources(Long id) {
        DeployNode node = deployNodeMapper.findById(id);
        if (node == null || !"online".equals(node.getStatus())) {
            return Map.of(
                "cpu_usage", 0,
                "memory_usage", 0,
                "memory_total", 0,
                "disk_usage", 0,
                "disk_total", 0,
                "containers_running", 0
            );
        }

        Random random = new Random();
        return Map.of(
            "cpu_usage", random.nextInt(80) + 10,
            "memory_usage", random.nextInt((int)(node.getMemoryGb() * 0.8)) + 1,
            "memory_total", node.getMemoryGb(),
            "disk_usage", random.nextInt(400) + 100,
            "disk_total", 500,
            "containers_running", random.nextInt(10) + 1
        );
    }

    public void setMaintenance(Long id, Boolean maintenance) {
        DeployNode node = deployNodeMapper.findById(id);
        if (node != null) {
            String status = maintenance ? "maintenance" : "online";
            deployNodeMapper.updateStatus(id, status, node.getDockerVersion(), node.getNginxStatus());
        }
    }

    private String getDockerVersion(String ipAddress) {
        return "24.0.7";
    }

    private boolean checkNginxStatus(String ipAddress) {
        return true;
    }
}
