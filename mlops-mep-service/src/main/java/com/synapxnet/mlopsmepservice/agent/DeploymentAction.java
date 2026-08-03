package com.synapxnet.mlopsmepservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示持久化、幂等且可在进程重启后审计的部署动作。
 */
@Data
public class DeploymentAction {
    private Long id;
    private String uid;
    private String deploymentUid;
    private String actionType;
    private Long fromRevision;
    private Long targetRevision;
    private String status;
    private String stage;
    private String requestId;
    private String workspaceId;
    private String incidentId;
    private String traceId;
    private String approvalId;
    private String idempotencyKey;
    private String requestDigest;
    private String expectedResourceVersion;
    private String verificationPolicyJson;
    private Boolean dryRun;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
