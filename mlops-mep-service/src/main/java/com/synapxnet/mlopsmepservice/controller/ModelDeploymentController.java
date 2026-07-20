package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.service.ModelDeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mep/deployments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModelDeploymentController {

    private final ModelDeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDeployments() {
        List<ModelDeployment> deployments = deploymentService.findAll();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", deployments
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDeploymentById(@PathVariable Long id) {
        ModelDeployment deployment = deploymentService.findById(id);
        if (deployment == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "部署不存在",
                "data", (Object) null
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", deployment
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDeployment(@RequestBody ModelDeployment deployment) {
        ModelDeployment created = deploymentService.create(deployment);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "创建部署任务成功",
            "data", created
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDeployment(@PathVariable Long id, @RequestBody ModelDeployment deployment) {
        deployment.setId(id);
        ModelDeployment updated = deploymentService.update(deployment);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDeployment(@PathVariable Long id) {
        deploymentService.delete(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "删除成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startDeployment(@PathVariable Long id) {
        deploymentService.start(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "启动成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopDeployment(@PathVariable Long id) {
        deploymentService.stop(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "停止成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<Map<String, Object>> restartDeployment(@PathVariable Long id) {
        deploymentService.restart(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "重启成功",
            "data", (Object) null
        ));
    }

    @PostMapping("/{id}/scale")
    public ResponseEntity<Map<String, Object>> scaleDeployment(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer replicas = params.get("replicas");
        deploymentService.scale(id, replicas);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "扩缩容成功",
            "data", (Object) null
        ));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<Map<String, Object>> getDeploymentLogs(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String since) {
        List<Map<String, Object>> logs = deploymentService.getLogs(id, limit, since);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", logs
        ));
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getDeploymentMetrics(
            @PathVariable Long id,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        List<Map<String, Object>> metrics = deploymentService.getMetrics(id, start, end);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", metrics
        ));
    }
}
