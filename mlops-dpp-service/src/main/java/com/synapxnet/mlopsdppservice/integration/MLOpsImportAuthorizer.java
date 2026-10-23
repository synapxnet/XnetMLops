package com.synapxnet.mlopsdppservice.integration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;

/** 校验 DataOps 数据产品导入请求的登录身份和组织成员关系。 */
@Component
public class MLOpsImportAuthorizer {

    private static final String MEMBERSHIP_SQL = """
            SELECT COUNT(*)
            FROM xnet_mlops_user_infos AS user_account
            INNER JOIN xnet_mlops_usr_organization_membership AS membership
                ON membership.user_id = user_account.Id
            WHERE user_account.phone = ?
              AND user_account.userId = ?
              AND membership.tenant_uid = ?
              AND membership.team_uid = ?
              AND membership.status = 1
              AND (? = '' OR membership.dept_uid = ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String jwtSecret;

    /** 注入 MLOps 元数据库和与登录服务一致的 JWT 密钥。 */
    public MLOpsImportAuthorizer(
            DataSource dataSource,
            @Value("${JWT_SECRET:}") String jwtSecret
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jwtSecret = jwtSecret;
    }

    /** 确认登录账号与请求中的用户、租户、部门和团队边界完全一致。 */
    public void authorize(String authorization, ImportIdentity identity) {
        String phone = parsePhone(authorization);
        String deptUid = identity.deptUid() == null ? "" : identity.deptUid().trim();
        Integer matchCount = jdbcTemplate.queryForObject(
                MEMBERSHIP_SQL,
                Integer.class,
                phone,
                identity.userId(),
                identity.tenantUid(),
                identity.teamUid(),
                deptUid,
                deptUid
        );
        if (matchCount == null || matchCount < 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号不属于指定的组织边界");
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
