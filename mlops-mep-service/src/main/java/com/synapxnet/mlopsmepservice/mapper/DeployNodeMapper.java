package com.synapxnet.mlopsmepservice.mapper;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeployNodeMapper {

    @Select("SELECT * FROM xnet_mlops_mep_deploy_node ORDER BY created_at DESC")
    List<DeployNode> findAll();

    @Select("SELECT * FROM xnet_mlops_mep_deploy_node WHERE id = #{id}")
    DeployNode findById(Long id);

    @Select("SELECT * FROM xnet_mlops_mep_deploy_node WHERE uid = #{uid}")
    DeployNode findByUid(String uid);

    @Select("SELECT * FROM xnet_mlops_mep_deploy_node WHERE status = 'online'")
    List<DeployNode> findOnlineNodes();

    @Insert("INSERT INTO xnet_mlops_mep_deploy_node (uid, name, ip_address, port, status, cpu_cores, memory_gb, gpu_info, docker_version, nginx_status, labels, description, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{ipAddress}, #{port}, #{status}, #{cpuCores}, #{memoryGb}, #{gpuInfo}, #{dockerVersion}, #{nginxStatus}, #{labels}, #{description}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DeployNode node);

    @Update("UPDATE xnet_mlops_mep_deploy_node SET name = #{name}, ip_address = #{ipAddress}, port = #{port}, " +
            "cpu_cores = #{cpuCores}, memory_gb = #{memoryGb}, gpu_info = #{gpuInfo}, labels = #{labels}, " +
            "description = #{description}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(DeployNode node);

    @Update("UPDATE xnet_mlops_mep_deploy_node SET status = #{status}, docker_version = #{dockerVersion}, nginx_status = #{nginxStatus}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("dockerVersion") String dockerVersion, @Param("nginxStatus") String nginxStatus);

    @Delete("DELETE FROM xnet_mlops_mep_deploy_node WHERE id = #{id}")
    int deleteById(Long id);
}
