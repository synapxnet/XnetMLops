package com.synapxnet.mlopsmepservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    /** 初始归因必须返回冻结基线指标，不得提前展示候选模型结果。 */
    @Test
    void quantitativeAttributionReturnsBaselineBeforePromotion() {
        QuantitativeRuntimeClient client = quantitativeClient("BASELINE");
        CompetitionModelLifecycleService service = service(client);
        Map<String, Object> result = service.attribution(
                context("req_quant_baseline"),
                new CompetitionModelLifecycleService.AttributionArguments(
                        "deploy_quant_ashare_research", "report_quant_a_share_v1"));

        assertEquals(-0.00993273, result.get("informationCoefficient"));
        assertEquals(false, result.get("passed"));
    }

    /** 提升后归因必须读取候选制品并返回真实改善值。 */
    @Test
    void quantitativeAttributionReturnsCandidateAfterPromotion() {
        QuantitativeRuntimeClient client = quantitativeClient("PROMOTED");
        CompetitionModelLifecycleService service = service(client);
        Map<String, Object> result = service.attribution(
                context("req_quant_promoted"),
                new CompetitionModelLifecycleService.AttributionArguments(
                        "deploy_quant_ashare_research", "report_quant_a_share_v1"));

        assertEquals(0.25228567, result.get("informationCoefficient"));
        assertEquals(0.2622184, result.get("informationCoefficientImprovement"));
        assertEquals(true, result.get("passed"));
    }

    /** 创建不执行写操作的生命周期服务；审批客户端不会在只读探针中发起网络请求。 */
    private CompetitionModelLifecycleService service() {
        GovernedApprovalVerifier verifier = new GovernedApprovalVerifier(
                new ObjectMapper(), "http://127.0.0.1", "x".repeat(32));
        return new CompetitionModelLifecycleService(verifier, mock(QuantitativeRuntimeClient.class));
    }

    /** 创建使用指定量化客户端的生命周期服务。 */
    private CompetitionModelLifecycleService service(QuantitativeRuntimeClient client) {
        GovernedApprovalVerifier verifier = new GovernedApprovalVerifier(
                new ObjectMapper(), "http://127.0.0.1", "x".repeat(32));
        return new CompetitionModelLifecycleService(verifier, client);
    }

    /** 构造返回真实基线和候选指标的量化运行时客户端替身。 */
    private QuantitativeRuntimeClient quantitativeClient(String stage) {
        QuantitativeRuntimeClient client = mock(QuantitativeRuntimeClient.class);
        when(client.supports("deploy_quant_ashare_research")).thenReturn(true);
        when(client.deployment()).thenReturn(Map.of("stage", stage));
        when(client.baselineMetrics()).thenReturn(Map.of(
                "productVersion", "a-share-factor-demo-v1",
                "test", Map.of(
                        "auc", 0.4449774,
                        "information_coefficient", -0.00993273,
                        "maximum_drawdown", -0.04578438,
                        "sharpe", -1.08298693)));
        when(client.candidateMetrics()).thenReturn(Map.of(
                "candidate", Map.of("test", Map.of(
                        "auc", 0.56883927,
                        "information_coefficient", 0.25228567,
                        "maximum_drawdown", -0.00223652,
                        "sharpe", 7.90211082)),
                "improvement", Map.of(
                        "information_coefficient", 0.2622184,
                        "maximum_drawdown", 0.04354786,
                        "sharpe", 8.98509775)));
        return client;
    }

    /** 创建隔离的探针调用上下文；输入请求 ID，返回固定比赛事件范围。 */
    private AgentContract.RequestContext context(String requestId) {
        return new AgentContract.RequestContext(
                "ws_goai_demo", "inc_probe_failure", "trace_probe_failure",
                "mlops.inference.probe", "", "enterprise-goai:verifier", requestId);
    }
}
