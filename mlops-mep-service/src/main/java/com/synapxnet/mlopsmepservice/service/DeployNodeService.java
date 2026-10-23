package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeployNodeService {

    private final DeployNodeMapper deployNodeMapper;

    /** 查询全部模型部署节点。 */
    public List<DeployNode> findAll() {
        return deployNodeMapper.findAll();
    }

    /** 按数据库主键查询模型部署节点。 */
    public DeployNode findById(Long id) {
        return deployNodeMapper.findById(id);
    }

    /** 创建部署节点，并使用真实 TCP 探测初始化连通状态。 */
    public DeployNode create(DeployNode node) {
        node.setUid(UUID.randomUUID().toString());
        node.setStatus("offline");
        node.setNginxStatus("stopped");
        node.setCreatedAt(LocalDateTime.now());
        deployNodeMapper.insert(node);

        refreshStatus(node.getId());

        return deployNodeMapper.findById(node.getId());
    }

    /** 更新部署节点的用户可编辑信息，不覆盖运行时核验结果。 */
    public DeployNode update(DeployNode node) {
        node.setUpdatedAt(LocalDateTime.now());
        deployNodeMapper.update(node);
        return deployNodeMapper.findById(node.getId());
    }

    /** 删除指定部署节点。 */
    public void delete(Long id) {
        deployNodeMapper.deleteById(id);
    }

    /** 测试节点 SSH 端口连通性，不伪造 Docker 或 Nginx 运行状态。 */
    public Map<String, Object> testConnection(String ipAddress, Integer port) {
        try {
            probeTcpConnectivity(ipAddress, port != null ? port : 22);
            return connectionResult(true, "连接成功；Docker 与 Nginx 状态需由受控运行时探针核验");
        } catch (Exception e) {
            log.error("测试节点连接失败: {}", e.getMessage());
            return connectionResult(false, "连接失败: " + e.getMessage());
        }
    }

    /** 刷新节点 TCP 连通状态，并保留数据库中已有的真实运行时核验值。 */
    public DeployNode refreshStatus(Long id) {
        DeployNode node = deployNodeMapper.findById(id);
        if (node == null) {
            return null;
        }

        try {
            probeTcpConnectivity(node.getIpAddress(), node.getPort());
            deployNodeMapper.updateConnectivityStatus(id, "online");
        } catch (Exception e) {
            log.warn("节点 {} 不可达: {}", node.getName(), e.getMessage());
            deployNodeMapper.updateConnectivityStatus(id, "offline");
        }

        return deployNodeMapper.findById(id);
    }

    /** 返回已持久化资源容量；没有真实采集器时明确返回不可用状态。 */
    /** 尚未接入资源采集器时拒绝伪造指标。Reject fabricated metrics without a resource collector. */
    public Map<String, Object> getResources(Long id) {
        throw new DeploymentRuntimeUnavailableException();
    }

    /** 切换节点维护状态，同时保留最近一次运行时核验信息。 */
    public void setMaintenance(Long id, Boolean maintenance) {
        DeployNode node = deployNodeMapper.findById(id);
        if (node != null) {
            String status = maintenance ? "maintenance" : "online";
            deployNodeMapper.updateStatus(id, status, node.getDockerVersion(), node.getNginxStatus());
        }
    }

    /** 在限定超时内探测指定 TCP 端口，成功后正常返回。 */
    private void probeTcpConnectivity(String ipAddress, Integer port) throws Exception {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("节点 IP 地址不能为空");
        }
        int resolvedPort = port != null ? port : 22;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, resolvedPort), 5000);
        }
    }

    /** 构造允许 runtime 元数据为空的连接测试结果。 */
    private Map<String, Object> connectionResult(boolean success, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("message", message);
        result.put("docker_version", null);
        result.put("nginx_status", null);
        result.put("runtime_verified", false);
        return result;
    }
}
