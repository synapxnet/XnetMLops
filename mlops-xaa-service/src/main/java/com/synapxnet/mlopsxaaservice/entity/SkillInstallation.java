package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 技能安装记录实体类
 * 记录租户安装的技能
 */
@Data
public class SkillInstallation {
    private Long id;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 租户UID
     */
    private String tenantUid;

    /**
     * 安装者ID
     */
    private String installedBy;

    /**
     * 安装时间
     */
    private Date installedAt;

    /**
     * 配置覆盖（JSON格式）
     */
    private String configOverride;

    /**
     * 状态: active, disabled
     */
    private String status;
}
