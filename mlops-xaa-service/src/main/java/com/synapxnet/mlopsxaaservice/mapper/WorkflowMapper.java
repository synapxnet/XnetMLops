package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowMapper {

    /**
     * 创建工作流
     */
    int insert(Workflow workflow);

    /**
     * 根据ID查询工作流
     */
    Workflow selectById(@Param("id") Long id);

    /**
     * 根据UID查询工作流
     */
    Workflow selectByUid(@Param("uid") String uid);

    /**
     * 查询所有工作流
     */
    List<Workflow> selectAll();

    /**
     * 根据状态查询工作流
     */
    List<Workflow> selectByStatus(@Param("status") String status);

    /**
     * 根据创建者ID查询工作流
     */
    List<Workflow> selectByCreatorId(@Param("creatorId") String creatorId);

    /**
     * 根据租户ID查询工作流
     */
    List<Workflow> selectByTenantUid(@Param("tenantUid") String tenantUid);

    /**
     * 更新工作流
     */
    int update(Workflow workflow);

    /**
     * 更新工作流状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 删除工作流
     */
    int deleteById(@Param("id") Long id);

    /**
     * 检查名称是否存在
     */
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
}
