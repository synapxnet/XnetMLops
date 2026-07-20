package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能助手会话实体
 */
@Data
public class AssistantConversation {
    private Long id;
    private String uid;
    private Long assistantId;
    private String title;
    private String summary;

    // 用户信息
    private String userId;
    private String userName;

    // 状态
    private String status;         // active, archived, deleted
    private Integer messageCount;
    private Long tokenCount;

    // 时间
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
