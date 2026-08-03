package com.synapxnet.mlopsmepservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示持久化模型输入契约，不包含训练或推理样本。
 */
@Data
public class ModelContract {
    private Long id;
    private String uid;
    private String modelUid;
    private String modelVersion;
    private Integer inputDimension;
    private String inputSchemaJson;
    private String contractHash;
    private String source;
    private String createdBy;
    private LocalDateTime createdAt;
}
