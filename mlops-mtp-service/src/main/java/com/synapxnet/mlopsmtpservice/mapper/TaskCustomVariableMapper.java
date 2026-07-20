package com.synapxnet.mlopsmtpservice.mapper;

import com.synapxnet.mlopsmtpservice.entity.TaskCustomVariable;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskCustomVariableMapper {

    @Insert({
            "INSERT INTO XnetMLops.xnet_mlops_mtp_task_custom_variable (uid, task_uid, name, value)",
            "VALUES (#{uid}, #{task_uid}, #{name}, #{value})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTaskCustomVariable(TaskCustomVariable variable);

    @Delete("DELETE FROM XnetMLops.xnet_mlops_mtp_task_custom_variable WHERE task_uid = #{task_uid}")
    int deleteByTaskUid(@Param("task_uid") String taskUid);

    @Select("SELECT * FROM XnetMLops.xnet_mlops_mtp_task_custom_variable WHERE task_uid = #{task_uid}")
    List<TaskCustomVariable> findByTaskUid(@Param("task_uid") String taskUid);

    @Select("SELECT COUNT(*) > 0 FROM XnetMLops.xnet_mlops_mtp_task_custom_variable WHERE task_uid = #{taskUid}")
    boolean existsByTaskUid(String taskUid);
}