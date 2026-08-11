package com.synapxnet.goai.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 Java 平台参数摘要与 OpenXnet 稳定 JSON 摘要完全一致。
 */
class GovernedApprovalVerifierDigestTests {

    /** 校验字段声明顺序不同于字母顺序时仍生成相同摘要。 */
    @Test
    void producesOpenXnetCompatibleStableDigest() {
        GovernedApprovalVerifier verifier = new GovernedApprovalVerifier(
                new ObjectMapper(), "http://127.0.0.1:8080", "x".repeat(32));

        String digest = verifier.digestArguments(new FallbackArguments(
                "INPUT_CONTRACT_DRIFT", "feature_set_risk_fallback_v1", "deploy_risk_prod"));

        assertEquals("e177fcee6ab25907b0831045585e75990524f1095e5ac2bc6ad65bc6a5587be2", digest);
    }

    /** 校验审批内省超时不能超出有界网络预算。 */
    @Test
    void rejectsTimeoutOutsideBoundedBudget() {
        assertThrows(IllegalArgumentException.class, () -> new GovernedApprovalVerifier(
                new ObjectMapper(), "http://127.0.0.1:8080", "x".repeat(32), 0, 20_000));
    }

    /** 使用非字母序字段声明模拟真实领域 DTO。 */
    private record FallbackArguments(
            String reasonCode,
            String featureSetUid,
            String deploymentUid) {
    }
}

