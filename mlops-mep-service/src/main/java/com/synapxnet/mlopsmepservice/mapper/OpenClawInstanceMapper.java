package com.synapxnet.mlopsmepservice.mapper;

import com.synapxnet.mlopsmepservice.entity.OpenClawInstance;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OpenClawInstanceMapper {

    @Select("SELECT * FROM xnet_mlops_mep_openclaw_instance ORDER BY created_at DESC")
    List<OpenClawInstance> findAll();

    @Select("SELECT * FROM xnet_mlops_mep_openclaw_instance WHERE id = #{id}")
    OpenClawInstance findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_mlops_mep_openclaw_instance WHERE uid = #{uid}")
    OpenClawInstance findByUid(@Param("uid") String uid);

    @Select("SELECT * FROM xnet_mlops_mep_openclaw_instance WHERE status = #{status}")
    List<OpenClawInstance> findByStatus(@Param("status") String status);

    @Insert("INSERT INTO xnet_mlops_mep_openclaw_instance " +
            "(uid, name, description, deploy_mode, deploy_node_id, workstation_id, gateway_host, gateway_port, gateway_token, " +
            "default_model, fallback_models, subagent_model, llm_service_id, api_key_id, api_key_ref_id, enabled_skills, skills_config, channels_config, " +
            "status, container_name, workspace_path, config_path, logs_path, " +
            "tenant_uid, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{description}, #{deployMode}, #{deployNodeId}, #{workstationId}, #{gatewayHost}, #{gatewayPort}, #{gatewayToken}, " +
            "#{defaultModel}, #{fallbackModels}, #{subagentModel}, #{llmServiceId}, #{apiKeyId}, #{apiKeyRefId}, #{enabledSkills}, #{skillsConfig}, #{channelsConfig}, " +
            "#{status}, #{containerName}, #{workspacePath}, #{configPath}, #{logsPath}, " +
            "#{tenantUid}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OpenClawInstance instance);

    @Update("UPDATE xnet_mlops_mep_openclaw_instance SET " +
            "name = #{name}, description = #{description}, deploy_mode = #{deployMode}, " +
            "deploy_node_id = #{deployNodeId}, workstation_id = #{workstationId}, gateway_host = #{gatewayHost}, gateway_port = #{gatewayPort}, " +
            "gateway_token = #{gatewayToken}, default_model = #{defaultModel}, fallback_models = #{fallbackModels}, subagent_model = #{subagentModel}, " +
            "llm_service_id = #{llmServiceId}, api_key_id = #{apiKeyId}, api_key_ref_id = #{apiKeyRefId}, enabled_skills = #{enabledSkills}, skills_config = #{skillsConfig}, " +
            "channels_config = #{channelsConfig}, workspace_path = #{workspacePath}, config_path = #{configPath}, " +
            "logs_path = #{logsPath}, updated_by = #{updatedBy}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(OpenClawInstance instance);

    @Update("UPDATE xnet_mlops_mep_openclaw_instance SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE xnet_mlops_mep_openclaw_instance SET " +
            "status = #{status}, container_name = #{containerName}, process_id = #{processId}, " +
            "last_error = #{lastError}, updated_at = NOW() WHERE id = #{id}")
    int updateRuntime(@Param("id") Long id, @Param("status") String status,
                      @Param("containerName") String containerName,
                      @Param("processId") String processId,
                      @Param("lastError") String lastError);

    @Update("UPDATE xnet_mlops_mep_openclaw_instance SET " +
            "total_conversations = #{totalConversations}, total_messages = #{totalMessages}, " +
            "last_active_at = #{lastActiveAt} WHERE id = #{id}")
    int updateStats(@Param("id") Long id, @Param("totalConversations") Long totalConversations,
                    @Param("totalMessages") Long totalMessages,
                    @Param("lastActiveAt") java.time.LocalDateTime lastActiveAt);

    @Delete("DELETE FROM xnet_mlops_mep_openclaw_instance WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
