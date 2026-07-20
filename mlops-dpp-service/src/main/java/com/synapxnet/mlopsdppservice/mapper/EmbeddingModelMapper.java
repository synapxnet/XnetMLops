package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.EmbeddingModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 嵌入模型 Mapper
 */
@Mapper
public interface EmbeddingModelMapper {

    @Results(id = "EmbeddingModelResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "provider", column = "provider"),
        @Result(property = "model_name", column = "model_name"),
        @Result(property = "dimension", column = "dimension"),
        @Result(property = "max_tokens", column = "max_tokens"),
        @Result(property = "batch_size", column = "batch_size"),
        @Result(property = "api_endpoint", column = "api_endpoint"),
        @Result(property = "api_key_encrypted", column = "api_key_encrypted"),
        @Result(property = "extra_config", column = "extra_config"),
        @Result(property = "is_default", column = "is_default"),
        @Result(property = "status", column = "status"),
        @Result(property = "tenant_uid", column = "tenant_uid"),
        @Result(property = "created_at", column = "created_at"),
        @Result(property = "updated_at", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_embedding_model WHERE id = #{id}")
    EmbeddingModel findById(@Param("id") Long id);

    @ResultMap("EmbeddingModelResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_embedding_model WHERE status = 'active' ORDER BY is_default DESC, id ASC")
    List<EmbeddingModel> findAllActive();

    @ResultMap("EmbeddingModelResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_embedding_model WHERE is_default = 1 AND status = 'active' LIMIT 1")
    EmbeddingModel findDefault();

    @Insert("INSERT INTO xnet_mlops_dpp_embedding_model (" +
            "name, provider, model_name, dimension, max_tokens, batch_size, " +
            "api_endpoint, extra_config, is_default, status, tenant_uid, created_at, updated_at" +
            ") VALUES (" +
            "#{name}, #{provider}, #{model_name}, #{dimension}, #{max_tokens}, #{batch_size}, " +
            "#{api_endpoint}, #{extra_config}, #{is_default}, #{status}, #{tenant_uid}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EmbeddingModel model);

    @Update("<script>" +
            "UPDATE xnet_mlops_dpp_embedding_model SET updated_at = NOW()" +
            "<if test='name != null'>, name = #{name}</if>" +
            "<if test='dimension != null'>, dimension = #{dimension}</if>" +
            "<if test='max_tokens != null'>, max_tokens = #{max_tokens}</if>" +
            "<if test='batch_size != null'>, batch_size = #{batch_size}</if>" +
            "<if test='api_endpoint != null'>, api_endpoint = #{api_endpoint}</if>" +
            "<if test='extra_config != null'>, extra_config = #{extra_config}</if>" +
            "<if test='is_default != null'>, is_default = #{is_default}</if>" +
            "<if test='status != null'>, status = #{status}</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int update(EmbeddingModel model);

    @Delete("DELETE FROM xnet_mlops_dpp_embedding_model WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE xnet_mlops_dpp_embedding_model SET is_default = 0 WHERE is_default = 1")
    int clearDefault();

    @Update("UPDATE xnet_mlops_dpp_embedding_model SET is_default = 1 WHERE id = #{id}")
    int setDefault(@Param("id") Long id);
}
