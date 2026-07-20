package com.synapxnet.mlopsmtpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
@Data
public class TaskInfo {
    private String id;
    private String uid;
    private String task_uid;
    private String job_uid;
    private String job_status;
    @JsonProperty("job_content")
    private String job_content;
    private Date start_at;
    private Date end_at;
    private int schedule_active;
}
