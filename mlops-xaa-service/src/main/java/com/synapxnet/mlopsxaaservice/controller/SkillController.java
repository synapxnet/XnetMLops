package com.synapxnet.mlopsxaaservice.controller;

import com.synapxnet.mlopsxaaservice.entity.Skill;
import com.synapxnet.mlopsxaaservice.entity.SkillCategory;
import com.synapxnet.mlopsxaaservice.entity.SkillInstallation;
import com.synapxnet.mlopsxaaservice.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能控制器 - XAS (Xnet-Agent-Skill)
 * 元技能仓库API
 */
@RestController
@RequestMapping("/api/xaa")
@CrossOrigin(origins = "*")
public class SkillController {

    private final SkillService skillService;

    @Autowired
    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    private static class ResponseUtils {
        static ResponseEntity<Map<String, Object>> success(Object data) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", data,
                    "error", "null"
            ));
        }

        static ResponseEntity<Map<String, Object>> success(String message, Object data) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", message,
                    "data", data,
                    "error", "null"
            ));
        }

        static ResponseEntity<Map<String, Object>> error(int code, String message) {
            return ResponseEntity.ok(Map.of(
                    "code", code,
                    "message", message,
                    "data", "null",
                    "error", message
            ));
        }
    }

    // ==================== 技能管理 API ====================

    /**
     * 创建技能
     */
    @PostMapping("/skills")
    public ResponseEntity<Map<String, Object>> createSkill(@RequestBody Skill skill) {
        try {
            Skill created = skillService.createSkill(skill);
            return ResponseUtils.success("技能创建成功", created);
        } catch (Exception e) {
            return ResponseUtils.error(500, "创建技能失败: " + e.getMessage());
        }
    }

    /**
     * 获取技能列表
     */
    @GetMapping("/skills")
    public ResponseEntity<Map<String, Object>> getAllSkills(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        try {
            List<Skill> skills;
            if (status != null && !status.isEmpty()) {
                skills = skillService.getSkillsByStatus(status);
            } else if (type != null && !type.isEmpty()) {
                skills = skillService.getSkillsByType(type);
            } else if (category != null && !category.isEmpty()) {
                skills = skillService.getSkillsByCategory(category);
            } else {
                skills = skillService.getAllSkills();
            }
            return ResponseUtils.success(skills);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取技能列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取仓库技能列表（已发布的技能）
     */
    @GetMapping("/skills/repository")
    public ResponseEntity<Map<String, Object>> getRepositorySkills(
            @RequestParam(required = false) String category) {
        try {
            List<Skill> skills;
            if (category != null && !category.isEmpty()) {
                skills = skillService.getPublishedSkillsByCategory(category);
            } else {
                skills = skillService.getPublishedSkills();
            }
            return ResponseUtils.success(skills);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取仓库技能失败: " + e.getMessage());
        }
    }

    /**
     * 搜索技能
     */
    @GetMapping("/skills/search")
    public ResponseEntity<Map<String, Object>> searchSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        try {
            List<Skill> skills = skillService.searchSkills(keyword, category);
            return ResponseUtils.success(skills);
        } catch (Exception e) {
            return ResponseUtils.error(500, "搜索技能失败: " + e.getMessage());
        }
    }

    /**
     * 获取技能详情
     */
    @GetMapping("/skills/{id}")
    public ResponseEntity<Map<String, Object>> getSkill(@PathVariable Long id) {
        try {
            Skill skill = skillService.getSkillById(id);
            return ResponseUtils.success(skill);
        } catch (RuntimeException e) {
            return ResponseUtils.error(404, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取技能失败: " + e.getMessage());
        }
    }

    /**
     * 更新技能
     */
    @PutMapping("/skills/{id}")
    public ResponseEntity<Map<String, Object>> updateSkill(
            @PathVariable Long id,
            @RequestBody Skill skill) {
        try {
            Skill updated = skillService.updateSkill(id, skill);
            return ResponseUtils.success("技能更新成功", updated);
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "更新技能失败: " + e.getMessage());
        }
    }

    /**
     * 发布技能
     */
    @PostMapping("/skills/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishSkill(@PathVariable Long id) {
        try {
            Skill skill = skillService.publishSkill(id);
            return ResponseUtils.success("技能发布成功", skill);
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "发布技能失败: " + e.getMessage());
        }
    }

    /**
     * 归档技能
     */
    @PostMapping("/skills/{id}/archive")
    public ResponseEntity<Map<String, Object>> archiveSkill(@PathVariable Long id) {
        try {
            Skill skill = skillService.archiveSkill(id);
            return ResponseUtils.success("技能已归档", skill);
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "归档技能失败: " + e.getMessage());
        }
    }

    /**
     * 删除技能
     */
    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Map<String, Object>> deleteSkill(@PathVariable Long id) {
        try {
            boolean success = skillService.deleteSkill(id);
            if (success) {
                return ResponseUtils.success("技能删除成功", "null");
            } else {
                return ResponseUtils.error(500, "删除技能失败");
            }
        } catch (Exception e) {
            return ResponseUtils.error(500, "删除技能失败: " + e.getMessage());
        }
    }

    // ==================== 分类 API ====================

    /**
     * 获取所有技能分类
     */
    @GetMapping("/skill-categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<SkillCategory> categories = skillService.getAllCategories();
            return ResponseUtils.success(categories);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 根据Key获取分类
     */
    @GetMapping("/skill-categories/{key}")
    public ResponseEntity<Map<String, Object>> getCategoryByKey(@PathVariable String key) {
        try {
            SkillCategory category = skillService.getCategoryByKey(key);
            if (category == null) {
                return ResponseUtils.error(404, "分类不存在: " + key);
            }
            return ResponseUtils.success(category);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取分类失败: " + e.getMessage());
        }
    }

    // ==================== 安装管理 API ====================

    /**
     * 安装技能
     */
    @PostMapping("/skills/{id}/install")
    public ResponseEntity<Map<String, Object>> installSkill(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-UID", required = false, defaultValue = "default") String tenantUid,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {
        try {
            skillService.installSkill(id, tenantUid, userId);
            return ResponseUtils.success("技能安装成功", "null");
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "安装技能失败: " + e.getMessage());
        }
    }

    /**
     * 卸载技能
     */
    @PostMapping("/skills/{id}/uninstall")
    public ResponseEntity<Map<String, Object>> uninstallSkill(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-UID", required = false, defaultValue = "default") String tenantUid) {
        try {
            skillService.uninstallSkill(id, tenantUid);
            return ResponseUtils.success("技能卸载成功", "null");
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "卸载技能失败: " + e.getMessage());
        }
    }

    /**
     * 检查技能是否已安装
     */
    @GetMapping("/skills/{id}/installed")
    public ResponseEntity<Map<String, Object>> isSkillInstalled(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-UID", required = false, defaultValue = "default") String tenantUid) {
        try {
            boolean installed = skillService.isSkillInstalled(id, tenantUid);
            Map<String, Object> result = new HashMap<>();
            result.put("installed", installed);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            return ResponseUtils.error(500, "检查安装状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取已安装的技能列表
     */
    @GetMapping("/skills/installed")
    public ResponseEntity<Map<String, Object>> getInstalledSkills(
            @RequestHeader(value = "X-Tenant-UID", required = false, defaultValue = "default") String tenantUid) {
        try {
            List<Skill> skills = skillService.getInstalledSkills(tenantUid);
            return ResponseUtils.success(skills);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取已安装技能失败: " + e.getMessage());
        }
    }

    /**
     * 获取安装记录
     */
    @GetMapping("/skill-installations")
    public ResponseEntity<Map<String, Object>> getInstallations(
            @RequestHeader(value = "X-Tenant-UID", required = false, defaultValue = "default") String tenantUid) {
        try {
            List<SkillInstallation> installations = skillService.getInstallations(tenantUid);
            return ResponseUtils.success(installations);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取安装记录失败: " + e.getMessage());
        }
    }
}
