package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.HadoopVersion;
import java.util.List;
import java.util.Map;

/**
 * Hadoop 版本服务接口
 */
public interface HadoopVersionService {

    /**
     * 获取所有版本
     */
    List<HadoopVersion> getAllVersions();

    /**
     * 获取稳定版本
     */
    List<HadoopVersion> getStableVersions();

    /**
     * 刷新版本列表（从 Apache 官网获取）
     */
    Map<String, Object> refreshVersions();

    /**
     * 获取版本统计信息
     */
    Map<String, Object> getVersionStats();
}
