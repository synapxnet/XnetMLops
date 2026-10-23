/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Copying, distribution or use requires explicit authorization.
 * 跨平台数据产品可用性契约回归 / Cross-platform product availability contract regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsdppservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsdppservice.controller.DataOpsProductImportController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DataOpsProductAvailabilityTests {
    /** 空地址不触发网络调用且提示配置。 Missing configuration requires no network call. */
    @Test void missingConfigurationIsExplicit() {
        DataOpsProductClient client = new DataOpsProductClient(RestClient.builder(), new ObjectMapper(), "", "");
        assertTrue(assertThrows(DataOpsUnavailableException.class, client::listProducts).getMessage().contains("未配置"));
    }

    /** 非法地址不能在应用启动时抛异常。 Invalid configuration remains an explicit unavailable capability. */
    @Test void invalidConfigurationIsExplicit() {
        DataOpsProductClient client = new DataOpsProductClient(RestClient.builder(), new ObjectMapper(), "invalid:secret", "");
        assertTrue(assertThrows(DataOpsUnavailableException.class, client::listProducts).getMessage().contains("未配置"));
    }

    /** 成功响应维持真实产品字段。 Successful reads preserve actual product fields. */
    @Test void readsActualProduct() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://dataops.test/api/tsk/recommendation-products"))
                .andRespond(withSuccess("{\"code\":0,\"data\":[{\"productVersion\":\"v2\",\"rowCount\":42,\"status\":\"published\"}]}", MediaType.APPLICATION_JSON));
        DataOpsProductClient client = new DataOpsProductClient(builder, new ObjectMapper(), "http://dataops.test", "");
        assertEquals(42, client.listProducts().get(0).rowCount());
        server.verify();
    }

    /** 上游业务错误不能把敏感响应原文透传。 Upstream application failures must not expose private details. */
    @Test void sanitizesUpstreamFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://dataops.test/api/tsk/recommendation-products"))
                .andRespond(withSuccess("{\"code\":403,\"message\":\"secret-token-value\"}", MediaType.APPLICATION_JSON));
        DataOpsProductClient client = new DataOpsProductClient(builder, new ObjectMapper(), "http://dataops.test", "");
        assertFalse(assertThrows(DataOpsUnavailableException.class, client::listProducts).getMessage().contains("secret-token-value"));
        server.verify();
    }

    /** 不可达依赖返回可重试异常。 An unreachable dependency returns a retriable availability error. */
    @Test void unreachableServiceIsExplicit() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://dataops.test/api/tsk/recommendation-products"))
                .andRespond(withException(new IOException("internal-address")));
        DataOpsProductClient client = new DataOpsProductClient(builder, new ObjectMapper(), "http://dataops.test", "");
        assertFalse(assertThrows(DataOpsUnavailableException.class, client::listProducts).getMessage().contains("internal-address"));
    }

    /** 不匹配的响应不能被伪装为空列表。 A malformed response must not masquerade as an empty product list. */
    @Test void rejectsMalformedResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://dataops.test/api/tsk/recommendation-products"))
                .andRespond(withSuccess("{\"code\":0,\"data\":{}}", MediaType.APPLICATION_JSON));
        DataOpsProductClient client = new DataOpsProductClient(builder, new ObjectMapper(), "http://dataops.test", "");
        assertThrows(DataOpsUnavailableException.class, client::listProducts);
    }

    /** HTTP 边界提供可消费的503错误。 The HTTP boundary exposes a consumable 503 contract. */
    @Test void returnsServiceUnavailableEnvelope() throws Exception {
        DataOpsDatasetImportService service = mock(DataOpsDatasetImportService.class);
        when(service.listAvailableProducts()).thenThrow(new DataOpsUnavailableException("DataOps 数据产品服务未配置"));
        MockMvcBuilders.standaloneSetup(new DataOpsProductImportController(service, mock(MLOpsImportAuthorizer.class)))
                .build().perform(get("/api/dpp/dataops-products"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("DataOps 数据产品服务未配置"));
    }
}
