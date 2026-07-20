package com.synapxnet.mlopsxaaservice.service;

import com.synapxnet.mlopsxaaservice.entity.Skill;
import com.synapxnet.mlopsxaaservice.entity.SkillCategory;
import com.synapxnet.mlopsxaaservice.entity.SkillInstallation;
import com.synapxnet.mlopsxaaservice.mapper.SkillMapper;
import com.synapxnet.mlopsxaaservice.mapper.SkillCategoryMapper;
import com.synapxnet.mlopsxaaservice.mapper.SkillInstallationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 技能服务 - XAS (Xnet-Agent-Skill)
 * 元技能仓库核心服务
 */
@Service
public class SkillService {

    private final SkillMapper skillMapper;
    private final SkillCategoryMapper categoryMapper;
    private final SkillInstallationMapper installationMapper;

    @Autowired
    public SkillService(SkillMapper skillMapper,
                        SkillCategoryMapper categoryMapper,
                        SkillInstallationMapper installationMapper) {
        this.skillMapper = skillMapper;
        this.categoryMapper = categoryMapper;
        this.installationMapper = installationMapper;
    }

    // ==================== 技能管理 ====================

    /**
     * 创建技能
     */
    @Transactional
    public Skill createSkill(Skill skill) {
        skill.setUid("SKILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (skill.getStatus() == null) {
            skill.setStatus("draft");
        }
        if (skill.getCategory() == null) {
            skill.setCategory("custom");
        }
        if (skill.getVersion() == null) {
            skill.setVersion(1);
        }
        if (skill.getInstallCount() == null) {
            skill.setInstallCount(0);
        }
        if (skill.getIsOfficial() == null) {
            skill.setIsOfficial(false);
        }

        skillMapper.insert(skill);
        return skill;
    }

    /**
     * 获取技能详情
     */
    public Skill getSkillById(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new RuntimeException("技能不存在: " + id);
        }
        return skill;
    }

    /**
     * 获取技能（通过UID）
     */
    public Skill getSkillByUid(String uid) {
        Skill skill = skillMapper.selectByUid(uid);
        if (skill == null) {
            throw new RuntimeException("技能不存在: " + uid);
        }
        return skill;
    }

    /**
     * 获取所有技能
     */
    public List<Skill> getAllSkills() {
        return skillMapper.selectAll();
    }

    /**
     * 获取已发布的技能（用于仓库浏览）
     */
    public List<Skill> getPublishedSkills() {
        return skillMapper.selectPublished();
    }

    /**
     * 根据状态获取技能
     */
    public List<Skill> getSkillsByStatus(String status) {
        return skillMapper.selectByStatus(status);
    }

    /**
     * 根据类型获取技能
     */
    public List<Skill> getSkillsByType(String type) {
        return skillMapper.selectByType(type);
    }

    /**
     * 根据分类获取技能
     */
    public List<Skill> getSkillsByCategory(String category) {
        return skillMapper.selectByCategory(category);
    }

    /**
     * 根据分类获取已发布的技能
     */
    public List<Skill> getPublishedSkillsByCategory(String category) {
        return skillMapper.selectPublishedByCategory(category);
    }

    /**
     * 搜索技能
     */
    public List<Skill> searchSkills(String keyword, String category) {
        return skillMapper.search(keyword, category);
    }

    /**
     * 更新技能
     */
    @Transactional
    public Skill updateSkill(Long id, Skill skill) {
        Skill existing = getSkillById(id);

        skill.setId(id);
        skill.setVersion(existing.getVersion() + 1);
        skillMapper.update(skill);

        return getSkillById(id);
    }

    /**
     * 发布技能
     */
    @Transactional
    public Skill publishSkill(Long id) {
        Skill skill = getSkillById(id);
        skill.setStatus("published");
        skillMapper.update(skill);
        return getSkillById(id);
    }

    /**
     * 归档技能
     */
    @Transactional
    public Skill archiveSkill(Long id) {
        Skill skill = getSkillById(id);
        skill.setStatus("archived");
        skillMapper.update(skill);
        return getSkillById(id);
    }

    /**
     * 删除技能
     */
    @Transactional
    public boolean deleteSkill(Long id) {
        return skillMapper.deleteById(id) > 0;
    }

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类
     */
    public List<SkillCategory> getAllCategories() {
        return categoryMapper.findAll();
    }

    /**
     * 根据Key获取分类
     */
    public SkillCategory getCategoryByKey(String key) {
        return categoryMapper.findByKey(key);
    }

    // ==================== 安装管理 ====================

    /**
     * 安装技能
     */
    @Transactional
    public void installSkill(Long skillId, String tenantUid, String userId) {
        // 检查是否已安装
        SkillInstallation existing = installationMapper.findBySkillIdAndTenantUid(skillId, tenantUid);
        if (existing != null) {
            throw new RuntimeException("技能已安装");
        }

        // 检查技能是否存在
        Skill skill = getSkillById(skillId);
        if (!"published".equals(skill.getStatus())) {
            throw new RuntimeException("只能安装已发布的技能");
        }

        // 创建安装记录
        SkillInstallation installation = new SkillInstallation();
        installation.setSkillId(skillId);
        installation.setTenantUid(tenantUid);
        installation.setInstalledBy(userId);
        installation.setStatus("active");
        installationMapper.insert(installation);

        // 增加安装次数
        skillMapper.incrementInstallCount(skillId);
    }

    /**
     * 卸载技能
     */
    @Transactional
    public void uninstallSkill(Long skillId, String tenantUid) {
        SkillInstallation existing = installationMapper.findBySkillIdAndTenantUid(skillId, tenantUid);
        if (existing == null) {
            throw new RuntimeException("技能未安装");
        }

        installationMapper.deleteBySkillIdAndTenantUid(skillId, tenantUid);

        // 减少安装次数
        skillMapper.decrementInstallCount(skillId);
    }

    /**
     * 检查技能是否已安装
     */
    public boolean isSkillInstalled(Long skillId, String tenantUid) {
        return installationMapper.findBySkillIdAndTenantUid(skillId, tenantUid) != null;
    }

    /**
     * 获取已安装的技能
     */
    public List<Skill> getInstalledSkills(String tenantUid) {
        List<SkillInstallation> installations = installationMapper.findByTenantUid(tenantUid);
        if (installations.isEmpty()) {
            return List.of();
        }

        List<Long> skillIds = installations.stream()
                .map(SkillInstallation::getSkillId)
                .collect(Collectors.toList());

        return skillMapper.selectByIds(skillIds);
    }

    /**
     * 获取已安装技能的安装信息
     */
    public List<SkillInstallation> getInstallations(String tenantUid) {
        return installationMapper.findByTenantUid(tenantUid);
    }
}
