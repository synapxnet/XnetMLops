package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示 MEP 持久化采集的真实服务指标。
 */
@Data
public class ServiceMetric {
    private Long id;
    private String deploymentUid;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private Long requestCount;
    private Long errorCount;
    private Integer avgResponseTime;
    private LocalDateTime timestamp;
}
