package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import com.synapxnet.mlopsmepservice.agent.provider.DeploymentRuntimeProvider;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import com.synapxnet.mlopsmepservice.mapper.ModelDeploymentMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 组合数据库期望状态、模型契约、部署修订和运行时实际状态。
 */
@Service
public class DeploymentEvidenceService {

    private final ModelDeploymentMapper deploymentMapper;
    private final AgentMepMapper agentMapper;
    private final List<DeploymentRuntimeProvider> runtimeProviders;

    /**
     * 创建部署证据服务。
     *
     * @param deploymentMapper 现有部署 Mapper
     * @param agentMapper Agent 契约和修订 Mapper
     * @param runtimeProviders 可替换 Runtime Provider 列表
     */
    public DeploymentEvidenceService(
            ModelDeploymentMapper deploymentMapper,
            AgentMepMapper agentMapper,
            List<DeploymentRuntimeProvider> runtimeProviders) {
        this.deploymentMapper = deploymentMapper;
        this.agentMapper = agentMapper;
        this.runtimeProviders = List.copyOf(runtimeProviders);
    }

    /**
     * 获取指定部署的期望状态和真实 Runtime readiness。
     *
     * @param deploymentUid 部署稳定 UID
     * @param includeRevisions 是否返回历史修订
     * @return 部署结构化证据
     */
    public MepAgentDtos.DeploymentEvidence get(String deploymentUid, boolean includeRevisions) {
        ModelDeployment deployment = requireDeployment(deploymentUid);
        ModelContract contract = agentMapper.findContract(deployment.getModelUid(), deployment.getModelVersion());
        List<DeploymentRevision> revisions = agentMapper.findRevisions(deploymentUid);
        List<String> warnings = new ArrayList<>();
        MepAgentDtos.RuntimeReadiness readiness = inspectRuntime(deployment, warnings);
        boolean dbRunning = "running".equalsIgnoreCase(deployment.getStatus());
        if (dbRunning != readiness.ready()) {
            warnings.add("DATABASE_RUNTIME_DRIFT");
        }
        return new MepAgentDtos.DeploymentEvidence(
                deployment.getUid(), deployment.getName(), deployment.getStatus(), deployment.getEndpoint(),
                deployment.getModelUid(), deployment.getModelName(), deployment.getModelVersion(),
                toContract(contract), value(deployment.getActiveRevision()),
                String.valueOf(value(deployment.getResourceVersion())), deployment.getImageName(),
                deployment.getReplicas() == null ? 0 : deployment.getReplicas(), deployment.getNodeUid(),
                readiness, toInstant(deployment.getLastVerifiedAt()),
                includeRevisions ? revisions.stream().map(this::toRevision).toList() : List.of(),
                List.copyOf(warnings));
    }

    /**
     * 获取部署 Entity，不存在时返回公共 404。
     *
     * @param deploymentUid 部署 UID
     * @return 部署 Entity
     */
    public ModelDeployment requireDeployment(String deploymentUid) {
        if (deploymentUid == null || deploymentUid.isBlank()) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "deploymentUid 不能为空");
        }
        ModelDeployment deployment = deploymentMapper.findByUid(deploymentUid);
        if (deployment == null) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "模型部署不存在");
        }
        return deployment;
    }

    /**
     * 选择支持当前部署的 Provider 并读取真实运行时状态。
     *
     * @param deployment 部署 Entity
     * @param warnings 警告收集器
     * @return 运行时 readiness
     */
    private MepAgentDtos.RuntimeReadiness inspectRuntime(
            ModelDeployment deployment,
            List<String> warnings) {
        DeploymentRuntimeProvider provider = runtimeProviders.stream()
                .filter(candidate -> candidate.supports(deployment)).findFirst().orElse(null);
        if (provider == null) {
            warnings.add("RUNTIME_PROVIDER_UNSUPPORTED");
            return new MepAgentDtos.RuntimeReadiness(false, false, "unsupported", null);
        }
        DeploymentRuntimeProvider.RuntimeInspection inspection = provider.inspect(deployment);
        if (!inspection.reachable()) {
            warnings.add("RUNTIME_UNAVAILABLE");
        }
        return new MepAgentDtos.RuntimeReadiness(
                inspection.reachable(), inspection.ready(), inspection.status(), inspection.runtimeVersion());
    }

    /** 将模型契约 Entity 转换为摘要。 */
    private MepAgentDtos.ModelContractSummary toContract(ModelContract contract) {
        return contract == null ? null : new MepAgentDtos.ModelContractSummary(
                contract.getUid(), contract.getInputDimension(), contract.getContractHash());
    }

    /** 将部署修订 Entity 转换为不含 Secret 的摘要。 */
    private MepAgentDtos.DeploymentRevisionSummary toRevision(DeploymentRevision revision) {
        ModelContract contract = agentMapper.findContractByUid(revision.getModelContractUid());
        return new MepAgentDtos.DeploymentRevisionSummary(
                revision.getUid(), revision.getRevisionNumber(), revision.getModelUid(), revision.getModelVersion(),
                toContract(contract), revision.getImageName(), revision.getSpecHash(),
                toInstant(revision.getCreatedAt()), revision.getSourceActionId());
    }

    /** 将可能为空的 Long 转换为稳定 0 值。 */
    private long value(Long value) {
        return value == null ? 0L : value;
    }

    /** 将数据库 UTC 本地时间转换为 RFC 3339。 */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
