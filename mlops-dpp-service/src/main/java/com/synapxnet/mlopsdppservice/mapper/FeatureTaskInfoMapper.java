package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.FeatureTaskInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FeatureTaskInfoMapper {

    @Results(id = "featureTaskInfoResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "taskUid", column = "task_uid"),
            @Result(property = "jobUid", column = "job_uid"),
            @Result(property = "jobStatus", column = "job_status"),
            @Result(property = "jobContent", column = "job_content"),
            @Result(property = "startAt", column = "start_at"),
            @Result(property = "endAt", column = "end_at"),
            @Result(property = "scheduleActive", column = "schedule_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_feature_task_info ORDER BY created_at DESC")
    List<FeatureTaskInfo> findAll();

    @ResultMap("featureTaskInfoResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_task_info WHERE id = #{id}")
    FeatureTaskInfo findById(Long id);

    @ResultMap("featureTaskInfoResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_task_info WHERE uid = #{uid}")
    FeatureTaskInfo findByUid(String uid);

    @ResultMap("featureTaskInfoResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_task_info WHERE task_uid = #{taskUid} ORDER BY created_at DESC")
    List<FeatureTaskInfo> findByTaskUid(String taskUid);

    @ResultMap("featureTaskInfoResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_task_info WHERE job_uid = #{jobUid}")
    FeatureTaskInfo findByJobUid(String jobUid);

    @ResultMap("featureTaskInfoResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_task_info WHERE task_uid = #{taskUid} AND schedule_active = #{scheduleActive} ORDER BY created_at DESC")
    List<FeatureTaskInfo> findByTaskUidAndScheduleActive(@Param("taskUid") String taskUid, @Param("scheduleActive") Integer scheduleActive);

    @Insert("INSERT INTO xnet_mlops_dpp_feature_task_info (" +
            "uid, task_uid, job_uid, job_status, job_content, start_at, end_at, schedule_active, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{taskUid}, #{jobUid}, #{jobStatus}, #{jobContent}, #{startAt}, #{endAt}, #{scheduleActive}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeatureTaskInfo taskInfo);

    @Update("UPDATE xnet_mlops_dpp_feature_task_info SET " +
            "job_status = #{jobStatus}, " +
            "job_content = #{jobContent}, " +
            "start_at = #{startAt}, " +
            "end_at = #{endAt}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(FeatureTaskInfo taskInfo);

    @Update("UPDATE xnet_mlops_dpp_feature_task_info SET " +
            "job_status = #{jobStatus}, " +
            "job_content = #{jobContent}, " +
            "start_at = #{startAt}, " +
            "end_at = #{endAt}, " +
            "updated_at = NOW() " +
            "WHERE job_uid = #{jobUid}")
    int updateByJobUid(FeatureTaskInfo taskInfo);

    @Delete("DELETE FROM xnet_mlops_dpp_feature_task_info WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM xnet_mlops_dpp_feature_task_info WHERE task_uid = #{taskUid}")
    int deleteByTaskUid(String taskUid);
}
