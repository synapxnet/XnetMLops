/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
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
