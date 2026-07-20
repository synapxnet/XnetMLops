package com.synapxnet.mlopsmepservice.mapper;

import com.synapxnet.mlopsmepservice.entity.LLMService;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LLMServiceMapper {

    @Select("SELECT * FROM xnet_mlops_mep_llm_service ORDER BY created_at DESC")
    List<LLMService> findAll();

    @Select("SELECT * FROM xnet_mlops_mep_llm_service WHERE id = #{id}")
    LLMService findById(Long id);

    @Select("SELECT * FROM xnet_mlops_mep_llm_service WHERE uid = #{uid}")
    LLMService findByUid(String uid);

    @Insert("INSERT INTO xnet_mlops_mep_llm_service (uid, name, type, description, endpoint, model_name, api_key, status, config, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{type}, #{description}, #{endpoint}, #{modelName}, #{apiKey}, #{status}, #{config}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LLMService service);

    @Update("UPDATE xnet_mlops_mep_llm_service SET name = #{name}, description = #{description}, endpoint = #{endpoint}, " +
            "model_name = #{modelName}, api_key = #{apiKey}, config = #{config}, updated_by = #{updatedBy}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(LLMService service);

    @Update("UPDATE xnet_mlops_mep_llm_service SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_mlops_mep_llm_service WHERE id = #{id}")
    int deleteById(Long id);
}
