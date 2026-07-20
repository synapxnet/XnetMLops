package com.synapxnet.mlopsxaaservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsxaaservice.entity.*;
import com.synapxnet.mlopsxaaservice.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WorkflowExecutionService {

    private final WorkflowExecutionMapper executionMapper;
    private final NodeExecutionMapper nodeExecutionMapper;
    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;
    private final ExternalServiceClient externalServiceClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public WorkflowExecutionService(WorkflowExecutionMapper executionMapper,
                                   NodeExecutionMapper nodeExecutionMapper,
                                   WorkflowMapper workflowMapper,
                                   WorkflowNodeMapper nodeMapper,
                                   WorkflowEdgeMapper edgeMapper,
                                   ExternalServiceClient externalServiceClient,
                                   ObjectMapper objectMapper) {
        this.executionMapper = executionMapper;
        this.nodeExecutionMapper = nodeExecutionMapper;
        this.workflowMapper = workflowMapper;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.externalServiceClient = externalServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动工作流执行
     */
    @Transactional
    public WorkflowExecution startExecution(Long workflowId, Map<String, Object> inputs,
                                            String triggeredBy, String triggerType) {
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在: " + workflowId);
        }

        WorkflowExecution execution = new WorkflowExecution();
        execution.setUid(UUID.randomUUID().toString());
        execution.setWorkflowId(workflowId);
        execution.setStatus("scheduled");
        execution.setTriggeredBy(triggeredBy);
        execution.setTriggerType(triggerType);

        try {
            execution.setInputsJson(objectMapper.writeValueAsString(inputs));
        } catch (JsonProcessingException e) {
            execution.setInputsJson("{}");
        }

        executionMapper.insert(execution);

        // 异步执行工作流
        executeWorkflowAsync(execution.getId());

        return execution;
    }

    /**
     * 异步执行工作流
     */
    @Async
    public void executeWorkflowAsync(Long executionId) {
        WorkflowExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return;
        }

        try {
            // 更新状态为运行中
            execution.setStatus("running");
            execution.setStartedAt(new Date());
            executionMapper.update(execution);

            // 获取工作流节点和边
            List<WorkflowNode> nodes = nodeMapper.selectByWorkflowId(execution.getWorkflowId());
            List<WorkflowEdge> edges = edgeMapper.selectByWorkflowId(execution.getWorkflowId());

            // 执行工作流图
            Map<String, Object> outputs = executeGraph(execution, nodes, edges);

            // 更新执行结果
            execution.setStatus("succeeded");
            execution.setFinishedAt(new Date());
            execution.setElapsedTime(execution.getFinishedAt().getTime() - execution.getStartedAt().getTime());
            execution.setOutputsJson(objectMapper.writeValueAsString(outputs));
            executionMapper.update(execution);

        } catch (Exception e) {
            execution.setStatus("failed");
            execution.setFinishedAt(new Date());
            execution.setErrorMessage(e.getMessage());
            if (execution.getStartedAt() != null) {
                execution.setElapsedTime(execution.getFinishedAt().getTime() - execution.getStartedAt().getTime());
            }
            executionMapper.update(execution);
        }
    }

    /**
     * 执行工作流图
     */
    private Map<String, Object> executeGraph(WorkflowExecution execution,
                                              List<WorkflowNode> nodes,
                                              List<WorkflowEdge> edges) throws Exception {
        // 构建节点映射
        Map<Long, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        // 构建边映射 (sourceNodeId -> List<Edge>)
        Map<Long, List<WorkflowEdge>> edgeMap = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            edgeMap.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
        }

        // 找到开始节点
        WorkflowNode startNode = nodes.stream()
                .filter(n -> "start".equals(n.getNodeType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("工作流缺少开始节点"));

        // 执行上下文
        Map<String, Object> context = new HashMap<>();
        try {
            Map<String, Object> inputs = objectMapper.readValue(
                    execution.getInputsJson(), Map.class);
            context.putAll(inputs);
        } catch (Exception e) {
            // ignore
        }

        // 从开始节点开始执行
        executeNode(execution, startNode, context, nodeMap, edgeMap);

        return context;
    }

    /**
     * 执行单个节点
     */
    private void executeNode(WorkflowExecution execution, WorkflowNode node,
                            Map<String, Object> context,
                            Map<Long, WorkflowNode> nodeMap,
                            Map<Long, List<WorkflowEdge>> edgeMap) throws Exception {
        // 创建节点执行记录
        NodeExecution nodeExecution = new NodeExecution();
        nodeExecution.setUid(UUID.randomUUID().toString());
        nodeExecution.setExecutionId(execution.getId());
        nodeExecution.setWorkflowId(execution.getWorkflowId());
        nodeExecution.setNodeId(node.getId());
        nodeExecution.setNodeType(node.getNodeType());
        nodeExecution.setNodeTitle(node.getTitle());
        nodeExecution.setStatus("running");
        nodeExecution.setStartedAt(new Date());
        nodeExecution.setInputsJson(objectMapper.writeValueAsString(context));
        nodeExecutionMapper.insert(nodeExecution);

        try {
            // 根据节点类型执行不同的逻辑
            Map<String, Object> outputs = executeNodeByType(node, context);

            // 合并输出到上下文
            if (outputs != null) {
                context.putAll(outputs);
            }

            // 更新节点执行状态
            nodeExecution.setStatus("succeeded");
            nodeExecution.setFinishedAt(new Date());
            nodeExecution.setElapsedTime(nodeExecution.getFinishedAt().getTime() -
                                         nodeExecution.getStartedAt().getTime());
            nodeExecution.setOutputsJson(objectMapper.writeValueAsString(outputs));
            nodeExecutionMapper.update(nodeExecution);

            // 如果不是结束节点，继续执行下一个节点
            if (!"end".equals(node.getNodeType())) {
                List<WorkflowEdge> outEdges = edgeMap.get(node.getId());
                if (outEdges != null) {
                    for (WorkflowEdge edge : outEdges) {
                        WorkflowNode nextNode = nodeMap.get(edge.getTargetNodeId());
                        if (nextNode != null) {
                            executeNode(execution, nextNode, context, nodeMap, edgeMap);
                        }
                    }
                }
            }

        } catch (Exception e) {
            nodeExecution.setStatus("failed");
            nodeExecution.setFinishedAt(new Date());
            nodeExecution.setErrorMessage(e.getMessage());
            nodeExecutionMapper.update(nodeExecution);
            throw e;
        }
    }

    /**
     * 根据节点类型执行节点
     */
    private Map<String, Object> executeNodeByType(WorkflowNode node,
                                                   Map<String, Object> context) throws Exception {
        String nodeType = node.getNodeType();
        Map<String, Object> config = objectMapper.readValue(
                node.getConfigJson() != null ? node.getConfigJson() : "{}", Map.class);

        switch (nodeType) {
            case "start":
            case "end":
                return new HashMap<>();

            case "dpp-task":
                return executeDppTask(config, context);

            case "dpp-dataset":
                return executeDppDataset(config, context);

            case "dpp-feature":
                return executeDppFeature(config, context);

            case "mtp-train":
                return executeMtpTrain(config, context);

            case "mtp-algorithm":
                return executeMtpAlgorithm(config, context);

            case "mep-deploy":
                return executeMepDeploy(config, context);

            case "mep-service":
                return executeMepService(config, context);

            case "http-request":
                return executeHttpRequest(config, context);

            case "code":
                return executeCode(config, context);

            case "variable-assigner":
                return executeVariableAssigner(config, context);

            default:
                return new HashMap<>();
        }
    }

    // DPP 任务执行
    private Map<String, Object> executeDppTask(Map<String, Object> config,
                                                Map<String, Object> context) {
        String taskId = (String) config.get("taskId");
        return externalServiceClient.callDppService("/api/dpp/tasks/" + taskId + "/execute", context);
    }

    // DPP 数据集操作
    private Map<String, Object> executeDppDataset(Map<String, Object> config,
                                                   Map<String, Object> context) {
        String datasetId = (String) config.get("datasetId");
        String operation = (String) config.getOrDefault("operation", "get");
        return externalServiceClient.callDppService("/api/dpp/datasets/" + datasetId, context);
    }

    // DPP 特征工程
    private Map<String, Object> executeDppFeature(Map<String, Object> config,
                                                   Map<String, Object> context) {
        String featureId = (String) config.get("featureId");
        return externalServiceClient.callDppService("/api/dpp/features/" + featureId + "/execute", context);
    }

    // MTP 训练任务
    private Map<String, Object> executeMtpTrain(Map<String, Object> config,
                                                 Map<String, Object> context) {
        String trainTaskId = (String) config.get("trainTaskId");
        return externalServiceClient.callMtpService("/api/mtp/train-tasks/" + trainTaskId + "/start", context);
    }

    // MTP 算法
    private Map<String, Object> executeMtpAlgorithm(Map<String, Object> config,
                                                     Map<String, Object> context) {
        String algorithmId = (String) config.get("algorithmId");
        return externalServiceClient.callMtpService("/api/mtp/algorithms/" + algorithmId, context);
    }

    // MEP 部署
    private Map<String, Object> executeMepDeploy(Map<String, Object> config,
                                                  Map<String, Object> context) {
        String deploymentId = (String) config.get("deploymentId");
        return externalServiceClient.callMepService("/api/mep/deployments/" + deploymentId + "/deploy", context);
    }

    // MEP 服务
    private Map<String, Object> executeMepService(Map<String, Object> config,
                                                   Map<String, Object> context) {
        String serviceId = (String) config.get("serviceId");
        return externalServiceClient.callMepService("/api/mep/services/" + serviceId, context);
    }

    // HTTP 请求
    private Map<String, Object> executeHttpRequest(Map<String, Object> config,
                                                    Map<String, Object> context) {
        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        return externalServiceClient.executeHttpRequest(url, method, context);
    }

    // 代码执行 (预留)
    private Map<String, Object> executeCode(Map<String, Object> config,
                                             Map<String, Object> context) {
        // TODO: 实现代码执行逻辑
        return new HashMap<>();
    }

    // 变量赋值
    private Map<String, Object> executeVariableAssigner(Map<String, Object> config,
                                                         Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> assignments = (Map<String, Object>) config.get("assignments");
        if (assignments != null) {
            result.putAll(assignments);
        }
        return result;
    }

    /**
     * 获取执行记录
     */
    public WorkflowExecution getExecutionById(Long id) {
        return executionMapper.selectById(id);
    }

    /**
     * 获取执行记录（通过UID）
     */
    public WorkflowExecution getExecutionByUid(String uid) {
        return executionMapper.selectByUid(uid);
    }

    /**
     * 获取工作流的所有执行记录
     */
    public List<WorkflowExecution> getExecutionsByWorkflowId(Long workflowId) {
        return executionMapper.selectByWorkflowId(workflowId);
    }

    /**
     * 获取节点执行记录
     */
    public List<NodeExecution> getNodeExecutions(Long executionId) {
        return nodeExecutionMapper.selectByExecutionId(executionId);
    }

    /**
     * 停止执行
     */
    @Transactional
    public void stopExecution(Long executionId) {
        WorkflowExecution execution = executionMapper.selectById(executionId);
        if (execution != null && !isFinished(execution.getStatus())) {
            execution.setStatus("stopped");
            execution.setFinishedAt(new Date());
            if (execution.getStartedAt() != null) {
                execution.setElapsedTime(execution.getFinishedAt().getTime() -
                                         execution.getStartedAt().getTime());
            }
            executionMapper.update(execution);
        }
    }

    private boolean isFinished(String status) {
        return "succeeded".equals(status) || "failed".equals(status) || "stopped".equals(status);
    }
}
