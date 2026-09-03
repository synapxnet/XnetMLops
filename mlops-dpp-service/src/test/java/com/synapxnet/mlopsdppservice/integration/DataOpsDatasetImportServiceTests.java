package com.synapxnet.mlopsdppservice.integration;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import com.synapxnet.mlopsdppservice.mapper.DatasetMapper;
import com.synapxnet.mlopsdppservice.service.DatasetService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataOpsDatasetImportServiceTests {

    /** 验证已发布产品按可信组织身份转换为真实 MLOps 数据集元数据。 */
    @Test
    void importsPublishedProductWithLineageAndDigests() {
        DataOpsProductClient client = mock(DataOpsProductClient.class);
        DatasetMapper mapper = mock(DatasetMapper.class);
        DatasetService datasetService = mock(DatasetService.class);
        DataOpsDatasetImportService importService = new DataOpsDatasetImportService(
                client,
                mapper,
                datasetService
        );
        when(mapper.findBySourceProductVersion("XnetDataOps", "recommendation-v1")).thenReturn(null);
        when(client.getProduct("recommendation-v1")).thenReturn(product("published"));
        when(datasetService.createDataset(any(Dataset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Dataset dataset = importService.importPublishedProduct(
                "recommendation-v1",
                new ImportIdentity("17870171303", "tenant-a", "team-a", "dept-a", "推荐团队", 3)
        );

        assertEquals("XnetDataOps", dataset.getSourcePlatform());
        assertEquals("recommendation-v1", dataset.getSourceProductVersion());
        assertEquals(80_000, dataset.getRowCount());
        assertEquals("schema-digest", dataset.getSchemaDigestSha256());
        assertEquals("artifact-digest", dataset.getArtifactDigestSha256());
        assertEquals("dataops://lineage/v1", dataset.getLineageReference());
        assertEquals("tenant-a", dataset.getTenant_uid());
        assertEquals("team-a", dataset.getTeam_uid());
        assertEquals("ready", dataset.getImportStatus());
    }

    /** 验证 DataOps 未发布版本不能进入 MLOps。 */
    @Test
    void rejectsUnpublishedProduct() {
        DataOpsProductClient client = mock(DataOpsProductClient.class);
        DatasetMapper mapper = mock(DatasetMapper.class);
        DatasetService datasetService = mock(DatasetService.class);
        DataOpsDatasetImportService importService = new DataOpsDatasetImportService(
                client,
                mapper,
                datasetService
        );
        when(mapper.findBySourceProductVersion("XnetDataOps", "recommendation-v1")).thenReturn(null);
        when(client.getProduct("recommendation-v1")).thenReturn(product("validated"));

        assertThrows(
                IllegalArgumentException.class,
                () -> importService.importPublishedProduct(
                        "recommendation-v1",
                        new ImportIdentity("17870171303", "tenant-a", "team-a", null, null, 3)
                )
        );
        verifyNoInteractions(datasetService);
    }

    /** 构造指定发布状态的 DataOps 产品测试对象。 */
    private DataOpsProduct product(String status) {
        return new DataOpsProduct(
                "recommendation_dcn_training",
                "recommendation-v1",
                80_000,
                40_000,
                40_000,
                "schema-digest",
                "artifact-digest",
                "dataops://lineage/v1",
                status,
                OffsetDateTime.now(),
                "published".equals(status) ? OffsetDateTime.now() : null
        );
    }
}
