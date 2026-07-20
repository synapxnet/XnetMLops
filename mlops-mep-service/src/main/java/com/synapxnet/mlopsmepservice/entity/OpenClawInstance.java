package com.synapxnet.mlopsmepservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * OpenClaw实例实体
 * 用于管理OpenClaw个人助手的部署实例
 */
@Data
public class OpenClawInstance {
    private Long id;
    private String uid;
    private String name;
    private String description;

    // 部署配置
    private String deployMode;        // docker, npm, source
    private String deployNodeId;      // 部署节点ID
    private Long workstationId;       // SMP工作站ID
    private String gatewayHost;       // Gateway主机地址
    private Integer gatewayPort;      // Gateway端口，默认18789
    private String gatewayToken;      // Gateway访问令牌

    // 模型配置
    private String defaultModel;      // 主模型（primary），如 anthropic/claude-opus-4-6
    private String fallbackModels;    // 回退模型列表，JSON数组（主模型失败时按顺序尝试）
    private String subagentModel;     // 子代理模型（用于子任务的轻量模型）
    private Long llmServiceId;        // 关联的MEP LLM服务ID
    private String apiKeyId;          // 加密后的API密钥（AES加密存储，直接输入时使用）
    private Long apiKeyRefId;         // 引用MEP API密钥管理模块中的密钥ID

    // 技能配置
    private String enabledSkills;     // 启用的技能列表，JSON数组
    private String skillsConfig;      // 技能配置，JSON对象

    // 渠道配置
    private String channelsConfig;    // 渠道配置，JSON对象

    // 运行状态
    private String status;            // stopped, starting, running, error
    private String containerName;     // Docker容器名称
    private String processId;         // 进程ID
    private String lastError;         // 最后错误信息

    // 资源配置
    private String workspacePath;     // 工作区路径
    private String configPath;        // 配置文件路径
    private String logsPath;          // 日志路径

    // 统计信息
    private Long totalConversations;  // 总对话数
    private Long totalMessages;       // 总消息数
    private LocalDateTime lastActiveAt; // 最后活跃时间

    // 元数据
    private String tenantUid;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
