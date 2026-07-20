package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.JenkinsVersion;

import java.util.List;
import java.util.Map;

/**
 * Jenkins 版本服务接口
 */
public interface JenkinsVersionService {

    /**
     * 获取所有稳定版本列表
     */
    List<JenkinsVersion> getStableVersions();

    /**
     * 获取所有LTS版本列表
     */
    List<JenkinsVersion> getLtsVersions();

    /**
     * 从 mirrors.jenkins.io 刷新版本列表
     * @return 同步结果
     */
    Map<String, Object> refreshVersions();

    /**
     * 获取版本统计信息
     */
    Map<String, Object> getVersionStats();
}
