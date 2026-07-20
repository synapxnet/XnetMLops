package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.SkillCategory;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 技能分类 Mapper
 */
@Mapper
public interface SkillCategoryMapper {

    /**
     * 获取所有分类
     */
    List<SkillCategory> findAll();

    /**
     * 根据ID获取分类
     */
    SkillCategory findById(Long id);

    /**
     * 根据Key获取分类
     */
    SkillCategory findByKey(String key);

    /**
     * 插入分类
     */
    int insert(SkillCategory category);

    /**
     * 更新分类
     */
    int update(SkillCategory category);

    /**
     * 删除分类
     */
    int deleteById(Long id);
}
