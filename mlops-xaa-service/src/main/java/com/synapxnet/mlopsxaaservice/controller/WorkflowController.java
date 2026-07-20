package com.synapxnet.mlopsxaaservice.controller;

import com.synapxnet.mlopsxaaservice.entity.Workflow;
import com.synapxnet.mlopsxaaservice.entity.WorkflowEdge;
import com.synapxnet.mlopsxaaservice.entity.WorkflowNode;
import com.synapxnet.mlopsxaaservice.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xaa")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Autowired
    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
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
     * 创建工作流
     */
    @PostMapping("/workflows")
    public ResponseEntity<Map<String, Object>> createWorkflow(@RequestBody Workflow workflow) {
        try {
            Workflow created = workflowService.createWorkflow(workflow);
            return ResponseUtils.success("工作流创建成功", created);
        } catch (Exception e) {
            return ResponseUtils.error(500, "创建工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流列表
     */
    @GetMapping("/workflows")
    public ResponseEntity<Map<String, Object>> getAllWorkflows(
            @RequestParam(required = false) String status) {
        try {
            List<Workflow> workflows;
            if (status != null && !status.isEmpty()) {
                workflows = workflowService.getWorkflowsByStatus(status);
            } else {
                workflows = workflowService.getAllWorkflows();
            }
            return ResponseUtils.success(workflows);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取工作流列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/workflows/{id}")
    public ResponseEntity<Map<String, Object>> getWorkflow(@PathVariable Long id) {
        try {
            Workflow workflow = workflowService.getWorkflowById(id);
            return ResponseUtils.success(workflow);
        } catch (RuntimeException e) {
            return ResponseUtils.error(404, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取工作流失败: " + e.getMessage());
        }
    }

    /**
     * 更新工作流
     */
    @PutMapping("/workflows/{id}")
    public ResponseEntity<Map<String, Object>> updateWorkflow(
            @PathVariable Long id,
            @RequestBody Workflow workflow) {
        try {
            Workflow updated = workflowService.updateWorkflow(id, workflow);
            return ResponseUtils.success("工作流更新成功", updated);
        } catch (RuntimeException e) {
            return ResponseUtils.error(400, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "更新工作流失败: " + e.getMessage());
        }
    }

    /**
     * 发布工作流
     */
    @PostMapping("/workflows/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishWorkflow(@PathVariable Long id) {
        try {
            Workflow published = workflowService.publishWorkflow(id);
            return ResponseUtils.success("工作流发布成功", published);
        } catch (Exception e) {
            return ResponseUtils.error(500, "发布工作流失败: " + e.getMessage());
        }
    }

    /**
     * 归档工作流
     */
    @PostMapping("/workflows/{id}/archive")
    public ResponseEntity<Map<String, Object>> archiveWorkflow(@PathVariable Long id) {
        try {
            Workflow archived = workflowService.archiveWorkflow(id);
            return ResponseUtils.success("工作流归档成功", archived);
        } catch (Exception e) {
            return ResponseUtils.error(500, "归档工作流失败: " + e.getMessage());
        }
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/workflows/{id}")
    public ResponseEntity<Map<String, Object>> deleteWorkflow(@PathVariable Long id) {
        try {
            boolean success = workflowService.deleteWorkflow(id);
            if (success) {
                return ResponseUtils.success("工作流删除成功", "null");
            } else {
                return ResponseUtils.error(500, "删除工作流失败");
            }
        } catch (Exception e) {
            return ResponseUtils.error(500, "删除工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流的节点
     */
    @GetMapping("/workflows/{id}/nodes")
    public ResponseEntity<Map<String, Object>> getWorkflowNodes(@PathVariable Long id) {
        try {
            List<WorkflowNode> nodes = workflowService.getWorkflowNodes(id);
            return ResponseUtils.success(nodes);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取节点失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流的边
     */
    @GetMapping("/workflows/{id}/edges")
    public ResponseEntity<Map<String, Object>> getWorkflowEdges(@PathVariable Long id) {
        try {
            List<WorkflowEdge> edges = workflowService.getWorkflowEdges(id);
            return ResponseUtils.success(edges);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取边失败: " + e.getMessage());
        }
    }

    /**
     * 保存工作流图（节点和边）
     */
    @PostMapping("/workflows/{id}/graph")
    public ResponseEntity<Map<String, Object>> saveWorkflowGraph(
            @PathVariable Long id,
            @RequestBody Map<String, Object> graphData) {
        try {
            @SuppressWarnings("unchecked")
            List<WorkflowNode> nodes = (List<WorkflowNode>) graphData.get("nodes");
            @SuppressWarnings("unchecked")
            List<WorkflowEdge> edges = (List<WorkflowEdge>) graphData.get("edges");

            workflowService.saveWorkflowGraph(id, nodes, edges);
            return ResponseUtils.success("工作流图保存成功", "null");
        } catch (Exception e) {
            return ResponseUtils.error(500, "保存工作流图失败: " + e.getMessage());
        }
    }
}
