/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 工作流图 HTTP 契约 / Workflow graph HTTP contract.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsxaaservice.controller;

import com.synapxnet.mlopsxaaservice.entity.WorkflowNode;
import com.synapxnet.mlopsxaaservice.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkflowGraphHttpTest {
    /** 真实 JSON 应反序列化成节点，保留坐标零值。 Real JSON must yield entities and preserve zero coordinates. */
    @Test void bindsNodeAndEdgeEntities() throws Exception {
        WorkflowService service = mock(WorkflowService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new WorkflowController(service)).build();
        mvc.perform(post("/api/xaa/workflows/1/graph").contentType(MediaType.APPLICATION_JSON).content("""
            {"nodes":[{"uid":"start-1","nodeType":"start","title":"Start","positionX":0,"positionY":0}],"edges":[]}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(service).saveWorkflowGraph(eq(1L), argThat(nodes -> nodes.size() == 1
            && nodes.get(0) instanceof WorkflowNode
            && nodes.get(0).getUid().equals("start-1")
            && nodes.get(0).getPositionX() == 0.0), argThat(java.util.List::isEmpty));
    }

    /** 缺失图列表不得清空已存图。 Missing graph lists must not erase persisted graphs. */
    @Test void incompleteGraphDoesNotReachPersistence() throws Exception {
        WorkflowService service = mock(WorkflowService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new WorkflowController(service)).build();
        mvc.perform(post("/api/xaa/workflows/1/graph").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
