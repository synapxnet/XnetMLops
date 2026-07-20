package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.JenkinsMaster;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * Jenkins Master Mapper
 * 数据访问层
 */
@Mapper
public interface JenkinsMasterMapper {

    // ==================== 查询操作 ====================

    /**
     * 查询所有Master节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters ORDER BY created_at DESC")
    List<JenkinsMaster> findAll();

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters WHERE id = #{id}")
    Optional<JenkinsMaster> findById(Long id);

    /**
     * 根据UID查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters WHERE uid = #{uid}")
    Optional<JenkinsMaster> findByUid(String uid);

    /**
     * 根据名称查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters WHERE name = #{name}")
    Optional<JenkinsMaster> findByName(String name);

    /**
     * 根据主机和端口查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters WHERE host = #{host} AND jenkins_port = #{jenkinsPort}")
    Optional<JenkinsMaster> findByHostAndJenkinsPort(@Param("host") String host, @Param("jenkinsPort") Integer jenkinsPort);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters WHERE status = #{status} ORDER BY created_at DESC")
    List<JenkinsMaster> findByStatus(String status);

    /**
     * 查询已部署且运行中的Master节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_jenkins_masters WHERE status IN ('deployed', 'running') ORDER BY created_at DESC")
    List<JenkinsMaster> findDeployedMasters();

    // ==================== 插入操作 ====================

    /**
     * 插入新记录
     */
    @Insert("INSERT INTO xnet_mlops_smp_jenkins_masters (" +
            "uid, name, host, port, username, encrypted_password, os_type, " +
            "jenkins_port, jenkins_home, jenkins_version, java_version, java_opts, " +
            "admin_username, encrypted_admin_password, credentials_config, " +
            "status, initial_password, deploy_log, " +
            "region, cpu_cores, ram_gb, disk_gb, " +
            "tenant_uid, description, created_by, updated_by" +
            ") VALUES (" +
            "#{uid}, #{name}, #{host}, #{port}, #{username}, #{encrypted_password}, #{os_type}, " +
            "#{jenkins_port}, #{jenkins_home}, #{jenkins_version}, #{java_version}, #{java_opts}, " +
            "#{admin_username}, #{encrypted_admin_password}, #{credentials_config}, " +
            "#{status}, #{initial_password}, #{deploy_log}, " +
            "#{region}, #{cpu_cores}, #{ram_gb}, #{disk_gb}, " +
            "#{tenant_uid}, #{description}, #{created_by}, #{updated_by}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JenkinsMaster jenkinsMaster);

    // ==================== 更新操作 ====================

    /**
     * 更新记录
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_masters SET " +
            "name = #{name}, " +
            "host = #{host}, " +
            "port = #{port}, " +
            "username = #{username}, " +
            "encrypted_password = #{encrypted_password}, " +
            "os_type = #{os_type}, " +
            "jenkins_port = #{jenkins_port}, " +
            "jenkins_home = #{jenkins_home}, " +
            "jenkins_version = #{jenkins_version}, " +
            "java_version = #{java_version}, " +
            "java_opts = #{java_opts}, " +
            "admin_username = #{admin_username}, " +
            "encrypted_admin_password = #{encrypted_admin_password}, " +
            "credentials_config = #{credentials_config}, " +
            "status = #{status}, " +
            "initial_password = #{initial_password}, " +
            "deploy_log = #{deploy_log}, " +
            "region = #{region}, " +
            "cpu_cores = #{cpu_cores}, " +
            "ram_gb = #{ram_gb}, " +
            "disk_gb = #{disk_gb}, " +
            "description = #{description}, " +
            "updated_by = #{updated_by}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int update(JenkinsMaster jenkinsMaster);

    /**
     * 更新状态和部署日志
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_masters SET " +
            "status = #{status}, " +
            "deploy_log = #{deployLog}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("deployLog") String deployLog);

    /**
     * 更新初始密码
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_masters SET " +
            "initial_password = #{initialPassword}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateInitialPassword(@Param("id") Long id, @Param("initialPassword") String initialPassword);

    /**
     * 更新心跳时间
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_masters SET " +
            "last_heartbeat = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateHeartbeat(Long id);

    /**
     * 更新凭证配置
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_masters SET " +
            "credentials_config = #{credentialsConfig}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateCredentialsConfig(@Param("id") Long id, @Param("credentialsConfig") String credentialsConfig);

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    @Delete("DELETE FROM xnet_mlops_smp_jenkins_masters WHERE id = #{id}")
    int deleteById(Long id);

    // ==================== 统计操作 ====================

    /**
     * 统计名称数量(用于唯一性校验)
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_jenkins_masters WHERE name = #{name} AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 统计主机和Jenkins端口数量(用于唯一性校验)
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_jenkins_masters WHERE host = #{host} AND jenkins_port = #{jenkinsPort} AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int countByHostAndJenkinsPort(@Param("host") String host, @Param("jenkinsPort") Integer jenkinsPort, @Param("excludeId") Long excludeId);
}
