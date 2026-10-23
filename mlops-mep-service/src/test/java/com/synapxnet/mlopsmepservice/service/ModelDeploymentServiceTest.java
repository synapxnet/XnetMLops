/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.agent.RecommendationRuntimeClient;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import com.synapxnet.mlopsmepservice.mapper.ModelDeploymentMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证部署指标摘要只对推荐模型调用固定实时探针。
 */
class ModelDeploymentServiceTest {

    /** 推荐模型部署必须返回十次真实探针的聚合指标。 */
    @Test
    void returnsLiveRecommendationMetrics() {
        ModelDeploymentMapper deploymentMapper = mock(ModelDeploymentMapper.class);
        DeployNodeMapper nodeMapper = mock(DeployNodeMapper.class);
        RecommendationRuntimeClient runtimeClient = mock(RecommendationRuntimeClient.class);
        ModelDeployment deployment = new ModelDeployment();
        deployment.setId(3L);
        deployment.setUid("recommendation-deployment");
        deployment.setModelName("recommendation_dcn_vdcn_1");
        deployment.setModelVersion("dcn_1");
        when(deploymentMapper.findById(3L)).thenReturn(deployment);
        when(runtimeClient.monitor(10)).thenReturn(Map.of(
                "p99ResponseTimeMs", 82L,
                "successRate", 100.0));
        ModelDeploymentService service = new ModelDeploymentService(
                deploymentMapper,
                nodeMapper,
                runtimeClient);

        Map<String, Object> result = service.getMetricSummary(3L);

        assertEquals(82L, result.get("p99ResponseTimeMs"));
        assertEquals(100.0, result.get("successRate"));
        verify(runtimeClient).monitor(10);
    }
}
