/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Copying, distribution or use requires explicit authorization.
 * DataOps 依赖不可用边界 / DataOps dependency availability boundary.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsdppservice.integration;

public class DataOpsUnavailableException extends RuntimeException {
    /** 只接收服务端编写的安全提示。 Accept only server-owned safe messages. */
    public DataOpsUnavailableException(String message) {
        super(message);
    }
}
