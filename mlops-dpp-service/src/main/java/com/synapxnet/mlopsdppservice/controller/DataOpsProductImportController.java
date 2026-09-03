package com.synapxnet.mlopsdppservice.controller;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import com.synapxnet.mlopsdppservice.integration.DataOpsDatasetImportService;
import com.synapxnet.mlopsdppservice.integration.DataOpsProduct;
import com.synapxnet.mlopsdppservice.integration.ImportIdentity;
import com.synapxnet.mlopsdppservice.integration.MLOpsImportAuthorizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dpp/dataops-products")
public class DataOpsProductImportController {

    private final DataOpsDatasetImportService importService;
    private final MLOpsImportAuthorizer importAuthorizer;

    /** 注入 DataOps 数据产品导入服务。 */
    public DataOpsProductImportController(
            DataOpsDatasetImportService importService,
            MLOpsImportAuthorizer importAuthorizer
    ) {
        this.importService = importService;
        this.importAuthorizer = importAuthorizer;
    }

    /** 查询 DataOps 数据产品供 MLOps 用户选择。 */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listProducts() {
        List<DataOpsProduct> products = importService.listAvailableProducts();
        return success(products);
    }

    /** 使用可信网关注入的组织身份导入一个已发布版本。 */
    @PostMapping("/{productVersion}/import")
    public ResponseEntity<Map<String, Object>> importProduct(
            @PathVariable("productVersion") String productVersion,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantUid,
            @RequestHeader("X-Team-Id") String teamUid,
            @RequestHeader(value = "X-Dept-Id", required = false) String deptUid,
            @RequestHeader(value = "X-Team-Name", required = false) String teamName,
            @RequestHeader(value = "X-Organization-Level", defaultValue = "3") int organizationLevel
    ) {
        ImportIdentity identity = new ImportIdentity(
                userId,
                tenantUid,
                teamUid,
                deptUid,
                teamName,
                organizationLevel
        );
        importAuthorizer.authorize(authorization, identity);
        Dataset dataset = importService.importPublishedProduct(productVersion, identity);
        return success(dataset);
    }

    /** 构造与现有 DPP 接口一致的成功响应。 */
    private ResponseEntity<Map<String, Object>> success(Object data) {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", data,
                "error", "null"
        ));
    }
}
