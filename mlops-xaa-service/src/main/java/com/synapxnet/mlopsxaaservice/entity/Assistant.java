package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 智能助手实体
 */
@Data
public class Assistant {
    private Long id;
    private String uid;
    private String name;
    private String description;
    private String avatar;

    // OpenClaw配置
    private Long openclawInstanceId;  // 关联的OpenClaw实例ID
    private String gatewayUrl;         // Gateway HTTP URL
    private String gatewayToken;       // Gateway 认证令牌

    // 模型配置
    private Long llmServiceId;         // 关联的MEP LLM服务ID
    private String defaultModel;       // 默认模型
    private String systemPrompt;       // 系统提示词
    private BigDecimal temperature;    // 温度参数
    private Integer maxTokens;         // 最大token数

    // 知识库配置
    private String knowledgeBaseIds;   // 关联的DPP知识库ID列表，JSON数组
    private Boolean ragEnabled;        // 是否启用RAG
    private Integer ragTopK;           // RAG返回结果数

    // 技能配置
    private String skillIds;           // 关联的XAA技能ID列表，JSON数组
    private Boolean toolsEnabled;      // 是否启用工具调用

    // UI配置
    private String uiConfig;           // UI配置，JSON对象
    private String welcomeMessage;     // 欢迎消息
    private String placeholder;        // 输入框占位符

    // 状态
    private String status;             // active, disabled
    private Boolean isDefault;         // 是否为默认助手

    // 统计信息
    private Long totalConversations;
    private Long totalMessages;
    private LocalDateTime lastUsedAt;

    // 元数据
    private String tenantUid;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
