package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiKey {
    private Long id;
    private String uid;
    private String name;
    private String keyHash; // 存储加密后的key
    private String keyMasked; // 脱敏显示的key
    private String encryptedKey; // AES加密存储的原始密钥（用于OpenClaw等服务引用）
    private String provider; // ollama, openai, deepseek, custom
    private String description;
    private String status; // active, disabled, expired
    private Long usageLimit;
    private Long usageCount;
    private LocalDateTime expiresAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
