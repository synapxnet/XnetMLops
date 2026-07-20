package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.SkillInstallation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 技能安装记录 Mapper
 */
@Mapper
public interface SkillInstallationMapper {

    /**
     * 根据租户获取已安装技能
     */
    List<SkillInstallation> findByTenantUid(String tenantUid);

    /**
     * 检查技能是否已安装
     */
    SkillInstallation findBySkillIdAndTenantUid(@Param("skillId") Long skillId, @Param("tenantUid") String tenantUid);

    /**
     * 安装技能
     */
    int insert(SkillInstallation installation);

    /**
     * 更新安装状态
     */
    int update(SkillInstallation installation);

    /**
     * 卸载技能
     */
    int deleteBySkillIdAndTenantUid(@Param("skillId") Long skillId, @Param("tenantUid") String tenantUid);

    /**
     * 统计技能安装次数
     */
    int countBySkillId(Long skillId);
}
