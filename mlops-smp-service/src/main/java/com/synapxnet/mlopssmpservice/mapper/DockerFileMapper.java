package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.DockerFile;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DockerFileMapper {
    @Select("SELECT * FROM xnet_mlops_smp_docker_file")
    List<DockerFile> findAll();

    @Select("SELECT * FROM xnet_mlops_smp_docker_file WHERE id = #{id}")
    Optional<DockerFile> findById(Integer id);

    @Select("SELECT * FROM xnet_mlops_smp_docker_file WHERE uid = #{uid}")
    Optional<DockerFile> findByUID(String uid);

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
