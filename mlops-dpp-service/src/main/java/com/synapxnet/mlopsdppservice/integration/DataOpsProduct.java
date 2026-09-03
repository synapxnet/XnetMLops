package com.synapxnet.mlopsdppservice.integration;

import java.time.OffsetDateTime;

/** 表示从 XnetDataOps 读取的只读数据产品契约。 */
public record DataOpsProduct(
        String productName,
        String productVersion,
        long rowCount,
        long positiveCount,
        long negativeCount,
        String schemaDigestSha256,
        String artifactDigestSha256,
        String lineageReference,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt
) {
}
