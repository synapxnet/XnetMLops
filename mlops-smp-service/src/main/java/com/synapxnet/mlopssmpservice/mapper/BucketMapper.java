package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.Bucket;
import org.apache.ibatis.annotations.*;

import java.util.*;

@Mapper
public interface BucketMapper {
    @Select("SELECT b.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_sys_bucket b " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON b.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON b.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON b.team_uid = tm.uid")
    List<Bucket> selectBucketsWithOrgInfo();

    @Insert("INSERT INTO xnet_mlops_sys_bucket (" +
            "uid, name, identifier, type, tenant_uid, dept_uid, team_uid, " +
            "current_size, max_size, status, created_at, updated_at, " +
            "access_key, authorized_tenants" +
            ") VALUES (" +
            "#{uid}, #{name}, #{identifier}, #{type}, #{tenant_uid}, #{dept_uid}, #{team_uid}, " +
            "#{current_size}, #{max_size}, #{status}, #{created_at}, #{updated_at}, " +
            "#{access_key}, #{authorized_tenants}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBucket(Bucket bucket);

    @Update("UPDATE xnet_mlops_sys_bucket SET " +
            "name = #{name}, " +
            "identifier = #{identifier}, " +
            "type = #{type}, " +
            "tenant_uid = #{tenant_uid}, " +
            "dept_uid = #{dept_uid}, " +
            "team_uid = #{team_uid}, " +
            "current_size = #{current_size}, " +
            "max_size = #{max_size}, " +
            "status = #{status}, " +
            "updated_at = #{updated_at}, " +
            "access_key = #{access_key}, " +
            "authorized_tenants = #{authorized_tenants} " +
            "WHERE id = #{id}")
    int updateBucket(Bucket bucket);

    @Delete("DELETE FROM xnet_mlops_sys_bucket WHERE id = #{id}")
    int deleteBucketById(Long id);

    @Select("SELECT b.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_sys_bucket b " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON b.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON b.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON b.team_uid = tm.uid " +
            "WHERE b.id = #{id}")
    Optional<Bucket> findById(Long id);

    @Select("SELECT b.*, " +
            "t.tenant_name AS tenant_name, " +
            "d.dept_name AS dept_name, " +
            "tm.team_name AS team_name " +
            "FROM xnet_mlops_sys_bucket b " +
            "LEFT JOIN xnet_mlops_sys_tenant t ON b.tenant_uid = t.uid " +
            "LEFT JOIN xnet_mlops_sys_department d ON b.dept_uid = d.uid " +
            "LEFT JOIN xnet_mlops_sys_team tm ON b.team_uid = tm.uid " +
            "WHERE b.id = #{id}")
    Bucket selectBucketById(Long id);
}



