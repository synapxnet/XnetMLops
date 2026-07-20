package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.entity.FeatureTaskInfo;
import com.synapxnet.mlopsdppservice.mapper.FeatureTaskInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FeatureTaskInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureTaskInfoService.class);

    @Autowired
    private FeatureTaskInfoMapper featureTaskInfoMapper;

    public List<FeatureTaskInfo> findAll() {
        return featureTaskInfoMapper.findAll();
    }

    public FeatureTaskInfo findById(Long id) {
        return featureTaskInfoMapper.findById(id);
    }

    public FeatureTaskInfo findByUid(String uid) {
        return featureTaskInfoMapper.findByUid(uid);
    }

    public List<FeatureTaskInfo> findByTaskUid(String taskUid) {
        return featureTaskInfoMapper.findByTaskUid(taskUid);
    }

    public FeatureTaskInfo findByJobUid(String jobUid) {
        return featureTaskInfoMapper.findByJobUid(jobUid);
    }

    public void saveOrUpdateTaskInfo(FeatureTaskInfo taskInfo) {
        try {
            FeatureTaskInfo existing = featureTaskInfoMapper.findByJobUid(taskInfo.getJobUid());
            if (existing != null) {
                taskInfo.setId(existing.getId());
                featureTaskInfoMapper.update(taskInfo);
                logger.info("更新任务信息: jobUid={}", taskInfo.getJobUid());
            } else {
                if (taskInfo.getUid() == null || taskInfo.getUid().isEmpty()) {
                    taskInfo.setUid("FTI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                }
                featureTaskInfoMapper.insert(taskInfo);
                logger.info("插入新任务信息: jobUid={}", taskInfo.getJobUid());
            }
        } catch (Exception e) {
            logger.error("保存或更新任务信息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    public int deleteById(Long id) {
        return featureTaskInfoMapper.deleteById(id);
    }

    public int deleteByTaskUid(String taskUid) {
        return featureTaskInfoMapper.deleteByTaskUid(taskUid);
    }
}
