package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.HadoopCluster;
import com.synapxnet.mlopssmpservice.entity.HadoopDeployConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hadoop 集群服务接口
 */
public interface HadoopClusterService {

    // ==================== CRUD 操作 ====================

    /**
     * 获取所有节点
     */
    List<HadoopCluster> getAll();

    /**
     * 获取所有 Master 节点
     */
    List<HadoopCluster> getMasters();

    /**
     * 获取所有 Node 节点
     */
    List<HadoopCluster> getNodes();

    /**
     * 根据 ID 获取节点
     */
    Optional<HadoopCluster> getById(Long id);

    /**
     * 根据 UID 获取节点
     */
    Optional<HadoopCluster> getByUid(String uid);

    /**
     * 创建节点
     */
    HadoopCluster create(HadoopCluster cluster, String userId);

    /**
     * 更新节点
     */
    HadoopCluster update(Long id, HadoopCluster cluster, String userId);

    /**
     * 删除节点
     */
    boolean delete(Long id);

    // ==================== 部署操作 ====================

    /**
     * 测试 SSH 连接
     */
    Map<String, Object> testConnection(HadoopCluster cluster);

    /**
     * 部署节点
     */
    Map<String, Object> deploy(Long id, HadoopDeployConfig config);

    /**
     * 预览部署脚本
     */
    String previewScript(String osType, String nodeType, HadoopDeployConfig config);

    // ==================== 状态操作 ====================

    /**
     * 检查节点状态
     */
    Map<String, Object> checkStatus(Long id);

    /**
     * 启动 Hadoop 服务
     */
    Map<String, Object> startServices(Long id);

    /**
     * 停止 Hadoop 服务
     */
    Map<String, Object> stopServices(Long id);

    /**
     * 重启 Hadoop 服务
     */
    Map<String, Object> restartServices(Long id);

    // ==================== 集群健康 ====================

    /**
     * 获取集群健康状态
     */
    Map<String, Object> getClusterHealth(Long masterId);

    /**
     * 获取 HDFS 状态
     */
    Map<String, Object> getHdfsStatus(Long masterId);

    /**
     * 获取 YARN 状态
     */
    Map<String, Object> getYarnStatus(Long masterId);
}
