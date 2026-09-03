package com.synapxnet.mlopsmtpservice.recommendation;

import lombok.Data;

import java.util.Date;

/** 保存推荐 DCN 训练的身份、审批、输入摘要、模型摘要和指标。 */
@Data
public class RecommendationTrainingRun {
    private Long id;
    private String runUid;
    private Long datasetId;
    private String tenantUid;
    private String userId;
    private String productVersion;
    private String approvalId;
    private String idempotencyKey;
    private String status;
    private String schemaDigestSha256;
    private String artifactDigestSha256;
    private String modelDigestSha256;
    private String artifactReference;
    private String metricsJson;
    private String errorSummary;
    private Date startedAt;
    private Date completedAt;
}
