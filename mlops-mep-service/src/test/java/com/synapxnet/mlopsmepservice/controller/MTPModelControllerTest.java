/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.service.MTPModelService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MTPModelControllerTest {

    /**
     * 验证控制器会把企业租户标识原样传给 MTP 服务，避免模型输出错误读取 default 租户。
     */
    @Test
    void shouldForwardTenantUidToMtpService() {
        MTPModelService service = mock(MTPModelService.class);
        List<Map<String, Object>> models = List.of(Map.of("uid", "TASK-DCN-1"));
        when(service.getOutputModels("TEN-SYNAPXNET")).thenReturn(models);
        MTPModelController controller = new MTPModelController(service);

        ResponseEntity<Map<String, Object>> response =
                controller.getMTPOutputModels("TEN-SYNAPXNET");

        assertEquals(models, response.getBody().get("data"));
        verify(service).getOutputModels("TEN-SYNAPXNET");
    }
}
