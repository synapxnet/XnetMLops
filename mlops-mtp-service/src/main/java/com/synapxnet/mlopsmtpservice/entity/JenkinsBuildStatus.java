package com.synapxnet.mlopsmtpservice.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JenkinsBuildStatus implements Serializable {
    private String parentTaskUID;
    private String jobName;
    private String buildUrl;
    private String queueUrl;
    private List<StageInfo> stages = new ArrayList<>();
    private String overallStatus;
    private String consoleOutput;
    private Date startTime;
    private Date endTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageInfo implements Serializable {
        private String stageName;
        private String status; // "SUCCESS", "FAILED", "IN_PROGRESS", "NOT_STARTED"
        private long durationMillis;
        private Date startTime;
    }
}
