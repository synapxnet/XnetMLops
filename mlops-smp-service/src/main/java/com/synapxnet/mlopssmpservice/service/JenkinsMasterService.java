package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.JenkinsMaster;
import com.synapxnet.mlopssmpservice.entity.JenkinsMasterDeployConfig;

import java.util.List;
import java.util.Map;

/**
 * Jenkins Master 服务接口
 */
public interface JenkinsMasterService {

    // ==================== CRUD操作 ====================

    /**
     * 创建Master配置
     */
    JenkinsMaster createMaster(JenkinsMaster master);

    /**
     * 更新Master配置
     */
    JenkinsMaster updateMaster(Long id, JenkinsMaster master);

    /**
     * 删除Master配置
     */
    void deleteMaster(Long id);

    /**
     * 根据ID获取Master
     */
    JenkinsMaster getMasterById(Long id);

    /**
     * 根据UID获取Master
     */
    JenkinsMaster getMasterByUid(String uid);

    /**
     * 获取所有Master
     */
    List<JenkinsMaster> getAllMasters();

    /**
     * 根据状态获取Master列表
     */
    List<JenkinsMaster> getMastersByStatus(String status);

    /**
     * 获取已部署的Master列表
     */
    List<JenkinsMaster> getDeployedMasters();

    // ==================== SSH连接 ====================

    /**
     * 测试SSH连接
     */
    Map<String, Object> testConnection(JenkinsMaster master);

    // ==================== 部署操作 ====================

    /**
     * 部署Jenkins Master
     */
    Map<String, Object> deployMaster(Long masterId, JenkinsMasterDeployConfig config);

    /**
     * 获取部署脚本
     */
    String getDeployScript(String osType, JenkinsMasterDeployConfig config);

    // ==================== 状态管理 ====================

    /**
     * 检查Master状态
     */
    Map<String, Object> checkMasterStatus(Long masterId);

    /**
     * 启动Jenkins
     */
    Map<String, Object> startJenkins(Long masterId);

    /**
     * 停止Jenkins
     */
    Map<String, Object> stopJenkins(Long masterId);

    /**
     * 重启Jenkins
     */
    Map<String, Object> restartJenkins(Long masterId);

    /**
     * 获取初始密码
     */
    String getInitialPassword(Long masterId);

    // ==================== 凭证管理 ====================

    /**
     * 配置凭证
     */
    Map<String, Object> configureCredentials(Long masterId, JenkinsMasterDeployConfig config);

    // ==================== Node管理 ====================

    /**
     * 在Master上创建Node配置
     */
    Map<String, Object> createNodeOnMaster(Long masterId, String nodeName, String workDir, String labels);

    /**
     * 获取Node的Secret
     */
    String getNodeSecret(Long masterId, String nodeName);

    // ==================== 卸载操作 ====================

    /**
     * 卸载Jenkins
     */
    Map<String, Object> uninstallJenkins(Long masterId);
}
