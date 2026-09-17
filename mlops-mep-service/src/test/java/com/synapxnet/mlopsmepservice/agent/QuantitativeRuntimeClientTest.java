/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** 验证量化运行时客户端不会接管其他模型部署。 */
class QuantitativeRuntimeClientTest {

    /** 确认存在测试构造器时生产构造器仍被明确注册为 Spring 注入入口。 */
    @Test
    void marksProductionConstructorForSpringInjection() throws NoSuchMethodException {
        assertTrue(QuantitativeRuntimeClient.class
                .getConstructor(String.class, String.class, ObjectMapper.class)
                .isAnnotationPresent(Autowired.class));
    }

    /** 确认客户端只识别固定 A 股研究部署标识。 */
    @Test
    void supportsOnlyAshareResearchDeployment() {
        QuantitativeRuntimeClient client = new QuantitativeRuntimeClient(
                "http://127.0.0.1:8091",
                "x".repeat(32),
                new ObjectMapper(),
                mock(HttpClient.class));
        assertTrue(client.supports("deploy_quant_ashare_research"));
        assertFalse(client.supports("deploy_quant_value_prod"));
        assertFalse(client.supports("deploy_recommendation_prod"));
    }
}
