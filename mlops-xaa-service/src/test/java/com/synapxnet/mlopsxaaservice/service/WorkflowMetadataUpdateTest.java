/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 工作流编辑持久化 / Workflow metadata persistence.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsxaaservice.service;

import com.synapxnet.mlopsxaaservice.entity.Workflow;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowMapper;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowNodeMapper;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowEdgeMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowMetadataUpdateTest {
    /** 编辑名称不能丢失已发布图。 Renaming must preserve the published graph. */
    @Test void metadataEditPreservesGraphAndPublication() {
        WorkflowMapper mapper = mock(WorkflowMapper.class);
        Workflow existing = new Workflow();
        existing.setId(7L); existing.setName("original"); existing.setType("workflow");
        existing.setStatus("published"); existing.setGraphJson("{\"nodes\":[1]}");
        existing.setConfigJson("{\"timeout\":30}"); existing.setVersion(4);
        when(mapper.selectById(7L)).thenReturn(existing);
        Workflow patch = new Workflow();
        patch.setName("updated"); patch.setDescription("new description");
        patch.setStatus("draft");
        new WorkflowService(mapper, mock(WorkflowNodeMapper.class), mock(WorkflowEdgeMapper.class))
            .updateWorkflow(7L, patch);
        verify(mapper).update(argThat(saved -> saved.getName().equals("updated")
            && saved.getStatus().equals("published") && saved.getVersion() == 5
            && saved.getType().equals("workflow")
            && saved.getGraphJson().equals(existing.getGraphJson())
            && saved.getConfigJson().equals(existing.getConfigJson())));
    }
}
