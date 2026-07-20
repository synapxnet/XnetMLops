package com.synapxnet.mlopsdppservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class FeatureTaskInfo {
    private Long id;
    private String uid;
    private String taskUid;        // 特征工程任务UID
    private String jobUid;         // Jenkins Job UID
    private String jobStatus;      // 任务状态
    @JsonProperty("job_content")
    private String jobContent;     // 任务详情JSON
    private Date startAt;
    private Date endAt;
    private Integer scheduleActive; // 0: 立即执行, 1: 调度执行
    private Date createdAt;
    private Date updatedAt;
}
