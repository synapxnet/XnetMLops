/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly forbidden to copy, distribute, or use without explicit authorization.
 * 模型制品登记持久化映射。 / Model artifact persistence mapper.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-16 | Version: 1.0.0 | Security Level: INTERNAL
 */
package com.synapxnet.mlopsmtpservice.mapper;

import com.synapxnet.mlopsmtpservice.entity.ModelArtifact;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 仅操作模型制品登记表。 / Operates only on the model artifact registry table. */
@Mapper
public interface ModelArtifactMapper {
    @Results(id = "modelArtifactMap", value = {
        @Result(property = "id", column = "id"), @Result(property = "uid", column = "uid"),
        @Result(property = "outputName", column = "output_name"), @Result(property = "framework", column = "framework"),
        @Result(property = "domain", column = "domain_name"), @Result(property = "teamUid", column = "team_uid"),
        @Result(property = "teamName", column = "team_name"), @Result(property = "tenantUid", column = "tenant_uid"),
        @Result(property = "deptUid", column = "dept_uid"), @Result(property = "userId", column = "user_id"),
        @Result(property = "description", column = "description"), @Result(property = "artifactPath", column = "artifact_path"),
        @Result(property = "createdAt", column = "created_at"), @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_mtp_model_artifact WHERE tenant_uid = #{tenantUid} ORDER BY created_at DESC")
    List<ModelArtifact> findByTenant(@Param("tenantUid") String tenantUid);

    @Select("SELECT COUNT(*) FROM xnet_mlops_mtp_model_artifact WHERE tenant_uid = #{tenantUid} AND output_name = #{outputName}")
    int countByName(@Param("tenantUid") String tenantUid, @Param("outputName") String outputName);

    @Insert("INSERT INTO xnet_mlops_mtp_model_artifact (uid, output_name, framework, domain_name, team_uid, team_name, tenant_uid, dept_uid, user_id, description, artifact_path, created_at, updated_at) VALUES (#{uid}, #{outputName}, #{framework}, #{domain}, #{teamUid}, #{teamName}, #{tenantUid}, #{deptUid}, #{userId}, #{description}, #{artifactPath}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelArtifact artifact);
}
