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
     * 保存工作流图（节点和边）
     */
    @Transactional
    public void saveWorkflowGraph(Long workflowId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        // 删除旧的节点和边
        edgeMapper.deleteByWorkflowId(workflowId);
        nodeMapper.deleteByWorkflowId(workflowId);

        // 插入新的节点
        if (nodes != null && !nodes.isEmpty()) {
            for (WorkflowNode node : nodes) {
                node.setWorkflowId(workflowId);
                if (node.getUid() == null) {
                    node.setUid(UUID.randomUUID().toString());
                }
            }
            nodeMapper.batchInsert(nodes);
        }

        // 插入新的边
        if (edges != null && !edges.isEmpty()) {
            for (WorkflowEdge edge : edges) {
                edge.setWorkflowId(workflowId);
                if (edge.getUid() == null) {
                    edge.setUid(UUID.randomUUID().toString());
                }
            }
            edgeMapper.batchInsert(edges);
        }
    }
}
