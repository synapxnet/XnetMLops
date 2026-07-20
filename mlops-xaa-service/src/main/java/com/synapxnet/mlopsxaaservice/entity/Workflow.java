package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 工作流实体类 - XAW (Xnet-Agent-Workflow)
 * 参考Dify工作流设计，支持DPP/MTP/MEP任务编排
 */
@Data
public class Workflow {
    private Long id;
    private String uid;
    private String name;
    private String description;

    /**
     * 工作流类型: workflow, chat, pipeline
     */
    private String type;

    /**
     * 工作流状态: draft, published, archived
     */
    private String status;

    /**
     * 工作流图结构JSON (nodes + edges)
     */
    private String graphJson;

    /**
     * 工作流配置JSON
     */
    private String configJson;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建者ID
     */
    private String creatorId;

    /**
     * 创建者名称
     */
    private String creatorName;

    /**
     * 租户ID
     */
    private String tenantUid;

    /**
     * 部门ID
     */
    private String deptUid;

    /**
     * 团队ID
     */
    private String teamUid;

    private Date createdAt;
    private Date updatedAt;
}
