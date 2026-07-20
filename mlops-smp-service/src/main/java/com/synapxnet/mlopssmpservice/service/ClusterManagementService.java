package com.synapxnet.mlopssmpservice.service;

import java.util.Map;

/**
 * 统一集群管理服务接口
 * 提供跨集群类型的统一管理功能
 */
public interface ClusterManagementService {

    /**
     * 获取集群拓扑数据
     * @param clusterType 集群类型 (hadoop, jenkins, redis, mysql, spark)
     * @return 拓扑数据，包含 masters, workers, stats
     */
    Map<String, Object> getClusterTopology(String clusterType);

    /**
     * 获取所有集群类型的概览统计
     * @return 各集群类型的拓扑概览
     */
    Map<String, Object> getAllClustersOverview();

    /**
     * 同步Hosts配置到集群所有节点
     * @param clusterType 集群类型
     * @param masterId Master节点ID
     * @return 同步结果，包含 success, message, syncedNodes, failedNodes
     */
    Map<String, Object> syncHostsToCluster(String clusterType, Long masterId);

    /**
     * 获取集群健康状态
     * @param clusterType 集群类型
     * @param masterId Master节点ID
     * @return 健康状态信息
     */
    Map<String, Object> getClusterHealth(String clusterType, Long masterId);
}
