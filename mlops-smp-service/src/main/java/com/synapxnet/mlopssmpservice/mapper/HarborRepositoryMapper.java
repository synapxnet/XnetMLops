package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.HarborRepository;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface HarborRepositoryMapper {
    /** 明确保留下划线领域字段，兼容全局驼峰设置。 Explicitly map snake-case domain fields under the global camel-case setting. */
    @Results(id = "harborRepositoryResultMap", value = {
            @Result(column = "id", property = "id", id = true),
            @Result(column = "uid", property = "uid"),
            @Result(column = "name", property = "name"),
            @Result(column = "url", property = "url"),
            @Result(column = "username", property = "username"),
            @Result(column = "password", property = "password"),
            @Result(column = "created_by", property = "created_by"),
            @Result(column = "updated_by", property = "updated_by"),
            @Result(column = "created_at", property = "created_at"),
            @Result(column = "updated_at", property = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_smp_harbor_repository")
    List<HarborRepository> findAll();

    @ResultMap("harborRepositoryResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_harbor_repository WHERE id = #{id}")
    Optional<HarborRepository> findById(Integer id);

    @ResultMap("harborRepositoryResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_harbor_repository WHERE uid = #{uid}")
    Optional<HarborRepository> findByUid(String uid);

    @Insert("INSERT INTO xnet_mlops_smp_harbor_repository (" +
            "name, url, username, password, created_by, updated_by" +
            ") VALUES (" +
            "#{name}, #{url}, #{username}, #{password}, #{created_by}, #{updated_by}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HarborRepository repository);

    @Update("<script>" +
            "UPDATE xnet_mlops_smp_harbor_repository SET " +
            "name = #{name}, " +
            "url = #{url}, " +
            "username = #{username}, " +
            "<if test='password != null and password != \"\"'>" +
            "password = #{password}, " +
            "</if>" +
            "updated_by = #{updated_by} " +
            "WHERE id = #{id}" +
            "</script>")
    int update(HarborRepository repository);

    @Delete("DELETE FROM xnet_mlops_smp_harbor_repository WHERE id = #{id}")
    int deleteById(Integer id);
}
