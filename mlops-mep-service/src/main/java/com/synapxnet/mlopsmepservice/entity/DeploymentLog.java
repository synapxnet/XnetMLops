package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeploymentLog {
    private Long id;
    private String deploymentUid;
    private String level; // info, warn, error
    private String message;
    private LocalDateTime timestamp;
}
