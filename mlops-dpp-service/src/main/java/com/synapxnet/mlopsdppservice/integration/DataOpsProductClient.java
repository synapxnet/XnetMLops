package com.synapxnet.mlopsdppservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataOpsProductClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean configured;

    /** 创建数据产品客户端，未配置时保留其他页面可用。 Create the client without blocking unrelated pages when unconfigured. */
    public DataOpsProductClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${xnet.dataops.base-url:}") String baseUrl,
            @Value("${xnet.dataops.service-token:}") String serviceToken
    ) {
        this.configured = validBaseUrl(baseUrl);
        RestClient.Builder configuredBuilder = restClientBuilder;
        if (configured) configuredBuilder.baseUrl(baseUrl.trim());
        if (serviceToken != null && !serviceToken.isBlank()) {
            configuredBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken.trim());
        }
        this.restClient = configuredBuilder.build();
        this.objectMapper = objectMapper;
    }

    /** 查询 DataOps 中可见的数据产品并返回结构化列表。 */
    public List<DataOpsProduct> listProducts() {
        JsonNode data = requestData("/api/tsk/recommendation-products");
        if (!data.isArray()) {
            throw new DataOpsUnavailableException("DataOps 数据产品响应格式不兼容，请检查服务版本后重试");
        }
        List<DataOpsProduct> products = new ArrayList<>();
        data.forEach(node -> products.add(convertProduct(node)));
        return products;
    }

    /** 按版本读取一个 DataOps 数据产品。 */
    public DataOpsProduct getProduct(String productVersion) {
        String encodedVersion = java.net.URLEncoder.encode(
                productVersion,
                java.nio.charset.StandardCharsets.UTF_8
        );
        return convertProduct(requestData("/api/tsk/recommendation-products/" + encodedVersion));
    }

    /** 校验配置与响应，避免把上游凭据或错误内容展示给用户。 Validate configuration and sanitize upstream failures. */
    private JsonNode requestData(String path) {
        if (!configured) throw new DataOpsUnavailableException("DataOps 数据产品服务未配置，请联系管理员配置连接后重试");
        JsonNode response;
        try {
            response = restClient.get().uri(path).retrieve().body(JsonNode.class);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new DataOpsUnavailableException("DataOps 数据产品服务暂不可用，请检查连接与服务授权后重试");
        }
        if (response == null) {
            throw new DataOpsUnavailableException("DataOps 数据产品服务返回空响应，请稍后重试");
        }
        if (response.path("code").asInt(-1) != 0) {
            throw new DataOpsUnavailableException("DataOps 数据产品服务未完成请求，请检查服务授权后重试");
        }
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new DataOpsUnavailableException("DataOps 数据产品响应格式不兼容，请检查服务版本后重试");
        }
        return data;
    }

    /** 将 JSON 数据产品节点转换为强类型对象。 Convert the product response into its typed contract. */
    private DataOpsProduct convertProduct(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, DataOpsProduct.class);
        } catch (Exception exception) {
            throw new DataOpsUnavailableException("DataOps 数据产品响应格式不兼容，请检查服务版本后重试");
        }
    }

    /** 仅接受明确的 HTTP 服务地址。 Accept only explicitly configured HTTP service addresses. */
    private static boolean validBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return false;
        try {
            java.net.URI uri = java.net.URI.create(baseUrl.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
