package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.KnowledgeBase;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper {

    @Results(id = "KnowledgeBaseResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "uid", column = "uid"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "icon", column = "icon"),
        @Result(property = "embedding_model_id", column = "embedding_model_id"),
        @Result(property = "embedding_provider", column = "embedding_provider"),
        @Result(property = "embedding_model", column = "embedding_model"),
        @Result(property = "embedding_dimension", column = "embedding_dimension"),
        @Result(property = "vector_db_type", column = "vector_db_type"),
        @Result(property = "vector_collection", column = "vector_collection"),
        @Result(property = "vector_index_type", column = "vector_index_type"),
        @Result(property = "chunk_strategy", column = "chunk_strategy"),
        @Result(property = "chunk_size", column = "chunk_size"),
        @Result(property = "chunk_overlap", column = "chunk_overlap"),
        @Result(property = "chunk_separator", column = "chunk_separator"),
        @Result(property = "retrieval_method", column = "retrieval_method"),
        @Result(property = "top_k", column = "top_k"),
        @Result(property = "score_threshold", column = "score_threshold"),
        @Result(property = "rerank_enabled", column = "rerank_enabled"),
        @Result(property = "rerank_model", column = "rerank_model"),
        @Result(property = "rerank_top_k", column = "rerank_top_k"),
        @Result(property = "status", column = "status"),
        @Result(property = "doc_count", column = "doc_count"),
        @Result(property = "chunk_count", column = "chunk_count"),
        @Result(property = "total_tokens", column = "total_tokens"),
        @Result(property = "total_size_bytes", column = "total_size_bytes"),
        @Result(property = "tenant_uid", column = "tenant_uid"),
        @Result(property = "creator_id", column = "creator_id"),
        @Result(property = "visibility", column = "visibility"),
        @Result(property = "created_at", column = "created_at"),
        @Result(property = "updated_at", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_knowledge_base WHERE id = #{id}")
    KnowledgeBase findById(@Param("id") Long id);

    @ResultMap("KnowledgeBaseResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_knowledge_base WHERE uid = #{uid}")
    KnowledgeBase findByUid(@Param("uid") String uid);

    @ResultMap("KnowledgeBaseResultMap")
    @Select("<script>" +
            "SELECT * FROM xnet_mlops_dpp_knowledge_base WHERE 1=1 " +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='tenant_uid != null'> AND tenant_uid = #{tenant_uid}</if>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<KnowledgeBase> findAll(@Param("status") String status, @Param("tenant_uid") String tenant_uid);

    @Insert("INSERT INTO xnet_mlops_dpp_knowledge_base (" +
            "uid, name, description, icon, " +
            "embedding_model_id, embedding_provider, embedding_model, embedding_dimension, " +
            "vector_db_type, vector_collection, vector_index_type, " +
            "chunk_strategy, chunk_size, chunk_overlap, chunk_separator, " +
            "retrieval_method, top_k, score_threshold, rerank_enabled, rerank_model, rerank_top_k, " +
            "status, doc_count, chunk_count, total_tokens, total_size_bytes, " +
            "tenant_uid, creator_id, visibility, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{name}, #{description}, #{icon}, " +
            "#{embedding_model_id}, #{embedding_provider}, #{embedding_model}, #{embedding_dimension}, " +
            "#{vector_db_type}, #{vector_collection}, #{vector_index_type}, " +
            "#{chunk_strategy}, #{chunk_size}, #{chunk_overlap}, #{chunk_separator}, " +
            "#{retrieval_method}, #{top_k}, #{score_threshold}, #{rerank_enabled}, #{rerank_model}, #{rerank_top_k}, " +
            "#{status}, #{doc_count}, #{chunk_count}, #{total_tokens}, #{total_size_bytes}, " +
            "#{tenant_uid}, #{creator_id}, #{visibility}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeBase kb);

    @Update("<script>" +
            "UPDATE xnet_mlops_dpp_knowledge_base SET updated_at = NOW()" +
            "<if test='name != null'>, name = #{name}</if>" +
            "<if test='description != null'>, description = #{description}</if>" +
            "<if test='icon != null'>, icon = #{icon}</if>" +
            "<if test='chunk_strategy != null'>, chunk_strategy = #{chunk_strategy}</if>" +
            "<if test='chunk_size != null'>, chunk_size = #{chunk_size}</if>" +
            "<if test='chunk_overlap != null'>, chunk_overlap = #{chunk_overlap}</if>" +
            "<if test='retrieval_method != null'>, retrieval_method = #{retrieval_method}</if>" +
            "<if test='top_k != null'>, top_k = #{top_k}</if>" +
            "<if test='score_threshold != null'>, score_threshold = #{score_threshold}</if>" +
            "<if test='rerank_enabled != null'>, rerank_enabled = #{rerank_enabled}</if>" +
            "<if test='rerank_model != null'>, rerank_model = #{rerank_model}</if>" +
            "<if test='status != null'>, status = #{status}</if>" +
            "<if test='visibility != null'>, visibility = #{visibility}</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int update(KnowledgeBase kb);

    @Delete("DELETE FROM xnet_mlops_dpp_knowledge_base WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE xnet_mlops_dpp_knowledge_base SET " +
            "doc_count = #{doc_count}, chunk_count = #{chunk_count}, " +
            "total_tokens = #{total_tokens}, total_size_bytes = #{total_size_bytes}, " +
            "updated_at = NOW() WHERE id = #{id}")
    int updateStats(@Param("id") Long id,
                    @Param("doc_count") Integer doc_count,
                    @Param("chunk_count") Integer chunk_count,
                    @Param("total_tokens") Long total_tokens,
                    @Param("total_size_bytes") Long total_size_bytes);

    @Update("UPDATE xnet_mlops_dpp_knowledge_base SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
