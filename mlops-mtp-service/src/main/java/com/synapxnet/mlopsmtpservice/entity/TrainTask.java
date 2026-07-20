package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class TrainTask {
    private Long id;
    private String uid;
    private String userId; // 保持驼峰式
    private String tenant_uid;
    private String task_name;
    private String task_type;
    private String encryption;
    private String task_zone;
    private String pod_type;
    private String resources;
    private String train_type;
    private String image_uid;
    private String image;
    private String description;
    private String algorithm_uid;
    private String algorithm_name;
    private String algorithm_version;
    private String task_route;
    private String train_config_content;
    private String train_config_format;
    private String notification_config;
    private String output_config;
    private String schedule_config;
    private Date created_at;
    private Date updated_at;
    private List<TaskDataset> datasets;
    private List<TaskCustomVariable> custom_variables; // 蛇形命名
}