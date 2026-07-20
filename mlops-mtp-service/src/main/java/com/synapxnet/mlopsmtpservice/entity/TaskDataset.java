package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;

@Data
public class TaskDataset {
    private String id;
    private String uid;
    private String task_uid;
    private String dataset_id;
    private String dataset_uid;
    private String dataset_name;
    private String dataset_file;
    private String bucket_identifier;
}