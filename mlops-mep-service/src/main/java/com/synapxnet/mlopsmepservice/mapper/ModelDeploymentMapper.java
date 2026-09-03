package com.synapxnet.mlopsmepservice.mapper;

import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModelDeploymentMapper {

    @Select("SELECT * FROM xnet_mlops_mep_model_deployment ORDER BY created_at DESC")
    List<ModelDeployment> findAll();

    @Select("SELECT * FROM xnet_mlops_mep_model_deployment WHERE id = #{id}")
    ModelDeployment findById(Long id);

    @Select("SELECT * FROM xnet_mlops_mep_model_deployment WHERE uid = #{uid}")
    ModelDeployment findByUid(String uid);

    @Select("SELECT * FROM xnet_mlops_mep_model_deployment WHERE node_uid = #{nodeUid}")
    List<ModelDeployment> findByNodeUid(String nodeUid);

    @Select("SELECT * FROM xnet_mlops_mep_model_deployment WHERE status = #{status}")
    List<ModelDeployment> findByStatus(String status);

    @Insert("INSERT INTO xnet_mlops_mep_model_deployment (uid, name, model_source, model_uid, model_name, model_version, " +
            "node_uid, node_name, status, container_id, container_name, image_name, port, endpoint, replicas, " +
            "resource_config, nginx_config, health_check, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{modelSource}, #{modelUid}, #{modelName}, #{modelVersion}, " +
            "#{nodeUid}, #{nodeName}, #{status}, #{containerId}, #{containerName}, #{imageName}, #{port}, #{endpoint}, #{replicas}, " +
            "#{resourceConfig}, #{nginxConfig}, #{healthCheck}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelDeployment deployment);

    @Update("UPDATE xnet_mlops_mep_model_deployment SET name = #{name}, replicas = #{replicas}, " +
            "resource_config = #{resourceConfig}, nginx_config = #{nginxConfig}, health_check = #{healthCheck}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int update(ModelDeployment deployment);

    @Update("UPDATE xnet_mlops_mep_model_deployment SET status = #{status}, container_id = #{containerId}, " +
            "endpoint = #{endpoint}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("containerId") String containerId, @Param("endpoint") String endpoint);

    @Update("UPDATE xnet_mlops_mep_model_deployment SET replicas = #{replicas}, updated_at = NOW() WHERE id = #{id}")
    int updateReplicas(@Param("id") Long id, @Param("replicas") Integer replicas);

    @Delete("DELETE FROM xnet_mlops_mep_model_deployment WHERE id = #{id}")
    int deleteById(Long id);
}
