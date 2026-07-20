package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 特征算子实体类
 * 定义不同类型的特征处理算子，用于数据转换和特征工程
 */
@Data
public class FeatureOperator {
    private Long id;
    private String uid;
    private String name;                    // 算子名称
    private String code;                    // 算子代码（唯一标识）
    private String description;             // 算子描述
    private String category;                // 算子类别: format_conversion, feature_transform, data_cleaning
    private String outputFormats;           // 支持的输出格式（JSON数组）: ["csv","parquet","tfrecord","txt"]
    private String featureColumns;          // 特征属性列配置（JSON数组）
    private String parameterSchema;         // 参数配置模板（JSON Schema）
    private Integer sortOrder;              // 排序顺序
    private Boolean enabled;                // 是否启用
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
