package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.KBDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识库文档 Mapper
 */
@Mapper
public interface KBDocumentMapper {

    @Results(id = "KBDocumentResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "uid", column = "uid"),
        @Result(property = "kb_id", column = "kb_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "original_name", column = "original_name"),
        @Result(property = "type", column = "type"),
        @Result(property = "mime_type", column = "mime_type"),
        @Result(property = "file_path", column = "file_path"),
        @Result(property = "file_size", column = "file_size"),
        @Result(property = "file_hash", column = "file_hash"),
        @Result(property = "status", column = "status"),
        @Result(property = "process_progress", column = "process_progress"),
        @Result(property = "error_message", column = "error_message"),
        @Result(property = "word_count", column = "word_count"),
        @Result(property = "char_count", column = "char_count"),
        @Result(property = "chunk_count", column = "chunk_count"),
        @Result(property = "token_count", column = "token_count"),
        @Result(property = "page_count", column = "page_count"),
        @Result(property = "metadata", column = "metadata"),
        @Result(property = "source_url", column = "source_url"),
        @Result(property = "source_type", column = "source_type"),
        @Result(property = "custom_chunk_size", column = "custom_chunk_size"),
        @Result(property = "custom_chunk_overlap", column = "custom_chunk_overlap"),
        @Result(property = "uploaded_by", column = "uploaded_by"),
        @Result(property = "created_at", column = "created_at"),
        @Result(property = "updated_at", column = "updated_at"),
        @Result(property = "indexed_at", column = "indexed_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_kb_document WHERE id = #{id}")
    KBDocument findById(@Param("id") Long id);

    @ResultMap("KBDocumentResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_kb_document WHERE uid = #{uid}")
    KBDocument findByUid(@Param("uid") String uid);

    @ResultMap("KBDocumentResultMap")
    @Select("<script>" +
            "SELECT * FROM xnet_mlops_dpp_kb_document WHERE kb_id = #{kb_id} " +
            "<if test='status != null'> AND status = #{status}</if>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<KBDocument> findByKbId(@Param("kb_id") Long kb_id, @Param("status") String status);

    @Insert("INSERT INTO xnet_mlops_dpp_kb_document (" +
            "uid, kb_id, name, original_name, type, mime_type, file_path, file_size, file_hash, " +
            "status, process_progress, error_message, " +
            "word_count, char_count, chunk_count, token_count, page_count, " +
            "metadata, source_url, source_type, " +
            "custom_chunk_size, custom_chunk_overlap, " +
            "uploaded_by, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{kb_id}, #{name}, #{original_name}, #{type}, #{mime_type}, #{file_path}, #{file_size}, #{file_hash}, " +
            "#{status}, #{process_progress}, #{error_message}, " +
            "#{word_count}, #{char_count}, #{chunk_count}, #{token_count}, #{page_count}, " +
            "#{metadata}, #{source_url}, #{source_type}, " +
            "#{custom_chunk_size}, #{custom_chunk_overlap}, " +
            "#{uploaded_by}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KBDocument doc);

    @Update("<script>" +
            "UPDATE xnet_mlops_dpp_kb_document SET updated_at = NOW()" +
            "<if test='status != null'>, status = #{status}</if>" +
            "<if test='process_progress != null'>, process_progress = #{process_progress}</if>" +
            "<if test='error_message != null'>, error_message = #{error_message}</if>" +
            "<if test='word_count != null'>, word_count = #{word_count}</if>" +
            "<if test='char_count != null'>, char_count = #{char_count}</if>" +
            "<if test='chunk_count != null'>, chunk_count = #{chunk_count}</if>" +
            "<if test='token_count != null'>, token_count = #{token_count}</if>" +
            "<if test='page_count != null'>, page_count = #{page_count}</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int update(KBDocument doc);

    @Delete("DELETE FROM xnet_mlops_dpp_kb_document WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM xnet_mlops_dpp_kb_document WHERE kb_id = #{kb_id}")
    int deleteByKbId(@Param("kb_id") Long kb_id);

    @Update("UPDATE xnet_mlops_dpp_kb_document SET " +
            "status = #{status}, process_progress = #{progress}, indexed_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateProcessStatus(@Param("id") Long id, @Param("status") String status, @Param("progress") Integer progress);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_kb_document WHERE kb_id = #{kb_id}")
    int countByKbId(@Param("kb_id") Long kb_id);

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM xnet_mlops_dpp_kb_document WHERE kb_id = #{kb_id}")
    Long sumFileSizeByKbId(@Param("kb_id") Long kb_id);
}
