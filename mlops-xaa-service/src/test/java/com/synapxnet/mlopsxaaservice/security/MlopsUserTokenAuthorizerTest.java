package com.synapxnet.mlopsxaaservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * XnetMLOps 用户令牌授权测试。
 */
class MlopsUserTokenAuthorizerTest {
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /**
     * 验证有效登录 JWT 返回手机号主体。
     */
    @Test
    void authorizesValidUserToken() {
        MlopsUserTokenAuthorizer authorizer = new MlopsUserTokenAuthorizer(SECRET);
        String token = createToken(new Date(System.currentTimeMillis() + 60_000));

        assertEquals("17870171303", authorizer.authorize("Bearer " + token));
    }

    /**
     * 验证过期登录 JWT 被拒绝。
     */
    @Test
    void rejectsExpiredUserToken() {
        MlopsUserTokenAuthorizer authorizer = new MlopsUserTokenAuthorizer(SECRET);
        String token = createToken(new Date(System.currentTimeMillis() - 1_000));

        assertThrows(IllegalArgumentException.class, () -> authorizer.authorize("Bearer " + token));
    }

    /**
     * 生成与 MLOps 登录服务兼容的测试 JWT。
     *
     * @param expiration 过期时间
     * @return HS256 JWT
     */
    private String createToken(Date expiration) {
        return Jwts.builder()
                .setSubject("17870171303")
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
}
