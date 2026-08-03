package com.synapxnet.goai.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证 MLOps 高风险 Adapter 的委托 Token 边界。 */
class DelegatedTokenVerifierTests {

    private static final String SECRET = "goai-mlops-delegation-test-secret-1234567";

    /** 回滚 Token 只能用于指定 Workspace 和工具。 */
    @Test
    void acceptsExactRollbackDelegation() throws Exception {
        DelegatedTokenVerifier verifier = new DelegatedTokenVerifier(SECRET, "openxnet-agent-adapter");
        String token = token("ws_goai_demo", List.of("mlops.deployment.rollback"));

        assertEquals("operator", verifier.verify(
                "Bearer " + token, "ws_goai_demo", "mlops.deployment.rollback"));
    }

    /** 回滚 Token 不能横向调用探针或其他 Workspace。 */
    @Test
    void rejectsDelegationReuse() throws Exception {
        DelegatedTokenVerifier verifier = new DelegatedTokenVerifier(SECRET, "openxnet-agent-adapter");
        String token = "Bearer " + token("ws_goai_demo", List.of("mlops.deployment.rollback"));

        assertEquals("PERMISSION_DENIED", assertThrows(
                AgentContractException.class,
                () -> verifier.verify(token, "ws_other", "mlops.deployment.rollback")).getCode());
        assertEquals("PERMISSION_DENIED", assertThrows(
                AgentContractException.class,
                () -> verifier.verify(token, "ws_goai_demo", "mlops.inference.probe")).getCode());
    }

    /** 生成仅用于单测的短期 HS256 Token。 */
    private String token(String workspaceId, List<String> tools) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String header = encode(mapper, Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encode(mapper, Map.of(
                "sub", "operator", "workspace_id", workspaceId,
                "aud", "openxnet-agent-adapter", "tools", tools,
                "exp", Instant.now().getEpochSecond() + 60));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return header + "." + payload + "." + signature;
    }

    /** 编码一个测试 JWT JSON 段。 */
    private String encode(ObjectMapper mapper, Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(value));
    }
}
