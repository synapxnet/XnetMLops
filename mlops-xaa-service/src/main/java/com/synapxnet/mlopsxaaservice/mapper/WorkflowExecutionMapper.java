package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowExecutionMapper {

    /**
     * 创建执行记录
     */
    int insert(WorkflowExecution execution);

    /**
     * 根据ID查询执行记录
     */
    WorkflowExecution selectById(@Param("id") Long id);

    /**
     * 根据UID查询执行记录
     */
    WorkflowExecution selectByUid(@Param("uid") String uid);

    /**
     * 根据工作流ID查询执行记录
     */
    List<WorkflowExecution> selectByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 根据状态查询执行记录
     */
    List<WorkflowExecution> selectByStatus(@Param("status") String status);

    /**
     * 查询正在运行的执行记录
     */
    List<WorkflowExecution> selectRunning();

    /**
     * 更新执行记录
     */
    int update(WorkflowExecution execution);

    /**
     * 更新执行状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    /**
     * 删除执行记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据工作流ID删除所有执行记录
     */
    int deleteByWorkflowId(@Param("workflowId") Long workflowId);
}
