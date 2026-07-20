package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import org.apache.ibatis.annotations.*;

import java.util.*;

@Mapper
public interface DatasetMapper {

    @Results(id = "datasetResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "userId", column = "userId"),
            @Result(property = "dataset_file", column = "dataset_file"),
            @Result(property = "type", column = "type"),
            @Result(property = "type_label", column = "type_label"),
            @Result(property = "zone", column = "zone"),
            @Result(property = "zone_label", column = "zone_label"),
            @Result(property = "encryption", column = "encryption"),
            @Result(property = "subdata_area", column = "subdata_area"),
            @Result(property = "bucket_name", column = "bucket_name"),
            @Result(property = "bucket_identifier", column = "bucket_identifier"),
            @Result(property = "tenant_uid", column = "tenant_uid"),
            @Result(property = "dept_uid", column = "dept_uid"),
            @Result(property = "team_uid", column = "team_uid"),
            @Result(property = "team_name", column = "team_name"),
            @Result(property = "description", column = "description"),
            @Result(property = "level", column = "level"),
            @Result(property = "created_at", column = "created_at"),
            @Result(property = "updated_at", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_dataset")
    List<Dataset> findAll();

    @Insert("INSERT INTO xnet_mlops_dpp_dataset (" +
            "uid, dataset_file, type, type_label, zone, zone_label, encryption, subdata_area, " +
            "bucket_name, bucket_identifier, team_uid, team_name, tenant_uid, dept_uid, level, userId, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{dataset_file}, #{type}, #{type_label}, #{zone}, #{zone_label}, #{encryption}, #{subdata_area}, " +
            "#{bucket_name}, #{bucket_identifier}, #{team_uid}, #{team_name}, #{tenant_uid}, #{dept_uid}, #{level}, #{userId}, #{created_at}, #{updated_at}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDataset(Dataset dataset);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_dataset WHERE dataset_file = #{dataset_file}")
    int countByDatasetFile(String dataset_file);

    @ResultMap("datasetResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_dataset WHERE id = #{id}")
    Dataset findById(Long id);

    @Update("UPDATE xnet_mlops_dpp_dataset SET " +
            "dataset_file = #{dataset_file}, " +
            "type = #{type}, " +
            "type_label = #{type_label}, " +
            "zone = #{zone}, " +
            "zone_label = #{zone_label}, " +
            "encryption = #{encryption}, " +
            "subdata_area = #{subdata_area}, " +
            "bucket_name = #{bucket_name}, " +
            "bucket_identifier = #{bucket_identifier}, " +
            "team_uid = #{team_uid}, " +
            "team_name = #{team_name}, " +
            "tenant_uid = #{tenant_uid}, " +
            "dept_uid = #{dept_uid}, " +
            "level = #{level}, " +
            "userId = #{userId}, " +
            "updated_at = #{updated_at} " +
            "WHERE id = #{id}")
    int updateDataset(Dataset dataset);

    @Delete("DELETE FROM xnet_mlops_dpp_dataset WHERE id = #{id}")
    int deleteById(Long id);

    @ResultMap("datasetResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_dataset WHERE id = #{id}")
    Dataset selectById(Long id);
}
