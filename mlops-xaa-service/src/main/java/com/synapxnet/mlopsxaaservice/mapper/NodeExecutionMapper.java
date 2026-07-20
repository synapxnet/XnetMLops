package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.NodeExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NodeExecutionMapper {

    /**
     * 创建节点执行记录
     */
    int insert(NodeExecution nodeExecution);

    /**
     * 批量创建节点执行记录
     */
    int batchInsert(@Param("nodeExecutions") List<NodeExecution> nodeExecutions);

    /**
     * 根据ID查询节点执行记录
     */
    NodeExecution selectById(@Param("id") Long id);

    /**
     * 根据执行ID查询所有节点执行记录
     */
    List<NodeExecution> selectByExecutionId(@Param("executionId") Long executionId);

    /**
     * 根据执行ID和节点ID查询节点执行记录
     */
    NodeExecution selectByExecutionIdAndNodeId(
            @Param("executionId") Long executionId,
            @Param("nodeId") Long nodeId);

    /**
     * 更新节点执行记录
     */
    int update(NodeExecution nodeExecution);

    /**
     * 更新节点执行状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    /**
     * 删除节点执行记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据执行ID删除所有节点执行记录
     */
    int deleteByExecutionId(@Param("executionId") Long executionId);
}
