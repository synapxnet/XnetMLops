package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.DockerFile;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DockerFileMapper {
    /** 明确保留下划线领域字段，兼容全局驼峰设置。 Explicitly map snake-case domain fields under the global camel-case setting. */
    @Results(id = "dockerFileResultMap", value = {
            @Result(column = "id", property = "id", id = true),
            @Result(column = "uid", property = "uid"),
            @Result(column = "name", property = "name"),
            @Result(column = "content", property = "content"),
            @Result(column = "tags", property = "tags"),
            @Result(column = "push_status", property = "push_status"),
            @Result(column = "harbor_uid", property = "harbor_uid"),
            @Result(column = "push_history", property = "push_history"),
            @Result(column = "created_by", property = "created_by"),
            @Result(column = "updated_by", property = "updated_by"),
            @Result(column = "created_at", property = "created_at"),
            @Result(column = "updated_at", property = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_smp_docker_file")
    List<DockerFile> findAll();

    @ResultMap("dockerFileResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_docker_file WHERE id = #{id}")
    Optional<DockerFile> findById(Integer id);

    @ResultMap("dockerFileResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_docker_file WHERE uid = #{uid}")
    Optional<DockerFile> findByUID(String uid);

    @ResultMap("dockerFileResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_docker_file WHERE harbor_uid = #{harbor_uid}")
    List<DockerFile> findByHarborUid(String harbor_uid);

    @Insert("INSERT INTO xnet_mlops_smp_docker_file (" +
            "name, content, tags, push_status, harbor_uid, push_history, " +
            "created_by, updated_by" +
            ") VALUES (" +
            "#{name}, #{content}, #{tags}, #{push_status}, #{harbor_uid}, #{push_history}, " +
            "#{created_by}, #{updated_by}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DockerFile dockerFile);

    @Update("UPDATE xnet_mlops_smp_docker_file SET " +
            "name = #{name}, " +
            "content = #{content}, " +
            "tags = #{tags}, " +
            "push_status = #{push_status}, " +
            "harbor_uid = #{harbor_uid}, " +
            "push_history = #{push_history}, " +
            "updated_by = #{updated_by} " +
            "WHERE id = #{id}")
    int update(DockerFile dockerFile);

    @Update("UPDATE xnet_mlops_smp_docker_file SET " +
            "push_status = #{push_status}, " +
            "push_history = #{push_history} " +
            "WHERE uid = #{uid}")
    int updatePushStatus(@Param("uid") String uid,
                         @Param("push_status") DockerFile.PushStatus pushStatus,
                         @Param("push_history") String pushHistory);

    @Delete("DELETE FROM xnet_mlops_smp_docker_file WHERE id = #{id}")
    int deleteById(Integer id);
}
