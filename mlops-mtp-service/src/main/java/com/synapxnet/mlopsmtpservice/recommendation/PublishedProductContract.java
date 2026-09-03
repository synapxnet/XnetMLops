package com.synapxnet.mlopsmtpservice.recommendation;

/** 保存 PostgreSQL 已发布数据产品的服务端校验结果。 */
public record PublishedProductContract(
        String productVersion,
        String schemaDigestSha256,
        String artifactDigestSha256,
        long rowCount
) {
}
