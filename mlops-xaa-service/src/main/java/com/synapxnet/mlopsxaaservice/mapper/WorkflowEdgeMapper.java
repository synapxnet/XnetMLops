package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.WorkflowEdge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowEdgeMapper {

    /**
     * 创建边
     */
    int insert(WorkflowEdge edge);

    /**
     * 批量创建边
     */
    int batchInsert(@Param("edges") List<WorkflowEdge> edges);

    /**
     * 根据ID查询边
     */
    WorkflowEdge selectById(@Param("id") Long id);

    /**
     * 根据工作流ID查询所有边
     */
    List<WorkflowEdge> selectByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 根据源节点ID查询边
     */
    List<WorkflowEdge> selectBySourceNodeId(@Param("sourceNodeId") Long sourceNodeId);

    /**
     * 根据目标节点ID查询边
     */
    List<WorkflowEdge> selectByTargetNodeId(@Param("targetNodeId") Long targetNodeId);

    /**
     * 更新边
     */
    int update(WorkflowEdge edge);

    /**
     * 删除边
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据工作流ID删除所有边
     */
    int deleteByWorkflowId(@Param("workflowId") Long workflowId);
}
