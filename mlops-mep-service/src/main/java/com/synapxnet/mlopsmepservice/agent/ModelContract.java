/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
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
