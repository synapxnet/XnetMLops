package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LLMService {
    private Long id;
    private String uid;
    private String name;
    private String type; // ollama, openai, deepseek, custom
    private String description;
    private String endpoint;
    private String modelName;
    private String apiKey;
    private String status; // running, stopped, error, deploying
    private String config; // JSON格式的配置
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
