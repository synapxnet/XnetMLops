package com.synapxnet.mlopsmtpservice.recommendation;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RecommendationTrainingMapper {

    /** 按租户边界读取一个已导入的 DataOps 数据集。 */
    @Select("SELECT id AS dataset_id, tenant_uid, source_platform, source_product_version, " +
            "schema_digest_sha256, artifact_digest_sha256, import_status " +
            "FROM xnet_mlops_dpp_dataset WHERE id=#{datasetId} AND tenant_uid=#{tenantUid}")
    @Results(id = "recommendationDatasetReferenceMap", value = {
            @Result(property = "datasetId", column = "dataset_id"),
            @Result(property = "tenantUid", column = "tenant_uid"),
            @Result(property = "sourcePlatform", column = "source_platform"),
            @Result(property = "productVersion", column = "source_product_version"),
            @Result(property = "schemaDigestSha256", column = "schema_digest_sha256"),
            @Result(property = "artifactDigestSha256", column = "artifact_digest_sha256"),
            @Result(property = "importStatus", column = "import_status")
    })
    RecommendationDatasetReference findDataset(
            @Param("datasetId") long datasetId,
            @Param("tenantUid") String tenantUid
    );

    /** 新增运行中的推荐训练审计记录。 */
    @Insert("INSERT INTO xnet_mlops_mtp_recommendation_run (" +
            "run_uid, dataset_id, tenant_uid, user_id, product_version, approval_id, idempotency_key, " +
            "status, schema_digest_sha256, artifact_digest_sha256, artifact_reference, started_at" +
            ") VALUES (" +
            "#{runUid}, #{datasetId}, #{tenantUid}, #{userId}, #{productVersion}, #{approvalId}, " +
            "#{idempotencyKey}, #{status}, #{schemaDigestSha256}, #{artifactDigestSha256}, " +
            "#{artifactReference}, #{startedAt}" +
            ")")
    void insertRun(RecommendationTrainingRun run);

    /** 按幂等键查询既有训练运行。 */
    @Select("SELECT * FROM xnet_mlops_mtp_recommendation_run WHERE idempotency_key=#{idempotencyKey}")
    @Results(id = "recommendationTrainingRunMap", value = {
            @Result(property = "runUid", column = "run_uid"),
            @Result(property = "datasetId", column = "dataset_id"),
            @Result(property = "tenantUid", column = "tenant_uid"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productVersion", column = "product_version"),
            @Result(property = "approvalId", column = "approval_id"),
            @Result(property = "idempotencyKey", column = "idempotency_key"),
            @Result(property = "schemaDigestSha256", column = "schema_digest_sha256"),
            @Result(property = "artifactDigestSha256", column = "artifact_digest_sha256"),
            @Result(property = "modelDigestSha256", column = "model_digest_sha256"),
            @Result(property = "artifactReference", column = "artifact_reference"),
            @Result(property = "metricsJson", column = "metrics_json"),
            @Result(property = "errorSummary", column = "error_summary"),
            @Result(property = "startedAt", column = "started_at"),
            @Result(property = "completedAt", column = "completed_at")
    })
    RecommendationTrainingRun findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /** 按运行标识查询训练审计记录。 */
    @Select("SELECT * FROM xnet_mlops_mtp_recommendation_run WHERE run_uid=#{runUid}")
    @org.apache.ibatis.annotations.ResultMap("recommendationTrainingRunMap")
    RecommendationTrainingRun findByRunUid(@Param("runUid") String runUid);

    /** 按租户查询最近的推荐训练运行。 */
    @Select("SELECT * FROM xnet_mlops_mtp_recommendation_run " +
            "WHERE tenant_uid=#{tenantUid} ORDER BY started_at DESC LIMIT 100")
    @org.apache.ibatis.annotations.ResultMap("recommendationTrainingRunMap")
    List<RecommendationTrainingRun> findRecentByTenant(@Param("tenantUid") String tenantUid);

    /** 将训练运行更新为成功并保存模型摘要和指标。 */
    @Update("UPDATE xnet_mlops_mtp_recommendation_run SET status='succeeded', " +
            "model_digest_sha256=#{modelDigestSha256}, metrics_json=#{metricsJson}, " +
            "completed_at=#{completedAt} WHERE run_uid=#{runUid} AND status='running'")
    int completeRun(RecommendationTrainingRun run);

    /** 将训练运行更新为失败并保存受限错误摘要。 */
    @Update("UPDATE xnet_mlops_mtp_recommendation_run SET status='failed', " +
            "error_summary=#{errorSummary}, completed_at=#{completedAt} " +
            "WHERE run_uid=#{runUid} AND status='running'")
    int failRun(RecommendationTrainingRun run);
}
