package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.ApiKey;
import com.synapxnet.mlopsmepservice.mapper.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();

    @Value("${openclaw.encryption.key:XnetMLops2026Key}")
    private String encryptionKey;

    public List<ApiKey> findAll() {
        return apiKeyMapper.findAll();
    }

    public ApiKey findById(Long id) {
        return apiKeyMapper.findById(id);
    }

    public ApiKey findByUid(String uid) {
        return apiKeyMapper.findByUid(uid);
    }

    public ApiKey findByName(String name) {
        return apiKeyMapper.findByName(name);
    }

    public Map<String, Object> create(Map<String, Object> params) {
        // 名称唯一性检查
        String name = (String) params.get("name");
        if (name != null && apiKeyMapper.findByName(name) != null) {
            throw new IllegalArgumentException("密钥名称 '" + name + "' 已存在，请使用其他名称");
        }

        // 支持用户提供密钥（第三方API密钥）或自动生成（平台密钥）
        String userProvidedKey = (String) params.get("api_key");
        String plainKey = (userProvidedKey != null && !userProvidedKey.isEmpty())
                ? userProvidedKey : generateApiKey();
        String keyHash = hashKey(plainKey);
        String keyMasked = maskKey(plainKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setUid(UUID.randomUUID().toString());
        apiKey.setName((String) params.get("name"));
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyMasked(keyMasked);
        apiKey.setEncryptedKey(encryptApiKey(plainKey));
        apiKey.setProvider((String) params.get("provider"));
        apiKey.setDescription((String) params.get("description"));
        apiKey.setStatus("active");
        apiKey.setUsageLimit(params.get("usage_limit") != null ? ((Number) params.get("usage_limit")).longValue() : null);
        apiKey.setUsageCount(0L);
        apiKey.setExpiresAt(params.get("expires_at") != null ? LocalDateTime.parse((String) params.get("expires_at") + "T00:00:00") : null);
        apiKey.setCreatedAt(LocalDateTime.now());

        apiKeyMapper.insert(apiKey);

        Map<String, Object> result = new HashMap<>();
        result.put("id", apiKey.getId());
        result.put("uid", apiKey.getUid());
        result.put("name", apiKey.getName());
        result.put("key_masked", keyMasked);
        result.put("plain_key", plainKey);
        result.put("provider", apiKey.getProvider());

        return result;
    }

    public ApiKey update(ApiKey apiKey) {
        // 名称唯一性检查（排除自身）
        if (apiKey.getName() != null) {
            ApiKey existing = apiKeyMapper.findByName(apiKey.getName());
            if (existing != null && !existing.getId().equals(apiKey.getId())) {
                throw new IllegalArgumentException("密钥名称 '" + apiKey.getName() + "' 已存在，请使用其他名称");
            }
        }
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.update(apiKey);
        return apiKeyMapper.findById(apiKey.getId());
    }

    public void delete(Long id) {
        apiKeyMapper.deleteById(id);
    }

    public Map<String, Object> regenerate(Long id, String userProvidedKey) {
        String plainKey = (userProvidedKey != null && !userProvidedKey.isEmpty())
                ? userProvidedKey : generateApiKey();
        String keyHash = hashKey(plainKey);
        String keyMasked = maskKey(plainKey);

        apiKeyMapper.updateKey(id, keyHash, keyMasked, encryptApiKey(plainKey));

        return Map.of("plain_key", plainKey);
    }

    /**
     * 解密API密钥（供OpenClaw等服务使用）
     */
    public String decryptApiKeyById(Long id) {
        ApiKey apiKey = apiKeyMapper.findById(id);
        if (apiKey == null || apiKey.getEncryptedKey() == null) {
            return null;
        }
        return decryptApiKey(apiKey.getEncryptedKey());
    }

    public void updateStatus(Long id, String status) {
        apiKeyMapper.updateStatus(id, status);
    }

    // ==================== AES 加密/解密 ====================

    private String encryptApiKey(String plainKey) {
        if (plainKey == null || plainKey.isEmpty()) return null;
        try {
            String key = padKey(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密API密钥失败: {}", e.getMessage());
            return null;
        }
    }

    private String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isEmpty()) return null;
        try {
            String key = padKey(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密API密钥失败: {}", e.getMessage());
            return null;
        }
    }

    private String padKey(String key) {
        if (key.length() < 16) {
            return String.format("%-16s", key).substring(0, 16);
        } else if (key.length() < 24) {
            return String.format("%-24s", key).substring(0, 24);
        } else {
            return String.format("%-32s", key).substring(0, 32);
        }
    }

    // ==================== Key 生成/哈希/掩码 ====================

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "sk-" + base64Encoder.encodeToString(randomBytes);
    }

    private String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash key failed", e);
        }
    }

    private String maskKey(String key) {
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 7) + "****" + key.substring(key.length() - 4);
    }
}
