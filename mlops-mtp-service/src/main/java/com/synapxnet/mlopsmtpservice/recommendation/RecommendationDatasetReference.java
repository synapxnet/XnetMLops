package com.synapxnet.mlopsmtpservice.recommendation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 保存训练任务从 MLOps 元数据库读取的数据集与 DataOps 契约引用。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDatasetReference {
    private long datasetId;
    private String tenantUid;
    private String sourcePlatform;
    private String productVersion;
    private String schemaDigestSha256;
    private String artifactDigestSha256;
    private String importStatus;
}
