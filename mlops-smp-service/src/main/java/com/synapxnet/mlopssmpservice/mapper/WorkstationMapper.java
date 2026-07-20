package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.Workstation;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工作站点 Mapper
 */
@Mapper
public interface WorkstationMapper {

    // ==================== 查询操作 ====================

    @Select("SELECT * FROM xnet_mlops_smp_workstation ORDER BY created_at DESC")
    @Results(id = "workstationResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "uid", column = "uid"),
        @Result(property = "name", column = "name"),
        @Result(property = "hostname", column = "hostname"),
        @Result(property = "hostnameMode", column = "hostname_mode"),
        @Result(property = "vendor", column = "vendor"),
        @Result(property = "serverType", column = "server_type"),
        @Result(property = "region", column = "region"),
        @Result(property = "osType", column = "os_type"),
        @Result(property = "osVersion", column = "os_version"),
        @Result(property = "cpuCores", column = "cpu_cores"),
        @Result(property = "ramGb", column = "ram_gb"),
        @Result(property = "diskGb", column = "disk_gb"),
        @Result(property = "hasGpu", column = "has_gpu"),
        @Result(property = "gpuCount", column = "gpu_count"),
        @Result(property = "gpuType", column = "gpu_type"),
        @Result(property = "gpuModel", column = "gpu_model"),
        @Result(property = "gpuMemory", column = "gpu_memory"),
        @Result(property = "availableDiskGb", column = "available_disk_gb"),
        @Result(property = "availableRamGb", column = "available_ram_gb"),
        @Result(property = "domain", column = "domain"),
        @Result(property = "ipAddress", column = "ip_address"),
        @Result(property = "sshPort", column = "ssh_port"),
        @Result(property = "sshUser", column = "ssh_user"),
        @Result(property = "authType", column = "auth_type"),
        @Result(property = "encryptedPassword", column = "encrypted_password"),
        @Result(property = "encryptedPrivateKey", column = "encrypted_private_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "lastHeartbeat", column = "last_heartbeat"),
        @Result(property = "lastCheckResult", column = "last_check_result"),
        @Result(property = "description", column = "description"),
        @Result(property = "createdBy", column = "created_by"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<Workstation> findAll();

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE id = #{id}")
    @ResultMap("workstationResultMap")
    Optional<Workstation> findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE uid = #{uid}")
    @ResultMap("workstationResultMap")
    Optional<Workstation> findByUid(@Param("uid") String uid);

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE name = #{name}")
    @ResultMap("workstationResultMap")
    Optional<Workstation> findByName(@Param("name") String name);

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE ip_address = #{ipAddress}")
    @ResultMap("workstationResultMap")
    Optional<Workstation> findByIpAddress(@Param("ipAddress") String ipAddress);

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE status = #{status} ORDER BY created_at DESC")
    @ResultMap("workstationResultMap")
    List<Workstation> findByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_workstation WHERE name = #{name}")
    int countByName(@Param("name") String name);

    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_workstation WHERE hostname = #{hostname}")
    int countByHostname(@Param("hostname") String hostname);

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE hostname = #{hostname}")
    @ResultMap("workstationResultMap")
    Optional<Workstation> findByHostname(@Param("hostname") String hostname);

    @Select("SELECT * FROM xnet_mlops_smp_workstation WHERE status = 'online' ORDER BY name")
    @ResultMap("workstationResultMap")
    List<Workstation> findOnlineWorkstations();

    // ==================== 插入操作 ====================

    @Insert("INSERT INTO xnet_mlops_smp_workstation (uid, name, hostname, hostname_mode, vendor, server_type, region, " +
            "os_type, os_version, cpu_cores, ram_gb, disk_gb, has_gpu, gpu_count, gpu_type, gpu_model, gpu_memory, " +
            "available_disk_gb, available_ram_gb, domain, ip_address, ssh_port, ssh_user, auth_type, " +
            "encrypted_password, encrypted_private_key, status, description, created_by, created_at, updated_at) " +
            "VALUES (#{uid}, #{name}, #{hostname}, #{hostnameMode}, #{vendor}, #{serverType}, #{region}, " +
            "#{osType}, #{osVersion}, #{cpuCores}, #{ramGb}, #{diskGb}, #{hasGpu}, #{gpuCount}, #{gpuType}, #{gpuModel}, #{gpuMemory}, " +
            "#{availableDiskGb}, #{availableRamGb}, #{domain}, #{ipAddress}, #{sshPort}, #{sshUser}, #{authType}, " +
            "#{encryptedPassword}, #{encryptedPrivateKey}, #{status}, #{description}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Workstation workstation);

    // ==================== 更新操作 ====================

    @Update("UPDATE xnet_mlops_smp_workstation SET " +
            "name = #{name}, hostname = #{hostname}, hostname_mode = #{hostnameMode}, " +
            "vendor = #{vendor}, server_type = #{serverType}, region = #{region}, " +
            "os_type = #{osType}, os_version = #{osVersion}, cpu_cores = #{cpuCores}, ram_gb = #{ramGb}, " +
            "disk_gb = #{diskGb}, has_gpu = #{hasGpu}, gpu_count = #{gpuCount}, gpu_type = #{gpuType}, " +
            "gpu_model = #{gpuModel}, gpu_memory = #{gpuMemory}, domain = #{domain}, ip_address = #{ipAddress}, " +
            "ssh_port = #{sshPort}, ssh_user = #{sshUser}, auth_type = #{authType}, " +
            "encrypted_password = #{encryptedPassword}, encrypted_private_key = #{encryptedPrivateKey}, " +
            "description = #{description}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(Workstation workstation);

    @Update("UPDATE xnet_mlops_smp_workstation SET status = #{status}, last_heartbeat = #{lastHeartbeat}, " +
            "last_check_result = #{lastCheckResult}, available_disk_gb = #{availableDiskGb}, " +
            "available_ram_gb = #{availableRamGb}, updated_at = NOW() WHERE id = #{id}")
    int updateHeartbeat(@Param("id") Long id, @Param("status") String status,
                        @Param("lastHeartbeat") LocalDateTime lastHeartbeat,
                        @Param("lastCheckResult") String lastCheckResult,
                        @Param("availableDiskGb") Integer availableDiskGb,
                        @Param("availableRamGb") Integer availableRamGb);

    @Update("UPDATE xnet_mlops_smp_workstation SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // ==================== 删除操作 ====================

    @Delete("DELETE FROM xnet_mlops_smp_workstation WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM xnet_mlops_smp_workstation WHERE uid = #{uid}")
    int deleteByUid(@Param("uid") String uid);
}
