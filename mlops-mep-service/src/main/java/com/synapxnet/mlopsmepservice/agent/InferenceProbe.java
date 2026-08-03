package com.synapxnet.mlopsmepservice.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示不保存原始输入输出的推理探针聚合记录。
 */
@Data
public class InferenceProbe {
    private Long id;
    private String uid;
    private String deploymentUid;
    private Long revisionNumber;
    private String testDatasetRef;
    private Integer sampleCount;
    private Integer successCount;
    private Integer errorCount;
    private BigDecimal errorRate;
    private BigDecimal p50Ms;
    private BigDecimal p95Ms;
    private Integer inputDimension;
    private String contractStatus;
    private String resultDigest;
    private String incidentId;
    private String traceId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
