package com.synapxnet.mlopsmtpservice.service;

import com.synapxnet.mlopsmtpservice.entity.TaskInfo;
import com.synapxnet.mlopsmtpservice.mapper.TaskInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskInfoService {

    private static final Logger logger = LoggerFactory.getLogger(TaskInfoService.class);

    private final TaskInfoMapper taskInfoMapper;

    @Autowired
    public TaskInfoService(TaskInfoMapper taskInfoMapper) {
        this.taskInfoMapper = taskInfoMapper;
    }

    @Transactional
    public void saveTaskInfo(TaskInfo taskInfo) {
        taskInfoMapper.insertTaskInfo(taskInfo);
    }

    @Transactional
    public void saveOrUpdateTaskInfo(TaskInfo taskInfo) {
        try {
            logger.info("TaskInfoService.saveOrUpdateTaskInfo 被调用: job_uid={}, task_uid={}",
                    taskInfo.getJob_uid(), taskInfo.getTask_uid());
            int updated = taskInfoMapper.updateByJobUid(taskInfo);
            logger.info("updateByJobUid 返回: {}", updated);
            if (updated == 0) {
                taskInfo.setUid(UUID.randomUUID().toString());
                logger.info("执行 insertTaskInfo, uid={}", taskInfo.getUid());
                taskInfoMapper.insertTaskInfo(taskInfo);
                logger.info("insertTaskInfo 完成");
            }
        } catch (Exception e) {
            logger.error("saveOrUpdateTaskInfo 执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 新增方法：根据task_uid获取所有任务信息
    public List<TaskInfo> getJobsByTaskUid(String taskUid) {
        return taskInfoMapper.findByTaskUid(taskUid);
    }

    // 新增方法：根据job_uid获取单个任务信息
    public TaskInfo getJobByJobUid(String jobUid) {
        return taskInfoMapper.findByJobUid(jobUid);
    }

    @Transactional
    public int deleteByTaskUid(String taskUid) {
        return taskInfoMapper.deleteByJobUid(taskUid);
    }

    public boolean deleteByJobUid(String jobUid) {
        try {
            int rowsDeleted = taskInfoMapper.deleteByJobUid(jobUid);
            return rowsDeleted > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除数据库记录失败: " + e.getMessage());
        }
    }
}
