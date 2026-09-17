package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ModelDeployment {
    private Long id;
    private String uid;
    private String name;
    private String modelSource; // mtp, llm
    private String modelUid;
    private String modelName;
    private String modelVersion;
    private String nodeUid;
    private String nodeName;
    private String status; // pending, deploying, running, failed, stopped
    private String containerId;
    private String containerName;
    private String imageName;
    private Integer port;
    private String endpoint;
    private Integer replicas;
    private String resourceConfig; // JSON格式的资源配置
    private String nginxConfig; // JSON格式的Nginx配置
    private String healthCheck; // JSON格式的健康检查配置
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long activeRevision;
    private Long resourceVersion;
    private LocalDateTime lastVerifiedAt;
}
