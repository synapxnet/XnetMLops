package com.synapxnet.mlopsmtpservice.recommendation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;

/** 校验推荐模型训练请求的登录身份和租户成员关系。 */
@Component
public class RecommendationTrainingWriteAuthorizer {

    private static final String MEMBERSHIP_SQL = """
            SELECT COUNT(*)
            FROM xnet_mlops_user_infos AS user_account
            INNER JOIN xnet_mlops_usr_organization_membership AS membership
                ON membership.user_id = user_account.Id
            WHERE user_account.phone = ?
              AND user_account.userId = ?
              AND membership.tenant_uid = ?
              AND membership.status = 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String jwtSecret;

    /** 注入 MLOps 元数据库和与登录服务一致的 JWT 密钥。 */
    public RecommendationTrainingWriteAuthorizer(
            DataSource dataSource,
            @Value("${JWT_SECRET:}") String jwtSecret
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jwtSecret = jwtSecret;
    }

    /** 确认登录账号、平台用户标识和租户边界来自同一有效成员关系。 */
    public void authorize(String authorization, String userId, String tenantUid) {
        String phone = parsePhone(authorization);
        Integer matchCount = jdbcTemplate.queryForObject(
                MEMBERSHIP_SQL,
                Integer.class,
                phone,
                userId,
                tenantUid
        );
        if (matchCount == null || matchCount < 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号不属于指定租户");
        }
    }

    /** 从 Bearer JWT 中解析已签名且未过期的账号手机号。 */
    private String parsePhone(String authorization) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要有效的登录令牌");
        }
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(authorization.substring("Bearer ".length()).trim())
                    .getBody();
            String phone = claims.getSubject();
            if (phone == null || phone.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌缺少账号标识");
            }
            return phone;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌无效或已过期");
        }
    }
}
