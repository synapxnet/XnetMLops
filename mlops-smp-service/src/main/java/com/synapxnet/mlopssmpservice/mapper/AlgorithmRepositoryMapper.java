package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.AlgorithmRepository;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AlgorithmRepositoryMapper {

    @Select("SELECT a.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_algorithms a " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON a.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON a.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON a.team_uid = tm.uid")
    List<AlgorithmRepository> selectAllWithOrgInfo();

    @Select("SELECT a.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_algorithms a " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON a.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON a.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON a.team_uid = tm.uid " +
            "WHERE a.id = #{id}")
    Optional<AlgorithmRepository> findById(Long id);

    @Select("SELECT a.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_algorithms a " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON a.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON a.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON a.team_uid = tm.uid " +
            "WHERE a.uid = #{uid}")
    Optional<AlgorithmRepository> findByUid(String uid);

    @Insert("INSERT INTO xnet_mlops_smp_algorithms (" +
            "uid, url, encrypted_token, algorithm, algorithm_version, description, " +
            "tenant_uid, dept_uid, team_uid, authorized_tenants, created_by, updated_by" +
            ") VALUES (" +
            "#{uid}, #{url}, #{encrypted_token}, #{algorithm}, #{algorithm_version}, #{description}, " +
            "#{tenant_uid}, #{dept_uid}, #{team_uid}, #{authorized_tenants}, #{created_by}, #{updated_by}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AlgorithmRepository algorithmRepository);

    @Update("UPDATE xnet_mlops_smp_algorithms SET " +
            "url = #{url}, " +
            "encrypted_token = #{encrypted_token}, " +
            "algorithm = #{algorithm}, " +
            "algorithm_version = #{algorithm_version}, " +
            "description = #{description}, " +
            "tenant_uid = #{tenant_uid}, " +
            "dept_uid = #{dept_uid}, " +
            "team_uid = #{team_uid}, " +
            "authorized_tenants = #{authorized_tenants}, " +
            "updated_by = #{updated_by} " +
            "WHERE id = #{id}")
    int update(AlgorithmRepository algorithmRepository);

    @Delete("DELETE FROM xnet_mlops_smp_algorithms WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_algorithms " +
            "WHERE url = #{url} " +
            "AND algorithm = #{algorithm} " +
            "AND algorithm_version = #{algorithmVersion} " +
            "AND id != COALESCE(#{excludeId}, -1)")
    int countByUrlAndNameAndVersion(
            @Param("url") String url,
            @Param("algorithm") String algorithm,
            @Param("algorithmVersion") String algorithmVersion,
            @Param("excludeId") Long excludeId);


    @Select("<script>" +
            "SELECT a.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_smp_algorithms a " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON a.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON a.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON a.team_uid = tm.uid " +
            "WHERE 1=1" +
            "<if test='tenantUid != null'> AND a.tenant_uid = #{tenantUid}</if>" +
            "<if test='deptUid != null'> AND a.dept_uid = #{deptUid}</if>" +
            "<if test='teamUid != null'> AND a.team_uid = #{teamUid}</if>" +
            "<if test='algorithm != null'> AND a.algorithm LIKE CONCAT('%', #{algorithm}, '%')</if>" +
            "<if test='version != null'> AND a.algorithm_version = #{version}</if>" +
            "</script>")
    List<AlgorithmRepository> searchAlgorithms(
            @Param("tenantUid") String tenantUid,
            @Param("deptUid") String deptUid,
            @Param("teamUid") String teamUid,
            @Param("algorithm") String algorithm,
            @Param("version") String version);
}