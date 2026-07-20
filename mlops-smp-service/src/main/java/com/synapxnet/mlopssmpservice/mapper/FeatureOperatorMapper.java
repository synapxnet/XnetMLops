package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface FeatureOperatorMapper {

    @Select("SELECT f.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_feature_operators f " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON f.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON f.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON f.team_uid = tm.uid")
    List<FeatureOperator> selectAllWithOrgInfo();

    @Select("SELECT f.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_feature_operators f " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON f.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON f.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON f.team_uid = tm.uid " +
            "WHERE f.id = #{id}")
    Optional<FeatureOperator> findById(Long id);

    @Select("SELECT f.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_feature_operators f " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON f.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON f.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON f.team_uid = tm.uid " +
            "WHERE f.uid = #{uid}")
    Optional<FeatureOperator> findByUid(String uid);

    @Insert("INSERT INTO xnet_mlops_smp_feature_operators (" +
            "uid, url, encrypted_token, operator_name, operator_code, operator_version, description, " +
            "tenant_uid, dept_uid, team_uid, authorized_tenants, created_by, updated_by" +
            ") VALUES (" +
            "#{uid}, #{url}, #{encrypted_token}, #{operator_name}, #{operator_code}, #{operator_version}, #{description}, " +
            "#{tenant_uid}, #{dept_uid}, #{team_uid}, #{authorized_tenants}, #{created_by}, #{updated_by}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeatureOperator featureOperator);

    @Update("UPDATE xnet_mlops_smp_feature_operators SET " +
            "url = #{url}, " +
            "encrypted_token = #{encrypted_token}, " +
            "operator_name = #{operator_name}, " +
            "operator_code = #{operator_code}, " +
            "operator_version = #{operator_version}, " +
            "description = #{description}, " +
            "tenant_uid = #{tenant_uid}, " +
            "dept_uid = #{dept_uid}, " +
            "team_uid = #{team_uid}, " +
            "authorized_tenants = #{authorized_tenants}, " +
            "updated_by = #{updated_by} " +
            "WHERE id = #{id}")
    int update(FeatureOperator featureOperator);

    @Delete("DELETE FROM xnet_mlops_smp_feature_operators WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_feature_operators " +
            "WHERE url = #{url} " +
            "AND operator_code = #{operatorCode} " +
            "AND operator_version = #{operatorVersion} " +
            "AND id != COALESCE(#{excludeId}, -1)")
    int countByUrlAndCodeAndVersion(
            @Param("url") String url,
            @Param("operatorCode") String operatorCode,
            @Param("operatorVersion") String operatorVersion,
            @Param("excludeId") Long excludeId);

    @Select("<script>" +
            "SELECT f.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_feature_operators f " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON f.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON f.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON f.team_uid = tm.uid " +
            "WHERE 1=1" +
            "<if test='tenantUid != null'> AND f.tenant_uid = #{tenantUid}</if>" +
            "<if test='deptUid != null'> AND f.dept_uid = #{deptUid}</if>" +
            "<if test='teamUid != null'> AND f.team_uid = #{teamUid}</if>" +
            "<if test='operatorName != null'> AND f.operator_name LIKE CONCAT('%', #{operatorName}, '%')</if>" +
            "<if test='operatorCode != null'> AND f.operator_code LIKE CONCAT('%', #{operatorCode}, '%')</if>" +
            "<if test='version != null'> AND f.operator_version = #{version}</if>" +
            "</script>")
    List<FeatureOperator> searchOperators(
            @Param("tenantUid") String tenantUid,
            @Param("deptUid") String deptUid,
            @Param("teamUid") String teamUid,
            @Param("operatorName") String operatorName,
            @Param("operatorCode") String operatorCode,
            @Param("version") String version);

    @Select("SELECT f.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_feature_operators f " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON f.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON f.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON f.team_uid = tm.uid " +
            "WHERE f.operator_code = #{operatorCode}")
    List<FeatureOperator> findByOperatorCode(@Param("operatorCode") String operatorCode);
}
