package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.DataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DataSourceMapper {

    @Results(id = "dataSourceResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "name", column = "name"),
            @Result(property = "type", column = "type"),
            @Result(property = "host", column = "host"),
            @Result(property = "port", column = "port"),
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "defaultDatabase", column = "default_database"),
            @Result(property = "allowedDatabases", column = "allowed_databases"),
            @Result(property = "description", column = "description"),
            @Result(property = "enabled", column = "enabled"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_sys_datasource ORDER BY created_at DESC")
    List<DataSource> findAll();

    @ResultMap("dataSourceResultMap")
    @Select("SELECT * FROM xnet_mlops_sys_datasource WHERE id = #{id}")
    Optional<DataSource> findById(Long id);

    @ResultMap("dataSourceResultMap")
    @Select("SELECT * FROM xnet_mlops_sys_datasource WHERE uid = #{uid}")
    Optional<DataSource> findByUid(String uid);

    @ResultMap("dataSourceResultMap")
    @Select("SELECT * FROM xnet_mlops_sys_datasource WHERE enabled = true ORDER BY name")
    List<DataSource> findAllEnabled();

    @Insert("INSERT INTO xnet_mlops_sys_datasource (" +
            "uid, name, type, host, port, username, password, " +
            "default_database, allowed_databases, description, enabled, created_by, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{name}, #{type}, #{host}, #{port}, #{username}, #{password}, " +
            "#{defaultDatabase}, #{allowedDatabases}, #{description}, #{enabled}, #{createdBy}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataSource dataSource);

    @Update("UPDATE xnet_mlops_sys_datasource SET " +
            "name = #{name}, " +
            "type = #{type}, " +
            "host = #{host}, " +
            "port = #{port}, " +
            "username = #{username}, " +
            "password = #{password}, " +
            "default_database = #{defaultDatabase}, " +
            "allowed_databases = #{allowedDatabases}, " +
            "description = #{description}, " +
            "enabled = #{enabled}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(DataSource dataSource);

    @Delete("DELETE FROM xnet_mlops_sys_datasource WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_sys_datasource WHERE name = #{name}")
    int countByName(String name);

    @Select("SELECT COUNT(*) FROM xnet_mlops_sys_datasource WHERE name = #{name} AND id != #{id}")
    int countByNameExcludeId(@Param("name") String name, @Param("id") Long id);
}
