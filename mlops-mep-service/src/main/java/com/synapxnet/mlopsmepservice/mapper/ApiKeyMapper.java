package com.synapxnet.mlopsmepservice.mapper;

import com.synapxnet.mlopsmepservice.entity.ApiKey;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApiKeyMapper {

    @Select("SELECT id, uid, name, key_hash, key_masked, encrypted_key, provider, description, status, " +
            "usage_limit, usage_count, expires_at, created_by, created_at, updated_at " +
            "FROM xnet_mlops_mep_api_key ORDER BY created_at DESC")
    @Results(id = "apiKeyResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "uid", column = "uid"),
        @Result(property = "name", column = "name"),
        @Result(property = "keyHash", column = "key_hash"),
        @Result(property = "keyMasked", column = "key_masked"),
        @Result(property = "encryptedKey", column = "encrypted_key"),
        @Result(property = "provider", column = "provider"),
        @Result(property = "description", column = "description"),
        @Result(property = "status", column = "status"),
        @Result(property = "usageLimit", column = "usage_limit"),
        @Result(property = "usageCount", column = "usage_count"),
        @Result(property = "expiresAt", column = "expires_at"),
        @Result(property = "createdBy", column = "created_by"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<ApiKey> findAll();

    @Select("SELECT id, uid, name, key_hash, key_masked, encrypted_key, provider, description, status, " +
            "usage_limit, usage_count, expires_at, created_by, created_at, updated_at " +
            "FROM xnet_mlops_mep_api_key WHERE id = #{id}")
    @ResultMap("apiKeyResultMap")
    ApiKey findById(Long id);

    @Select("SELECT id, uid, name, key_hash, key_masked, encrypted_key, provider, description, status, " +
            "usage_limit, usage_count, expires_at, created_by, created_at, updated_at " +
            "FROM xnet_mlops_mep_api_key WHERE uid = #{uid}")
    @ResultMap("apiKeyResultMap")
    ApiKey findByUid(String uid);

    @Select("SELECT id, uid, name, key_hash, key_masked, encrypted_key, provider, description, status, " +
            "usage_limit, usage_count, expires_at, created_by, created_at, updated_at " +
            "FROM xnet_mlops_mep_api_key WHERE name = #{name}")
    @ResultMap("apiKeyResultMap")
    ApiKey findByName(String name);

    @Insert("INSERT INTO xnet_mlops_mep_api_key (uid, name, key_hash, key_masked, encrypted_key, provider, description, status, usage_limit, usage_count, expires_at, created_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{keyHash}, #{keyMasked}, #{encryptedKey}, #{provider}, #{description}, #{status}, #{usageLimit}, #{usageCount}, #{expiresAt}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApiKey apiKey);

    @Update("UPDATE xnet_mlops_mep_api_key SET name = #{name}, description = #{description}, status = #{status}, " +
            "usage_limit = #{usageLimit}, expires_at = #{expiresAt}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(ApiKey apiKey);

    @Update("UPDATE xnet_mlops_mep_api_key SET key_hash = #{keyHash}, key_masked = #{keyMasked}, encrypted_key = #{encryptedKey}, updated_at = NOW() WHERE id = #{id}")
    int updateKey(@Param("id") Long id, @Param("keyHash") String keyHash, @Param("keyMasked") String keyMasked, @Param("encryptedKey") String encryptedKey);

    @Update("UPDATE xnet_mlops_mep_api_key SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE xnet_mlops_mep_api_key SET usage_count = usage_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementUsageCount(Long id);

    @Delete("DELETE FROM xnet_mlops_mep_api_key WHERE id = #{id}")
    int deleteById(Long id);
}
