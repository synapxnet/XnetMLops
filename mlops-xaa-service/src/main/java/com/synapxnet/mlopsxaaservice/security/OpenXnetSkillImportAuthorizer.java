package com.synapxnet.mlopsxaaservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsxaaservice.dto.OpenXnetSkillImportRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 校验 OpenXnet 到 XnetMLOps 技能仓库的职责限定委托令牌。
 */
@Service
public class OpenXnetSkillImportAuthorizer {
    private static final String EXPECTED_AUDIENCE = "openxnet-mlops-skill-registry";
    private static final String EXPECTED_ISSUER = "openxnet-desktop";
    private static final String REQUIRED_SCOPE = "mlops:skill:import";
    private static final int MAX_TOKEN_SEGMENT_BYTES = 16 * 1024;

    private final ObjectMapper objectMapper;
    private final String delegationSecret;

    /**
     * 创建 Skill 导入授权器。
     *
     * @param objectMapper Jackson JSON 解析器
     * @param delegationSecret OpenXnet 委托签名密钥
     */
    public OpenXnetSkillImportAuthorizer(
            ObjectMapper objectMapper,
            @Value("${openxnet.agent-delegation-secret:}") String delegationSecret) {
        this.objectMapper = objectMapper;
        this.delegationSecret = delegationSecret == null ? "" : delegationSecret.trim();
    }

    /**
     * 校验一次候选 Skill 导入请求并返回可信主体。
     *
     * @param authorization Bearer 委托令牌
     * @param idempotencyKey 请求摘要幂等键
     * @param tenantUid Workspace 租户头
     * @param userId 调用者头
     * @param request 候选 Skill 请求体
     * @return 已通过签名和声明校验的主体
     */
    public AuthorizedImport authorize(
            String authorization,
            String idempotencyKey,
            String tenantUid,
            String userId,
            OpenXnetSkillImportRequest request) {
        if (delegationSecret.length() < 32) {
            throw new IllegalStateException("OpenXnet 技能导入签名密钥未配置");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("缺少 OpenXnet 技能导入委托令牌");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        String[] segments = token.split("\\.", -1);
        if (segments.length != 3) {
            throw new IllegalArgumentException("OpenXnet 技能导入委托令牌格式无效");
        }
        verifySignature(segments);
        JsonNode header = decodeJsonSegment(segments[0], "令牌头");
        JsonNode payload = decodeJsonSegment(segments[1], "令牌载荷");
        if (!"HS256".equals(readRequiredText(header, "alg"))) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌算法无效");
        }
        validateClaims(payload, request, idempotencyKey, tenantUid, userId);
        return new AuthorizedImport(readRequiredText(payload, "sub"), readRequiredText(payload, "workspace_id"));
    }

    /**
     * 校验 JWT HMAC 签名。
     *
     * @param segments JWT 三段内容
     */
    private void verifySignature(String[] segments) {
        byte[] supplied;
        try {
            supplied = Base64.getUrlDecoder().decode(segments[2]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌签名无效", exception);
        }
        byte[] expected = sign((segments[0] + "." + segments[1]).getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌签名无效");
        }
    }

    /**
     * 使用共享密钥计算 HS256 签名。
     *
     * @param signingInput JWT 待签名字节
     * @return HMAC-SHA256 签名
     */
    private byte[] sign(byte[] signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(delegationSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(signingInput);
        } catch (Exception exception) {
            throw new IllegalStateException("OpenXnet 技能导入签名校验失败", exception);
        }
    }

    /**
     * 解码并解析一个有界 JWT JSON 段。
     *
     * @param segment Base64URL 段
     * @param label 错误标签
     * @return JSON 对象节点
     */
    private JsonNode decodeJsonSegment(String segment, String label) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(segment);
            if (decoded.length == 0 || decoded.length > MAX_TOKEN_SEGMENT_BYTES) {
                throw new IllegalArgumentException(label + "超出大小限制");
            }
            JsonNode node = objectMapper.readTree(decoded);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException(label + "必须是 JSON 对象");
            }
            return node;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "无效", exception);
        }
    }

    /**
     * 校验令牌有效期、受众、权限范围和请求绑定声明。
     *
     * @param payload JWT 载荷
     * @param request Skill 请求体
     * @param idempotencyKey 幂等键
     * @param tenantUid Workspace 租户头
     * @param userId 调用者头
     */
    private void validateClaims(
            JsonNode payload,
            OpenXnetSkillImportRequest request,
            String idempotencyKey,
            String tenantUid,
            String userId) {
        long now = Instant.now().getEpochSecond();
        long issuedAt = readRequiredLong(payload, "iat");
        long expiresAt = readRequiredLong(payload, "exp");
        if (issuedAt > now + 30 || expiresAt <= now || expiresAt - issuedAt > 300) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌已过期或时限无效");
        }
        if (!EXPECTED_AUDIENCE.equals(readRequiredText(payload, "aud"))) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌受众无效");
        }
        if (!EXPECTED_ISSUER.equals(readRequiredText(payload, "iss"))) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌签发方无效");
        }
        JsonNode scopes = payload.get("scopes");
        if (scopes == null || !scopes.isArray() || !containsText(scopes, REQUIRED_SCOPE)) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌权限不足");
        }
        String subject = readRequiredText(payload, "sub");
        String workspaceId = readRequiredText(payload, "workspace_id");
        String skillId = readRequiredText(payload, "skill_id");
        String artifactDigest = readRequiredText(payload, "artifact_digest");
        if (!workspaceId.equals(request.getWorkspaceId()) || !workspaceId.equals(tenantUid)) {
            throw new IllegalArgumentException("OpenXnet 技能导入 Workspace 不匹配");
        }
        if (!skillId.equals(request.getSkillId())) {
            throw new IllegalArgumentException("OpenXnet 技能导入 Skill ID 不匹配");
        }
        if (!artifactDigest.equals(request.getArtifactDigest()) || !artifactDigest.equals(idempotencyKey)) {
            throw new IllegalArgumentException("OpenXnet 技能导入摘要不匹配");
        }
        if (userId != null && !userId.isBlank() && !subject.equals(userId.trim())) {
            throw new IllegalArgumentException("OpenXnet 技能导入主体不匹配");
        }
    }

    /**
     * 判断 JSON 数组是否包含指定文本。
     *
     * @param array JSON 数组
     * @param expected 目标文本
     * @return 包含时返回 true
     */
    private boolean containsText(JsonNode array, String expected) {
        for (JsonNode item : array) {
            if (item.isTextual() && expected.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取必填文本声明。
     *
     * @param node JSON 对象
     * @param field 字段名
     * @return 非空文本
     */
    private String readRequiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        String text = value != null && value.isTextual() ? value.asText() : "";
        if (text.isBlank() || text.length() > 512 || text.chars().anyMatch(character -> character < 32 || character == 127)) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌缺少声明: " + field);
        }
        return text;
    }

    /**
     * 读取必填整数声明。
     *
     * @param node JSON 对象
     * @param field 字段名
     * @return 长整数值
     */
    private long readRequiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new IllegalArgumentException("OpenXnet 技能导入令牌缺少声明: " + field);
        }
        return value.asLong();
    }

    /**
     * 已授权 Skill 导入身份。
     *
     * @param subject 企业账户主体
     * @param workspaceId Workspace ID
     */
    public record AuthorizedImport(String subject, String workspaceId) {
    }
}
