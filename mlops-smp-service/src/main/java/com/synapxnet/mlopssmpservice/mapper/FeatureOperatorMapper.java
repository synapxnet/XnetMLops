package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface FeatureOperatorMapper {

    /** 明确保留下划线领域字段，兼容全局驼峰设置。 Explicitly map snake-case domain fields under the global camel-case setting. */
    @Results(id = "featureOperatorResultMap", value = {
            @Result(column = "id", property = "id", id = true),
            @Result(column = "uid", property = "uid"),
            @Result(column = "url", property = "url"),
            @Result(column = "encrypted_token", property = "encrypted_token"),
            @Result(column = "operator_name", property = "operator_name"),
            @Result(column = "operator_code", property = "operator_code"),
            @Result(column = "operator_version", property = "operator_version"),
            @Result(column = "description", property = "description"),
            @Result(column = "tenant_uid", property = "tenant_uid"),
            @Result(column = "dept_uid", property = "dept_uid"),
            @Result(column = "team_uid", property = "team_uid"),
            @Result(column = "created_by", property = "created_by"),
            @Result(column = "updated_by", property = "updated_by"),
            @Result(column = "created_at", property = "created_at"),
            @Result(column = "updated_at", property = "updated_at"),
            @Result(column = "authorized_tenants", property = "authorized_tenants"),
            @Result(column = "tenant_name", property = "tenant_name"),
            @Result(column = "dept_name", property = "dept_name"),
            @Result(column = "team_name", property = "team_name")
    })
    @Select("SELECT f.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_feature_operators f " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON f.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON f.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON f.team_uid = tm.uid")
    List<FeatureOperator> selectAllWithOrgInfo();

    @ResultMap("featureOperatorResultMap")
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

    @ResultMap("featureOperatorResultMap")
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

    @ResultMap("featureOperatorResultMap")
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

    @ResultMap("featureOperatorResultMap")
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
