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
