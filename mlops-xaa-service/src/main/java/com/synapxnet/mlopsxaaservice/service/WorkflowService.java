package com.synapxnet.mlopsxaaservice.service;

import com.synapxnet.mlopsxaaservice.entity.Workflow;
import com.synapxnet.mlopsxaaservice.entity.WorkflowEdge;
import com.synapxnet.mlopsxaaservice.entity.WorkflowNode;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowEdgeMapper;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowMapper;
import com.synapxnet.mlopsxaaservice.mapper.WorkflowNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;

    @Autowired
    public WorkflowService(WorkflowMapper workflowMapper,
                          WorkflowNodeMapper nodeMapper,
                          WorkflowEdgeMapper edgeMapper) {
        this.workflowMapper = workflowMapper;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
    }

    /**
     * 创建工作流
     */
    @Transactional
    public Workflow createWorkflow(Workflow workflow) {
        // 检查名称是否存在
        if (workflowMapper.countByName(workflow.getName(), null) > 0) {
            throw new RuntimeException("工作流名称已存在: " + workflow.getName());
        }

        workflow.setUid(UUID.randomUUID().toString());
        if (workflow.getStatus() == null) {
            workflow.setStatus("draft");
        }
        if (workflow.getVersion() == null) {
            workflow.setVersion(1);
        }

        workflowMapper.insert(workflow);
        return workflow;
    }

    /**
     * 获取工作流详情（包含节点和边）
     */
    public Workflow getWorkflowById(Long id) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在: " + id);
        }
        return workflow;
    }

    /**
     * 获取工作流（通过UID）
     */
    public Workflow getWorkflowByUid(String uid) {
        Workflow workflow = workflowMapper.selectByUid(uid);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在: " + uid);
        }
        return workflow;
    }

    /**
     * 获取所有工作流
     */
    public List<Workflow> getAllWorkflows() {
        return workflowMapper.selectAll();
    }

    /**
     * 根据状态获取工作流
     */
    public List<Workflow> getWorkflowsByStatus(String status) {
        return workflowMapper.selectByStatus(status);
    }

    /**
     * 更新工作流
     */
    @Transactional
    public Workflow updateWorkflow(Long id, Workflow workflow) {
        Workflow existing = getWorkflowById(id);

        // 检查名称是否与其他工作流冲突
        if (workflow.getName() != null && !workflow.getName().equals(existing.getName())) {
            if (workflowMapper.countByName(workflow.getName(), id) > 0) {
                throw new RuntimeException("工作流名称已存在: " + workflow.getName());
            }
        }

        workflow.setId(id);
        workflow.setVersion(existing.getVersion() + 1);
        workflowMapper.update(workflow);

        return getWorkflowById(id);
    }

    /**
     * 发布工作流
     */
    @Transactional
    public Workflow publishWorkflow(Long id) {
        workflowMapper.updateStatus(id, "published");
        return getWorkflowById(id);
    }

    /**
     * 归档工作流
     */
    @Transactional
    public Workflow archiveWorkflow(Long id) {
        workflowMapper.updateStatus(id, "archived");
        return getWorkflowById(id);
    }

    /**
     * 删除工作流（级联删除节点和边）
     */
    @Transactional
    public boolean deleteWorkflow(Long id) {
        // 先删除边
        edgeMapper.deleteByWorkflowId(id);
        // 再删除节点
        nodeMapper.deleteByWorkflowId(id);
        // 最后删除工作流
        return workflowMapper.deleteById(id) > 0;
    }

    /**
     * 获取工作流的所有节点
     */
    public List<WorkflowNode> getWorkflowNodes(Long workflowId) {
        return nodeMapper.selectByWorkflowId(workflowId);
    }

    /**
     * 获取工作流的所有边
     */
    public List<WorkflowEdge> getWorkflowEdges(Long workflowId) {
        return edgeMapper.selectByWorkflowId(workflowId);
    }

    /**
     * 校验图后保存，并将画布UID映射到新数据库外键。
     * Validate the graph and map canvas UIDs to newly generated database foreign keys.
     */
    @Transactional
    public void saveWorkflowGraph(Long workflowId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        getWorkflowById(workflowId);
        List<WorkflowNode> graphNodes = nodes == null ? List.of() : nodes;
        List<WorkflowEdge> graphEdges = edges == null ? List.of() : edges;
        Map<String, WorkflowNode> nodesByUid = new LinkedHashMap<>();
        Map<Long, String> legacyIds = new LinkedHashMap<>();
        for (WorkflowNode node : graphNodes) {
            if (node == null) throw new IllegalArgumentException("工作流节点不能为空");
            if (node.getUid() == null || node.getUid().isBlank()) node.setUid(UUID.randomUUID().toString());
            if (nodesByUid.putIfAbsent(node.getUid(), node) != null) throw new IllegalArgumentException("工作流节点UID重复");
            if (node.getId() != null && legacyIds.putIfAbsent(node.getId(), node.getUid()) != null) throw new IllegalArgumentException("工作流节点ID重复");
        }
        // 在删除旧图前验证端点，防止无效请求破坏持久化图。Validate endpoints before deleting the old graph.
        for (WorkflowEdge edge : graphEdges) {
            if (edge == null) throw new IllegalArgumentException("工作流连线不能为空");
            String source = edge.getSourceNodeUid() != null ? edge.getSourceNodeUid() : legacyIds.get(edge.getSourceNodeId());
            String target = edge.getTargetNodeUid() != null ? edge.getTargetNodeUid() : legacyIds.get(edge.getTargetNodeId());
            if (!nodesByUid.containsKey(source) || !nodesByUid.containsKey(target)) throw new IllegalArgumentException("工作流连线引用了不存在的节点");
            edge.setSourceNodeUid(source);
            edge.setTargetNodeUid(target);
        }
        edgeMapper.deleteByWorkflowId(workflowId);
        nodeMapper.deleteByWorkflowId(workflowId);

        // 使用已有单条INSERT取回真实主键。Use the existing insert mapper to retrieve generated IDs.
        for (WorkflowNode node : graphNodes) {
            node.setId(null);
            node.setWorkflowId(workflowId);
            nodeMapper.insert(node);
            if (node.getId() == null) throw new IllegalStateException("节点主键未返回，工作流保存已回滚");
        }

        if (!graphEdges.isEmpty()) {
            for (WorkflowEdge edge : graphEdges) {
                edge.setId(null);
                edge.setWorkflowId(workflowId);
                edge.setSourceNodeId(nodesByUid.get(edge.getSourceNodeUid()).getId());
                edge.setTargetNodeId(nodesByUid.get(edge.getTargetNodeUid()).getId());
                if (edge.getUid() == null || edge.getUid().isBlank()) {
                    edge.setUid(UUID.randomUUID().toString());
                }
            }
            edgeMapper.batchInsert(graphEdges);
        }
    }
}
