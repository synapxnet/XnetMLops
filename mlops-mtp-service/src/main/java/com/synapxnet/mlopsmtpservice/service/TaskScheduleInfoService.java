package com.synapxnet.mlopsmtpservice.service;

import com.synapxnet.mlopsmtpservice.entity.TaskInfo;
import com.synapxnet.mlopsmtpservice.mapper.TaskScheduleInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskScheduleInfoService {

    private final TaskScheduleInfoMapper taskScheduleInfoMapper;

    @Autowired
    public TaskScheduleInfoService(TaskScheduleInfoMapper taskScheduleInfoMapper) {
        this.taskScheduleInfoMapper = taskScheduleInfoMapper;
    }

    @Transactional
    public void saveTaskInfo(TaskInfo taskInfo) {
        taskScheduleInfoMapper.insertTaskInfo(taskInfo);
    }

    @Transactional
    public void saveOrUpdateTaskInfo(TaskInfo taskInfo) {
        int updated = taskScheduleInfoMapper.updateByJobUid(taskInfo);
        if (updated == 0) {
            taskInfo.setUid(UUID.randomUUID().toString());
            taskScheduleInfoMapper.insertTaskInfo(taskInfo);
        }
    }

    // 新增方法：根据task_uid获取所有任务信息
    public List<TaskInfo> getJobsByTaskUid(String taskUid) {
        return taskScheduleInfoMapper.findByTaskUid(taskUid);
    }

    // 新增方法：根据job_uid获取单个任务信息
    public TaskInfo getJobByJobUid(String jobUid) {
        return taskScheduleInfoMapper.findByJobUid(jobUid);
    }

    @Transactional
    public int deleteByTaskUid(String taskUid) {
        return taskScheduleInfoMapper.deleteByJobUid(taskUid);
    }

    public boolean deleteByJobUid(String jobUid) {
        try {
            int rowsDeleted = taskScheduleInfoMapper.deleteByJobUid(jobUid);
            return rowsDeleted > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除数据库记录失败: " + e.getMessage());
        }
    }
}