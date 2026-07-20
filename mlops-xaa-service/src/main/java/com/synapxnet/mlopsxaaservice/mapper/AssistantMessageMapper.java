package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.AssistantMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssistantMessageMapper {

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_message WHERE conversation_id = #{conversationId} " +
            "ORDER BY created_at ASC")
    List<AssistantMessage> findByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_message WHERE conversation_id = #{conversationId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<AssistantMessage> findRecentByConversationId(@Param("conversationId") Long conversationId,
                                                       @Param("limit") int limit);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_message WHERE id = #{id}")
    AssistantMessage findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_message WHERE uid = #{uid}")
    AssistantMessage findByUid(@Param("uid") String uid);

    @Insert("INSERT INTO xnet_mlops_xaa_assistant_message " +
            "(uid, conversation_id, role, content, token_count, model_used, " +
            "rag_sources, tool_calls, metadata, created_at) " +
            "VALUES (#{uid}, #{conversationId}, #{role}, #{content}, #{tokenCount}, #{modelUsed}, " +
            "#{ragSources}, #{toolCalls}, #{metadata}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssistantMessage message);

    @Delete("DELETE FROM xnet_mlops_xaa_assistant_message WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM xnet_mlops_xaa_assistant_message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT COUNT(*) FROM xnet_mlops_xaa_assistant_message WHERE conversation_id = #{conversationId}")
    int countByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT SUM(token_count) FROM xnet_mlops_xaa_assistant_message WHERE conversation_id = #{conversationId}")
    Long sumTokensByConversationId(@Param("conversationId") Long conversationId);
}
