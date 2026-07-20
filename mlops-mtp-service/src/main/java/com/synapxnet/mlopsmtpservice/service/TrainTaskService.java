package com.synapxnet.mlopsmtpservice.service;

import com.synapxnet.mlopsmtpservice.entity.*;
import com.synapxnet.mlopsmtpservice.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainTaskService {

    private final TrainTaskMapper trainTaskMapper;
    private final TaskDatasetMapper taskDatasetMapper;
    private final TaskCustomVariableMapper taskCustomVariableMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public TrainTask createOrUpdateTask(TrainTask task, String userId, String tenantUid) {
        task.setUserId(userId);
        task.setTenant_uid(tenantUid);

        // 判断是创建还是更新
        boolean isUpdate = false;
        Optional<TrainTask> existingTask = Optional.empty();

        if (task.getUid() != null && !task.getUid().trim().isEmpty()) {
            existingTask = trainTaskMapper.findByUidAndTenantUid(task.getUid(), tenantUid);
            isUpdate = existingTask.isPresent();
        }

        // 检查任务名称唯一性
        if (task.getTask_name() != null && !task.getTask_name().trim().isEmpty()) {
            int count = trainTaskMapper.countByTaskName(task.getTask_name(), tenantUid);
            if (isUpdate) {
                // 更新时，如果名称相同但不是同一个任务，则报错
                if (count > 0 && existingTask.isPresent() &&
                    !existingTask.get().getTask_name().equals(task.getTask_name())) {
                    throw new RuntimeException("任务名称已存在: " + task.getTask_name());
                }
            } else {
                // 创建时，如果名称已存在，则报错
                if (count > 0) {
                    throw new RuntimeException("任务名称已存在: " + task.getTask_name());
                }
            }
        }

        if (isUpdate) {
            // 更新任务
            int updated = trainTaskMapper.updateTrainTask(task);
            if (updated == 0) {
                throw new RuntimeException("Failed to update task: " + task.getUid());
            }

            // 删除旧的关联数据（数据集和自定义变量）
            taskDatasetMapper.deleteByTaskUid(task.getUid());
            taskCustomVariableMapper.deleteByTaskUid(task.getUid());
        } else {
            // 创建新任务 - 确保生成新UID
            if (task.getUid() != null) {
                // 如果已有uid但数据库中不存在，重新生成（防止前端传递随机uid）
                task.setUid(UUID.randomUUID().toString());
            } else {
                task.setUid(UUID.randomUUID().toString());
            }
            trainTaskMapper.insertTrainTask(task);
        }

        // 插入数据集关联
        if (task.getDatasets() != null && !task.getDatasets().isEmpty()) {
            for (TaskDataset dataset : task.getDatasets()) {
                dataset.setUid(UUID.randomUUID().toString());
                dataset.setTask_uid(task.getUid());
                taskDatasetMapper.insertTaskDataset(dataset);
            }
        }

        // 插入自定义变量
        if (task.getCustom_variables() != null && !task.getCustom_variables().isEmpty()) {
            for (TaskCustomVariable variable : task.getCustom_variables()) {
                variable.setUid(UUID.randomUUID().toString());
                variable.setTask_uid(task.getUid());
                taskCustomVariableMapper.insertTaskCustomVariable(variable);
            }
        }

        return task;
    }

    public Optional<TrainTask> findByUidAndTenantUid(String uid, String tenantUid) {
        return trainTaskMapper.findByUidAndTenantUid(uid, tenantUid);
    }

    public Optional<DockerFile> findImageByUid(String uid) {
        return trainTaskMapper.findByImageUid(uid);
    }

    public Optional<HarborRepository> findByHarborUid(String uid) {
        return trainTaskMapper.findByHarborUid(uid);
    }

    public List<TrainTask> findAllByTenantUid(String tenantUid) {
        List<TrainTask> trainTasks = trainTaskMapper.findAllByTenantUid(tenantUid);

        if (trainTasks == null || trainTasks.isEmpty()) {
            return trainTasks;
        }

        // 为每个任务单独查询关联数据
        for (TrainTask trainTask : trainTasks) {
            String taskUid = trainTask.getUid();
            List<TaskDataset> datasets = taskDatasetMapper.findByTaskUid(taskUid);
            List<TaskCustomVariable> customVariables = taskCustomVariableMapper.findByTaskUid(taskUid);

            trainTask.setDatasets(datasets);
            trainTask.setCustom_variables(customVariables);
        }

        return trainTasks;
    }

    @Transactional
    public void deleteTask(String uid, String tenantUid) {
        // 先删除关联数据
        taskDatasetMapper.deleteByTaskUid(uid);
        taskCustomVariableMapper.deleteByTaskUid(uid);

        // 再删除主任务
        trainTaskMapper.deleteByUidAndTenantUid(uid, tenantUid);
    }

    // 可选：添加批量插入数据集的性能优化方法
    @Transactional
    public int batchInsertDatasets(List<TaskDataset> datasets) {
        if (datasets == null || datasets.isEmpty()) {
            return 0;
        }

        // 批量设置UID
        for (TaskDataset dataset : datasets) {
            if (dataset.getUid() == null) {
                dataset.setUid(UUID.randomUUID().toString());
            }
        }

        return taskDatasetMapper.batchInsertTaskDataset(datasets);
    }
}
