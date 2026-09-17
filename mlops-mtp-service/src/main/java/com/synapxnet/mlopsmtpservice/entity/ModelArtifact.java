/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly forbidden to copy, distribute, or use without explicit authorization.
 * 模型制品登记实体。 / Model artifact registry entity.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-16 | Version: 1.0.0 | Security Level: INTERNAL
 */
package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;
import java.util.Date;

/** 可追踪的模型制品元数据。 / Traceable model artifact metadata. */
@Data
public class ModelArtifact {
    private Long id;
    private String uid;
    private String outputName;
    private String framework;
    private String domain;
    private String teamUid;
    private String teamName;
    private String tenantUid;
    private String deptUid;
    private String userId;
    private String description;
    private String artifactPath;
    private Date createdAt;
    private Date updatedAt;
}
