package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;

@Data
public class TaskCustomVariable {
    private Long id;
    private String uid;
    private String task_uid;
    private String name;
    private String value;
}