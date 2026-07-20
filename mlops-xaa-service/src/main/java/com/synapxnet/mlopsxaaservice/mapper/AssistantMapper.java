package com.synapxnet.mlopsxaaservice.mapper;

import com.synapxnet.mlopsxaaservice.entity.Assistant;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssistantMapper {

    @Select("SELECT * FROM xnet_mlops_xaa_assistant ORDER BY created_at DESC")
    List<Assistant> findAll();

    @Select("SELECT * FROM xnet_mlops_xaa_assistant WHERE id = #{id}")
    Assistant findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant WHERE uid = #{uid}")
    Assistant findByUid(@Param("uid") String uid);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant WHERE status = #{status} ORDER BY created_at DESC")
    List<Assistant> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM xnet_mlops_xaa_assistant WHERE is_default = TRUE LIMIT 1")
    Assistant findDefault();

    @Insert("INSERT INTO xnet_mlops_xaa_assistant " +
            "(uid, name, description, avatar, openclaw_instance_id, gateway_url, gateway_token, " +
            "llm_service_id, default_model, system_prompt, temperature, max_tokens, " +
            "knowledge_base_ids, rag_enabled, rag_top_k, skill_ids, tools_enabled, " +
            "ui_config, welcome_message, placeholder, status, is_default, " +
            "tenant_uid, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{description}, #{avatar}, #{openclawInstanceId}, #{gatewayUrl}, #{gatewayToken}, " +
            "#{llmServiceId}, #{defaultModel}, #{systemPrompt}, #{temperature}, #{maxTokens}, " +
            "#{knowledgeBaseIds}, #{ragEnabled}, #{ragTopK}, #{skillIds}, #{toolsEnabled}, " +
            "#{uiConfig}, #{welcomeMessage}, #{placeholder}, #{status}, #{isDefault}, " +
            "#{tenantUid}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Assistant assistant);

    @Update("UPDATE xnet_mlops_xaa_assistant SET " +
            "name = #{name}, description = #{description}, avatar = #{avatar}, " +
            "openclaw_instance_id = #{openclawInstanceId}, gateway_url = #{gatewayUrl}, gateway_token = #{gatewayToken}, " +
            "llm_service_id = #{llmServiceId}, default_model = #{defaultModel}, " +
            "system_prompt = #{systemPrompt}, temperature = #{temperature}, max_tokens = #{maxTokens}, " +
            "knowledge_base_ids = #{knowledgeBaseIds}, rag_enabled = #{ragEnabled}, rag_top_k = #{ragTopK}, " +
            "skill_ids = #{skillIds}, tools_enabled = #{toolsEnabled}, " +
            "ui_config = #{uiConfig}, welcome_message = #{welcomeMessage}, placeholder = #{placeholder}, " +
            "status = #{status}, is_default = #{isDefault}, " +
            "updated_by = #{updatedBy}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(Assistant assistant);

    @Update("UPDATE xnet_mlops_xaa_assistant SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE xnet_mlops_xaa_assistant SET is_default = FALSE WHERE is_default = TRUE")
    int clearDefault();

    @Update("UPDATE xnet_mlops_xaa_assistant SET is_default = TRUE, updated_at = NOW() WHERE id = #{id}")
    int setDefault(@Param("id") Long id);

    @Update("UPDATE xnet_mlops_xaa_assistant SET " +
            "total_conversations = total_conversations + 1, " +
            "last_used_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int incrementConversations(@Param("id") Long id);

    @Update("UPDATE xnet_mlops_xaa_assistant SET " +
            "total_messages = total_messages + 1, " +
            "last_used_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int incrementMessages(@Param("id") Long id);

    @Delete("DELETE FROM xnet_mlops_xaa_assistant WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
