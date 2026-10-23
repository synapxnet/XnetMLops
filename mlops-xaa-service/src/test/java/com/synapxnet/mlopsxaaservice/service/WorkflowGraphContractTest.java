/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Unauthorized copying, distribution or use is forbidden.
 * 图保存外键映射与无效请求回归。Graph foreign-key mapping and invalid-request regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-14 | Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsxaaservice.service;

import com.synapxnet.mlopsxaaservice.entity.Workflow;
import com.synapxnet.mlopsxaaservice.entity.WorkflowNode;
import com.synapxnet.mlopsxaaservice.entity.WorkflowEdge;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowMapper;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowNodeMapper;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowEdgeMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowGraphContractTest {
    private final WorkflowMapper workflows = mock(WorkflowMapper.class);
    private final WorkflowNodeMapper nodes = mock(WorkflowNodeMapper.class);
    private final WorkflowEdgeMapper edges = mock(WorkflowEdgeMapper.class);
    private final WorkflowService service = new WorkflowService(workflows, nodes, edges);

    /** 构造属于当前图的输入节点。Create input nodes belonging to the current graph. */
    private WorkflowNode node(String uid, long oldId) {
        WorkflowNode node = new WorkflowNode(); node.setUid(uid); node.setId(oldId); return node;
    }

    /** 模拟数据库生成新主键，不复用客户端编号。Simulate generated keys rather than client IDs. */
    private void prepare() {
        when(workflows.selectById(7L)).thenReturn(new Workflow());
        doAnswer(invocation -> {
            WorkflowNode node = invocation.getArgument(0);
            assertNull(node.getId());
            node.setId("start".equals(node.getUid()) ? 101L : 102L);
            return 1;
        }).when(nodes).insert(any(WorkflowNode.class));
    }

    /** 画布UID在重新建图后仍连接到新主键。Canvas UIDs resolve to new keys after graph replacement. */
    @Test
    void uidEdgesResolveGeneratedKeys() {
        prepare();
        WorkflowEdge edge = new WorkflowEdge(); edge.setSourceNodeUid("start"); edge.setTargetNodeUid("end");
        service.saveWorkflowGraph(7L, List.of(node("start", 1L), node("end", 2L)), List.of(edge));
        assertEquals(101L, edge.getSourceNodeId()); assertEquals(102L, edge.getTargetNodeId());
        assertEquals(7L, edge.getWorkflowId());
        verify(edges).batchInsert(List.of(edge));
    }

    /** 兼容旧客户端的数字ID并重新映射。Remap numeric IDs from legacy clients. */
    @Test
    void legacyIdsResolveGeneratedKeys() {
        prepare();
        WorkflowEdge edge = new WorkflowEdge(); edge.setSourceNodeId(1L); edge.setTargetNodeId(2L);
        service.saveWorkflowGraph(7L, List.of(node("start", 1L), node("end", 2L)), List.of(edge));
        assertEquals(101L, edge.getSourceNodeId()); assertEquals(102L, edge.getTargetNodeId());
    }

    /** 悬空连线必须在删除旧图之前拒绝。Reject dangling edges before deleting persisted graph data. */
    @Test
    void danglingEdgesDoNotMutateDatabase() {
        when(workflows.selectById(7L)).thenReturn(new Workflow());
        WorkflowEdge edge = new WorkflowEdge(); edge.setSourceNodeUid("start"); edge.setTargetNodeUid("missing");
        assertThrows(IllegalArgumentException.class, () -> service.saveWorkflowGraph(7L, List.of(node("start", 1L)), List.of(edge)));
        verifyNoInteractions(nodes, edges);
    }

    /** 重复UID必须在删除旧图之前拒绝。Reject duplicate UIDs before deleting persisted graph data. */
    @Test
    void duplicateUidsDoNotMutateDatabase() {
        when(workflows.selectById(7L)).thenReturn(new Workflow());
        assertThrows(IllegalArgumentException.class, () -> service.saveWorkflowGraph(7L, List.of(node("start", 1L), node("start", 2L)), List.of()));
        verifyNoInteractions(nodes, edges);
    }
}
