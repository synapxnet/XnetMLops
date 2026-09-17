package com.synapxnet.mlopsmepservice.mapper;

import com.synapxnet.mlopsmepservice.entity.DeployNode;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeployNodeMapper {

    /** 查询全部部署节点。 */
    @Select("SELECT * FROM xnet_mlops_mep_deploy_node ORDER BY created_at DESC")
    List<DeployNode> findAll();

    /** 按主键查询部署节点。 */
    @Select("SELECT * FROM xnet_mlops_mep_deploy_node WHERE id = #{id}")
    DeployNode findById(Long id);

    /** 按稳定 UID 查询部署节点。 */
    @Select("SELECT * FROM xnet_mlops_mep_deploy_node WHERE uid = #{uid}")
    DeployNode findByUid(String uid);

    /** 查询当前标记为在线的部署节点。 */
    @Select("SELECT * FROM xnet_mlops_mep_deploy_node WHERE status = 'online'")
    List<DeployNode> findOnlineNodes();

    /** 新增部署节点记录。 */
    @Insert("INSERT INTO xnet_mlops_mep_deploy_node (uid, name, ip_address, port, status, cpu_cores, memory_gb, gpu_info, docker_version, nginx_status, labels, description, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{ipAddress}, #{port}, #{status}, #{cpuCores}, #{memoryGb}, #{gpuInfo}, #{dockerVersion}, #{nginxStatus}, #{labels}, #{description}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DeployNode node);

    /** 更新用户可编辑的部署节点信息。 */
    @Update("UPDATE xnet_mlops_mep_deploy_node SET name = #{name}, ip_address = #{ipAddress}, port = #{port}, " +
            "cpu_cores = #{cpuCores}, memory_gb = #{memoryGb}, gpu_info = #{gpuInfo}, labels = #{labels}, " +
            "description = #{description}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(DeployNode node);

    /** 更新经过运行时探针核验的节点状态与组件版本。 */
    @Update("UPDATE xnet_mlops_mep_deploy_node SET status = #{status}, docker_version = #{dockerVersion}, nginx_status = #{nginxStatus}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("dockerVersion") String dockerVersion, @Param("nginxStatus") String nginxStatus);

    /** 仅更新 TCP 连通状态，不覆盖最近一次真实运行时核验结果。 */
    @Update("UPDATE xnet_mlops_mep_deploy_node SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateConnectivityStatus(@Param("id") Long id, @Param("status") String status);

    /** 删除指定部署节点。 */
    @Delete("DELETE FROM xnet_mlops_mep_deploy_node WHERE id = #{id}")
    int deleteById(Long id);
}
