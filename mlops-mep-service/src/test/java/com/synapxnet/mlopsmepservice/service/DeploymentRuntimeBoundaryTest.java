/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Unauthorized copying, distribution or use is forbidden.
 * 未接执行器时禁止模拟成功的回归测试。Regression tests against simulated success without a runtime.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.controller.ModelDeploymentController;
import com.synapxnet.mlopsmepservice.agent.RecommendationRuntimeClient;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import com.synapxnet.mlopsmepservice.mapper.ModelDeploymentMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeploymentRuntimeBoundaryTest {
    /** 启停扩缩容不应写入虚假状态。Runtime actions must not persist fictional states. */
    @Test
    void disconnectedActionsDoNotMutateRecords() {
        ModelDeploymentMapper mapper = mock(ModelDeploymentMapper.class);
        DeployNodeMapper nodes = mock(DeployNodeMapper.class);
        ModelDeploymentService service = new ModelDeploymentService(mapper, nodes, mock(RecommendationRuntimeClient.class));
        assertThrows(DeploymentRuntimeUnavailableException.class, () -> service.start(1L));
        assertThrows(DeploymentRuntimeUnavailableException.class, () -> service.stop(1L));
        assertThrows(DeploymentRuntimeUnavailableException.class, () -> service.restart(1L));
        assertThrows(DeploymentRuntimeUnavailableException.class, () -> service.scale(1L, 2));
        verifyNoInteractions(mapper, nodes);
    }

    /** 保存配置不会启动线程或伪造容器编号。Saving configuration must not launch threads or invent container IDs. */
    @Test
    void configurationRemainsPending() {
        ModelDeploymentMapper mapper = mock(ModelDeploymentMapper.class);
        DeployNodeMapper nodes = mock(DeployNodeMapper.class);
        ModelDeploymentService service = new ModelDeploymentService(mapper, nodes, mock(RecommendationRuntimeClient.class));
        ModelDeployment deployment = new ModelDeployment();
        deployment.setNodeUid("test-node");
        deployment.setContainerId("untrusted-client-container");
        ModelDeployment saved = service.create(deployment);
        assertEquals("pending", saved.getStatus());
        assertNull(saved.getContainerId());
        verify(mapper).insert(deployment);
        verifyNoMoreInteractions(mapper);
    }

    /** API错误与空数据保持明确语义。API errors retain explicit status and null data. */
    @Test
    void unavailableRuntimeHasExplicitHttpStatus() {
        ModelDeploymentController controller = new ModelDeploymentController(mock(ModelDeploymentService.class));
        var response = controller.runtimeUnavailable(new DeploymentRuntimeUnavailableException());
        assertEquals(503, response.getStatusCode().value());
        assertEquals(503, response.getBody().get("code"));
        assertNull(response.getBody().get("data"));
        assertTrue(response.getBody().get("message").toString().contains("尚未接入"));
    }

    /** 缺失资源采样器时不返回随机数或全零。Missing resource collectors must not fabricate metrics or zeros. */
    @Test
    void disconnectedNodeSamplerDoesNotReadOrWriteDatabase() {
        DeployNodeMapper mapper = mock(DeployNodeMapper.class);
        DeployNodeService service = new DeployNodeService(mapper);
        assertThrows(DeploymentRuntimeUnavailableException.class, () -> service.getResources(1L));
        verifyNoInteractions(mapper);
    }

    /** 不存在详情与删除回执允许空data。Missing details and deletion receipts permit null data. */
    @Test
    void nullableResponsesDoNotThrow() {
        ModelDeploymentService service = mock(ModelDeploymentService.class);
        ModelDeploymentController controller = new ModelDeploymentController(service);
        var missing = controller.getDeploymentById(1L);
        assertEquals(404, missing.getBody().get("code"));
        assertNull(missing.getBody().get("data"));
        var deleted = controller.deleteDeployment(1L);
        assertEquals(0, deleted.getBody().get("code"));
        assertNull(deleted.getBody().get("data"));
        verify(service).delete(1L);
    }
}
