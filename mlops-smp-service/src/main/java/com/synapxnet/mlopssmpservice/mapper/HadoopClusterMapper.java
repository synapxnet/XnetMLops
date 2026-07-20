package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.HadoopCluster;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * Hadoop 集群 Mapper
 * 数据访问层
 */
@Mapper
public interface HadoopClusterMapper {

    // ==================== 查询操作 ====================

    /**
     * 查询所有节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster ORDER BY created_at DESC")
    List<HadoopCluster> findAll();

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE id = #{id}")
    Optional<HadoopCluster> findById(Long id);

    /**
     * 根据UID查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE uid = #{uid}")
    Optional<HadoopCluster> findByUid(String uid);

    /**
     * 根据节点类型查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE node_type = #{nodeType} ORDER BY created_at DESC")
    List<HadoopCluster> findByNodeType(@Param("nodeType") String nodeType);

    /**
     * 查询所有Master节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE node_type = 'master' ORDER BY created_at DESC")
    List<HadoopCluster> findMasters();

    /**
     * 查询所有Node节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE node_type = 'node' ORDER BY created_at DESC")
    List<HadoopCluster> findNodes();

    /**
     * 根据Master ID查询关联的Node节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE master_id = #{masterId} ORDER BY created_at DESC")
    List<HadoopCluster> findByMasterId(@Param("masterId") Long masterId);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE status = #{status} ORDER BY created_at DESC")
    List<HadoopCluster> findByStatus(@Param("status") String status);

    /**
     * 查询已部署的节点
     */
    @Select("SELECT * FROM xnet_mlops_smp_hadoop_cluster WHERE status IN ('deployed', 'running') ORDER BY created_at DESC")
    List<HadoopCluster> findDeployedClusters();

    // ==================== 插入操作 ====================

    /**
     * 插入新记录
     */
    @Insert("INSERT INTO xnet_mlops_smp_hadoop_cluster (" +
            "uid, name, description, host, port, ssh_user, ssh_password, ssh_private_key, " +
            "hadoop_version, os_type, node_type, deploy_mode, components, " +
            "hdfs_data_dirs, hdfs_replication, hdfs_block_size, " +
            "yarn_memory, yarn_cpu, ha_master_host, zk_cluster, " +
            "status, deploy_log, master_id, created_by" +
            ") VALUES (" +
            "#{uid}, #{name}, #{description}, #{host}, #{port}, #{sshUser}, #{sshPassword}, #{sshPrivateKey}, " +
            "#{hadoopVersion}, #{osType}, #{nodeType}, #{deployMode}, #{components}, " +
            "#{hdfsDataDirs}, #{hdfsReplication}, #{hdfsBlockSize}, " +
            "#{yarnMemory}, #{yarnCpu}, #{haMasterHost}, #{zkCluster}, " +
            "#{status}, #{deployLog}, #{masterId}, #{createdBy}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HadoopCluster hadoopCluster);

    // ==================== 更新操作 ====================

    /**
     * 更新记录
     */
    @Update("UPDATE xnet_mlops_smp_hadoop_cluster SET " +
            "name = #{name}, " +
            "description = #{description}, " +
            "host = #{host}, " +
            "port = #{port}, " +
            "ssh_user = #{sshUser}, " +
            "ssh_password = #{sshPassword}, " +
            "ssh_private_key = #{sshPrivateKey}, " +
            "hadoop_version = #{hadoopVersion}, " +
            "os_type = #{osType}, " +
            "deploy_mode = #{deployMode}, " +
            "components = #{components}, " +
            "hdfs_data_dirs = #{hdfsDataDirs}, " +
            "hdfs_replication = #{hdfsReplication}, " +
            "hdfs_block_size = #{hdfsBlockSize}, " +
            "yarn_memory = #{yarnMemory}, " +
            "yarn_cpu = #{yarnCpu}, " +
            "ha_master_host = #{haMasterHost}, " +
            "zk_cluster = #{zkCluster}, " +
            "status = #{status}, " +
            "deploy_log = #{deployLog}, " +
            "master_id = #{masterId}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int update(HadoopCluster hadoopCluster);

    /**
     * 更新状态和部署日志
     */
    @Update("UPDATE xnet_mlops_smp_hadoop_cluster SET " +
            "status = #{status}, " +
            "deploy_log = #{deployLog}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("deployLog") String deployLog);

    /**
     * 仅更新状态（用于实时状态刷新）
     */
    @Update("UPDATE xnet_mlops_smp_hadoop_cluster SET " +
            "status = #{status}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int updateStatusOnly(@Param("id") Long id, @Param("status") String status);

    // ==================== 删除操作 ====================

    /**
     * 根据ID删除
     */
    @Delete("DELETE FROM xnet_mlops_smp_hadoop_cluster WHERE id = #{id}")
    int deleteById(Long id);

    // ==================== 统计操作 ====================

    /**
     * 统计名称数量(用于唯一性校验)
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_hadoop_cluster WHERE name = #{name} AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 统计主机数量(用于唯一性校验)
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_hadoop_cluster WHERE host = #{host} AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int countByHost(@Param("host") String host, @Param("excludeId") Long excludeId);

    /**
     * 按节点类型统计数量
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_hadoop_cluster WHERE node_type = #{nodeType}")
    int countByNodeType(@Param("nodeType") String nodeType);

    /**
     * 按状态统计数量
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_hadoop_cluster WHERE status = #{status}")
    int countByStatus(@Param("status") String status);
}
