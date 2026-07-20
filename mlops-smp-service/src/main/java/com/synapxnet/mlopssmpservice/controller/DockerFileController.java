package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.config.PipelineParamEnricher;
import com.synapxnet.mlopssmpservice.entity.DockerFile;
import com.synapxnet.mlopssmpservice.entity.PipelineConfigParams;
import com.synapxnet.mlopssmpservice.service.DockerFileService;
import com.synapxnet.mlopssmpservice.service.JenkinsService;
import org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/smp/dockerfile")
public class DockerFileController {

    private final PipelineParamEnricher paramEnricher;
    private final DockerFileService dockerFileService;
    private final JenkinsService jenkinsService;

    @Autowired
    public DockerFileController(DockerFileService dockerFileService,PipelineParamEnricher paramEnricher,JenkinsService jenkinsService) {
        this.dockerFileService = dockerFileService;
        this.paramEnricher = paramEnricher;
        this.jenkinsService = jenkinsService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createDockerFile(@RequestBody DockerFile dockerFile) {
        try {
            DockerFile created = dockerFileService.createDockerFile(dockerFile);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "Docker文件创建成功",
                    "data", created,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "创建失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }
    @PostMapping("/pipeline")
    public ResponseEntity<Map<String, Object>> createPipeline(
            @RequestParam String dockerUID,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestBody PipelineConfigParams params) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 生成随机UID
            String taskUID = UUID.randomUUID().toString();

            // 获取 DockerFile 对象
            DockerFile dockerFile = dockerFileService.getDockerFileByUID(dockerUID)
                    .orElseThrow(() -> new ResourceNotFoundException("DockerFile not found with UID: " + dockerUID));
            paramEnricher.enrichParamsForDockerBuild(params, dockerFile, taskUID);

            // 创建 Docker Pipeline 作业
            String result = jenkinsService.createDockerPipelineJob(taskUID, params);

            // 构建成功响应
            response.put("code", 0);
            response.put("message", "Pipeline created successfully");
            response.put("data", result);
            response.put("error", "null");

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            // DockerFile 未找到
            response.put("code", -1);
            response.put("message", "Resource not found");
            response.put("data", "null");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 其他异常
            response.put("code", -1);
            response.put("message", "Failed to create pipeline");
            response.put("data", "null");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDockerFiles() {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", dockerFileService.getAllDockerFiles(),
                "error", "null"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDockerFileById(@PathVariable Integer id) {
        Optional<DockerFile> dockerFile = dockerFileService.getDockerFileById(id);
        if (dockerFile.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", dockerFile.get(),
                    "error", "null"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", -1,
                "message", "Docker文件未找到",
                "data", "null",
                "error", "Docker file not found"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDockerFile(
            @PathVariable Integer id,
            @RequestBody DockerFile dockerFile) {
        try {
            DockerFile updated = dockerFileService.updateDockerFile(id, dockerFile);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", updated,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "更新失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/update-push-status")
    public ResponseEntity<Map<String, Object>> updatePushStatus(
            @RequestBody Map<String, Object> request) {
        try {
            String uid = (String) request.get("uid");
            String statusStr = (String) request.get("status");
            String pushHistory = (String) request.get("pushHistory");

            DockerFile.PushStatus status = DockerFile.PushStatus.valueOf(statusStr);
            dockerFileService.updatePushStatus(uid, status, pushHistory);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "推送状态更新成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "更新失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteDockerFile(@PathVariable Integer id) {
        try {
            dockerFileService.deleteDockerFile(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "删除失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }
}
