package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 技能分类实体类
 */
@Data
public class SkillCategory {
    private Long id;

    /**
     * 分类标识 (如: document, creative, development)
     */
    private String key;

    /**
     * 分类名称（中文）
     */
    private String name;

    /**
     * 分类名称（英文）
     */
    private String nameEn;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 图标名称
     */
    private String icon;

    /**
     * 主题颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    private Date createdAt;
    private Date updatedAt;
}
