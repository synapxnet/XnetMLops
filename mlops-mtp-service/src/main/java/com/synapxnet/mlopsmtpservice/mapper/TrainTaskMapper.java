package com.synapxnet.mlopsmtpservice.mapper;

import com.synapxnet.mlopsmtpservice.entity.DockerFile;
import com.synapxnet.mlopsmtpservice.entity.HarborRepository;
import com.synapxnet.mlopsmtpservice.entity.TrainTask;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface TrainTaskMapper {

    @Insert({
            "INSERT INTO XnetMLops.xnet_mlops_mtp_train_task (",
            "uid, userId, tenant_uid, task_name, task_type, encryption, task_zone,",
            "pod_type, resources, train_type, image_uid, image, description, algorithm_uid, algorithm_name,",
            "algorithm_version, task_route, train_config_content, train_config_format,",
            "notification_config, output_config, schedule_config, created_at",
            ") VALUES (",
            "#{uid}, #{userId}, #{tenant_uid}, #{task_name}, #{task_type}, #{encryption},",
            "#{task_zone}, #{pod_type}, #{resources}, #{train_type}, #{image_uid}, #{image}, #{description},",
            "#{algorithm_uid}, #{algorithm_name}, #{algorithm_version}, #{task_route}, #{train_config_content},",
            "#{train_config_format}, #{notification_config}, #{output_config}, #{schedule_config}, NOW()",
            ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTrainTask(TrainTask trainTask);

    // 添加检查 task_uid 唯一性的方法
    @Select("SELECT COUNT(*) FROM XnetMLops.xnet_mlops_mtp_train_task WHERE uid = #{uid}")
    int countByUid(@Param("uid") String uid);

    @Update({
            "UPDATE XnetMLops.xnet_mlops_mtp_train_task SET",
            "task_name = #{task_name},",
            "task_type = #{task_type},",
            "encryption = #{encryption},",
            "task_zone = #{task_zone},",
            "pod_type = #{pod_type},",
            "resources = #{resources},",
            "train_type = #{train_type},",
            "image_uid = #{image_uid},",
            "image = #{image},",
            "description = #{description},",
            "algorithm_uid = #{algorithm_uid},",
            "algorithm_name = #{algorithm_name},",
            "algorithm_version = #{algorithm_version},",
            "task_route = #{task_route},",
            "train_config_content = #{train_config_content},",
            "train_config_format = #{train_config_format},",
            "notification_config = #{notification_config},",
            "output_config = #{output_config},",
            "schedule_config = #{schedule_config},",
            "updated_at = NOW()",
            "WHERE uid = #{uid} AND tenant_uid = #{tenant_uid}"
    })
    int updateTrainTask(TrainTask trainTask);

    @Select("SELECT * FROM XnetMLops.xnet_mlops_mtp_train_task WHERE uid = #{uid} AND tenant_uid = #{tenant_uid}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "userId", column = "userId"),
            @Result(property = "tenant_uid", column = "tenant_uid"),
            @Result(property = "datasets", column = "uid",
                    many = @Many(select = "com.synapxnet.mlopsmtpservice.mapper.TaskDatasetMapper.findByTaskUid")),
            @Result(property = "custom_variables", column = "uid",
                    many = @Many(select = "com.synapxnet.mlopsmtpservice.mapper.TaskCustomVariableMapper.findByTaskUid"))
    })
    Optional<TrainTask> findByUidAndTenantUid(@Param("uid") String uid, @Param("tenant_uid") String tenantUid);

    @Select("SELECT * FROM XnetMLops.xnet_mlops_mtp_train_task WHERE tenant_uid = #{tenant_uid}")
    List<TrainTask> findAllByTenantUid(String tenant_uid);

    @Delete("DELETE FROM XnetMLops.xnet_mlops_mtp_train_task WHERE uid = #{uid} AND tenant_uid = #{tenant_uid}")
    int deleteByUidAndTenantUid(@Param("uid") String uid, @Param("tenant_uid") String tenantUid);

    @Select("SELECT COUNT(*) FROM XnetMLops.xnet_mlops_mtp_train_task WHERE task_name = #{task_name} AND tenant_uid = #{tenant_uid}")
    int countByTaskName(@Param("task_name") String taskName, @Param("tenant_uid") String tenantUid);

    @Select("SELECT * FROM xnet_mlops_smp_docker_file WHERE uid = #{uid}")
    Optional<DockerFile> findByImageUid(String uid);

    @Select("SELECT * FROM xnet_mlops_smp_harbor_repository WHERE uid = #{uid}")
    Optional<HarborRepository> findByHarborUid(String uid);
}