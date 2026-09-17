package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.service.ModelDeploymentService;
import com.synapxnet.mlopsmepservice.service.DeploymentRuntimeUnavailableException;
import java.util.LinkedHashMap;
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

    /** 读取实际持久化配置或观测结果。Read persisted configuration or observed results. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDeployments() {
        List<ModelDeployment> deployments = deploymentService.findAll();
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", deployments
        ));
    }

    /** 读取实际持久化配置或观测结果。Read persisted configuration or observed results. */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDeploymentById(@PathVariable Long id) {
        ModelDeployment deployment = deploymentService.findById(id);
        if (deployment == null) {
            return ResponseEntity.ok(envelope(
                "code", 404,
                "message", "部署不存在",
                "data", (Object) null
            ));
        }
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", deployment
        ));
    }

    /** 调用真实服务并传播未接入错误。Call the service and propagate disconnected-runtime failures. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDeployment(@RequestBody ModelDeployment deployment) {
        ModelDeployment created = deploymentService.create(deployment);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "部署配置已保存，真实执行器尚未接入",
            "data", created
        ));
    }

    /** 更新已保存配置。Update persisted configuration. */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDeployment(@PathVariable Long id, @RequestBody ModelDeployment deployment) {
        deployment.setId(id);
        ModelDeployment updated = deploymentService.update(deployment);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    /** 删除持久化配置，保留服务执行边界。Delete persisted configuration within service execution boundaries. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDeployment(@PathVariable Long id) {
        deploymentService.delete(id);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "删除成功",
            "data", (Object) null
        ));
    }

    /** 调用真实服务并传播未接入错误。Call the service and propagate disconnected-runtime failures. */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startDeployment(@PathVariable Long id) {
        deploymentService.start(id);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "启动成功",
            "data", (Object) null
        ));
    }

    /** 调用真实服务并传播未接入错误。Call the service and propagate disconnected-runtime failures. */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopDeployment(@PathVariable Long id) {
        deploymentService.stop(id);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "停止成功",
            "data", (Object) null
        ));
    }

    /** 调用真实服务并传播未接入错误。Call the service and propagate disconnected-runtime failures. */
    @PostMapping("/{id}/restart")
    public ResponseEntity<Map<String, Object>> restartDeployment(@PathVariable Long id) {
        deploymentService.restart(id);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "重启成功",
            "data", (Object) null
        ));
    }

    /** 调用真实服务并传播未接入错误。Call the service and propagate disconnected-runtime failures. */
    @PostMapping("/{id}/scale")
    public ResponseEntity<Map<String, Object>> scaleDeployment(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer replicas = params.get("replicas");
        deploymentService.scale(id, replicas);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "扩缩容成功",
            "data", (Object) null
        ));
    }

    /** 读取实际持久化配置或观测结果。Read persisted configuration or observed results. */
    @GetMapping("/{id}/logs")
    public ResponseEntity<Map<String, Object>> getDeploymentLogs(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String since) {
        List<Map<String, Object>> logs = deploymentService.getLogs(id, limit, since);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", logs
        ));
    }

    /** 读取实际持久化配置或观测结果。Read persisted configuration or observed results. */
    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getDeploymentMetrics(
            @PathVariable Long id,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        List<Map<String, Object>> metrics = deploymentService.getMetrics(id, start, end);
        return ResponseEntity.ok(envelope(
            "code", 0,
            "message", "success",
            "data", metrics
        ));
    }
    /** 返回真实执行依赖缺失，不返回成功状态。Report missing runtime dependencies without success. */
    @ExceptionHandler(DeploymentRuntimeUnavailableException.class)
    public ResponseEntity<Map<String, Object>> runtimeUnavailable(DeploymentRuntimeUnavailableException error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 503);
        body.put("message", error.getMessage());
        body.put("data", null);
        return ResponseEntity.status(503).body(body);
    }

    /** 支持空data的既有响应包络。Build the existing response envelope with nullable data.
     * 输入为固定服务端键值对。Inputs are fixed server-owned key/value pairs. */
    private static Map<String, Object> envelope(Object... entries) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) body.put((String) entries[i], entries[i + 1]);
        return body;
    }

    /** 读取独立采集或持久化的指标摘要。Read independently collected or persisted metric summaries. */
    @GetMapping("/{id}/metrics/summary")
    public ResponseEntity<Map<String, Object>> getDeploymentMetricSummary(@PathVariable Long id) {
        return ResponseEntity.ok(envelope("code", 0, "message", "success", "data", deploymentService.getMetricSummary(id)));
    }
}
