package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeployNode {
    private Long id;
    private String uid;
    private String name;
    private String ipAddress;
    private Integer port;
    private String status; // online, offline, maintenance
    private Integer cpuCores;
    private Integer memoryGb;
    private String gpuInfo;
    private String dockerVersion;
    private String nginxStatus; // running, stopped
    private String labels; // JSON数组格式的标签
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
