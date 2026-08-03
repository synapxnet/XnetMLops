package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 仅在显式 test Profile 中启用的固定审批验证器，禁止进入生产默认配置。
 */
@Component
@Profile("test")
public class TestApprovalVerifier implements ApprovalVerifier {

    /**
     * 仅接受比赛测试审批 ID，并继续验证执行人与审批人职责分离。
     */
    @Override
    public ApprovalDecision verify(
            String approvalId,
            MepAgentDtos.RollbackArguments arguments,
            String requestDigest,
            String expectedResourceVersion,
            AgentContract.RequestContext context) {
        if (!"apr_goai_demo_approved".equals(approvalId)) {
            throw new AgentContractException(403, "APPROVAL_INVALID", "test Profile 审批引用无效");
        }
        String approver = "goai-test-approver";
        if (approver.equals(context.actorId())) {
            throw new AgentContractException(403, "APPROVAL_INVALID", "审批人与执行人不能相同");
        }
        return new ApprovalDecision(approvalId, approver, context.actorId());
    }
}
