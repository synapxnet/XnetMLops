package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证比赛模型生命周期探针的正常路径和受控失败注入边界。
 */
class CompetitionModelLifecycleServiceTest {

    /** 正常验证数据集必须返回健康探针，避免失败开关污染标准 Live 场景。 */
    @Test
    void regularVerificationDatasetPasses() {
        Map<String, Object> result = service().probe(
                context("req_probe_regular"),
                new MepAgentDtos.InferenceProbeArguments(
                        "deploy_quant_value_prod", "fixture://goai/quant-eod-v1", 100, 10_000));

        assertEquals(true, result.get("passed"));
        assertEquals("MATCHED", result.get("contractStatus"));
    }

    /** 只有固定失败数据集才模拟独立验证失败并触发 OpenXnet 补偿链路。 */
    @Test
    void dedicatedVerificationFailureDatasetFails() {
        Map<String, Object> result = service().probe(
                context("req_probe_failure"),
                new MepAgentDtos.InferenceProbeArguments(
                        "deploy_quant_value_prod", "fixture://goai/verification-failure-v1", 100, 10_000));

        assertEquals(false, result.get("passed"));
        assertEquals("MISMATCHED", result.get("contractStatus"));
    }

    /** 创建不执行写操作的生命周期服务；审批客户端不会在只读探针中发起网络请求。 */
    private CompetitionModelLifecycleService service() {
        GovernedApprovalVerifier verifier = new GovernedApprovalVerifier(
                new ObjectMapper(), "http://127.0.0.1", "x".repeat(32));
        return new CompetitionModelLifecycleService(verifier);
    }

    /** 创建隔离的探针调用上下文；输入请求 ID，返回固定比赛事件范围。 */
    private AgentContract.RequestContext context(String requestId) {
        return new AgentContract.RequestContext(
                "ws_goai_demo", "inc_probe_failure", "trace_probe_failure",
                "mlops.inference.probe", "", "enterprise-goai:verifier", requestId);
    }
}
