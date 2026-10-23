package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.JenkinsNode;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface JenkinsNodeMapper {

    /** 明确保留下划线领域字段，兼容全局驼峰设置。 Explicitly map snake-case domain fields under the global camel-case setting. */
    @Results(id = "jenkinsNodeResultMap", value = {
            @Result(column = "id", property = "id", id = true),
            @Result(column = "uid", property = "uid"),
            @Result(column = "name", property = "name"),
            @Result(column = "host", property = "host"),
            @Result(column = "port", property = "port"),
            @Result(column = "username", property = "username"),
            @Result(column = "encrypted_password", property = "encrypted_password"),
            @Result(column = "os_type", property = "os_type"),
            @Result(column = "region", property = "region"),
            @Result(column = "container_type", property = "container_type"),
            @Result(column = "resource_type", property = "resource_type"),
            @Result(column = "resource_spec", property = "resource_spec"),
            @Result(column = "cpu_cores", property = "cpu_cores"),
            @Result(column = "ram_gb", property = "ram_gb"),
            @Result(column = "gpu_memory", property = "gpu_memory"),
            @Result(column = "gpu_model", property = "gpu_model"),
            @Result(column = "gpu_count", property = "gpu_count"),
            @Result(column = "status", property = "status"),
            @Result(column = "jenkins_url", property = "jenkins_url"),
            @Result(column = "agent_name", property = "agent_name"),
            @Result(column = "work_dir", property = "work_dir"),
            @Result(column = "java_version", property = "java_version"),
            @Result(column = "python_version", property = "python_version"),
            @Result(column = "agent_version", property = "agent_version"),
            @Result(column = "labels", property = "labels"),
            @Result(column = "description", property = "description"),
            @Result(column = "deploy_log", property = "deploy_log"),
            @Result(column = "last_heartbeat", property = "last_heartbeat"),
            @Result(column = "created_by", property = "created_by"),
            @Result(column = "updated_by", property = "updated_by"),
            @Result(column = "created_at", property = "created_at"),
            @Result(column = "updated_at", property = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_nodes ORDER BY created_at DESC")
    List<JenkinsNode> findAll();

    @ResultMap("jenkinsNodeResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_nodes WHERE id = #{id}")
    Optional<JenkinsNode> findById(Long id);

    @ResultMap("jenkinsNodeResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_nodes WHERE uid = #{uid}")
    Optional<JenkinsNode> findByUid(String uid);

    @ResultMap("jenkinsNodeResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_nodes WHERE name = #{name}")
    Optional<JenkinsNode> findByName(String name);

    @ResultMap("jenkinsNodeResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_nodes WHERE host = #{host} AND port = #{port}")
    Optional<JenkinsNode> findByHostAndPort(@Param("host") String host, @Param("port") Integer port);

    @ResultMap("jenkinsNodeResultMap")
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_nodes WHERE status = #{status}")
    List<JenkinsNode> findByStatus(String status);

    @Insert("INSERT INTO xnet_mlops_smp_jenkins_nodes (" +
            "uid, name, host, port, username, encrypted_password, os_type, region, " +
            "container_type, resource_type, resource_spec, cpu_cores, ram_gb, gpu_memory, gpu_model, gpu_count, " +
            "status, jenkins_url, agent_name, work_dir, java_version, python_version, agent_version, " +
            "labels, description, deploy_log, created_by, updated_by" +
            ") VALUES (" +
            "#{uid}, #{name}, #{host}, #{port}, #{username}, #{encrypted_password}, #{os_type}, #{region}, " +
            "#{container_type}, #{resource_type}, #{resource_spec}, #{cpu_cores}, #{ram_gb}, #{gpu_memory}, #{gpu_model}, #{gpu_count}, " +
            "#{status}, #{jenkins_url}, #{agent_name}, #{work_dir}, #{java_version}, #{python_version}, #{agent_version}, " +
            "#{labels}, #{description}, #{deploy_log}, #{created_by}, #{updated_by}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JenkinsNode jenkinsNode);

    @Update("UPDATE xnet_mlops_smp_jenkins_nodes SET " +
            "name = #{name}, " +
            "host = #{host}, " +
            "port = #{port}, " +
            "username = #{username}, " +
            "encrypted_password = #{encrypted_password}, " +
            "os_type = #{os_type}, " +
            "region = #{region}, " +
            "container_type = #{container_type}, " +
            "resource_type = #{resource_type}, " +
            "resource_spec = #{resource_spec}, " +
            "cpu_cores = #{cpu_cores}, " +
            "ram_gb = #{ram_gb}, " +
            "gpu_memory = #{gpu_memory}, " +
            "gpu_model = #{gpu_model}, " +
            "gpu_count = #{gpu_count}, " +
            "status = #{status}, " +
            "jenkins_url = #{jenkins_url}, " +
            "agent_name = #{agent_name}, " +
            "work_dir = #{work_dir}, " +
            "java_version = #{java_version}, " +
            "python_version = #{python_version}, " +
            "agent_version = #{agent_version}, " +
            "labels = #{labels}, " +
            "description = #{description}, " +
            "deploy_log = #{deploy_log}, " +
            "updated_by = #{updated_by}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int update(JenkinsNode jenkinsNode);

    @Update("UPDATE xnet_mlops_smp_jenkins_nodes SET " +
            "status = #{status}, " +
            "deploy_log = #{deployLog}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("deployLog") String deployLog);

    @Update("UPDATE xnet_mlops_smp_jenkins_nodes SET " +
            "last_heartbeat = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateHeartbeat(Long id);

    @Delete("DELETE FROM xnet_mlops_smp_jenkins_nodes WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_jenkins_nodes WHERE name = #{name} AND id != COALESCE(#{excludeId}, -1)")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_jenkins_nodes WHERE host = #{host} AND port = #{port} AND id != COALESCE(#{excludeId}, -1)")
    int countByHostAndPort(@Param("host") String host, @Param("port") Integer port, @Param("excludeId") Long excludeId);
}
