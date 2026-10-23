/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Copying, distribution or use requires explicit authorization.
 * 节点与模型服务空响应回归 / Nullable node and model service response regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import com.synapxnet.mlopsmepservice.entity.LLMService;
import com.synapxnet.mlopsmepservice.service.DeployNodeService;
import com.synapxnet.mlopsmepservice.service.DeploymentRuntimeUnavailableException;
import com.synapxnet.mlopsmepservice.service.LLMServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NullableResponseContractTests {
    /** 缺失节点返回业务404，不因空data崩溃。 Missing nodes return the domain 404 without a null-map failure. */
    @Test void missingNodeReturnsNotFound() throws Exception {
        DeployNodeService service = mock(DeployNodeService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DeployNodeController(service)).build();
        mvc.perform(get("/mep/nodes/999")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(post("/mep/nodes/999/refresh")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
    }

    /** 删除与维护操作允许空结果。 Delete and maintenance operations support nullable result data. */
    @Test void nodeMutationsReturnNullableData() throws Exception {
        DeployNodeService service = mock(DeployNodeService.class);
        when(service.findById(1L)).thenReturn(new DeployNode());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DeployNodeController(service)).build();
        mvc.perform(post("/mep/nodes/1/maintenance").contentType(MediaType.APPLICATION_JSON).content("{\"maintenance\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(delete("/mep/nodes/1")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(service).setMaintenance(1L, true);
        verify(service).delete(1L);
    }

    /** 缺失维护状态在写入前拒绝。 Reject a missing maintenance flag before mutation. */
    @Test void rejectsMissingMaintenanceFlag() throws Exception {
        DeployNodeService service = mock(DeployNodeService.class);
        MockMvcBuilders.standaloneSetup(new DeployNodeController(service)).build()
                .perform(post("/mep/nodes/1/maintenance").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /** 资源采集不可用由HTTP503清楚表达。 Unavailable resource collection returns HTTP 503. */
    @Test void resourceCollectorMissingIsUnavailable() throws Exception {
        DeployNodeService service = mock(DeployNodeService.class);
        when(service.getResources(1L)).thenThrow(new DeploymentRuntimeUnavailableException());
        MockMvcBuilders.standaloneSetup(new DeployNodeController(service)).build()
                .perform(get("/mep/nodes/1/resources")).andExpect(status().isServiceUnavailable());
    }

    /** 缺失模型服务不会导致500。 Missing model services do not cause an internal server error. */
    @Test void missingModelServiceReturnsNotFound() throws Exception {
        LLMServiceService service = mock(LLMServiceService.class);
        MockMvcBuilders.standaloneSetup(new LLMServiceController(service)).build()
                .perform(get("/mep/llm-services/999")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
    }

    /** 模型连接配置的操作完成后保留空data。 Model connection mutations preserve nullable result data. */
    @Test void modelConnectionMutationsReturnNullableData() throws Exception {
        LLMServiceService service = mock(LLMServiceService.class);
        when(service.findById(1L)).thenReturn(new LLMService());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LLMServiceController(service)).build();
        mvc.perform(post("/mep/llm-services/1/start")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/mep/llm-services/1/stop")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(delete("/mep/llm-services/1")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(service).start(1L);
        verify(service).stop(1L);
        verify(service).delete(1L);
    }
}
