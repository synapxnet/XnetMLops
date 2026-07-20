package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.Workstation;
import java.util.List;
import java.util.Map;

/**
 * 工作站点服务接口
 */
public interface WorkstationService {

    /**
     * 获取所有工作站点
     */
    List<Workstation> getAll();

    /**
     * 根据ID获取工作站点
     */
    Workstation getById(Long id);

    /**
     * 根据UID获取工作站点
     */
    Workstation getByUid(String uid);

    /**
     * 创建工作站点 (需要先通过测试连接)
     */
    Workstation create(Workstation workstation, String userId);

    /**
     * 更新工作站点
     */
    Workstation update(Long id, Workstation workstation, String userId);

    /**
     * 删除工作站点
     */
    void delete(Long id);

    /**
     * 测试SSH连接
     * @return 包含连接结果和服务器资源信息
     */
    Map<String, Object> testConnection(Workstation workstation);

    /**
     * 检查服务器状态 (心跳检测)
     */
    Map<String, Object> checkStatus(Long id);

    /**
     * 批量心跳检测 (定时任务调用)
     */
    void heartbeatCheck();

    /**
     * 检查名称是否已存在
     */
    boolean isNameExists(String name);

    /**
     * 检查主机名是否已存在
     */
    boolean isHostnameExists(String hostname);

    /**
     * 获取在线的工作站点列表
     */
    List<Workstation> getOnlineWorkstations();

    /**
     * 根据主机名获取工作站点
     */
    Workstation getByHostname(String hostname);

    /**
     * 获取工作站点的SSH凭证（解密后的密码/私钥）
     * 用于部署模块进行SSH连接
     */
    Map<String, Object> getCredentials(Long id);
}
