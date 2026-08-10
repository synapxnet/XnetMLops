package com.synapxnet.mlopsxaaservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsxaaservice.dto.OpenXnetSkillImportRequest;
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
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public SkillService(SkillMapper skillMapper,
                        SkillCategoryMapper categoryMapper,
                        SkillInstallationMapper installationMapper,
                        ObjectMapper objectMapper) {
        this.skillMapper = skillMapper;
        this.categoryMapper = categoryMapper;
        this.installationMapper = installationMapper;
        this.objectMapper = objectMapper;
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
     * 幂等导入一个 OpenXnet 企业 Skill 候选，始终保持草稿状态且不创建安装记录。
     *
     * @param request 已通过委托令牌校验的候选包
     * @param creatorId 已认证企业账户主体
     * @return 新建、更新或摘要相同的仓库草稿
     */
    @Transactional
    public Skill importOpenXnetSkill(OpenXnetSkillImportRequest request, String creatorId) {
        validateOpenXnetImport(request);
        String repositoryUid = createOpenXnetRepositoryUid(request.getWorkspaceId(), request.getSkillId());
        Skill existing = skillMapper.selectByUid(repositoryUid);
        if (existing != null && !request.getWorkspaceId().equals(existing.getTenantUid())) {
            throw new IllegalArgumentException("OpenXnet 技能仓库租户不匹配");
        }
        if (existing != null && request.getArtifactDigest().equals(readArtifactDigest(existing.getConfigJson()))) {
            return existing;
        }
        Skill imported = existing == null ? new Skill() : existing;
        imported.setUid(repositoryUid);
        imported.setName(request.getName().trim());
        imported.setDescription(normalizeOptionalText(request.getDescription()));
        imported.setType("workflow");
        imported.setStatus("draft");
        imported.setCategory("enterprise");
        imported.setTags(createOpenXnetTags(request));
        imported.setContentMd(request.getContentMd());
        imported.setConfigJson(createOpenXnetConfigJson(request));
        imported.setIcon("apartment");
        imported.setHasScripts(hasFilePrefix(request.getFiles(), "scripts/"));
        imported.setHasReferences(hasFilePrefix(request.getFiles(), "references/"));
        imported.setHasAssets(hasFilePrefix(request.getFiles(), "assets/"));
        imported.setSourceUrl("openxnet://workspace/" + request.getWorkspaceId() + "/skills/" + request.getSkillId());
        imported.setIsOfficial(false);
        imported.setCreatorId(creatorId);
        imported.setTenantUid(request.getWorkspaceId());
        if (existing == null) {
            imported.setInstallCount(0);
            imported.setVersion(1);
            skillMapper.insert(imported);
            return imported;
        }
        imported.setVersion(Math.max(1, existing.getVersion() == null ? 1 : existing.getVersion() + 1));
        skillMapper.update(imported);
        return skillMapper.selectById(imported.getId());
    }

    /**
     * 校验 OpenXnet 候选包的 ID、摘要、文本预算和文件清单。
     *
     * @param request 候选包
     */
    private void validateOpenXnetImport(OpenXnetSkillImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("OpenXnet 技能导入请求不能为空");
        }
        requireIdentifier(request.getWorkspaceId(), "Workspace ID");
        requireIdentifier(request.getSkillId(), "Skill ID");
        requireIdentifier(request.getFamilyId(), "Skill Family ID");
        requireText(request.getName(), "技能名称", 160, true);
        requireText(request.getDescription(), "技能描述", 16_000, false);
        requireText(request.getVersion(), "技能版本", 64, true);
        requireText(request.getLifecycleStatus(), "生命周期状态", 32, true);
        requireText(request.getEvidenceOrigin(), "证据来源", 32, true);
        requireText(request.getEnvironmentScope(), "环境范围", 32, true);
        if (!Set.of("candidate", "verified", "active", "deprecated", "retired", "legacy")
                .contains(request.getLifecycleStatus())) {
            throw new IllegalArgumentException("生命周期状态无效");
        }
        if (!Set.of("work", "rehearsal", "manual", "external", "legacy")
                .contains(request.getEvidenceOrigin())) {
            throw new IllegalArgumentException("证据来源无效");
        }
        if (!Set.of("synthetic", "simulation", "staging", "shadow", "canary", "production", "legacy")
                .contains(request.getEnvironmentScope())) {
            throw new IllegalArgumentException("环境范围无效");
        }
        if (request.getContentMd() == null
                || request.getContentMd().isBlank()
                || request.getContentMd().getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) {
            throw new IllegalArgumentException("SKILL.md 内容无效或超出 1 MiB");
        }
        if (request.getManifestJson() == null
                || request.getManifestJson().getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) {
            throw new IllegalArgumentException("OpenXnet Skill 清单无效或超出 1 MiB");
        }
        parseManifest(request.getManifestJson());
        if (request.getArtifactDigest() == null || !request.getArtifactDigest().matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("OpenXnet Skill 摘要无效");
        }
        if (!request.getArtifactDigest().equals(createArtifactDigest(request.getContentMd(), request.getManifestJson()))) {
            throw new IllegalArgumentException("OpenXnet Skill 内容与摘要不匹配");
        }
        List<String> files = request.getFiles() == null ? List.of() : request.getFiles();
        if (files.size() > 10_000) {
            throw new IllegalArgumentException("OpenXnet Skill 文件数量超出限制");
        }
        for (String file : files) {
            if (file == null || file.isBlank() || file.length() > 512
                    || file.startsWith("/") || file.contains("\\") || file.contains("..")) {
                throw new IllegalArgumentException("OpenXnet Skill 文件路径无效");
            }
        }
    }

    /**
     * 校验仓库稳定 ID。
     *
     * @param value ID 文本
     * @param label 错误标签
     */
    private void requireIdentifier(String value, String label) {
        if (value == null || !value.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")) {
            throw new IllegalArgumentException(label + " 无效");
        }
    }

    /**
     * 校验有界文本。
     *
     * @param value 文本
     * @param label 错误标签
     * @param maximumLength 最大字符数
     * @param required 是否必填
     */
    private void requireText(String value, String label, int maximumLength, boolean required) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(label + " 不能为空");
            }
            return;
        }
        if (value.length() > maximumLength || value.indexOf('\0') >= 0 || (required && value.isBlank())) {
            throw new IllegalArgumentException(label + " 无效");
        }
    }

    /**
     * 解析并校验 OpenXnet v2 清单对象。
     *
     * @param manifestJson 清单 JSON
     * @return JSON 对象节点
     */
    private JsonNode parseManifest(String manifestJson) {
        try {
            JsonNode manifest = objectMapper.readTree(manifestJson);
            if (manifest == null || !manifest.isObject()) {
                throw new IllegalArgumentException("OpenXnet Skill 清单必须是 JSON 对象");
            }
            return manifest;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("OpenXnet Skill 清单 JSON 无效", exception);
        }
    }

    /**
     * 为 Workspace 内的 Skill 生成稳定仓库 UID。
     *
     * @param workspaceId Workspace ID
     * @param skillId Skill ID
     * @return 固定 OPENXNET 前缀 UID
     */
    private String createOpenXnetRepositoryUid(String workspaceId, String skillId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((workspaceId + ":" + skillId).getBytes(StandardCharsets.UTF_8));
            return "OPENXNET-" + HexFormat.of().formatHex(digest).substring(0, 24).toUpperCase();
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 OpenXnet Skill 仓库 UID", exception);
        }
    }

    /**
     * 按 OpenXnet 契约重算候选包摘要。
     *
     * @param contentMd SKILL.md 内容
     * @param manifestJson OpenXnet 扩展清单
     * @return 小写 SHA-256 摘要
     */
    private String createArtifactDigest(String contentMd, String manifestJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(contentMd.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(manifestJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算 OpenXnet Skill 制品摘要", exception);
        }
    }

    /**
     * 构造 MLOps 仓库持久化的 OpenXnet 治理元数据。
     *
     * @param request 候选包
     * @return UTF-8 JSON 文本
     */
    private String createOpenXnetConfigJson(OpenXnetSkillImportRequest request) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("schema", "openxnet.mlops.skill-import.v1");
        config.put("source", "openxnet");
        config.put("workspace_id", request.getWorkspaceId());
        config.put("skill_id", request.getSkillId());
        config.put("family_id", request.getFamilyId());
        config.put("artifact_digest", request.getArtifactDigest());
        config.put("package_version", request.getVersion());
        config.put("lifecycle_status", request.getLifecycleStatus());
        config.put("evidence_origin", request.getEvidenceOrigin());
        config.put("environment_scope", request.getEnvironmentScope());
        config.put("production_eligible", Boolean.TRUE.equals(request.getProductionEligible()));
        config.put("files", request.getFiles() == null ? List.of() : request.getFiles());
        config.put("manifest", parseManifest(request.getManifestJson()));
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception exception) {
            throw new IllegalArgumentException("OpenXnet Skill 仓库配置无法序列化", exception);
        }
    }

    /**
     * 从已有仓库配置读取制品摘要。
     *
     * @param configJson 已有配置 JSON
     * @return 摘要或空字符串
     */
    private String readArtifactDigest(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return "";
        }
        try {
            JsonNode value = objectMapper.readTree(configJson).get("artifact_digest");
            return value != null && value.isTextual() ? value.asText() : "";
        } catch (Exception exception) {
            return "";
        }
    }

    /**
     * 构造仓库检索标签。
     *
     * @param request 候选包
     * @return 逗号分隔标签
     */
    private String createOpenXnetTags(OpenXnetSkillImportRequest request) {
        return String.join(",", List.of(
                "OpenXnet",
                "企业候选",
                request.getEnvironmentScope(),
                request.getLifecycleStatus()));
    }

    /**
     * 判断文件清单是否包含指定目录前缀。
     *
     * @param files 文件清单
     * @param prefix 目录前缀
     * @return 包含时返回 true
     */
    private boolean hasFilePrefix(List<String> files, String prefix) {
        return files != null && files.stream().anyMatch(file -> file.startsWith(prefix));
    }

    /**
     * 规范可选文本为空字符串。
     *
     * @param value 原始文本
     * @return 去空格文本
     */
    private String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim();
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
     * 查询当前 MLOps 用户可见的 OpenXnet 企业候选。
     *
     * @param creatorId 已认证用户主体
     * @param category 可选技能分类
     * @return 当前主体创建的 OpenXnet 草稿
     */
    public List<Skill> getOpenXnetCandidates(String creatorId, String category) {
        requireText(creatorId, "MLOps 用户主体", 160, true);
        if (category != null && !category.isBlank()) {
            requireText(category, "技能分类", 64, false);
        }
        return skillMapper.selectOpenXnetCandidatesByCreator(creatorId, normalizeOptionalText(category));
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
