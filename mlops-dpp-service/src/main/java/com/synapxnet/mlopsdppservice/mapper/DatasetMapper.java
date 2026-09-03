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
            @Result(property = "sourcePlatform", column = "source_platform"),
            @Result(property = "sourceProductName", column = "source_product_name"),
            @Result(property = "sourceProductVersion", column = "source_product_version"),
            @Result(property = "sourceUri", column = "source_uri"),
            @Result(property = "rowCount", column = "row_count"),
            @Result(property = "byteSize", column = "byte_size"),
            @Result(property = "schemaDigestSha256", column = "schema_digest_sha256"),
            @Result(property = "artifactDigestSha256", column = "artifact_digest_sha256"),
            @Result(property = "lineageReference", column = "lineage_reference"),
            @Result(property = "importStatus", column = "import_status"),
            @Result(property = "importedAt", column = "imported_at"),
            @Result(property = "level", column = "level"),
            @Result(property = "created_at", column = "created_at"),
            @Result(property = "updated_at", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_dataset")
    List<Dataset> findAll();

    @Insert("INSERT INTO xnet_mlops_dpp_dataset (" +
            "uid, dataset_file, type, type_label, zone, zone_label, encryption, subdata_area, " +
            "bucket_name, bucket_identifier, team_uid, team_name, tenant_uid, dept_uid, level, userId, description, " +
            "source_platform, source_product_name, source_product_version, source_uri, row_count, byte_size, " +
            "schema_digest_sha256, artifact_digest_sha256, lineage_reference, import_status, imported_at, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{dataset_file}, #{type}, #{type_label}, #{zone}, #{zone_label}, #{encryption}, #{subdata_area}, " +
            "#{bucket_name}, #{bucket_identifier}, #{team_uid}, #{team_name}, #{tenant_uid}, #{dept_uid}, #{level}, #{userId}, #{description}, " +
            "#{sourcePlatform}, #{sourceProductName}, #{sourceProductVersion}, #{sourceUri}, #{rowCount}, #{byteSize}, " +
            "#{schemaDigestSha256}, #{artifactDigestSha256}, #{lineageReference}, #{importStatus}, #{importedAt}, #{created_at}, #{updated_at}" +
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
            "description = #{description}, " +
            "source_platform = #{sourcePlatform}, " +
            "source_product_name = #{sourceProductName}, " +
            "source_product_version = #{sourceProductVersion}, " +
            "source_uri = #{sourceUri}, " +
            "row_count = #{rowCount}, " +
            "byte_size = #{byteSize}, " +
            "schema_digest_sha256 = #{schemaDigestSha256}, " +
            "artifact_digest_sha256 = #{artifactDigestSha256}, " +
            "lineage_reference = #{lineageReference}, " +
            "import_status = #{importStatus}, " +
            "imported_at = #{importedAt}, " +
            "updated_at = #{updated_at} " +
            "WHERE id = #{id}")
    int updateDataset(Dataset dataset);

    @Delete("DELETE FROM xnet_mlops_dpp_dataset WHERE id = #{id}")
    int deleteById(Long id);

    @ResultMap("datasetResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_dataset WHERE id = #{id}")
    Dataset selectById(Long id);

    @ResultMap("datasetResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_dataset WHERE source_platform = #{sourcePlatform} " +
            "AND source_product_version = #{sourceProductVersion} LIMIT 1")
    Dataset findBySourceProductVersion(
            @Param("sourcePlatform") String sourcePlatform,
            @Param("sourceProductVersion") String sourceProductVersion
    );
}
