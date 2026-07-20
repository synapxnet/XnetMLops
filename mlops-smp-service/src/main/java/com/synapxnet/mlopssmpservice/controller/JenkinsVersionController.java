package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.JenkinsVersion;
import com.synapxnet.mlopssmpservice.service.JenkinsVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jenkins 版本管理 Controller
 */
@RestController
@RequestMapping("/api/smp/jenkins-versions")
@CrossOrigin
public class JenkinsVersionController {

    @Autowired
    private JenkinsVersionService jenkinsVersionService;

    /**
     * 获取稳定版本列表
     * GET /api/smp/jenkins-versions/stable
     */
    @GetMapping("/stable")
    public ResponseEntity<Map<String, Object>> getStableVersions() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<JenkinsVersion> versions = jenkinsVersionService.getStableVersions();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", versions);
            response.put("count", versions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取LTS版本列表
     * GET /api/smp/jenkins-versions/lts
     */
    @GetMapping("/lts")
    public ResponseEntity<Map<String, Object>> getLtsVersions() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<JenkinsVersion> versions = jenkinsVersionService.getLtsVersions();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", versions);
            response.put("count", versions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 刷新版本列表（从镜像站重新获取）
     * POST /api/smp/jenkins-versions/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshVersions() {
        Map<String, Object> result = jenkinsVersionService.refreshVersions();
        // 包装响应以适配前端拦截器（需要code和data字段）
        Map<String, Object> response = new HashMap<>();
        boolean success = Boolean.TRUE.equals(result.get("success"));
        response.put("code", success ? 0 : -1);
        response.put("success", success);
        response.put("data", result); // 将原始结果放入data字段
        if (success) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取版本统计信息
     * GET /api/smp/jenkins-versions/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVersionStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = jenkinsVersionService.getVersionStats();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
