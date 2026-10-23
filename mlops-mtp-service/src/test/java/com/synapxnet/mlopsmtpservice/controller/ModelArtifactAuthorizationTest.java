/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型制品身份边界 / Model artifact identity boundaries.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.mapper.ModelArtifactMapper;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingWriteAuthorizer;
import com.synapxnet.mlopsmtpservice.service.HdfsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ModelArtifactAuthorizationTest {
    private ModelArtifactMapper mapper;
    private HdfsService hdfs;
    private RecommendationTrainingWriteAuthorizer authorizer;
    private MockMvc mvc;

    /** 隔离持久层并走真实HTTP绑定。 Isolate persistence while testing actual HTTP binding. */
    @BeforeEach void setup() {
        mapper = mock(ModelArtifactMapper.class); hdfs = mock(HdfsService.class);
        authorizer = mock(RecommendationTrainingWriteAuthorizer.class);
        mvc = MockMvcBuilders.standaloneSetup(new ModelArtifactController(mapper, hdfs, authorizer)).build();
    }

    /** 别名头不可切换读取租户。 Legacy headers cannot switch the queried tenant. */
    @Test void conflictingReadHeadersDenied() throws Exception {
        mvc.perform(get("/api/mtp/model-artifacts").header("X-Tenant-Uid", "tenant-a")
            .header("X-Tenant-Id", "tenant-b").header("X-User-Id", "user-a"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(mapper, hdfs);
    }

    /** 别名头不可切换写入租户。 Legacy headers cannot switch the write tenant. */
    @Test void conflictingWriteHeadersDenied() throws Exception {
        mvc.perform(post("/api/mtp/model-artifacts").header("X-Tenant-Uid", "tenant-a")
            .header("X-Tenant-Id", "tenant-b").header("X-User-Id", "user-a")
            .contentType(MediaType.APPLICATION_JSON).content("{\"outputName\":\"model\"}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(mapper, hdfs);
    }

    /** 未登录或非成员不能读取。 Missing authentication or membership blocks reads. */
    @Test void authenticationRunsBeforeQuery() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(authorizer)
            .authorize(null, "user-a", "tenant-a");
        mvc.perform(get("/api/mtp/model-artifacts").header("X-Tenant-Uid", "tenant-a")
            .header("X-User-Id", "user-a")).andExpect(status().isUnauthorized());
        verifyNoInteractions(mapper);
    }

    /** 请求体不能伪造租户归属。 Request bodies cannot forge tenant ownership. */
    @Test void bodyIdentityConflictDenied() throws Exception {
        mvc.perform(post("/api/mtp/model-artifacts").header("X-Tenant-Uid", "tenant-a")
            .header("X-User-Id", "user-a").header("Authorization", "Bearer local-test")
            .contentType(MediaType.APPLICATION_JSON).content("{\"outputName\":\"model\",\"tenantUid\":\"tenant-b\"}"))
            .andExpect(status().isForbidden());
        verify(authorizer).authorize("Bearer local-test", "user-a", "tenant-a");
        verifyNoInteractions(mapper, hdfs);
    }

    /** 合法身份沿用已验证租户。 Valid requests read only their authenticated tenant. */
    @Test void validReadUsesVerifiedTenant() throws Exception {
        when(mapper.findByTenant("tenant-a")).thenReturn(List.of());
        mvc.perform(get("/api/mtp/model-artifacts").header("X-Tenant-Uid", "tenant-a")
            .header("X-Tenant-Id", "tenant-a").header("X-User-Id", "user-a")
            .header("Authorization", "Bearer local-test"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(authorizer).authorize("Bearer local-test", "user-a", "tenant-a");
        verify(mapper).findByTenant("tenant-a");
    }
}
