package com.synapxnet.mlopsxaaservice.controller;

import com.synapxnet.mlopsxaaservice.entity.NodeExecution;
import com.synapxnet.mlopsxaaservice.entity.WorkflowExecution;
import com.synapxnet.mlopsxaaservice.service.WorkflowExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xaa")
@CrossOrigin(origins = "*")
public class WorkflowExecutionController {

    private final WorkflowExecutionService executionService;

    @Autowired
    public WorkflowExecutionController(WorkflowExecutionService executionService) {
        this.executionService = executionService;
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

    /**
     * 执行工作流
     */
    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable Long workflowId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputs = request != null ?
                    (Map<String, Object>) request.getOrDefault("inputs", Map.of()) : Map.of();
            String triggeredBy = request != null ?
                    (String) request.getOrDefault("triggeredBy", "system") : "system";
            String triggerType = request != null ?
                    (String) request.getOrDefault("triggerType", "manual") : "manual";

            WorkflowExecution execution = executionService.startExecution(
                    workflowId, inputs, triggeredBy, triggerType);
            return ResponseUtils.success("工作流执行已启动", execution);
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "执行工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取执行记录详情
     */
    @GetMapping("/executions/{id}")
    public ResponseEntity<Map<String, Object>> getExecution(@PathVariable Long id) {
        try {
            WorkflowExecution execution = executionService.getExecutionById(id);
            if (execution == null) {
                return ResponseUtils.error(404, "执行记录不存在: " + id);
            }
            return ResponseUtils.success(execution);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取执行记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流的所有执行记录
     */
    @GetMapping("/workflows/{workflowId}/executions")
    public ResponseEntity<Map<String, Object>> getWorkflowExecutions(@PathVariable Long workflowId) {
        try {
            List<WorkflowExecution> executions = executionService.getExecutionsByWorkflowId(workflowId);
            return ResponseUtils.success(executions);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取执行记录列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取执行记录的节点执行详情
     */
    @GetMapping("/executions/{executionId}/nodes")
    public ResponseEntity<Map<String, Object>> getNodeExecutions(@PathVariable Long executionId) {
        try {
            List<NodeExecution> nodeExecutions = executionService.getNodeExecutions(executionId);
            return ResponseUtils.success(nodeExecutions);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取节点执行记录失败: " + e.getMessage());
        }
    }

    /**
     * 停止执行
     */
    @PostMapping("/executions/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopExecution(@PathVariable Long id) {
        try {
            executionService.stopExecution(id);
            return ResponseUtils.success("执行已停止", "null");
        } catch (Exception e) {
            return ResponseUtils.error(500, "停止执行失败: " + e.getMessage());
        }
    }
}
