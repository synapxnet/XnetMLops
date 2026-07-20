package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.KBChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识库分块 Mapper
 */
@Mapper
public interface KBChunkMapper {

    @Results(id = "KBChunkResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "uid", column = "uid"),
        @Result(property = "doc_id", column = "doc_id"),
        @Result(property = "kb_id", column = "kb_id"),
        @Result(property = "content", column = "content"),
        @Result(property = "content_hash", column = "content_hash"),
        @Result(property = "position", column = "position"),
        @Result(property = "start_index", column = "start_index"),
        @Result(property = "end_index", column = "end_index"),
        @Result(property = "page_number", column = "page_number"),
        @Result(property = "embedding_id", column = "embedding_id"),
        @Result(property = "token_count", column = "token_count"),
        @Result(property = "char_count", column = "char_count"),
        @Result(property = "parent_chunk_id", column = "parent_chunk_id"),
        @Result(property = "chunk_level", column = "chunk_level"),
        @Result(property = "metadata", column = "metadata"),
        @Result(property = "keywords", column = "keywords"),
        @Result(property = "summary", column = "summary"),
        @Result(property = "created_at", column = "created_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_kb_chunk WHERE id = #{id}")
    KBChunk findById(@Param("id") Long id);

    @ResultMap("KBChunkResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_kb_chunk WHERE doc_id = #{doc_id} ORDER BY position ASC")
    List<KBChunk> findByDocId(@Param("doc_id") Long doc_id);

    @ResultMap("KBChunkResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_kb_chunk WHERE kb_id = #{kb_id} ORDER BY doc_id, position ASC")
    List<KBChunk> findByKbId(@Param("kb_id") Long kb_id);

    @Insert("INSERT INTO xnet_mlops_dpp_kb_chunk (" +
            "uid, doc_id, kb_id, content, content_hash, " +
            "position, start_index, end_index, page_number, " +
            "embedding_id, token_count, char_count, " +
            "parent_chunk_id, chunk_level, " +
            "metadata, keywords, summary, created_at" +
            ") VALUES (" +
            "#{uid}, #{doc_id}, #{kb_id}, #{content}, #{content_hash}, " +
            "#{position}, #{start_index}, #{end_index}, #{page_number}, " +
            "#{embedding_id}, #{token_count}, #{char_count}, " +
            "#{parent_chunk_id}, #{chunk_level}, " +
            "#{metadata}, #{keywords}, #{summary}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KBChunk chunk);

    @Insert("<script>" +
            "INSERT INTO xnet_mlops_dpp_kb_chunk (" +
            "uid, doc_id, kb_id, content, content_hash, " +
            "position, start_index, end_index, page_number, " +
            "embedding_id, token_count, char_count, " +
            "parent_chunk_id, chunk_level, " +
            "metadata, keywords, summary, created_at" +
            ") VALUES " +
            "<foreach collection='chunks' item='c' separator=','>" +
            "(#{c.uid}, #{c.doc_id}, #{c.kb_id}, #{c.content}, #{c.content_hash}, " +
            "#{c.position}, #{c.start_index}, #{c.end_index}, #{c.page_number}, " +
            "#{c.embedding_id}, #{c.token_count}, #{c.char_count}, " +
            "#{c.parent_chunk_id}, #{c.chunk_level}, " +
            "#{c.metadata}, #{c.keywords}, #{c.summary}, NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("chunks") List<KBChunk> chunks);

    @Delete("DELETE FROM xnet_mlops_dpp_kb_chunk WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM xnet_mlops_dpp_kb_chunk WHERE doc_id = #{doc_id}")
    int deleteByDocId(@Param("doc_id") Long doc_id);

    @Delete("DELETE FROM xnet_mlops_dpp_kb_chunk WHERE kb_id = #{kb_id}")
    int deleteByKbId(@Param("kb_id") Long kb_id);

    @Update("UPDATE xnet_mlops_dpp_kb_chunk SET embedding_id = #{embedding_id} WHERE id = #{id}")
    int updateEmbeddingId(@Param("id") Long id, @Param("embedding_id") String embedding_id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_kb_chunk WHERE kb_id = #{kb_id}")
    int countByKbId(@Param("kb_id") Long kb_id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_kb_chunk WHERE doc_id = #{doc_id}")
    int countByDocId(@Param("doc_id") Long doc_id);

    @Select("SELECT COALESCE(SUM(token_count), 0) FROM xnet_mlops_dpp_kb_chunk WHERE kb_id = #{kb_id}")
    Long sumTokensByKbId(@Param("kb_id") Long kb_id);
}
