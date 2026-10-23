/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.FeatureDriftRuntimeClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 暴露比赛模型归因、训练、评估、登记、备用特征、灰度和发布验证工具。
 */
@RestController
public class CompetitionModelLifecycleToolController {
    // 仅启用的真实运行时分流，缺失响应不得回退。 Route only enabled real execution; never fall back on missing evidence.
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private FeatureDriftRuntimeClient featureDriftRuntime;


    private final CompetitionModelLifecycleService lifecycleService;

    /**
     * 创建比赛模型生命周期 Controller。
     *
     * @param lifecycleService 状态化模型生命周期服务
     */
    public CompetitionModelLifecycleToolController(CompetitionModelLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /** 读取模型归因报告。 */
    @PostMapping("/api/agent/v1/tools/mlops.attribution.report.get:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> attribution(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.AttributionArguments> body,
            HttpServletRequest request) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = context(request, "mlops.attribution.report.get", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        Map<String, Object> data = lifecycleService.attribution(context, body.arguments());
        return AgentContract.success(data, context, "XnetMLOps/attribution", String.valueOf(data.get("resourceVersion")), startedNanos);
    }

    /** 发布候选特征流水线。 */
    @PostMapping("/api/agent/v1/tools/mlops.feature.pipeline.publish:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> publishFeaturePipeline(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.FeaturePipelineArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, "mlops.feature.pipeline.publish", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.publishFeaturePipeline(body, context);
    }

    /** 启动并行训练搜索。 */
    @PostMapping({
            "/api/agent/v1/tools/mlops.training.search.start:invoke",
            "/api/agent/v1/tools/mlops.model.iteration.start:invoke"
    })
    public AgentContract.ToolResponse<Map<String, Object>> startTraining(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.TrainingSearchArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, body.toolName(), body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.startTrainingSearch(body, context);
    }

    /** 执行候选模型质量门评估。 */
    @PostMapping("/api/agent/v1/tools/mlops.model.evaluation.run:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> evaluate(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.EvaluationArguments> body,
            HttpServletRequest request) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = context(request, "mlops.model.evaluation.run", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        Map<String, Object> data = lifecycleService.evaluate(context, body.arguments());
        return AgentContract.success(data, context, "XnetMLOps/model-evaluation", String.valueOf(data.get("resourceVersion")), startedNanos);
    }

    /** 登记候选模型和模型卡。 */
    @PostMapping("/api/agent/v1/tools/mlops.model.register:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> registerModel(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.ModelRegisterArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, "mlops.model.register", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.registerModel(body, context);
    }

    /** 启用备用特征集。 */
    @PostMapping("/api/agent/v1/tools/mlops.feature.fallback.apply:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> applyFallback(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.FallbackApplyArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, "mlops.feature.fallback.apply", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.applyFallback(body, context);
    }

    /** 退出备用特征集。 */
    @PostMapping("/api/agent/v1/tools/mlops.feature.fallback.remove:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> removeFallback(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.FallbackRemoveArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, "mlops.feature.fallback.remove", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.removeFallback(body, context);
    }

    /** 应用候选修订灰度发布。 */
    @PostMapping("/api/agent/v1/tools/mlops.deployment.canary.apply:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> applyCanary(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.CanaryArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, "mlops.deployment.canary.apply", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.applyCanary(body, context);
    }

    /** 将候选修订提升到目标流量。 */
    @PostMapping("/api/agent/v1/tools/mlops.deployment.promote:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> promote(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.PromoteArguments> body,
            HttpServletRequest request) {
        AgentContract.RequestContext context = context(request, "mlops.deployment.promote", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        return lifecycleService.promote(body, context);
    }

    /** 读取模型发布全链路验证状态。 */
    @PostMapping("/api/agent/v1/tools/mlops.release.validation.get:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> releaseValidation(
            @RequestBody AgentContract.ToolRequest<CompetitionModelLifecycleService.ReleaseValidationArguments> body,
            HttpServletRequest request) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = context(request, "mlops.release.validation.get", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        Map<String, Object> data = lifecycleService.releaseValidation(context, body.arguments());
        return AgentContract.success(data, context, "XnetMLOps/release-validation", String.valueOf(data.get("resourceVersion")), startedNanos);
    }

    /**
     * 读取并校验 Agent 上下文。
     */
    private AgentContract.RequestContext context(
            HttpServletRequest request,
            String toolName,
            AgentContract.ToolRequest<?> body) {
        return AgentContract.requireContext(request, toolName, body);
    }
}
