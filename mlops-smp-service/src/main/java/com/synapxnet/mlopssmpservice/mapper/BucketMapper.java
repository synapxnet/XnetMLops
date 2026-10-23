package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.Bucket;
import org.apache.ibatis.annotations.*;

import java.util.*;

@Mapper
public interface BucketMapper {
    /** 显式保留旧实体的下划线字段。 Preserve legacy entity snake-case fields explicitly. */
    @Results(id = "bucketEntityMap", value = {
        @Result(column = "tenant_uid", property = "tenant_uid"),
        @Result(column = "dept_uid", property = "dept_uid"),
        @Result(column = "team_uid", property = "team_uid"),
        @Result(column = "current_size", property = "current_size"),
        @Result(column = "max_size", property = "max_size"),
        @Result(column = "created_at", property = "created_at"),
        @Result(column = "updated_at", property = "updated_at"),
        @Result(column = "access_key", property = "access_key"),
        @Result(column = "authorized_tenants", property = "authorized_tenants"),
        @Result(column = "tenant_name", property = "tenant_name"),
        @Result(column = "dept_name", property = "dept_name"),
        @Result(column = "team_name", property = "team_name")
    })
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

    @ResultMap("bucketEntityMap")
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

    @ResultMap("bucketEntityMap")
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



