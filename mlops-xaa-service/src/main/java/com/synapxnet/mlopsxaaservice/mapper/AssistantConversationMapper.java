package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.AssistantConversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssistantConversationMapper {

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_conversation WHERE assistant_id = #{assistantId} " +
            "AND status != 'deleted' ORDER BY updated_at DESC")
    List<AssistantConversation> findByAssistantId(@Param("assistantId") Long assistantId);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_conversation WHERE id = #{id}")
    AssistantConversation findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_conversation WHERE uid = #{uid}")
    AssistantConversation findByUid(@Param("uid") String uid);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant_conversation WHERE user_id = #{userId} " +
            "AND status != 'deleted' ORDER BY updated_at DESC")
    List<AssistantConversation> findByUserId(@Param("userId") String userId);

    @Insert("INSERT INTO xnet_mlops_xaa_assistant_conversation " +
            "(uid, assistant_id, title, summary, user_id, user_name, status, created_at) " +
            "VALUES (#{uid}, #{assistantId}, #{title}, #{summary}, #{userId}, #{userName}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssistantConversation conversation);

    @Update("UPDATE xnet_mlops_xaa_assistant_conversation SET " +
            "title = #{title}, summary = #{summary}, updated_at = NOW() WHERE id = #{id}")
    int update(AssistantConversation conversation);

    @Update("UPDATE xnet_mlops_xaa_assistant_conversation SET " +
            "message_count = message_count + 1, " +
            "token_count = token_count + #{tokenCount}, " +
            "last_message_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int incrementMessage(@Param("id") Long id, @Param("tokenCount") int tokenCount);

    @Update("UPDATE xnet_mlops_xaa_assistant_conversation SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_mlops_xaa_assistant_conversation WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
