package com.synapxnet.mlopsdppservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataOpsProductClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /** 创建只访问 XnetDataOps 数据产品 API 的内部客户端。 */
    public DataOpsProductClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${xnet.dataops.base-url}") String baseUrl,
            @Value("${xnet.dataops.service-token:}") String serviceToken
    ) {
        RestClient.Builder configuredBuilder = restClientBuilder.baseUrl(baseUrl);
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
            throw new IllegalStateException("DataOps 数据产品列表格式不正确");
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

    /** 调用 DataOps 接口并统一校验响应状态和 data 节点。 */
    private JsonNode requestData(String path) {
        JsonNode response = restClient.get().uri(path).retrieve().body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("DataOps 返回空响应");
        }
        if (response.path("code").asInt(-1) != 0) {
            throw new IllegalStateException("DataOps 调用失败: " + response.path("message").asText());
        }
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new IllegalStateException("DataOps 响应缺少 data 字段");
        }
        return data;
    }

    /** 将 JSON 数据产品节点转换为强类型对象。 */
    private DataOpsProduct convertProduct(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, DataOpsProduct.class);
        } catch (Exception exception) {
            throw new IllegalStateException("DataOps 数据产品契约解析失败", exception);
        }
    }
}
