package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;

@Data
public class PipelineConfigParams {
    // 基础配置
    private String description;
    private String defaultBranch;
    private String defaultEnvironment;

    // 数据集相关
    private String datasetPath;      // 数据集路径 (HDFS)
    private String datasetFolder;    // 本地文件夹名称 (新增)

    // 代码仓库相关
    private String gitRepoUrl;       // 代码仓库URL
    private String gitRepoId;        // 仓库ID (新增)
    private String gitRepoPassword;  // 仓库密码 (新增)
    private String gitBranch;        // 仓库分支 (新增)
    private String credentialsId;    // Jenkins凭证ID

    // 训练相关
    private String pythonEntryPoint; // Python入口脚本
    private String containerName;    // 容器名称
    private String modelOutputPath;  // 模型输出路径

    // 清理相关
    private String containerToRemove; // 要删除的容器名称

    // 算法版本信息 (新增)
    private String algorithmName;
    private String algorithmVersion;

    // doccker 容器拉取
    private String dockerImageName;
    private String dockerImageTags;
    private String harborUrl;
    private String harborCredentialsId;

    // 调度配置
    private String scheduleConfig;

    public String getScheduleConfig() {
        return scheduleConfig;
    }

    public void setScheduleConfig(String scheduleConfig) {
        this.scheduleConfig = scheduleConfig;
    }
}