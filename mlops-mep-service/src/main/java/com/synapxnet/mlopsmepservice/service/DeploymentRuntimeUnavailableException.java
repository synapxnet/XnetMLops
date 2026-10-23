/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Unauthorized copying, distribution or use is forbidden.
 * 真实部署能力缺失。Missing real deployment capability.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.service;

@org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
public class DeploymentRuntimeUnavailableException extends RuntimeException {
    /** 未接真实运行时不产生虚假执行回执。Never issue simulated execution receipts without a real runtime. */
    public DeploymentRuntimeUnavailableException() {
        super("真实部署执行器与运行观测尚未接入，未执行操作或生成模拟数据。");
    }
}
