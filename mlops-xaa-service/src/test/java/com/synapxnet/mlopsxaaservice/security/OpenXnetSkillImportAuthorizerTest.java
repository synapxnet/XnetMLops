package com.synapxnet.mlopsxaaservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsxaaservice.dto.OpenXnetSkillImportRequest;
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

/**
 * OpenXnet 企业 Skill 导入委托授权测试。
 */
class OpenXnetSkillImportAuthorizerTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证签名、Workspace、Skill 与摘要全部匹配时返回可信主体。
     */
    @Test
    void authorizesBoundSkillImport() throws Exception {
        OpenXnetSkillImportRequest request = createRequest();
        OpenXnetSkillImportAuthorizer authorizer = new OpenXnetSkillImportAuthorizer(objectMapper, SECRET);
        String token = createToken(request, "17870171303");

        OpenXnetSkillImportAuthorizer.AuthorizedImport result = authorizer.authorize(
                "Bearer " + token,
                request.getArtifactDigest(),
                request.getWorkspaceId(),
                "17870171303",
                request);

        assertEquals("17870171303", result.subject());
        assertEquals("ws_goai_demo", result.workspaceId());
    }

    /**
     * 验证请求摘要与令牌绑定不一致时拒绝导入。
     */
    @Test
    void rejectsDigestMismatch() throws Exception {
        OpenXnetSkillImportRequest request = createRequest();
        OpenXnetSkillImportAuthorizer authorizer = new OpenXnetSkillImportAuthorizer(objectMapper, SECRET);
        String token = createToken(request, "17870171303");

        assertThrows(IllegalArgumentException.class, () -> authorizer.authorize(
                "Bearer " + token,
                "f".repeat(64),
                request.getWorkspaceId(),
                "17870171303",
                request));
    }

    /**
     * 创建最小合法候选包。
     *
     * @return Skill 导入请求
     */
    private OpenXnetSkillImportRequest createRequest() {
        OpenXnetSkillImportRequest request = new OpenXnetSkillImportRequest();
        request.setWorkspaceId("ws_goai_demo");
        request.setSkillId("goai-evidence-collect");
        request.setFamilyId("goai-evidence-collect");
        request.setName("证据采集");
        request.setDescription("只读采集跨平台证据");
        request.setVersion("1.0.0");
        request.setContentMd("# Skill\n");
        request.setManifestJson("{}");
        request.setArtifactDigest("a".repeat(64));
        request.setLifecycleStatus("candidate");
        request.setEvidenceOrigin("rehearsal");
        request.setEnvironmentScope("simulation");
        request.setProductionEligible(false);
        request.setFiles(List.of("SKILL.md", "openxnet.skill.json"));
        return request;
    }

    /**
     * 为测试请求签发五分钟 HS256 委托令牌。
     *
     * @param request Skill 导入请求
     * @param subject 企业账户主体
     * @return JWT 文本
     */
    private String createToken(OpenXnetSkillImportRequest request, String subject) throws Exception {
        long issuedAt = Instant.now().getEpochSecond();
        String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encode(Map.of(
                "iss", "openxnet-desktop",
                "aud", "openxnet-mlops-skill-registry",
                "sub", subject,
                "iat", issuedAt,
                "exp", issuedAt + 300,
                "scopes", List.of("mlops:skill:import"),
                "workspace_id", request.getWorkspaceId(),
                "skill_id", request.getSkillId(),
                "artifact_digest", request.getArtifactDigest()));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }

    /**
     * 把测试 JSON 对象编码为 Base64URL。
     *
     * @param value JSON 对象
     * @return 无填充 Base64URL 文本
     */
    private String encode(Object value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
    }
}
