package com.synapxnet.mlopsmepservice.agent;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 为 MEP Agent 证据、探针和受控动作提供参数化持久化操作。
 */
@Mapper
public interface AgentMepMapper {

    /** 根据模型和版本查询输入契约。 */
    @Select("SELECT * FROM xnet_mlops_mep_model_contract "
            + "WHERE model_uid = #{modelUid} AND model_version = #{modelVersion} LIMIT 1")
    ModelContract findContract(
            @Param("modelUid") String modelUid,
            @Param("modelVersion") String modelVersion);

    /** 根据契约 UID 查询输入契约。 */
    @Select("SELECT * FROM xnet_mlops_mep_model_contract WHERE uid = #{uid} LIMIT 1")
    ModelContract findContractByUid(@Param("uid") String uid);

    /** 查询部署全部修订并按修订号倒序返回。 */
    @Select("SELECT * FROM xnet_mlops_mep_deployment_revision "
            + "WHERE deployment_uid = #{deploymentUid} ORDER BY revision_number DESC")
    List<DeploymentRevision> findRevisions(@Param("deploymentUid") String deploymentUid);

    /** 查询部署指定修订。 */
    @Select("SELECT * FROM xnet_mlops_mep_deployment_revision "
            + "WHERE deployment_uid = #{deploymentUid} AND revision_number = #{revisionNumber} LIMIT 1")
    DeploymentRevision findRevision(
            @Param("deploymentUid") String deploymentUid,
            @Param("revisionNumber") Long revisionNumber);

    /** 按 Workspace、动作类型和幂等键查询已有动作。 */
    @Select("SELECT * FROM xnet_mlops_mep_deployment_action WHERE workspace_id = #{workspaceId} "
            + "AND action_type = #{actionType} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    DeploymentAction findActionByIdempotency(
            @Param("workspaceId") String workspaceId,
            @Param("actionType") String actionType,
            @Param("idempotencyKey") String idempotencyKey);

    /** 按动作 UID 查询持久化状态。 */
    @Select("SELECT * FROM xnet_mlops_mep_deployment_action WHERE uid = #{uid} LIMIT 1")
    DeploymentAction findActionByUid(@Param("uid") String uid);

    /** 追加创建部署动作；唯一键负责最终幂等裁决。 */
    @Insert("INSERT INTO xnet_mlops_mep_deployment_action "
            + "(uid, deployment_uid, action_type, from_revision, target_revision, status, stage, request_id, "
            + "workspace_id, incident_id, trace_id, approval_id, idempotency_key, request_digest, "
            + "expected_resource_version, verification_policy_json, dry_run, created_by) VALUES "
            + "(#{uid}, #{deploymentUid}, #{actionType}, #{fromRevision}, #{targetRevision}, #{status}, #{stage}, "
            + "#{requestId}, #{workspaceId}, #{incidentId}, #{traceId}, #{approvalId}, #{idempotencyKey}, "
            + "#{requestDigest}, #{expectedResourceVersion}, #{verificationPolicyJson}, #{dryRun}, #{createdBy})")
    int insertAction(DeploymentAction action);

    /** 更新动作状态机阶段、错误和时间，不修改幂等与治理字段。 */
    @Update("UPDATE xnet_mlops_mep_deployment_action SET status = #{status}, stage = #{stage}, "
            + "error_code = #{errorCode}, error_message = #{errorMessage}, "
            + "started_at = COALESCE(started_at, #{startedAt}), completed_at = #{completedAt} WHERE uid = #{uid}")
    int updateAction(DeploymentAction action);

    /** 插入推理探针聚合事实。 */
    @Insert("INSERT INTO xnet_mlops_mep_inference_probe "
            + "(uid, deployment_uid, revision_number, test_dataset_ref, sample_count, success_count, error_count, "
            + "error_rate, p50_ms, p95_ms, input_dimension, contract_status, result_digest, incident_id, trace_id, "
            + "started_at, completed_at) VALUES (#{uid}, #{deploymentUid}, #{revisionNumber}, #{testDatasetRef}, "
            + "#{sampleCount}, #{successCount}, #{errorCount}, #{errorRate}, #{p50Ms}, #{p95Ms}, #{inputDimension}, "
            + "#{contractStatus}, #{resultDigest}, #{incidentId}, #{traceId}, #{startedAt}, #{completedAt})")
    int insertProbe(InferenceProbe probe);

    /**
     * 以乐观锁切换活动修订并递增资源版本；影响行数必须为 1。
     */
    @Update("UPDATE xnet_mlops_mep_model_deployment SET active_revision = #{target.revisionNumber}, "
            + "model_uid = #{target.modelUid}, model_version = #{target.modelVersion}, "
            + "image_name = #{target.imageName}, status = 'running', resource_version = resource_version + 1, "
            + "last_verified_at = NOW(3), updated_at = NOW() "
            + "WHERE uid = #{deploymentUid} AND resource_version = #{expectedVersion}")
    int activateRevision(
            @Param("deploymentUid") String deploymentUid,
            @Param("expectedVersion") Long expectedVersion,
            @Param("target") DeploymentRevision target);

    /**
     * 将进程重启前遗留的 RUNNING 动作明确标记失败，保留事实供重新审批或重试。
     */
    @Update("UPDATE xnet_mlops_mep_deployment_action SET status = 'FAILED', stage = 'COMPENSATING', "
            + "error_code = 'PROCESS_RESTARTED', error_message = '执行进程重启，动作需人工核验', "
            + "completed_at = NOW(3) WHERE status = 'RUNNING'")
    int failInterruptedActions();
}
