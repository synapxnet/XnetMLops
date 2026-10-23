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
 * 表示不含凭据明文且可回放的部署修订。
 */
@Data
public class DeploymentRevision {
    private Long id;
    private String uid;
    private String deploymentUid;
    private Long revisionNumber;
    private String modelUid;
    private String modelVersion;
    private String modelContractUid;
    private String imageName;
    private String specJson;
    private String specHash;
    private String createdBy;
    private LocalDateTime createdAt;
    private String sourceActionId;
}
