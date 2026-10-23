/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. No copying, distribution or use without authorization.
 * SMP资源身份与范围校验 / SMP resource identity and scope validation.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopssmpservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class SmpResourceAccessService {
    private static final Set<String> ADMIN_ROLES = Set.of("admin", "super", "superadmin");
    private final URI loginBase;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();

    /** 登录地址仅由部署配置提供，禁止请求参数选择身份服务。 Use a deployment-controlled login address, never one selected by a request. */
    public SmpResourceAccessService(@Value("${xnet.login.base-url:${XNET_LOGIN_BASE_URL:http://127.0.0.1:8181}}") String loginBase, ObjectMapper json) {
        this.loginBase = URI.create(loginBase);
        if (!Set.of("http", "https").contains(this.loginBase.getScheme()) || this.loginBase.getHost() == null
                || this.loginBase.getUserInfo() != null || this.loginBase.getQuery() != null || this.loginBase.getFragment() != null
                || !(this.loginBase.getPath().isEmpty() || this.loginBase.getPath().equals("/"))) {
            throw new IllegalArgumentException("SMP login base URL must be an HTTP(S) origin");
        }
        this.json = json;
    }

    /** 复核JWT、有效组织成员关系与真实账号，客户端身份声明不能代替认证。 Recheck JWT, active organization membership and the actual account instead of trusting client claims. */
    public Scope requireScope(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        String tenant = scopeHeader(request, "X-Tenant-Uid");
        String department = scopeHeader(request, "X-Dept-Uid");
        String team = scopeHeader(request, "X-Team-Uid");
        rejectConflict(request.getHeader("X-Tenant-Id"), tenant);
        rejectConflict(request.getHeader("X-Dept-Id"), department);
        rejectConflict(request.getHeader("X-Team-Id"), team);
        var access = send("/api/auth/organization-access", authorization, tenant, department, team);
        if (access.statusCode() != 204) rejectResponse(access.statusCode());
        var identity = send("/api/user/info", authorization, tenant, department, team);
        if (identity.statusCode() != 200) rejectResponse(identity.statusCode());
        try {
            JsonNode response = json.readTree(identity.body());
            JsonNode user = response.path("data");
            String userId = user.path("userId").asText("");
            if (response.path("code").asInt(-1) != 0 || userId.isBlank()) throw new IllegalArgumentException("Invalid identity response");
            rejectConflict(request.getHeader("X-User-Id"), userId);
            boolean admin = false;
            if (user.path("roles").isArray()) {
                for (JsonNode role : user.path("roles")) {
                    if (role.isTextual() && ADMIN_ROLES.contains(role.textValue().toLowerCase(Locale.ROOT))) admin = true;
                }
            }
            return new Scope(userId, tenant, department, team, admin);
        } catch (ResponseStatusException denied) {
            throw denied;
        } catch (Exception invalid) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务返回不可用，未授权执行");
        }
    }

    /** 无租户归属的基础设施敏感操作仅允许平台管理员。 Restrict sensitive operations on unscoped infrastructure to verified platform administrators. */
    public Scope requireAdministrator(HttpServletRequest request) {
        Scope scope = requireScope(request);
        if (!scope.administrator()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该操作需要平台管理员权限");
        return scope;
    }

    /** 保持组织标识完整，不从可伪造别名或查询参数补全。 Require complete organization identifiers without filling them from aliases or query parameters. */
    private String scopeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank() || value.length() > 128 || value.contains("\r") || value.contains("\n")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请选择有访问权限的团队");
        }
        return value;
    }

    /** 显式冲突必须拒绝，避免一处认证另一处执行。 Reject explicit identity conflicts rather than authenticating one scope and executing another. */
    private static void rejectConflict(String supplied, String verified) {
        if (supplied != null && !supplied.isBlank() && !Objects.equals(supplied, verified)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请求范围与已认证身份不一致");
        }
    }

    /** 固定身份服务调用设置超时、不跟随重定向且不记录凭据。 Use bounded identity requests without redirects or credential logging. */
    private HttpResponse<String> send(String path, String authorization, String tenant, String department, String team) {
        var request = HttpRequest.newBuilder(loginBase.resolve(path)).timeout(Duration.ofSeconds(5))
                .header("Authorization", authorization).header("X-Tenant-Uid", tenant).header("X-Dept-Uid", department).header("X-Team-Uid", team).GET().build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份校验中断，未授权执行");
        } catch (Exception unavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用，未授权执行");
        }
    }

    /** 身份失效与成员权限不足分别返回明确状态，其余故障保持拒绝。 Distinguish invalid identity and membership denial while failing closed on other errors. */
    private static void rejectResponse(int status) {
        if (status == 401 || status == 404) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
        if (status == 403) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有当前团队的数据访问权限");
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用，未授权执行");
    }

    public record Scope(String userId, String tenantUid, String deptUid, String teamUid, boolean administrator) {
        /** 查询和请求体中的组织参数只能与已验证范围一致。 Require query and body organization fields to match the verified scope. */
        public void requireMatching(String tenant, String department, String team) {
            rejectConflict(tenant, tenantUid);
            rejectConflict(department, deptUid);
            rejectConflict(team, teamUid);
        }
        /** 只读取真实归属于当前组织的特征算子。 Read only operators actually owned by the validated organization. */
        public boolean contains(FeatureOperator operator) {
            return operator != null && Objects.equals(tenantUid, operator.getTenant_uid()) && Objects.equals(deptUid, operator.getDept_uid()) && Objects.equals(teamUid, operator.getTeam_uid());
        }
        /** 资源归属核对在读取或修改之前执行。 Check actual ownership before returning or mutating the resource. */
        public void requireResource(FeatureOperator operator) {
            if (!contains(operator)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "资源不属于当前团队");
        }
    }
}
