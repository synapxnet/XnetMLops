package com.synapxnet.mlopsmtpservice.mapper;

import com.synapxnet.mlopsmtpservice.entity.TaskInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaskScheduleInfoMapper {
    @Insert("INSERT INTO xnet_mlops_mtp_train_schedule_info (" +
            "uid, task_uid, job_uid, job_status, job_content, start_at, end_at, schedule_active " +
            ") VALUES (" +
            "#{uid}, #{task_uid}, #{job_uid}, #{job_status}, #{job_content}, #{start_at}, #{end_at}, #{schedule_active}" +
            ")")
    void insertTaskInfo(TaskInfo taskInfo);

    @Update("UPDATE xnet_mlops_mtp_train_schedule_info SET " +
            "task_uid = #{task_uid}, " +
            "job_status = #{job_status}, " +
            "job_content = #{job_content}, " +
            "start_at = #{start_at}, " +
            "end_at = #{end_at}, " +
            "schedule_active = #{schedule_active} " +
            "WHERE job_uid = #{job_uid}")
    int updateByJobUid(TaskInfo taskInfo);

    // 新增方法：根据task_uid查询所有相关任务
    @Select("SELECT job_uid, job_status, start_at, end_at, schedule_active " +
            "FROM xnet_mlops_mtp_train_schedule_info " +
            "WHERE task_uid = #{taskUid}")
    @Results({
            @Result(property = "job_uid", column = "job_uid"),
            @Result(property = "job_status", column = "job_status"),
            @Result(property = "start_at", column = "start_at"),
            @Result(property = "end_at", column = "end_at"),
            @Result(property = "schedule_active", column = "schedule_active")
    })
    List<TaskInfo> findByTaskUid(@Param("taskUid") String taskUid);

    // 新增方法：根据job_uid查询单个任务详情
    @Select("SELECT job_uid, job_status, job_content, start_at, end_at, schedule_active " +
            "FROM xnet_mlops_mtp_train_schedule_info " +
            "WHERE job_uid = #{jobUid}")
    @Results({
            @Result(property = "job_uid", column = "job_uid"),
            @Result(property = "job_status", column = "job_status"),
            @Result(property = "job_content", column = "job_content"),
            @Result(property = "start_at", column = "start_at"),
            @Result(property = "end_at", column = "end_at"),
            @Result(property = "schedule_active", column = "schedule_active")
    })
    TaskInfo findByJobUid(@Param("jobUid") String jobUid);

    @Delete("DELETE FROM xnet_mlops_mtp_train_schedule_info WHERE job_uid = #{jobUid}")
    int deleteByJobUid(@Param("jobUid") String jobUid);
}
