/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型治理资源登记 / Model governance resource registration.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-15
 * Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContractException;
import java.util.List;

/** 明确注册同一部署的独立治理资源。 / Explicitly register independent governed resources belonging to one deployment. */
record CompetitionModelResourceBinding(String deploymentUid, long candidateRevision, String kind) {
    /** 从平台已登记资源中取得部署关联。 / Resolve deployment associations from the platform-owned registry. */
    static CompetitionModelResourceBinding forDeployment(String deploymentUid) {
        if (deploymentUid == null) throw new AgentContractException(400, "INVALID_ARGUMENT", "deploymentUid 不能为空");
        return switch (deploymentUid) {
            case "deploy_quant_ashare_research" -> new CompetitionModelResourceBinding(deploymentUid, 2L, "quant");
            case "deploy_quant_value_prod" -> new CompetitionModelResourceBinding(deploymentUid, 19L, "quant");
            case "deploy_risk_prod" -> new CompetitionModelResourceBinding(deploymentUid, 19L, "risk");
            case "deploy_recommendation_prod" -> new CompetitionModelResourceBinding(deploymentUid, 6L, "recommendation");
            default -> throw new AgentContractException(404, "RESOURCE_NOT_REGISTERED", "部署与治理资源尚未登记关联");
        };
    }

    /** 列出当前部署的规范资源，不合并回滚与流量版本。 / List canonical resources without merging rollback and traffic versions. */
    List<String> resourceIds() {
        if ("recommendation".equals(kind)) return List.of(deploymentUid);
        if ("risk".equals(kind)) return List.of(deploymentUid, deploymentUid + "/traffic", deploymentUid + "/feature-set",
                deploymentUid + "/revisions/" + candidateRevision, "experiment_risk_" + candidateRevision);
        return List.of(deploymentUid, deploymentUid + "/traffic", deploymentUid + "/revisions/" + candidateRevision,
                "pipeline_quant_" + candidateRevision, "experiment_quant_" + candidateRevision);
    }

    /** 拒绝未登记的资源或跨部署参数。 / Reject unregistered resources or arguments belonging to another deployment. */
    void requireResource(String canonicalResourceId) {
        if (!resourceIds().contains(canonicalResourceId)) {
            throw new AgentContractException(404, "RESOURCE_NOT_REGISTERED", "目标资源不属于该部署的登记关联");
        }
    }
}
