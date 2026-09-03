package com.synapxnet.mlopsdppservice.integration;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import com.synapxnet.mlopsdppservice.mapper.DatasetMapper;
import com.synapxnet.mlopsdppservice.service.DatasetService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DataOpsDatasetImportService {

    private static final Pattern IDENTITY_PATTERN = Pattern.compile("^[A-Za-z0-9._:@-]{1,128}$");

    private final DataOpsProductClient dataOpsProductClient;
    private final DatasetMapper datasetMapper;
    private final DatasetService datasetService;

    /** 注入 DataOps 客户端、数据集持久层和领域服务。 */
    public DataOpsDatasetImportService(
            DataOpsProductClient dataOpsProductClient,
            DatasetMapper datasetMapper,
            DatasetService datasetService
    ) {
        this.dataOpsProductClient = dataOpsProductClient;
        this.datasetMapper = datasetMapper;
        this.datasetService = datasetService;
    }

    /** 查询 DataOps 中可供评审和导入的数据产品。 */
    public List<DataOpsProduct> listAvailableProducts() {
        return dataOpsProductClient.listProducts();
    }

    /** 将已发布的 DataOps 数据产品幂等登记为 MLOps 数据集。 */
    public Dataset importPublishedProduct(String productVersion, ImportIdentity identity) {
        validateIdentity(identity);
        Dataset existing = datasetMapper.findBySourceProductVersion("XnetDataOps", productVersion);
        if (existing != null) {
            return existing;
        }

        DataOpsProduct product = dataOpsProductClient.getProduct(productVersion);
        if (!"published".equals(product.status())) {
            throw new IllegalArgumentException("只有 DataOps 已发布的数据产品可以导入 MLOps");
        }
        Dataset dataset = buildDataset(product, identity);
        return datasetService.createDataset(dataset);
    }

    /** 校验网关注入的用户、租户和团队身份字段。 */
    private void validateIdentity(ImportIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("导入身份不能为空");
        }
        validateIdentityField("X-User-Id", identity.userId());
        validateIdentityField("X-Tenant-Id", identity.tenantUid());
        validateIdentityField("X-Team-Id", identity.teamUid());
        if (identity.organizationLevel() < 0 || identity.organizationLevel() > 3) {
            throw new IllegalArgumentException("X-Organization-Level 必须位于 0 到 3 之间");
        }
    }

    /** 校验单个可信身份头的格式。 */
    private void validateIdentityField(String fieldName, String value) {
        if (value == null || !IDENTITY_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(fieldName + " 缺失或格式不合法");
        }
    }

    /** 根据 DataOps 产品契约和组织身份构造 MLOps 数据集。 */
    private Dataset buildDataset(DataOpsProduct product, ImportIdentity identity) {
        Dataset dataset = new Dataset();
        dataset.setUserId(identity.userId());
        dataset.setDataset_file(product.productName() + "-" + product.productVersion());
        dataset.setType("POSTGRESQL_DATA_PRODUCT");
        dataset.setType_label("PostgreSQL 数据产品");
        dataset.setZone("production");
        dataset.setZone_label("生产数据产品区");
        dataset.setEncryption(true);
        dataset.setSubdata_area("recommendation");
        dataset.setBucket_name("XnetDataOps");
        dataset.setBucket_identifier("recommendation-curated");
        dataset.setTenant_uid(identity.tenantUid());
        dataset.setDept_uid(normalizeOptional(identity.deptUid()));
        dataset.setTeam_uid(identity.teamUid());
        dataset.setTeam_name(normalizeOptional(identity.teamName()));
        dataset.setLevel(identity.organizationLevel());
        dataset.setDescription("由 XnetDataOps 发布的真实推荐训练数据产品");
        dataset.setSourcePlatform("XnetDataOps");
        dataset.setSourceProductName(product.productName());
        dataset.setSourceProductVersion(product.productVersion());
        dataset.setSourceUri(
                "dataops://recommendation_curated/dcn_training?product_version=" + product.productVersion()
        );
        dataset.setRowCount(product.rowCount());
        dataset.setSchemaDigestSha256(product.schemaDigestSha256());
        dataset.setArtifactDigestSha256(product.artifactDigestSha256());
        dataset.setLineageReference(product.lineageReference());
        dataset.setImportStatus("ready");
        dataset.setImportedAt(new Date());
        return dataset;
    }

    /** 将空白可选字段转换为 null，避免存储无意义空字符串。 */
    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
