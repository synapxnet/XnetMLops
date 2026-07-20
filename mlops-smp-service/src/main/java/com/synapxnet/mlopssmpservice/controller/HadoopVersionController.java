package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.HadoopVersion;
import com.synapxnet.mlopssmpservice.service.HadoopVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hadoop 版本控制器
 * 提供版本获取和同步 API
 */
@RestController
@RequestMapping("/api/hadoop/versions")
@CrossOrigin(origins = "*")
public class HadoopVersionController {

    @Autowired
    private HadoopVersionService hadoopVersionService;

    /**
     * 获取所有版本
     */
    @GetMapping
    public Map<String, Object> getAllVersions() {
        try {
            List<HadoopVersion> versions = hadoopVersionService.getAllVersions();
            return buildResponse(0, "success", versions);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 获取稳定版本
     */
    @GetMapping("/stable")
    public Map<String, Object> getStableVersions() {
        try {
            List<HadoopVersion> versions = hadoopVersionService.getStableVersions();
            return buildResponse(0, "success", versions);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 刷新版本列表（从 Apache 官网获取）
     */
    @PostMapping("/refresh")
    public Map<String, Object> refreshVersions() {
        try {
            Map<String, Object> result = hadoopVersionService.refreshVersions();
            return buildResponse(0, "success", result);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    /**
     * 获取版本统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getVersionStats() {
        try {
            Map<String, Object> stats = hadoopVersionService.getVersionStats();
            return buildResponse(0, "success", stats);
        } catch (Exception e) {
            return buildResponse(500, e.getMessage(), null);
        }
    }

    private Map<String, Object> buildResponse(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
