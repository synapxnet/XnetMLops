package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 技能实体类 - XAS (Xnet-Agent-Skill)
 * 元技能仓库核心实体
 */
@Data
public class Skill {
    private Long id;
    private String uid;
    private String name;
    private String description;

    /**
     * 技能类型: tool, prompt, chain, workflow
     */
    private String type;

    /**
     * 技能状态: draft, published, archived
     */
    private String status;

    /**
     * 技能分类: document, creative, development, data, enterprise, ai-ml, utilities, custom
     */
    private String category;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * SKILL.md 内容
     */
    private String contentMd;

    /**
     * 技能配置JSON
     */
    private String configJson;

    /**
     * 技能图标
     */
    private String icon;

    /**
     * 是否包含脚本
     */
    private Boolean hasScripts;

    /**
     * 是否包含参考文档
     */
    private Boolean hasReferences;

    /**
     * 是否包含资源文件
     */
    private Boolean hasAssets;

    /**
     * 安装次数
     */
    private Integer installCount;

    /**
     * 来源URL
     */
    private String sourceUrl;

    /**
     * 是否官方技能
     */
    private Boolean isOfficial;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建者ID
     */
    private String creatorId;

    /**
     * 租户ID
     */
    private String tenantUid;

    private Date createdAt;
    private Date updatedAt;
}
