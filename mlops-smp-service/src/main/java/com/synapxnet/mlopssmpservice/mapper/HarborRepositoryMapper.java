package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.HarborRepository;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface HarborRepositoryMapper {
    @Select("SELECT * FROM xnet_mlops_smp_harbor_repository")
    List<HarborRepository> findAll();

    @Select("SELECT * FROM xnet_mlops_smp_harbor_repository WHERE id = #{id}")
    Optional<HarborRepository> findById(Integer id);

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
