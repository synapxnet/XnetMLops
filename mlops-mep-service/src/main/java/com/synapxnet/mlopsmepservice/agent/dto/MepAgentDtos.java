package com.synapxnet.mlopsmepservice.agent.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 集中定义 MEP Agent 工具的强类型输入和输出，不暴露数据库 Entity。
 */
public final class MepAgentDtos {

    /** 阻止 DTO 容器被实例化。 */
    private MepAgentDtos() {
    }

    /** 表示部署证据查询参数。 */
    public record DeploymentGetArguments(String deploymentUid, Boolean includeRevisions) {
    }

    /** 表示模型输入契约摘要。 */
    public record ModelContractSummary(String uid, int inputDimension, String contractHash) {
    }

    /** 表示部署修订摘要。 */
    public record DeploymentRevisionSummary(
            String uid,
            long revisionNumber,
            String modelUid,
            String modelVersion,
            ModelContractSummary modelContract,
            String imageRef,
            String specHash,
            Instant createdAt,
            String sourceActionId) {
    }

    /** 表示运行时实际检查结果。 */
    public record RuntimeReadiness(boolean reachable, boolean ready, String runtimeStatus, String runtimeVersion) {
    }

    /** 表示可跨平台引用的部署证据。 */
    public record DeploymentEvidence(
            String deploymentUid,
            String name,
            String status,
            String endpoint,
            String modelUid,
            String modelName,
            String modelVersion,
            ModelContractSummary modelContract,
            long activeRevision,
            String resourceVersion,
            String imageRef,
            int replicas,
            String nodeRef,
            RuntimeReadiness readiness,
            Instant lastVerifiedAt,
            List<DeploymentRevisionSummary> revisions,
            List<String> warnings) {
    }

    /** 表示推理探针参数。 */
    public record InferenceProbeArguments(
            String deploymentUid,
            String testDatasetRef,
            Integer sampleLimit,
            Integer timeoutMs) {
    }

    /** 表示探针失败类别聚合。 */
    public record ProbeFailure(String category, int count, String sampleRef) {
    }

    /** 表示模型输入维度检查结论。 */
    public enum ContractStatus {
        MATCHED,
        MISMATCHED,
        UNKNOWN
    }

    /** 表示真实探针的聚合结果。 */
    public record InferenceProbeResult(
            String probeUid,
            String deploymentUid,
            long revisionNumber,
            int sampleCount,
            int successCount,
            int errorCount,
            BigDecimal errorRate,
            BigDecimal p50Ms,
            BigDecimal p95Ms,
            int observedInputDimension,
            int expectedInputDimension,
            ContractStatus contractStatus,
            List<ProbeFailure> failures,
            Instant startedAt,
            Instant completedAt,
            String resultDigest) {
    }

    /** 表示回滚后的独立验证阈值。 */
    public record VerificationPolicy(BigDecimal maxErrorRate, BigDecimal maxP95Ms) {
    }

    /** 表示高风险部署回滚领域参数。 */
    public record RollbackArguments(
            String deploymentUid,
            Long targetRevision,
            VerificationPolicy verificationPolicy) {
    }

    /** 表示回滚动作受理或预检结果。 */
    public record RollbackAcceptance(
            String actionId,
            String status,
            String stage,
            boolean dryRun,
            String actionResourceUri,
            List<String> executionPlan) {
    }

    /** 表示可刷新恢复的部署动作状态。 */
    public record DeploymentActionView(
            String actionId,
            String deploymentUid,
            long fromRevision,
            long targetRevision,
            String status,
            String stage,
            boolean dryRun,
            String errorCode,
            String errorMessage,
            Instant startedAt,
            Instant completedAt) {
    }
}
