package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.Workstation;
import com.synapxnet.mlopssmpservice.service.WorkstationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作站点控制器
 */
@RestController
@RequestMapping("/api/smp/workstations")
@CrossOrigin(origins = "*")
public class WorkstationController {

    private static final Logger logger = LoggerFactory.getLogger(WorkstationController.class);

    @Autowired
    private WorkstationService workstationService;

    /**
     * 获取所有工作站点
     */
    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            List<Workstation> workstations = workstationService.getAll();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("data", workstations);
            response.put("message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取工作站点列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 根据ID获取工作站点
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Workstation workstation = workstationService.getById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("data", workstation);
            response.put("message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取工作站点失败: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 根据UID获取工作站点
     */
    @GetMapping("/uid/{uid}")
    public ResponseEntity<?> getByUid(@PathVariable String uid) {
        try {
            Workstation workstation = workstationService.getByUid(uid);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("data", workstation);
            response.put("message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取工作站点失败: {}", uid, e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 测试SSH连接
     */
    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody Workstation workstation) {
        try {
            Map<String, Object> result = workstationService.testConnection(workstation);
            Map<String, Object> response = new HashMap<>();
            boolean success = Boolean.TRUE.equals(result.get("success"));
            response.put("code", success ? 0 : 1);
            response.put("data", result);
            response.put("message", result.get("message"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("测试连接失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "测试连接失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 创建工作站点 (需要先通过测试连接)
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Workstation workstation,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            // 检查名称是否存在
            if (workstationService.isNameExists(workstation.getName())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 1,
                    "message", "服务器名称已存在: " + workstation.getName()
                ));
            }

            Workstation created = workstationService.create(workstation, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("data", created);
            response.put("message", "创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建工作站点失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "创建失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 更新工作站点
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Workstation workstation,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Workstation updated = workstationService.update(id, workstation, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("data", updated);
            response.put("message", "更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新工作站点失败: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "更新失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 删除工作站点
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            workstationService.delete(id);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "删除成功"
            ));
        } catch (Exception e) {
            logger.error("删除工作站点失败: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "删除失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 检查服务器状态
     */
    @PostMapping("/{id}/check")
    public ResponseEntity<?> checkStatus(@PathVariable Long id) {
        try {
            Map<String, Object> result = workstationService.checkStatus(id);
            Map<String, Object> response = new HashMap<>();
            boolean success = Boolean.TRUE.equals(result.get("success"));
            response.put("code", success ? 0 : 1);
            response.put("data", result);
            response.put("message", success ? "检查完成" : result.get("message"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检查服务器状态失败: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "检查失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 检查名称是否存在
     */
    @GetMapping("/check-name")
    public ResponseEntity<?> checkName(@RequestParam(value = "name") String name) {
        try {
            boolean exists = workstationService.isNameExists(name);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", Map.of("exists", exists),
                "message", "success"
            ));
        } catch (Exception e) {
            logger.error("检查名称失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "检查失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 检查主机名是否存在
     */
    @GetMapping("/check-hostname")
    public ResponseEntity<?> checkHostname(@RequestParam(value = "hostname") String hostname) {
        try {
            boolean exists = workstationService.isHostnameExists(hostname);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", Map.of("exists", exists),
                "message", "success"
            ));
        } catch (Exception e) {
            logger.error("检查主机名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "检查失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取在线的工作站点列表（用于选择器）
     */
    @GetMapping("/online")
    public ResponseEntity<?> getOnlineWorkstations() {
        try {
            List<Workstation> workstations = workstationService.getOnlineWorkstations();
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", workstations,
                "message", "success"
            ));
        } catch (Exception e) {
            logger.error("获取在线工作站点失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取工作站点的SSH凭证（用于部署模块）
     * 返回解密后的密码或私钥
     */
    @GetMapping("/{id}/credentials")
    public ResponseEntity<?> getCredentials(@PathVariable Long id) {
        try {
            Map<String, Object> credentials = workstationService.getCredentials(id);
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", credentials,
                "message", "success"
            ));
        } catch (Exception e) {
            logger.error("获取工作站点凭证失败: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 1,
                "message", "获取凭证失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取厂商选项
     */
    @GetMapping("/options/vendors")
    public ResponseEntity<?> getVendorOptions() {
        List<Map<String, String>> vendors = List.of(
            Map.of("value", "huawei", "label", "华为云"),
            Map.of("value", "tencent", "label", "腾讯云"),
            Map.of("value", "alibaba", "label", "阿里云"),
            Map.of("value", "aws", "label", "AWS"),
            Map.of("value", "azure", "label", "Azure"),
            Map.of("value", "google", "label", "Google Cloud"),
            Map.of("value", "other", "label", "其他")
        );
        return ResponseEntity.ok(Map.of("code", 0, "data", vendors));
    }

    /**
     * 获取服务器类型选项
     */
    @GetMapping("/options/server-types")
    public ResponseEntity<?> getServerTypeOptions() {
        List<Map<String, String>> types = List.of(
            Map.of("value", "vps", "label", "VPS"),
            Map.of("value", "cvm", "label", "CVM (腾讯云)"),
            Map.of("value", "ecs", "label", "ECS (阿里云)"),
            Map.of("value", "cce", "label", "CCE (华为云)"),
            Map.of("value", "ec2", "label", "EC2 (AWS)"),
            Map.of("value", "vm", "label", "虚拟机"),
            Map.of("value", "physical", "label", "物理机"),
            Map.of("value", "other", "label", "其他")
        );
        return ResponseEntity.ok(Map.of("code", 0, "data", types));
    }

    /**
     * 获取地域选项
     */
    @GetMapping("/options/regions")
    public ResponseEntity<?> getRegionOptions() {
        List<Map<String, String>> regions = List.of(
            Map.of("value", "beijing", "label", "北京"),
            Map.of("value", "shanghai", "label", "上海"),
            Map.of("value", "guangzhou", "label", "广州"),
            Map.of("value", "shenzhen", "label", "深圳"),
            Map.of("value", "hangzhou", "label", "杭州"),
            Map.of("value", "chengdu", "label", "成都"),
            Map.of("value", "nanjing", "label", "南京"),
            Map.of("value", "hongkong", "label", "香港"),
            Map.of("value", "singapore", "label", "新加坡"),
            Map.of("value", "other", "label", "其他")
        );
        return ResponseEntity.ok(Map.of("code", 0, "data", regions));
    }

    /**
     * 获取操作系统类型选项
     */
    @GetMapping("/options/os-types")
    public ResponseEntity<?> getOsTypeOptions() {
        List<Map<String, String>> osTypes = List.of(
            Map.of("value", "linux", "label", "Linux"),
            Map.of("value", "windows", "label", "Windows"),
            Map.of("value", "macos", "label", "macOS")
        );
        return ResponseEntity.ok(Map.of("code", 0, "data", osTypes));
    }

    /**
     * 获取操作系统版本选项
     */
    @GetMapping("/options/os-versions")
    public ResponseEntity<?> getOsVersionOptions(@RequestParam(required = false) String osType) {
        List<Map<String, String>> versions;

        if ("windows".equals(osType)) {
            versions = List.of(
                Map.of("value", "Windows Server 2022", "label", "Windows Server 2022"),
                Map.of("value", "Windows Server 2019", "label", "Windows Server 2019"),
                Map.of("value", "Windows Server 2016", "label", "Windows Server 2016"),
                Map.of("value", "Windows 11", "label", "Windows 11"),
                Map.of("value", "Windows 10", "label", "Windows 10")
            );
        } else if ("macos".equals(osType)) {
            versions = List.of(
                Map.of("value", "macOS Sonoma", "label", "macOS Sonoma (14)"),
                Map.of("value", "macOS Ventura", "label", "macOS Ventura (13)"),
                Map.of("value", "macOS Monterey", "label", "macOS Monterey (12)")
            );
        } else {
            // Linux (默认)
            versions = List.of(
                Map.of("value", "OpenCloudOS 9", "label", "OpenCloudOS 9"),
                Map.of("value", "OpenCloudOS 8", "label", "OpenCloudOS 8"),
                Map.of("value", "CentOS 9", "label", "CentOS Stream 9"),
                Map.of("value", "CentOS 8", "label", "CentOS Stream 8"),
                Map.of("value", "CentOS 7", "label", "CentOS 7"),
                Map.of("value", "Ubuntu 24.04", "label", "Ubuntu 24.04 LTS"),
                Map.of("value", "Ubuntu 22.04", "label", "Ubuntu 22.04 LTS"),
                Map.of("value", "Ubuntu 20.04", "label", "Ubuntu 20.04 LTS"),
                Map.of("value", "Debian 12", "label", "Debian 12 (Bookworm)"),
                Map.of("value", "Debian 11", "label", "Debian 11 (Bullseye)"),
                Map.of("value", "Rocky Linux 9", "label", "Rocky Linux 9"),
                Map.of("value", "AlmaLinux 9", "label", "AlmaLinux 9"),
                Map.of("value", "Amazon Linux 2023", "label", "Amazon Linux 2023"),
                Map.of("value", "Other", "label", "其他")
            );
        }

        return ResponseEntity.ok(Map.of("code", 0, "data", versions));
    }
}
