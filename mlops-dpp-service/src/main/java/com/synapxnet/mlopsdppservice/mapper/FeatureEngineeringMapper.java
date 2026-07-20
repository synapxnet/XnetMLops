package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.FeatureEngineering;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FeatureEngineeringMapper {

    @Results(id = "featureEngineeringResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "name", column = "name"),
            @Result(property = "description", column = "description"),
            @Result(property = "datasourceId", column = "datasource_id"),
            @Result(property = "datasourceName", column = "datasource_name"),
            @Result(property = "database", column = "database"),
            @Result(property = "tableName", column = "table_name"),
            @Result(property = "selectedColumns", column = "selected_columns"),
            @Result(property = "transformConfig", column = "transform_config"),
            @Result(property = "zone", column = "zone"),
            @Result(property = "bucketUid", column = "bucket_uid"),
            @Result(property = "bucketName", column = "bucket_name"),
            @Result(property = "subDataArea", column = "sub_data_area"),
            @Result(property = "dataType", column = "data_type"),
            @Result(property = "encryption", column = "encryption"),
            @Result(property = "pushToDataset", column = "push_to_dataset"),
            @Result(property = "targetDatasetId", column = "target_dataset_id"),
            @Result(property = "targetDatasetName", column = "target_dataset_name"),
            @Result(property = "outputPath", column = "output_path"),
            @Result(property = "operatorId", column = "operator_id"),
            @Result(property = "operatorCode", column = "operator_code"),
            @Result(property = "operatorName", column = "operator_name"),
            @Result(property = "outputFormat", column = "output_format"),
            @Result(property = "featureConfig", column = "feature_config"),
            @Result(property = "operatorParams", column = "operator_params"),
            @Result(property = "imageUid", column = "image_uid"),
            @Result(property = "imageName", column = "image_name"),
            @Result(property = "imageTag", column = "image_tag"),
            @Result(property = "harborUrl", column = "harbor_url"),
            @Result(property = "harborCredentialsId", column = "harbor_credentials_id"),
            @Result(property = "scheduleConfig", column = "schedule_config"),
            @Result(property = "scheduleActive", column = "schedule_active"),
            @Result(property = "notificationConfig", column = "notification_config"),
            @Result(property = "notificationEnabled", column = "notification_enabled"),
            @Result(property = "jobUid", column = "job_uid"),
            @Result(property = "lastBuildStatus", column = "last_build_status"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "teamUid", column = "team_uid"),
            @Result(property = "teamName", column = "team_name"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_feature_engineering ORDER BY created_at DESC")
    List<FeatureEngineering> findAll();

    @ResultMap("featureEngineeringResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_engineering WHERE id = #{id}")
    FeatureEngineering findById(Long id);

    @ResultMap("featureEngineeringResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_engineering WHERE uid = #{uid}")
    FeatureEngineering findByUid(String uid);

    @ResultMap("featureEngineeringResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_engineering WHERE team_uid = #{teamUid} ORDER BY created_at DESC")
    List<FeatureEngineering> findByTeamUid(String teamUid);

    @Insert("INSERT INTO xnet_mlops_dpp_feature_engineering (" +
            "uid, name, description, datasource_id, datasource_name, `database`, table_name, " +
            "selected_columns, transform_config, zone, bucket_uid, bucket_name, " +
            "sub_data_area, data_type, encryption, push_to_dataset, target_dataset_id, target_dataset_name, output_path, " +
            "operator_id, operator_code, operator_name, output_format, feature_config, operator_params, " +
            "image_uid, image_name, image_tag, harbor_url, harbor_credentials_id, " +
            "schedule_config, schedule_active, notification_config, notification_enabled, " +
            "job_uid, last_build_status, " +
            "status, created_by, team_uid, team_name, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{name}, #{description}, #{datasourceId}, #{datasourceName}, #{database}, #{tableName}, " +
            "#{selectedColumns}, #{transformConfig}, #{zone}, #{bucketUid}, #{bucketName}, " +
            "#{subDataArea}, #{dataType}, #{encryption}, #{pushToDataset}, #{targetDatasetId}, #{targetDatasetName}, #{outputPath}, " +
            "#{operatorId}, #{operatorCode}, #{operatorName}, #{outputFormat}, #{featureConfig}, #{operatorParams}, " +
            "#{imageUid}, #{imageName}, #{imageTag}, #{harborUrl}, #{harborCredentialsId}, " +
            "#{scheduleConfig}, #{scheduleActive}, #{notificationConfig}, #{notificationEnabled}, " +
            "#{jobUid}, #{lastBuildStatus}, " +
            "#{status}, #{createdBy}, #{teamUid}, #{teamName}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeatureEngineering featureEngineering);

    @Update("UPDATE xnet_mlops_dpp_feature_engineering SET " +
            "name = #{name}, " +
            "description = #{description}, " +
            "datasource_id = #{datasourceId}, " +
            "datasource_name = #{datasourceName}, " +
            "`database` = #{database}, " +
            "table_name = #{tableName}, " +
            "selected_columns = #{selectedColumns}, " +
            "transform_config = #{transformConfig}, " +
            "zone = #{zone}, " +
            "bucket_uid = #{bucketUid}, " +
            "bucket_name = #{bucketName}, " +
            "sub_data_area = #{subDataArea}, " +
            "data_type = #{dataType}, " +
            "encryption = #{encryption}, " +
            "push_to_dataset = #{pushToDataset}, " +
            "target_dataset_id = #{targetDatasetId}, " +
            "target_dataset_name = #{targetDatasetName}, " +
            "output_path = #{outputPath}, " +
            "operator_id = #{operatorId}, " +
            "operator_code = #{operatorCode}, " +
            "operator_name = #{operatorName}, " +
            "output_format = #{outputFormat}, " +
            "feature_config = #{featureConfig}, " +
            "operator_params = #{operatorParams}, " +
            "image_uid = #{imageUid}, " +
            "image_name = #{imageName}, " +
            "image_tag = #{imageTag}, " +
            "harbor_url = #{harborUrl}, " +
            "harbor_credentials_id = #{harborCredentialsId}, " +
            "schedule_config = #{scheduleConfig}, " +
            "schedule_active = #{scheduleActive}, " +
            "notification_config = #{notificationConfig}, " +
            "notification_enabled = #{notificationEnabled}, " +
            "job_uid = #{jobUid}, " +
            "last_build_status = #{lastBuildStatus}, " +
            "status = #{status}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(FeatureEngineering featureEngineering);

    @Update("UPDATE xnet_mlops_dpp_feature_engineering SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_mlops_dpp_feature_engineering WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_feature_engineering WHERE name = #{name}")
    int countByName(String name);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_feature_engineering WHERE name = #{name} AND id != #{id}")
    int countByNameExcludeId(@Param("name") String name, @Param("id") Long id);
}
