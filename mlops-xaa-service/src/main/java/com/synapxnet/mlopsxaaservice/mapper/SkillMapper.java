package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.Skill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillMapper {

    /**
     * 创建技能
     */
    int insert(Skill skill);

    /**
     * 根据ID查询技能
     */
    Skill selectById(@Param("id") Long id);

    /**
     * 根据UID查询技能
     */
    Skill selectByUid(@Param("uid") String uid);

    /**
     * 查询所有技能
     */
    List<Skill> selectAll();

    /**
     * 根据状态查询技能
     */
    List<Skill> selectByStatus(@Param("status") String status);

    /**
     * 根据类型查询技能
     */
    List<Skill> selectByType(@Param("type") String type);

    /**
     * 根据分类查询技能
     */
    List<Skill> selectByCategory(@Param("category") String category);

    /**
     * 获取已发布的技能
     */
    List<Skill> selectPublished();

    /**
     * 根据分类获取已发布的技能
     */
    List<Skill> selectPublishedByCategory(@Param("category") String category);

    /**
     * 按创建主体查询 OpenXnet 企业候选。
     *
     * @param creatorId 已认证用户主体
     * @param category 可选技能分类
     * @return 当前主体可见的候选草稿
     */
    List<Skill> selectOpenXnetCandidatesByCreator(
            @Param("creatorId") String creatorId,
            @Param("category") String category);

    /**
     * 搜索技能
     */
    List<Skill> search(@Param("keyword") String keyword, @Param("category") String category);

    /**
     * 根据ID列表查询技能
     */
    List<Skill> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 更新技能
     */
    int update(Skill skill);

    /**
     * 增加安装次数
     */
    int incrementInstallCount(@Param("id") Long id);

    /**
     * 减少安装次数
     */
    int decrementInstallCount(@Param("id") Long id);

    /**
     * 删除技能
     */
    int deleteById(@Param("id") Long id);
}
