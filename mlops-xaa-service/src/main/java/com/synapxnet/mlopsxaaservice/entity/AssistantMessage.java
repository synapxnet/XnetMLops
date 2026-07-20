package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能助手消息实体
 */
@Data
public class AssistantMessage {
    private Long id;
    private String uid;
    private Long conversationId;
    private String role;           // user, assistant, system
    private String content;

    // 元数据
    private Integer tokenCount;
    private String modelUsed;
    private String ragSources;     // JSON数组，RAG来源文档
    private String toolCalls;      // JSON数组，工具调用记录
    private String metadata;       // JSON对象，其他元数据

    // 时间
    private LocalDateTime createdAt;
}
