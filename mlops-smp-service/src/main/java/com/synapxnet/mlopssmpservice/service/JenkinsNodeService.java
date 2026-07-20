package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.JenkinsNode;
import com.synapxnet.mlopssmpservice.entity.JenkinsNodeDeployConfig;
import java.util.List;
import java.util.Map;

public interface JenkinsNodeService {

    // CRUD操作
    JenkinsNode createNode(JenkinsNode node);
    JenkinsNode updateNode(Long id, JenkinsNode node);
    void deleteNode(Long id);
    JenkinsNode getNodeById(Long id);
    JenkinsNode getNodeByUid(String uid);
    List<JenkinsNode> getAllNodes();
    List<JenkinsNode> getNodesByStatus(String status);

    // SSH连接测试
    Map<String, Object> testConnection(JenkinsNode node);

    // 部署操作
    Map<String, Object> deployNode(Long nodeId, JenkinsNodeDeployConfig config);

    // 获取部署脚本
    String getDeployScript(String osType, JenkinsNodeDeployConfig config);

    // 检查节点状态
    Map<String, Object> checkNodeStatus(Long nodeId);

    // 停止Agent
    Map<String, Object> stopAgent(Long nodeId);

    // 启动Agent
    Map<String, Object> startAgent(Long nodeId);

    // 卸载Agent
    Map<String, Object> uninstallAgent(Long nodeId);
}
