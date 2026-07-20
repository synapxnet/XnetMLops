package com.synapxnet.mlopsmtpservice.mapper;

import com.synapxnet.mlopsmtpservice.entity.*;

import org.apache.ibatis.annotations.*;

import java.util.Optional;
import java.util.List;

@Mapper
public interface AlgorithmMapper {

    @Insert("INSERT INTO xnet_mlops_mtp_algorithms (" +
            "uid,userId, algorithm_name, version, zone,zone_label, encryption, subdata_area, " +
            "bucket_name, bucket_identifier, team_uid,team_name, description, " +
            "tenant_uid, dept_uid, level, is_CAS, cloud_algorithm_id" +
            ") VALUES (" +
            "#{uid},#{userId}, #{algorithm_name}, #{version}, #{zone},#{zone_label}, #{encryption}, #{subdata_area}, " +
            "#{bucket_name}, #{bucket_identifier}, #{team_uid}, #{team_name}, #{description}, " +
            "#{tenant_uid}, #{dept_uid}, #{level}, #{isCAS}, #{cloud_algorithm_id}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAlgorithm(Algorithm algorithm);

    @Select("SELECT COUNT(*) FROM xnet_mlops_mtp_algorithms WHERE algorithm_name = #{algorithmName}")
    int countByAlgorithmName(String algorithmName);

    @Select("SELECT *, is_CAS AS isCAS FROM xnet_mlops_mtp_algorithms WHERE id = #{id}")
    Optional<Algorithm> findById(Long id);

    @Select("SELECT *, is_CAS AS isCAS FROM xnet_mlops_mtp_algorithms WHERE uid = #{uid}")
    Optional<Algorithm> findAlgorithmsByUID(String uid);

    @Select("SELECT *, is_CAS AS isCAS FROM xnet_mlops_mtp_algorithms")
    List<Algorithm> findAll();

    @Update("UPDATE xnet_mlops_mtp_algorithms SET " +
            "uid = #{uid}, " +
            "userId = #{userId}, " +
            "algorithm_name = #{algorithm_name}, " +
            "version = #{version}, " +
            "zone = #{zone}, " +
            "zone_label = #{zone_label}, " +
            "encryption = #{encryption}, " +
            "subdata_area = #{subdata_area}, " +
            "bucket_name = #{bucket_name}, " +
            "bucket_identifier = #{bucket_identifier}, " +
            "team_uid = #{team_uid}, " +
            "team_name = #{team_name}, " +
            "description = #{description}, " +
            "tenant_uid = #{tenant_uid}, " +
            "dept_uid = #{dept_uid}, " +
            "level = #{level}, " +
            "is_CAS = #{isCAS}, " +
            "cloud_algorithm_id = #{cloud_algorithm_id} " +
            "WHERE id = #{id}")
    int updateAlgorithm(Algorithm algorithm);

    @Delete("DELETE FROM xnet_mlops_mtp_algorithms WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT *, is_CAS AS isCAS FROM xnet_mlops_mtp_algorithms WHERE id = #{id}")
    Algorithm selectById(Long id);

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
}
