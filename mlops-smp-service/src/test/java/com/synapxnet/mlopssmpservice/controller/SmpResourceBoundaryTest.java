/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. No copying, distribution or use without authorization.
 * 资源范围与管理员权限回归 / Resource scope and administrator boundary regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopssmpservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import com.synapxnet.mlopssmpservice.entity.Workstation;
import com.synapxnet.mlopssmpservice.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SmpResourceBoundaryTest {
    private HttpServer login;
    private MockMvc mvc;
    private FeatureOperatorService operators;
    private WorkstationService workstations;
    private volatile int organizationStatus;
    private final AtomicInteger redirects = new AtomicInteger();

    /** 使用本机登录协议端点及真实控制器，存储和SSH调用保持隔离。 Use local login protocol endpoints and actual controllers with persistence and SSH isolated. */
    @BeforeEach
    void setUp() throws Exception {
        organizationStatus = 204;
        login = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        login.createContext("/api/auth/organization-access", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("Authorization");
            boolean valid = "Bearer member".equals(token) || "Bearer admin".equals(token);
            boolean member = "TEN-A".equals(exchange.getRequestHeaders().getFirst("X-Tenant-Uid"))
                    && "DEPT-A".equals(exchange.getRequestHeaders().getFirst("X-Dept-Uid"))
                    && "TEAM-A".equals(exchange.getRequestHeaders().getFirst("X-Team-Uid"));
            int status = !valid ? 401 : !member ? 403 : organizationStatus;
            if (status == 302) exchange.getResponseHeaders().set("Location", "/capture");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        login.createContext("/api/user/info", exchange -> {
            boolean admin = "Bearer admin".equals(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = new ObjectMapper().writeValueAsBytes(Map.of("code", 0, "data", Map.of("userId", admin ? "ADMIN-ID" : "MEMBER-ID", "roles", List.of(admin ? "admin" : "user"))));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        login.createContext("/capture", exchange -> { redirects.incrementAndGet(); exchange.sendResponseHeaders(204, -1); exchange.close(); });
        login.start();
        var access = new SmpResourceAccessService("http://127.0.0.1:" + login.getAddress().getPort(), new ObjectMapper());
        operators = mock(FeatureOperatorService.class);
        workstations = mock(WorkstationService.class);
        var workstationController = new WorkstationController();
        ReflectionTestUtils.setField(workstationController, "workstationService", workstations);
        ReflectionTestUtils.setField(workstationController, "resourceAccess", access);
        mvc = MockMvcBuilders.standaloneSetup(new FeatureOperatorController(operators, access), workstationController).build();
    }

    /** 停止仅本机测试服务。 Stop the loopback-only test service. */
    @AfterEach
    void tearDown() { login.stop(0); }

    /** 添加当前团队请求头，不包含客户端管理员声明。 Add current-team headers without any client administrator claim. */
    private MockHttpServletRequestBuilder member(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).header("X-Tenant-Uid", "TEN-A").header("X-Dept-Uid", "DEPT-A").header("X-Team-Uid", "TEAM-A");
    }

    /** 创建有明确真实归属的测试算子。 Create a test operator with explicit ownership. */
    private FeatureOperator operator(String tenant, String team) {
        var result = new FeatureOperator(); result.setId(1L); result.setTenant_uid(tenant); result.setDept_uid("DEPT-A"); result.setTeam_uid(team); result.setOperator_name("local-test");
        return result;
    }

    /** 匿名请求在读取任何资源前被拒绝。 Reject anonymous requests before resource access. */
    @Test
    void requiresActualLogin() throws Exception {
        mvc.perform(get("/api/smp/workstations/1/credentials")).andExpect(status().isUnauthorized());
        verifyNoInteractions(workstations);
    }

    /** 查询参数不可扩大已验证组织范围。 Query parameters cannot expand the validated organization scope. */
    @ParameterizedTest
    @ValueSource(strings = {"tenantUid", "deptUid", "teamUid"})
    void rejectsCrossScopeQuery(String name) throws Exception {
        mvc.perform(member(get("/api/smp/feature-operators").param(name, "FOREIGN"), "member")).andExpect(status().isForbidden());
        verifyNoInteractions(operators);
    }

    /** 未提供查询过滤时仍把当前团队传入真实查询。 Apply the validated team even when no query filters are supplied. */
    @Test
    void scopesUnfilteredQueries() throws Exception {
        when(operators.searchOperators("TEN-A", "DEPT-A", "TEAM-A", null, null, null)).thenReturn(List.of(operator("TEN-A", "TEAM-A")));
        mvc.perform(member(get("/api/smp/feature-operators"), "member")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].team_uid").value("TEAM-A"));
        verify(operators).searchOperators("TEN-A", "DEPT-A", "TEAM-A", null, null, null);
    }

    /** 通过ID读取和修改也检查数据库资源归属。 Check stored ownership for ID-based reads and writes too. */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void rejectsForeignResourceById(String method) throws Exception {
        when(operators.getOperatorById(1L)).thenReturn(operator("TEN-B", "TEAM-B"));
        mvc.perform(member(request(HttpMethod.valueOf(method), "/api/smp/feature-operators/1").contentType(MediaType.APPLICATION_JSON).content("{}"), "member")).andExpect(status().isForbidden());
        verify(operators, never()).updateOperator(anyLong(), any());
        verify(operators, never()).deleteOperator(anyLong());
    }

    /** 同租户不同团队仍不能直接读取。 A different team in the same tenant is not an implicit grant. */
    @Test
    void rejectsDifferentTeamInSameTenant() throws Exception {
        when(operators.getOperatorByUid("foreign")).thenReturn(operator("TEN-A", "TEAM-B"));
        mvc.perform(member(get("/api/smp/feature-operators/uid/foreign"), "member")).andExpect(status().isForbidden());
    }

    /** 相同算子代码跨团队的结果只保留当前归属。 Filter shared-code results to the validated owner scope. */
    @Test
    void filtersCodeResults() throws Exception {
        when(operators.getOperatorsByCode("op")).thenReturn(List.of(operator("TEN-A", "TEAM-A"), operator("TEN-B", "TEAM-B")));
        mvc.perform(member(get("/api/smp/feature-operators/code/op"), "member")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }

    /** 身份服务拒绝伪造组织头，别名头也不能改变资源范围。 The identity service rejects forged scope headers and aliases cannot override scope. */
    @Test
    void rejectsForgedHeaders() throws Exception {
        mvc.perform(get("/api/smp/feature-operators").header("Authorization", "Bearer member").header("X-Tenant-Uid", "TEN-B").header("X-Dept-Uid", "DEPT-A").header("X-Team-Uid", "TEAM-A")).andExpect(status().isForbidden());
        mvc.perform(member(get("/api/smp/feature-operators").header("X-Tenant-Id", "TEN-B"), "member")).andExpect(status().isForbidden());
        mvc.perform(member(get("/api/smp/workstations/1/credentials").header("X-User-Id", "ADMIN-ID").header("X-Roles", "admin"), "member")).andExpect(status().isForbidden());
        verifyNoInteractions(operators, workstations);
    }

    /** 无租户字段工作站的敏感操作统一限制到真实管理员。 Restrict every sensitive unscoped-workstation operation to a verified administrator. */
    @ParameterizedTest
    @CsvSource({"GET,/api/smp/workstations/1/credentials", "POST,/api/smp/workstations/test", "POST,/api/smp/workstations", "PUT,/api/smp/workstations/1", "DELETE,/api/smp/workstations/1", "POST,/api/smp/workstations/1/check"})
    void deniesNonAdministratorInfrastructureAccess(String method, String path) throws Exception {
        mvc.perform(member(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON).content("{}").header("X-Roles", "admin"), "member")).andExpect(status().isForbidden());
        verifyNoInteractions(workstations);
    }

    /** 普通工作站列表与详情不返回明文或加密凭据。 Ordinary workstation lists and details exclude both plaintext and encrypted credentials. */
    @ParameterizedTest
    @ValueSource(strings = {"/api/smp/workstations", "/api/smp/workstations/1", "/api/smp/workstations/uid/local", "/api/smp/workstations/online"})
    void omitsSecretsFromOrdinaryWorkstationResponses(String path) throws Exception {
        var workstation = new Workstation();
        workstation.setId(1L);
        workstation.setName("local-resource");
        workstation.setPassword("synthetic-plaintext-secret");
        workstation.setPrivateKey("synthetic-private-secret");
        workstation.setEncryptedPassword("synthetic-encrypted-secret");
        workstation.setEncryptedPrivateKey("synthetic-key-secret");
        when(workstations.getAll()).thenReturn(List.of(workstation));
        when(workstations.getById(1L)).thenReturn(workstation);
        when(workstations.getByUid("local")).thenReturn(workstation);
        when(workstations.getOnlineWorkstations()).thenReturn(List.of(workstation));
        String response = mvc.perform(member(get(path), "member")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("local-resource"));
        for (String secret : List.of("password", "privateKey", "encryptedPassword", "encryptedPrivateKey", "synthetic-")) {
            assertFalse(response.contains(secret), "Ordinary workstation response must not contain " + secret);
        }
    }

    /** 实际管理员仍可取得部署凭据，审计操作者来自身份服务。 Actual administrators retain deployment access and audit identity comes from the identity service. */
    @Test
    void retainsAdministratorOperations() throws Exception {
        when(workstations.getCredentials(1L)).thenReturn(Map.of("password", "synthetic-test-only"));
        when(workstations.create(any(), eq("ADMIN-ID"))).thenReturn(new Workstation());
        mvc.perform(member(get("/api/smp/workstations/1/credentials"), "admin")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(member(post("/api/smp/workstations").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"local\",\"createdBy\":\"forged\"}"), "admin")).andExpect(status().isOk());
        verify(workstations).create(any(), eq("ADMIN-ID"));
    }

    /** 退出或失效JWT即使携带合法范围也不能访问。 Reject revoked or expired JWTs even with otherwise valid scope headers. */
    @Test
    void rejectsInvalidToken() throws Exception {
        mvc.perform(member(get("/api/smp/feature-operators"), "expired")).andExpect(status().isUnauthorized());
        verifyNoInteractions(operators);
    }

    /** 身份服务故障或重定向不会被当作通过，也不转发凭据。 Identity failures and redirects never authorize access or forward credentials. */
    @ParameterizedTest
    @ValueSource(ints = {302, 500})
    void failsClosedOnIdentityFailure(int status) throws Exception {
        organizationStatus = status;
        mvc.perform(member(get("/api/smp/workstations/1/credentials"), "admin")).andExpect(status().isServiceUnavailable());
        assertEquals(0, redirects.get());
        verifyNoInteractions(workstations);
    }
}
