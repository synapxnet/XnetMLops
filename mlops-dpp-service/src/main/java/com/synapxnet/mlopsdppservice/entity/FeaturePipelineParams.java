package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;

@Data
public class FeaturePipelineParams {
    // 基础配置
    private String description;
    private String taskName;
    private String taskUid;

    // 数据源相关
    private String datasourceName;
    private String database;
    private String tableName;
    private String selectedColumns;    // JSON格式

    // 特征工程配置
    private String operatorCode;
    private String operatorName;
    private String outputFormat;
    private String featureConfig;      // JSON格式
    private String operatorParams;     // JSON格式

    // 输出配置
    private Boolean pushToDataset;
    private String targetDatasetName;
    private String outputPath;
    private Boolean encryption;

    // 存储配置
    private String zone;
    private String bucketUid;
    private String bucketName;

    // Docker镜像相关
    private String dockerImageName;
    private String dockerImageTag;
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
