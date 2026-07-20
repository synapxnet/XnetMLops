package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.WorkflowNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowNodeMapper {

    /**
     * 创建节点
     */
    int insert(WorkflowNode node);

    /**
     * 批量创建节点
     */
    int batchInsert(@Param("nodes") List<WorkflowNode> nodes);

    /**
     * 根据ID查询节点
     */
    WorkflowNode selectById(@Param("id") Long id);

    /**
     * 根据UID查询节点
     */
    WorkflowNode selectByUid(@Param("uid") String uid);

    /**
     * 根据工作流ID查询所有节点
     */
    List<WorkflowNode> selectByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 根据工作流ID和节点类型查询节点
     */
    List<WorkflowNode> selectByWorkflowIdAndType(
            @Param("workflowId") Long workflowId,
            @Param("nodeType") String nodeType);

    /**
     * 更新节点
     */
    int update(WorkflowNode node);

    /**
     * 删除节点
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据工作流ID删除所有节点
     */
    int deleteByWorkflowId(@Param("workflowId") Long workflowId);
}
