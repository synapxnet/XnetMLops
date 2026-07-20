package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.util.Date;

@Data
public class FeatureEngineering {
    private Long id;
    private String uid;
    private String name;
    private String description;
    private Long datasourceId;
    private String datasourceName;
    private String database;
    private String tableName;
    private String selectedColumns;    // JSON格式存储选中的字段
    private String transformConfig;    // JSON格式存储转换配置
    private String zone;               // 数据区域
    private String bucketUid;          // 存储桶UID
    private String bucketName;         // 存储桶名称

    // 新增字段：子数据域、数据类型、是否加密
    private String subDataArea;        // 子数据域
    private String dataType;           // 数据类型
    private Boolean encryption;        // 是否需要加密

    // 输出配置
    private Boolean pushToDataset;     // 是否推送至数据集
    private Long targetDatasetId;      // 目标数据集ID（已有数据集）
    private String targetDatasetName;  // 目标数据集名称（新建或已有）
    private String outputPath;         // 输出路径（不推送至数据集时使用）

    // 特征算子相关字段
    private Long operatorId;           // 特征算子ID
    private String operatorCode;       // 特征算子代码
    private String operatorName;       // 特征算子名称
    private String outputFormat;       // 输出文件格式: csv, parquet, tfrecord, txt, json
    private String featureConfig;      // 字段特征配置（JSON格式，每个字段的类型、默认值等）
    private String operatorParams;     // 算子参数配置（JSON格式）

    // 镜像相关字段
    private String imageUid;           // 镜像UID
    private String imageName;          // 镜像名称
    private String imageTag;           // 镜像标签
    private String harborUrl;          // Harbor仓库地址
    private String harborCredentialsId; // Harbor凭证ID

    // 调度配置
    private String scheduleConfig;     // 调度配置（JSON格式）
    private Boolean scheduleActive;    // 是否启用调度

    // 通知配置
    private String notificationConfig; // 通知配置（JSON格式）
    private Boolean notificationEnabled; // 是否启用通知

    // Jenkins相关
    private String jobUid;             // Jenkins任务UID
    private String lastBuildStatus;    // 最后构建状态

    private String status;             // draft, processing, completed, failed
    private String createdBy;
    private String teamUid;
    private String teamName;
    private Date createdAt;
    private Date updatedAt;
}
