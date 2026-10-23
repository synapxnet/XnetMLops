/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.mapper.DeployNodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ServerSocket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证部署节点探测只记录真实可验证状态。 */
@ExtendWith(MockitoExtension.class)
class DeployNodeServiceTest {

    @Mock
    private DeployNodeMapper deployNodeMapper;

    @InjectMocks
    private DeployNodeService deployNodeService;

    /** 验证 TCP 测试成功时不会返回伪造的 Docker 或 Nginx 状态。 */
    @Test
    void shouldNotFabricateRuntimeMetadataWhenTcpConnectionSucceeds() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Map<String, Object> result = deployNodeService.testConnection(
                    "127.0.0.1", server.getLocalPort());

            assertTrue((Boolean) result.get("success"));
            assertNull(result.get("docker_version"));
            assertNull(result.get("nginx_status"));
            assertFalse((Boolean) result.get("runtime_verified"));
        }
    }

    /** 验证刷新在线状态时不会覆盖最近一次真实运行时核验结果。 */
    @Test
    void shouldPreserveVerifiedRuntimeMetadataWhenRefreshingConnectivity() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            DeployNode node = new DeployNode();
            node.setId(5L);
            node.setName("推荐模型部署节点");
            node.setIpAddress("127.0.0.1");
            node.setPort(server.getLocalPort());
            node.setDockerVersion("29.1.3");
            node.setNginxStatus("stopped");
            when(deployNodeMapper.findById(5L)).thenReturn(node);

            DeployNode refreshed = deployNodeService.refreshStatus(5L);

            assertEquals(node, refreshed);
            verify(deployNodeMapper).updateConnectivityStatus(5L, "online");
            verify(deployNodeMapper, never()).updateStatus(
                    anyLong(), any(), any(), any());
        }
    }

    /** 验证空节点地址会被明确判定为连接失败。 */
    @Test
    void shouldRejectBlankNodeAddress() {
        Map<String, Object> result = deployNodeService.testConnection(" ", 22);

        assertFalse((Boolean) result.get("success"));
        assertFalse((Boolean) result.get("runtime_verified"));
    }
}
